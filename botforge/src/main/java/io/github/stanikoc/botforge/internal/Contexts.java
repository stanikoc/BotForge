package io.github.stanikoc.botforge.internal;

import io.github.stanikoc.botforge.action.Action;
import io.github.stanikoc.botforge.action.ButtonContext;
import io.github.stanikoc.botforge.action.CommandContext;
import io.github.stanikoc.botforge.action.Context;
import io.github.stanikoc.botforge.action.TextContext;
import io.github.stanikoc.botforge.internal.json.JsonObject;
import io.github.stanikoc.botforge.keyboard.InlineButton;
import io.github.stanikoc.botforge.keyboard.InlineKeyboard;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

final class Contexts {
    private Contexts() {}

    abstract static class Base<A extends Action> implements Context<A> {
        final TelegramApi api;
        private final A action;

        Base(TelegramApi api, A action) {
            this.api = api;
            this.action = action;
        }

        @Override
        public A action() {
            return action;
        }

        @Override
        public long reply(String text) {
            return sendMessage(action.chatId(), text, null);
        }

        @Override
        public void reply(String text, InlineKeyboard keyboard) {
            sendMessage(action.chatId(), text, keyboard);
        }

        @Override
        public long send(long chatId, String text) {
            return sendMessage(chatId, text, null);
        }

        @Override
        public long send(long chatId, String text, InlineKeyboard keyboard) {
            return sendMessage(chatId, text, keyboard);
        }

        private long sendMessage(long chatId, String text, @Nullable InlineKeyboard keyboard) {
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("chat_id", chatId);
            params.put("text", text);
            if (keyboard != null) {
                params.put("reply_markup", markup(keyboard));
            }

            return JsonObject.of(api.call("sendMessage", params)).requireLong("message_id");
        }
    }

    static final class Command extends Base<Action.Command> implements CommandContext {
        Command(TelegramApi api, Action.Command action) {
            super(api, action);
        }
    }

    static final class Text extends Base<Action.Text> implements TextContext {
        Text(TelegramApi api, Action.Text action) {
            super(api, action);
        }
    }

    static final class Button extends Base<Action.Button> implements ButtonContext {
        private volatile boolean answered;

        Button(TelegramApi api, Action.Button action) {
            super(api, action);
        }

        @Override
        public void answer() {
            answerCallback(null, false);
        }

        @Override
        public void answer(String text) {
            answerCallback(text, false);
        }

        @Override
        public void alert(String text) {
            answerCallback(text, true);
        }

        @Override
        public void edit(String text) {
            editMessage(text, null);
        }

        @Override
        public void edit(String text, InlineKeyboard keyboard) {
            editMessage(text, keyboard);
        }

        boolean answered() {
            return answered;
        }

        private synchronized void answerCallback(@Nullable String text, boolean alert) {
            if (answered) {
                throw new IllegalStateException("This callback was already answered");
            }

            answered = true;
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("callback_query_id", action().callbackId());
            if (text != null) {
                params.put("text", text);
                params.put("show_alert", alert);
            }

            api.call("answerCallbackQuery", params);
        }

        private void editMessage(String text, @Nullable InlineKeyboard keyboard) {
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("chat_id", action().chatId());
            params.put("message_id", action().messageId());
            params.put("text", text);
            if (keyboard != null) {
                params.put("reply_markup", markup(keyboard));
            }

            api.call("editMessageText", params);
        }
    }

    static Map<String, Object> markup(InlineKeyboard keyboard) {
        Object[][] rows = new Object[keyboard.rowCount()][];
        for (int row = 0; row < rows.length; row++) {
            Object[] buttons = new Object[keyboard.rowSize(row)];
            for (int column = 0; column < buttons.length; column++) {
                buttons[column] = button(keyboard.button(row, column));
            }

            rows[row] = buttons;
        }

        return Map.of("inline_keyboard", rows);
    }

    private static Map<String, String> button(InlineButton button) {
        return switch (button) {
            case InlineButton.Callback c -> Map.of("text", c.text(), "callback_data", c.data());
            case InlineButton.Url u -> Map.of("text", u.text(), "url", u.url());
        };
    }
}
