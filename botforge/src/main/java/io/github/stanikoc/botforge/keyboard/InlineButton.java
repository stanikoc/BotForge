package io.github.stanikoc.botforge.keyboard;

import io.github.stanikoc.botforge.internal.Validation;

import java.util.Objects;

/**
 * A button attached to a message.
 */
public sealed interface InlineButton {
    String text();

    /**
     * A button that sends {@code namespace:payload} back to the bot when pressed. The handler
     * registered for {@code namespace} receives {@code payload}.
     *
     * @throws IllegalArgumentException if the namespace contains {@code :} or the callback data
     *                                  exceeds Telegram's 64-byte limit
     */
    static InlineButton callback(String text, String namespace, String payload) {
        Validation.namespace(namespace);
        return new Callback(text, payload.isEmpty() ? namespace : namespace + ':' + payload);
    }

    /**
     * A callback button with an empty payload.
     */
    static InlineButton callback(String text, String namespace) {
        return callback(text, namespace, "");
    }

    /**
     * A button that opens a link.
     */
    static InlineButton url(String text, String url) {
        return new Url(text, url);
    }

    /**
     * A callback button with an empty payload.
     */
    record Callback(String text, String data) implements InlineButton {
        public Callback {
            Objects.requireNonNull(text, "text");
            Validation.callbackData(data);
        }
    }

    /**
     * A button that opens a link.
     */
    record Url(String text, String url) implements InlineButton {
        public Url {
            Objects.requireNonNull(text, "text");
            Objects.requireNonNull(url, "url");
        }
    }
}
