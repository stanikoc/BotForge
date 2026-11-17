package io.github.stanikoc.botforge;

import io.github.stanikoc.botforge.keyboard.InlineButton;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BotBuilderTest {
    @Test
    void validatesCommandNamesAndDescriptions() {
        var builder = Bot.builder("token");
        assertThrows(IllegalArgumentException.class, () -> builder.command("", "Start", _ -> {}));
        assertThrows(IllegalArgumentException.class, () -> builder.command("/start", "Start", _ -> {}));
        assertThrows(IllegalArgumentException.class, () -> builder.command("Start", "Start", _ -> {}));
        assertThrows(IllegalArgumentException.class, () -> builder.command("a-b", "Start", _ -> {}));
        assertThrows(IllegalArgumentException.class, () -> builder.command("a".repeat(33), "Start", _ -> {}));
        assertThrows(IllegalArgumentException.class, () -> builder.command("start", " ", _ -> {}));
        assertThrows(IllegalArgumentException.class, () -> builder.command("start", "d".repeat(257), _ -> {}));
        assertDoesNotThrow(() -> builder.command("a".repeat(32), "d".repeat(256), _ -> {}));
        assertDoesNotThrow(() -> builder.command("start_2", "Start", _ -> {}));
    }

    @Test
    void rejectsDuplicatesOnBuild() {
        var commands = Bot.builder("token")
                .command("start", "One", _ -> {})
                .command("start", "Two", _ -> {});
        assertThrows(IllegalArgumentException.class, commands::build);

        var buttons = Bot.builder("token")
                .button("vote", _ -> {})
                .button("Vote", _ -> {});
        assertThrows(IllegalArgumentException.class, buttons::build);
    }

    @Test
    void validatesButtonNamespacesAndCallbackData() {
        assertThrows(IllegalArgumentException.class, () -> Bot.builder("token").button("", _ -> {}));
        assertThrows(IllegalArgumentException.class, () -> Bot.builder("token").button("a:b", _ -> {}));
        assertThrows(IllegalArgumentException.class, () -> Bot.builder("token").button("n".repeat(65), _ -> {}));
        assertThrows(IllegalArgumentException.class, () -> InlineButton.callback("x", "vote", "p".repeat(60)));
        assertEquals(new InlineButton.Callback("x", "vote:up"), InlineButton.callback("x", "vote", "up"));
        assertEquals(new InlineButton.Callback("x", "vote"), InlineButton.callback("x", "vote"));
    }

    @Test
    void rejectsBlankToken() {
        assertThrows(IllegalArgumentException.class, () -> Bot.builder(""));
        assertThrows(IllegalArgumentException.class, () -> Bot.builder(" "));
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    void rejectsNullHandlersAndConditions() {
        var builder = Bot.builder("token");
        assertThrows(NullPointerException.class, () -> builder.command("start", "Start", null));
        assertThrows(NullPointerException.class, () -> builder.button("vote", null));
        assertThrows(NullPointerException.class, () -> builder.text(null, _ -> {}));
        assertThrows(NullPointerException.class, () -> builder.text(_ -> true, null));
        assertThrows(NullPointerException.class, () -> builder.fallbackText(null));
    }

    @Test
    void buildsBotWithAllRouteTypes() {
        Bot bot = Bot.builder("token")
                .command("start", "Start", _ -> {})
                .button("vote", _ -> {})
                .text(t -> t.startsWith("hi"), _ -> {})
                .fallbackText(_ -> {})
                .publishCommands(false)
                .build();

        assertNotNull(bot);
    }
}
