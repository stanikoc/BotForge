package io.github.stanikoc.botforge.internal.json;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

public record JsonObject(Map<String, @Nullable Object> map) {
    public static JsonObject of(@Nullable Object value) {
        if (value instanceof Map<?, ?> m) {
            return wrap(m);
        }

        throw new JsonException("Expected a JSON object");
    }

    @SuppressWarnings("unchecked")
    private static JsonObject wrap(Map<?, ?> map) {
        return new JsonObject((Map<String, @Nullable Object>) map);
    }

    public @Nullable JsonObject object(String key) {
        return map.get(key) instanceof Map<?, ?> m ? wrap(m) : null;
    }

    public JsonObject requireObject(String key) {
        JsonObject value = object(key);
        if (value == null) {
            throw missing(key, "object");
        }

        return value;
    }

    public @Nullable String string(String key) {
        return map.get(key) instanceof String s ? s : null;
    }

    public String requireString(String key) {
        String value = string(key);
        if (value == null) {
            throw missing(key, "string");
        }

        return value;
    }

    public List<?> array(String key) {
        return map.get(key) instanceof List<?> list ? list : List.of();
    }

    public boolean bool(String key) {
        return map.get(key) instanceof Boolean b && b;
    }

    public long longOr(String key, long fallback) {
        return map.get(key) instanceof Number n ? n.longValue() : fallback;
    }

    public long requireLong(String key) {
        if (map.get(key) instanceof Number n) {
            return n.longValue();
        }

        throw missing(key, "number");
    }

    private static JsonException missing(String key, String type) {
        return new JsonException("Expected " + type + " field '" + key + "'");
    }
}
