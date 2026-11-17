package io.github.stanikoc.botforge;

/**
 * A configured Telegram bot. Create one with {@link #builder(String)}; all handlers are
 * registered on the builder and fixed once {@link BotBuilder#build()} is called.
 */
public interface Bot {
    /**
     * Starts building a bot.
     *
     * @param token the token issued by BotFather
     */
    static BotBuilder builder(String token) {
        return new BotBuilder(token);
    }

    /**
     * Connects to Telegram and processes updates until {@link #stop()} is called. Blocks the
     * calling thread. Each update is handled on its own virtual thread.
     *
     * @throws TelegramException if the bot cannot start, for example because the token is invalid
     * @throws IllegalStateException if the bot is already running
     */
    void run();

    /**
     * Stops polling, waits for running handlers to finish and makes {@link #run()} return.
     * Safe to call from any thread, including a shutdown hook.
     */
    void stop();
}
