package io.github.stanikoc.botforge.action;

import java.util.List;

/**
 * Something a user did that a bot can react to.
 */
public sealed interface Action {
    long chatId();

    User user();

    long messageId();

    default long userId() {
        return user().id();
    }

    /**
     * A {@code /name} message.
     *
     * @param name     the command name in lowercase, without the slash or {@code @BotName}
     * @param argument everything after the command, trimmed; empty if nothing followed it
     */
    record Command(long chatId, User user, long messageId, String name, String argument) implements Action {
        public List<String> args() {
            return argument.isEmpty() ? List.of() : List.of(argument.split("\\s+"));
        }
    }

    /**
     * A press of an inline button.
     *
     * @param callbackId the id Telegram expects in the callback answer
     * @param namespace  the part of the callback data before the first {@code :}
     * @param payload    the part after it; empty if there was none
     */
    record Button(long chatId, User user, long messageId, String callbackId, String namespace, String payload)
            implements Action {}

    /**
     * A text message that isn't a command.
     */
    record Text(long chatId, User user, long messageId, String text) implements Action {}
}
