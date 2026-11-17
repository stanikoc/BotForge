package io.github.stanikoc.botforge.action;

/**
 * Handles a command. Exceptions are logged and don't stop the bot.
 */
@FunctionalInterface
public interface CommandHandler {
    void handle(CommandContext ctx) throws Exception;
}
