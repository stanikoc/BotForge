package io.github.stanikoc.botforge.internal;

import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.Map;

public interface TelegramApi {
    Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    @Nullable Object call(String method, Map<String, ?> params, Duration timeout);

    default @Nullable Object call(String method, Map<String, ?> params) {
        return call(method, params, DEFAULT_TIMEOUT);
    }
}
