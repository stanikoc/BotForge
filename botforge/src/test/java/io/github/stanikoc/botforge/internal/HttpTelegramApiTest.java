package io.github.stanikoc.botforge.internal;

import io.github.stanikoc.botforge.TelegramException;
import io.github.stanikoc.botforge.internal.json.Json;
import io.github.stanikoc.botforge.internal.json.JsonException;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpTelegramApiTest {
    @FunctionalInterface
    private interface Responder {
        HttpResponse<String> respond(HttpRequest request) throws IOException, InterruptedException;
    }

    private static final class StubHttpClient extends HttpClient {
        final List<HttpRequest> requests = new ArrayList<>();
        private final Queue<Responder> responses = new ArrayDeque<>();

        void enqueue(int statusCode, String body) {
            responses.add(req -> new StubHttpResponse(req, statusCode, body));
        }

        void enqueue(Responder responder) {
            responses.add(responder);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> handler)
                throws IOException, InterruptedException {
            requests.add(request);
            return (HttpResponse<T>) responses.remove().respond(request);
        }

        @Override public Optional<CookieHandler> cookieHandler() { return Optional.empty(); }
        @Override public Optional<Duration> connectTimeout() { return Optional.empty(); }
        @Override public Redirect followRedirects() { return Redirect.NEVER; }
        @Override public Optional<ProxySelector> proxy() { return Optional.empty(); }
        @Override public SSLContext sslContext() { throw new UnsupportedOperationException(); }
        @Override public SSLParameters sslParameters() { throw new UnsupportedOperationException(); }
        @Override public Optional<Authenticator> authenticator() { return Optional.empty(); }
        @Override public Version version() { return Version.HTTP_2; }
        @Override public Optional<Executor> executor() { return Optional.empty(); }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(
                HttpRequest request, HttpResponse.BodyHandler<T> handler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(
                HttpRequest request, HttpResponse.BodyHandler<T> handler, HttpResponse.PushPromiseHandler<T> push) {
            throw new UnsupportedOperationException();
        }
    }

    private record StubHttpResponse(HttpRequest request, int statusCode, String body) implements HttpResponse<String> {
        @Override public Optional<HttpResponse<String>> previousResponse() { return Optional.empty(); }
        @Override public HttpHeaders headers() { return HttpHeaders.of(Map.of(), (k, v) -> true); }
        @Override public Optional<SSLSession> sslSession() { return Optional.empty(); }
        @Override public URI uri() { return request.uri(); }
        @Override public HttpClient.Version version() { return HttpClient.Version.HTTP_2; }
    }

    private final StubHttpClient client = new StubHttpClient();
    private final HttpTelegramApi api = new HttpTelegramApi("https://example.test/bot123/", client);

    @Test
    void constructsWithToken() {
        new HttpTelegramApi("123:abc");
    }

    @Test
    void sendsJsonPostAndReturnsResult() {
        client.enqueue(200, "{\"ok\": true, \"result\": {\"username\": \"ForgeBot\"}}");

        Object result = api.call("getMe", Map.of("extra", 1), Duration.ofSeconds(15));

        assertEquals(Map.of("username", "ForgeBot"), result);
        HttpRequest req = client.requests.getFirst();
        assertEquals("POST", req.method());
        assertEquals(URI.create("https://example.test/bot123/getMe"), req.uri());
        assertEquals(Optional.of(Duration.ofSeconds(15)), req.timeout());
        assertEquals(Optional.of("application/json"), req.headers().firstValue("Content-Type"));
        assertEquals(Map.of("extra", 1L), Json.parse(readBody(req)));
    }

    @Test
    void returnsNullWhenResultIsNullOrMissing() {
        client.enqueue(200, "{\"ok\": true}");
        assertNull(api.call("deleteWebhook", Map.of()));
    }

    @Test
    void throwsTelegramExceptionOnApiError() {
        client.enqueue(400, "{\"ok\": false, \"error_code\": 400, \"description\": \"Bad Request\"}");

        TelegramException ex = assertThrows(
                TelegramException.class, () -> api.call("sendMessage", Map.of()));
        assertEquals(400, ex.errorCode());
        assertEquals("sendMessage: Bad Request", ex.getMessage());
    }

    @Test
    void fallsBackToHttpStatusAndGenericErrorWhenFieldsMissing() {
        client.enqueue(502, "{\"ok\": false}");

        TelegramException ex = assertThrows(
                TelegramException.class, () -> api.call("getUpdates", Map.of()));
        assertEquals(502, ex.errorCode());
        assertEquals("getUpdates: error 502", ex.getMessage());
    }

    @Test
    void throwsTelegramExceptionOnInvalidJsonResponse() {
        client.enqueue(500, "not json");

        TelegramException ex = assertThrows(
                TelegramException.class, () -> api.call("getMe", Map.of()));
        assertEquals(500, ex.errorCode());
        assertInstanceOf(JsonException.class, ex.getCause());
    }

    @Test
    void wrapsIOExceptionAsTelegramException() {
        client.enqueue(req -> { throw new IOException("connection reset"); });

        TelegramException ex = assertThrows(
                TelegramException.class, () -> api.call("getMe", Map.of()));
        assertEquals(0, ex.errorCode());
        assertInstanceOf(IOException.class, ex.getCause());
    }

    @Test
    void restoresInterruptFlagWhenSendIsInterrupted() {
        client.enqueue(req -> { throw new InterruptedException("stop"); });

        try {
            TelegramException ex = assertThrows(
                    TelegramException.class, () -> api.call("getMe", Map.of()));
            assertEquals(0, ex.errorCode());
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void retriesOnRateLimitAndSucceeds() {
        client.enqueue(429, "{\"ok\": false, \"error_code\": 429, \"description\": \"Too Many Requests\", \"parameters\": {\"retry_after\": 0}}");
        client.enqueue(200, "{\"ok\": true, \"result\": true}");

        assertEquals(Boolean.TRUE, api.call("sendMessage", Map.of()));
        assertEquals(2, client.requests.size());
    }

    @Test
    void stopsRetryingAfterMaxRateLimitAttempts() {
        String rateLimited = "{\"ok\": false, \"error_code\": 429, \"description\": \"Too Many Requests\", \"parameters\": {\"retry_after\": 0}}";
        for (int i = 0; i < 4; i++) {
            client.enqueue(429, rateLimited);
        }

        TelegramException ex = assertThrows(
                TelegramException.class, () -> api.call("sendMessage", Map.of()));
        assertEquals(429, ex.errorCode());
        assertEquals(4, client.requests.size());
    }

    @Test
    void doesNotRetry429WithoutParameters() {
        client.enqueue(429, "{\"ok\": false, \"error_code\": 429, \"description\": \"Too Many Requests\"}");

        TelegramException ex = assertThrows(
                TelegramException.class, () -> api.call("sendMessage", Map.of()));
        assertEquals(429, ex.errorCode());
        assertEquals(1, client.requests.size());
    }

    @Test
    void restoresInterruptFlagWhenRateLimitSleepIsInterrupted() {
        client.enqueue(req -> {
            Thread.currentThread().interrupt();
            return new StubHttpResponse(
                    req,
                    429,
                    "{\"ok\": false, \"error_code\": 429, \"parameters\": {\"retry_after\": 1}}");
        });

        try {
            TelegramException ex = assertThrows(
                    TelegramException.class, () -> api.call("sendMessage", Map.of()));
            assertEquals(0, ex.errorCode());
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    private static String readBody(HttpRequest request) {
        var out = new ByteArrayOutputStream();
        request.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(ByteBuffer item) {
                byte[] bytes = new byte[item.remaining()];
                item.get(bytes);
                out.writeBytes(bytes);
            }

            @Override
            public void onError(Throwable throwable) {
                throw new AssertionError(throwable);
            }

            @Override
            public void onComplete() {}
        });
        return out.toString(StandardCharsets.UTF_8);
    }
}
