/**
 * BotForge: a small Telegram bot framework.
 * <p>
 * Only the API packages are exported. Registries, HTTP and JSON handling live in
 * {@code io.github.stanikoc.botforge.internal} and are not accessible to bots.
 */
module io.github.stanikoc.botforge {
    requires transitive org.jspecify;
    requires java.net.http;

    exports io.github.stanikoc.botforge;
    exports io.github.stanikoc.botforge.action;
    exports io.github.stanikoc.botforge.keyboard;
}
