package io.github.stanikoc.botforge;

import io.github.stanikoc.botforge.action.ButtonHandler;
import io.github.stanikoc.botforge.action.CommandHandler;
import io.github.stanikoc.botforge.action.TextHandler;
import io.github.stanikoc.botforge.internal.BotImpl;
import io.github.stanikoc.botforge.internal.ButtonRoute;
import io.github.stanikoc.botforge.internal.CommandRoute;
import io.github.stanikoc.botforge.internal.Dispatcher;
import io.github.stanikoc.botforge.internal.HttpTelegramApi;
import io.github.stanikoc.botforge.internal.TextRoute;
import io.github.stanikoc.botforge.internal.Validation;
import io.github.stanikoc.botforge.registry.RegistryImpl;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public final class BotBuilder {
    private final String token;
    private final List<CommandRoute> commands = new ArrayList<>();
    private final List<ButtonRoute> buttons = new ArrayList<>();
    private final List<TextRoute> texts = new ArrayList<>();
    private @Nullable TextHandler fallbackText;
    private boolean publishCommands = true;

    BotBuilder(String token) {
        if (token.isBlank()) {
            throw new IllegalArgumentException("Token must not be blank");
        }

        this.token = token;
    }

    /**
     * Handles {@code /name} messages. In groups, {@code /name@YourBot} is matched too, while
     * commands addressed to other bots are ignored.
     *
     * @param name        1 to 32 characters of {@code a-z}, {@code 0-9} and {@code _}, without the slash
     * @param description 1 to 256 characters, shown in Telegram's command menu
     */
    public BotBuilder command(String name, String description, CommandHandler handler) {
        commands.add(new CommandRoute(
                Validation.commandName(name),
                Validation.commandDescription(description),
                Objects.requireNonNull(handler, "handler")));
        return this;
    }

    /**
     * Handles presses of inline buttons whose callback data belongs to {@code namespace}, as created
     * by {@link io.github.stanikoc.botforge.keyboard.InlineButton#callback(String, String, String)}.
     * If the handler doesn't answer the callback, it is answered automatically afterwards.
     */
    public BotBuilder button(String namespace, ButtonHandler handler) {
        buttons.add(new ButtonRoute(
                Validation.namespace(namespace),
                Objects.requireNonNull(handler, "handler")));
        return this;
    }

    /**
     * Handles plain text messages matching {@code condition}. Conditions are checked in the order
     * they were added, and only the first match runs.
     */
    public BotBuilder text(Predicate<String> condition, TextHandler handler) {
        texts.add(new TextRoute(
                Objects.requireNonNull(condition, "condition"),
                Objects.requireNonNull(handler, "handler")));
        return this;
    }

    /**
     * Handles plain text messages that no {@link #text(Predicate, TextHandler)} condition matched.
     */
    public BotBuilder fallbackText(TextHandler handler) {
        this.fallbackText = Objects.requireNonNull(handler, "handler");
        return this;
    }

    /**
     * Whether to replace the bot's command menu in Telegram with the registered commands on
     * startup. Enabled by default.
     */
    public BotBuilder publishCommands(boolean publish) {
        this.publishCommands = publish;
        return this;
    }

    /**
     * Freezes the registered handlers and creates the bot.
     *
     * @throws IllegalArgumentException if two commands or two button namespaces share a name
     */
    public Bot build() {
        var commandRegistry = new RegistryImpl<>(commands.toArray(CommandRoute[]::new));
        var buttonRegistry = new RegistryImpl<>(buttons.toArray(ButtonRoute[]::new));
        var api = new HttpTelegramApi(token);
        var dispatcher = new Dispatcher(
                api, commandRegistry, buttonRegistry, texts.toArray(TextRoute[]::new), fallbackText);
        return new BotImpl(api, dispatcher, commandRegistry, publishCommands);
    }
}
