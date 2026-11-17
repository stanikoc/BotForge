package io.github.stanikoc.botforge.action;

/**
 * Handles a button press. Exceptions are logged and don't stop the bot.
 */
@FunctionalInterface
public interface ButtonHandler {
    void handle(ButtonContext ctx) throws Exception;
}
