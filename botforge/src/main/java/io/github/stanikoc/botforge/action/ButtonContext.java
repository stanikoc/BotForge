package io.github.stanikoc.botforge.action;

import io.github.stanikoc.botforge.keyboard.InlineKeyboard;

/**
 * Context for a button press. Telegram shows a loading indicator until the callback is answered;
 * if the handler doesn't call one of the {@code answer} methods, BotForge answers silently after it
 * returns. A callback can be answered only once.
 */
public interface ButtonContext extends Context<Action.Button> {
    default String payload() {
        return action().payload();
    }

    /**
     * Answers the callback without showing anything.
     */
    void answer();

    /**
     * Answers the callback with a short notification at the top of the chat.
     */
    void answer(String text);

    /**
     * Answers the callback with a dialog the user has to dismiss.
     */
    void alert(String text);

    /**
     * Replaces the text of the message holding the button. This removes its keyboard.
     */
    void edit(String text);

    /**
     * Replaces the text and keyboard of the message holding the button.
     */
    void edit(String text, InlineKeyboard keyboard);
}
