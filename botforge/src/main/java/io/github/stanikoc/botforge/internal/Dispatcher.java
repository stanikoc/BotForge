package io.github.stanikoc.botforge.internal;

import io.github.stanikoc.botforge.TelegramException;
import io.github.stanikoc.botforge.action.Action;
import io.github.stanikoc.botforge.action.TextHandler;
import io.github.stanikoc.botforge.registry.Registry;
import org.jspecify.annotations.Nullable;

public final class Dispatcher {
    private static final System.Logger LOG = System.getLogger("io.github.stanikoc.botforge");

    private final TelegramApi api;
    private final Registry<CommandRoute> commands;
    private final Registry<ButtonRoute> buttons;
    private final TextRoute[] texts;
    private final @Nullable TextHandler fallbackText;

    public Dispatcher(
            TelegramApi api,
            Registry<CommandRoute> commands,
            Registry<ButtonRoute> buttons,
            TextRoute[] texts,
            @Nullable TextHandler fallbackText) {
        this.api = api;
        this.commands = commands;
        this.buttons = buttons;
        this.texts = texts.clone();
        this.fallbackText = fallbackText;
    }

    public void dispatch(Action action) throws Exception {
        switch (action) {
            case Action.Command command -> dispatchCommand(command);
            case Action.Button button -> dispatchButton(button);
            case Action.Text text -> dispatchText(text);
        }
    }

    private void dispatchCommand(Action.Command action) throws Exception {
        var route = commands.getByName(action.name());
        if (route.isPresent()) {
            route.get().handler().handle(new Contexts.Command(api, action));
        }
    }

    private void dispatchButton(Action.Button action) throws Exception {
        var ctx = new Contexts.Button(api, action);
        try {
            var route = buttons.getByName(action.namespace());
            if (route.isPresent()) {
                route.get().handler().handle(ctx);
            }
        } finally {
            if (!ctx.answered()) {
                try {
                    ctx.answer();
                } catch (TelegramException | IllegalStateException e) {
                    LOG.log(System.Logger.Level.WARNING, "Could not answer callback: " + e.getMessage());
                }
            }
        }
    }

    private void dispatchText(Action.Text action) throws Exception {
        var ctx = new Contexts.Text(api, action);
        for (var route : texts) {
            if (route.condition().test(action.text())) {
                route.handler().handle(ctx);
                return;
            }
        }

        if (fallbackText != null) {
            fallbackText.handle(ctx);
        }
    }
}
