package io.github.stanikoc.botforge.internal;

import io.github.stanikoc.botforge.action.Action;
import io.github.stanikoc.botforge.action.User;
import io.github.stanikoc.botforge.internal.json.JsonObject;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

public final class UpdateParser {
    private UpdateParser() {}

    public static @Nullable Action parse(JsonObject update, String botUsername) {
        JsonObject message = update.object("message");
        if (message != null) {
            return parseMessage(message, botUsername);
        }

        JsonObject callback = update.object("callback_query");
        if (callback != null) {
            return parseCallback(callback);
        }

        return null;
    }

    private static @Nullable Action parseMessage(JsonObject message, String botUsername) {
        String text = message.string("text");
        JsonObject from = message.object("from");
        if (text == null || from == null) {
            return null;
        }

        long chatId = message.requireObject("chat").requireLong("id");
        long messageId = message.requireLong("message_id");
        User user = parseUser(from);

        int commandLength = Math.min(leadingCommandLength(message), text.length());
        if (commandLength <= 1) {
            return new Action.Text(chatId, user, messageId, text);
        }

        String command = text.substring(1, commandLength);
        int at = command.indexOf('@');
        if (at >= 0) {
            if (!command.substring(at + 1).equalsIgnoreCase(botUsername)) {
                return null;
            }

            command = command.substring(0, at);
        }

        String argument = text.substring(commandLength).strip();
        return new Action.Command(chatId, user, messageId, command.toLowerCase(Locale.ROOT), argument);
    }

    private static int leadingCommandLength(JsonObject message) {
        for (Object raw : message.array("entities")) {
            JsonObject entity = JsonObject.of(raw);
            if ("bot_command".equals(entity.string("type")) && entity.longOr("offset", -1) == 0) {
                return (int) entity.requireLong("length");
            }
        }

        return 0;
    }

    private static @Nullable Action parseCallback(JsonObject callback) {
        String data = callback.string("data");
        JsonObject message = callback.object("message");
        if (data == null || message == null) {
            return null;
        }

        int colon = data.indexOf(':');
        String namespace = colon < 0 ? data : data.substring(0, colon);
        String payload = colon < 0 ? "" : data.substring(colon + 1);
        return new Action.Button(
                message.requireObject("chat").requireLong("id"),
                parseUser(callback.requireObject("from")),
                message.requireLong("message_id"),
                callback.requireString("id"),
                namespace,
                payload);
    }

    private static User parseUser(JsonObject from) {
        return new User(
                from.requireLong("id"),
                from.requireString("first_name"),
                from.string("last_name"),
                from.string("username"),
                from.bool("is_bot"));
    }
}
