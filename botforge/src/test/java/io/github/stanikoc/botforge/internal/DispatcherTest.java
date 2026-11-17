package io.github.stanikoc.botforge.internal;

import io.github.stanikoc.botforge.TelegramException;
import io.github.stanikoc.botforge.action.Action;
import io.github.stanikoc.botforge.action.TextHandler;
import io.github.stanikoc.botforge.action.User;
import io.github.stanikoc.botforge.internal.json.Json;
import io.github.stanikoc.botforge.keyboard.InlineButton;
import io.github.stanikoc.botforge.keyboard.InlineKeyboard;
import io.github.stanikoc.botforge.registry.RegistryImpl;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DispatcherTest {
    private static final User USER = new User(7, "Alice", null, null, false);

    /** Records calls instead of talking to Telegram. */
    private static final class FakeApi implements TelegramApi {
        final List<String> calls = new ArrayList<>();
        final List<Map<String, ?>> params = new ArrayList<>();
        boolean failCallbackAnswer;

        @Override
        public Object call(@NonNull String method, @NonNull Map<String, ?> params, @NonNull Duration timeout) {
            calls.add(method);
            this.params.add(params);
            if (failCallbackAnswer && "answerCallbackQuery".equals(method)) {
                throw new TelegramException(400, "query is too old");
            }
            return Map.of("message_id", 99L);
        }
    }

    private final FakeApi api = new FakeApi();

    private Dispatcher dispatcher(
            List<CommandRoute> commands, List<ButtonRoute> buttons, List<TextRoute> texts, @Nullable TextHandler fallback) {
        return new Dispatcher(
                api,
                new RegistryImpl<>(commands.toArray(CommandRoute[]::new)),
                new RegistryImpl<>(buttons.toArray(ButtonRoute[]::new)),
                texts.toArray(TextRoute[]::new),
                fallback);
    }

    @Test
    void routesCommandsByName() throws Exception {
        List<String> seen = new ArrayList<>();
        var d = dispatcher(List.of(
                new CommandRoute("start", "Start", ctx -> seen.add("start")),
                new CommandRoute("echo", "Echo", ctx -> {
                    seen.add("echo " + ctx.argument());
                    assertEquals(99L, ctx.reply(ctx.argument()));
                })), List.of(), List.of(), null);

        d.dispatch(new Action.Command(1, USER, 2, "ECHO", "hi there"));
        d.dispatch(new Action.Command(1, USER, 2, "unknown", ""));

        assertEquals(List.of("echo hi there"), seen);
        assertEquals(List.of("sendMessage"), api.calls);
        assertEquals(Map.of("chat_id", 1L, "text", "hi there"), api.params.getFirst());
    }

    @Test
    void answersCallbackAutomaticallyWhenHandlerDoesNot() throws Exception {
        var d = dispatcher(List.of(), List.of(new ButtonRoute("vote", ctx -> {})), List.of(), null);

        d.dispatch(new Action.Button(1, USER, 2, "cb", "vote", "up"));
        d.dispatch(new Action.Button(1, USER, 2, "cb2", "unregistered", ""));

        assertEquals(List.of("answerCallbackQuery", "answerCallbackQuery"), api.calls);
        assertEquals(Map.of("callback_query_id", "cb2"), api.params.get(1));
    }

    @Test
    void answersCallbackEvenWhenHandlerThrows() {
        var d = dispatcher(List.of(), List.of(new ButtonRoute("vote", ctx -> {
            throw new IllegalStateException("boom");
        })), List.of(), null);

        assertThrows(IllegalStateException.class, () -> d.dispatch(new Action.Button(1, USER, 2, "cb", "vote", "")));
        assertEquals(List.of("answerCallbackQuery"), api.calls);
    }

    @Test
    void ignoresAutoAnswerFailureWithoutMaskingHandlerResultOrException() throws Exception {
        api.failCallbackAnswer = true;
        var okDispatcher = dispatcher(List.of(), List.of(new ButtonRoute("vote", ctx -> {})), List.of(), null);
        okDispatcher.dispatch(new Action.Button(1, USER, 2, "cb", "vote", ""));

        var failingDispatcher = dispatcher(List.of(), List.of(new ButtonRoute("vote", ctx -> {
            throw new IllegalStateException("handler boom");
        })), List.of(), null);
        IllegalStateException thrown = assertThrows(
                IllegalStateException.class,
                () -> failingDispatcher.dispatch(new Action.Button(1, USER, 2, "cb2", "vote", "")));
        assertEquals("handler boom", thrown.getMessage());
    }

    @Test
    void doesNotAnswerTwiceAndEditsWithKeyboard() throws Exception {
        var keyboard = InlineKeyboard.of(InlineButton.callback("Again", "vote", "up"));
        var d = dispatcher(List.of(), List.of(new ButtonRoute("vote", ctx -> {
            ctx.answer("Thanks " + ctx.payload());
            ctx.edit("Edited", keyboard);
            assertThrows(IllegalStateException.class, ctx::answer);
        })), List.of(), null);

        d.dispatch(new Action.Button(1, USER, 2, "cb", "vote", "up"));

        assertEquals(List.of("answerCallbackQuery", "editMessageText"), api.calls);
        assertEquals("Thanks up", api.params.get(0).get("text"));
        assertEquals(
                Map.of("inline_keyboard", List.of(List.of(Map.of("text", "Again", "callback_data", "vote:up")))),
                Json.parse(Json.write(api.params.get(1).get("reply_markup"))));
    }

    @Test
    void textRoutesUseFirstMatchThenFallback() throws Exception {
        List<String> seen = new ArrayList<>();
        var d = dispatcher(List.of(), List.of(), List.of(
                new TextRoute(t -> t.startsWith("hello"), ctx -> seen.add("hello")),
                new TextRoute(t -> t.contains("o"), ctx -> seen.add("contains o"))),
                ctx -> seen.add("fallback " + ctx.text()));

        d.dispatch(new Action.Text(1, USER, 2, "hello world"));
        d.dispatch(new Action.Text(1, USER, 2, "foo"));
        d.dispatch(new Action.Text(1, USER, 2, "bar"));

        assertEquals(List.of("hello", "contains o", "fallback bar"), seen);
    }

    @Test
    void ignoresUnmatchedTextWithoutFallbackAndClonesRoutes() throws Exception {
        List<String> seen = new ArrayList<>();
        TextRoute[] routes = {new TextRoute(t -> t.equals("hi"), ctx -> seen.add(ctx.text()))};
        var d = new Dispatcher(api, new RegistryImpl<>(), new RegistryImpl<>(), routes, null);
        routes[0] = new TextRoute(t -> true, ctx -> seen.add("mutated"));

        d.dispatch(new Action.Text(1, USER, 2, "other"));
        assertTrue(seen.isEmpty());
        assertTrue(api.calls.isEmpty());

        d.dispatch(new Action.Text(1, USER, 2, "hi"));
        assertEquals(List.of("hi"), seen);
    }
}
