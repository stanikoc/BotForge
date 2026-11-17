package io.github.stanikoc.botforge.action;

import io.github.stanikoc.botforge.keyboard.InlineKeyboard;

/**
 * What a handler receives: the action, plus ways to respond to it. Every method that talks to
 * Telegram throws {@link io.github.stanikoc.botforge.TelegramException} on failure.
 */
public interface Context<A extends Action> {
    A action();

    default long chatId() {
        return action().chatId();
    }

    default User user() {
        return action().user();
    }

    long reply(String text);

    void reply(String text, InlineKeyboard keyboard);

    long send(long chatId, String text);

    long send(long chatId, String text, InlineKeyboard keyboard);
}
