package io.github.stanikoc.botforge.internal;

import io.github.stanikoc.botforge.TelegramException;
import io.github.stanikoc.botforge.internal.json.Json;
import io.github.stanikoc.botforge.internal.json.JsonException;
import io.github.stanikoc.botforge.internal.json.JsonObject;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

public final class HttpTelegramApi implements TelegramApi {
    private static final int MAX_RATE_LIMIT_RETRIES = 3;

    private final HttpClient client;
    private final String baseUrl;

    public HttpTelegramApi(String token) {
        this("https://api.telegram.org/bot" + token + "/",
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
    }

    HttpTelegramApi(String baseUrl, HttpClient client) {
        this.baseUrl = baseUrl;
        this.client = client;
    }

    @Override
    public @Nullable Object call(String method, Map<String, ?> params, Duration timeout) {
        String body = Json.write(params);
        for (int attempt = 0; ; attempt++) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + method))
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response;
            try {
                response = client.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (IOException e) {
                throw new TelegramException(0, method + " failed: " + e, e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new TelegramException(0, method + " interrupted", e);
            }

            JsonObject json;
            try {
                json = Json.parseObject(response.body());
            } catch (JsonException e) {
                throw new TelegramException(response.statusCode(), method + " returned an invalid response", e);
            }

            if (json.bool("ok")) {
                return json.map().get("result");
            }

            int code = (int) json.longOr("error_code", response.statusCode());
            String description = json.string("description");
            JsonObject parameters = json.object("parameters");
            if (code == 429 && parameters != null && attempt < MAX_RATE_LIMIT_RETRIES) {
                sleep(method, parameters.longOr("retry_after", 1));
                continue;
            }

            throw new TelegramException(code, method + ": " + (description != null ? description : "error " + code));
        }
    }

    private static void sleep(String method, long seconds) {
        try {
            Thread.sleep(Duration.ofSeconds(seconds));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TelegramException(0, method + " interrupted", e);
        }
    }
}
