package io.github.stanikoc.botforge.internal;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TelegramApiTest {
    @Test
    void defaultCallDelegatesWithDefaultTimeout() {
        var captured = new Object() {
            String method;
            Map<String, ?> params;
            Duration timeout;
        };

        TelegramApi api = (method, params, timeout) -> {
            captured.method = method;
            captured.params = params;
            captured.timeout = timeout;
            return "ok";
        };

        Map<String, Object> params = Map.of("chat_id", 1L);
        assertEquals("ok", api.call("getMe", params));
        assertEquals("getMe", captured.method);
        assertEquals(params, captured.params);
        assertEquals(TelegramApi.DEFAULT_TIMEOUT, captured.timeout);
    }
}
