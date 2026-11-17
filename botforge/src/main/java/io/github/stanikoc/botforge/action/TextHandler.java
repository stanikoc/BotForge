package io.github.stanikoc.botforge.action;

/**
 * Handles a plain text message. Exceptions are logged and don't stop the bot.
 */
@FunctionalInterface
public interface TextHandler {
    void handle(TextContext ctx) throws Exception;
}
