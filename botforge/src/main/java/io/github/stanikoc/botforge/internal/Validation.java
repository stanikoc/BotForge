package io.github.stanikoc.botforge.internal;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public final class Validation {
    private static final Pattern COMMAND_NAME = Pattern.compile("[a-z0-9_]{1,32}");
    private static final int MAX_CALLBACK_BYTES = 64;

    private Validation() {}

    public static String commandName(String name) {
        if (!COMMAND_NAME.matcher(name).matches()) {
            throw new IllegalArgumentException(
                    "Command name must be 1-32 characters of a-z, 0-9 and _ (without '/'): " + name);
        }

        return name;
    }

    public static String commandDescription(String description) {
        if (description.isBlank() || description.length() > 256) {
            throw new IllegalArgumentException("Command description must be 1-256 characters");
        }

        return description;
    }

    public static String namespace(String namespace) {
        if (namespace.isEmpty() || namespace.indexOf(':') >= 0) {
            throw new IllegalArgumentException("Button namespace must be non-empty and contain no ':': " + namespace);
        }

        callbackData(namespace);
        return namespace;
    }

    public static void callbackData(String data) {
        int bytes = data.getBytes(StandardCharsets.UTF_8).length;
        if (bytes == 0 || bytes > MAX_CALLBACK_BYTES) {
            throw new IllegalArgumentException(
                    "Callback data must be 1-" + MAX_CALLBACK_BYTES + " bytes in UTF-8, got " + bytes + ": " + data);
        }

    }
}
