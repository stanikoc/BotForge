package io.github.stanikoc.botforge.example;

import io.github.stanikoc.botforge.Bot;
import io.github.stanikoc.botforge.keyboard.InlineButton;
import io.github.stanikoc.botforge.keyboard.InlineKeyboard;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A reference bot showing commands, inline buttons and text routing.
 * <p>
 * Run with {@code BOT_TOKEN=<token from BotFather> ./gradlew :botforge-example:run}.
 */
public class ExampleBot {
    private static final InlineKeyboard VOTE_KEYBOARD = InlineKeyboard.of(
            InlineButton.callback("👍", "vote", "up"),
            InlineButton.callback("👎", "vote", "down"));

    static void main() {
        String token = System.getenv("BOT_TOKEN");
        if (token == null || token.isBlank()) {
            IO.println("Set the BOT_TOKEN environment variable to your bot's token.");
            return;
        }

        var up = new AtomicInteger();
        var down = new AtomicInteger();

        Bot bot = Bot.builder(token)
                .command("start", "Say hello", ctx ->
                        ctx.reply("Hi " + ctx.user().firstName() + "! Try /vote, /echo or just say hello."))
                .command("echo", "Repeat what you write after the command", ctx ->
                        ctx.reply(ctx.argument().isEmpty() ? "Usage: /echo <text>" : ctx.argument()))
                .command("vote", "Vote with inline buttons", ctx ->
                        ctx.reply("Do you like BotForge?", VOTE_KEYBOARD))
                .button("vote", ctx -> {
                    switch (ctx.payload()) {
                        case "up" -> up.incrementAndGet();
                        case "down" -> down.incrementAndGet();
                        default -> {
                            ctx.alert("Unknown vote");
                            return;
                        }
                    }

                    ctx.answer("Thanks for voting!");
                    ctx.edit("Votes so far: 👍 " + up.get() + "  👎 " + down.get(), VOTE_KEYBOARD);
                })
                .text(text -> text.toLowerCase(Locale.ROOT).startsWith("hello"), ctx ->
                        ctx.reply("Hello yourself, " + ctx.user().firstName() + "!"))
                .fallbackText(ctx -> ctx.reply("I don't understand that yet. Try /start."))
                .build();

        Runtime.getRuntime().addShutdownHook(new Thread(bot::stop));
        //noinspection JavaPrintToLogpoint
        IO.println("Bot running. Press Ctrl+C to stop.");
        bot.run();
    }
}
