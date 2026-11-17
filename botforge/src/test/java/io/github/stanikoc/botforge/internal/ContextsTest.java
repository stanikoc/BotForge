package io.github.stanikoc.botforge.internal;

import io.github.stanikoc.botforge.action.Action;
import io.github.stanikoc.botforge.action.User;
import io.github.stanikoc.botforge.internal.json.Json;
import io.github.stanikoc.botforge.keyboard.InlineButton;
import io.github.stanikoc.botforge.keyboard.InlineKeyboard;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContextsTest {
    private static final User ALICE = new User(7, "Alice", "Smith", "alice", false);

    private static final class RecordingApi implements TelegramApi {
        final List<String> methods = new ArrayList<>();
        final List<Map<String, ?>> params = new ArrayList<>();
        long nextMessageId = 100;

        @Override
        public Object call(@NonNull String method, @NonNull Map<String, ?> params, @NonNull Duration timeout) {
            methods.add(method);
            this.params.add(params);
            return Map.of("message_id", nextMessageId++);
        }
    }

    private final RecordingApi api = new RecordingApi();

    @Test
    void exposesActionDataThroughContextDefaultMethods() {
        var cmdAction = new Action.Command(10, ALICE, 20, "echo", "one two");
        var cmdCtx = new Contexts.Command(api, cmdAction);
        assertEquals(cmdAction, cmdCtx.action());
        assertEquals(10, cmdCtx.chatId());
        assertEquals(ALICE, cmdCtx.user());
        assertEquals("echo", cmdCtx.name());
        assertEquals("one two", cmdCtx.argument());
        assertEquals(List.of("one", "two"), cmdCtx.args());

        var textAction = new Action.Text(11, ALICE, 21, "hello");
        var textCtx = new Contexts.Text(api, textAction);
        assertEquals(textAction, textCtx.action());
        assertEquals(11, textCtx.chatId());
        assertEquals(ALICE, textCtx.user());
        assertEquals("hello", textCtx.text());

        var buttonAction = new Action.Button(12, ALICE, 22, "cb", "vote", "up");
        var buttonCtx = new Contexts.Button(api, buttonAction);
        assertEquals(buttonAction, buttonCtx.action());
        assertEquals(12, buttonCtx.chatId());
        assertEquals(ALICE, buttonCtx.user());
        assertEquals("up", buttonCtx.payload());
    }

    @Test
    void replyAndSendBuildSendMessagePayloads() {
        var keyboard = InlineKeyboard.of(InlineButton.url("Docs", "https://example.com"));
        var ctx = new Contexts.Text(api, new Action.Text(10, ALICE, 20, "hi"));

        assertEquals(100L, ctx.reply("back"));
        ctx.reply("choose", keyboard);
        assertEquals(102L, ctx.send(999L, "direct"));
        assertEquals(103L, ctx.send(888L, "direct kb", keyboard));

        assertEquals(List.of("sendMessage", "sendMessage", "sendMessage", "sendMessage"), api.methods);
        assertEquals(Map.of("chat_id", 10L, "text", "back"), api.params.get(0));
        assertEquals(10L, api.params.get(1).get("chat_id"));
        assertEquals("choose", api.params.get(1).get("text"));
        assertEquals(
                Map.of("inline_keyboard", List.of(List.of(Map.of("text", "Docs", "url", "https://example.com")))),
                Json.parse(Json.write(api.params.get(1).get("reply_markup"))));
        assertEquals(Map.of("chat_id", 999L, "text", "direct"), api.params.get(2));
        assertEquals(888L, api.params.get(3).get("chat_id"));
    }

    @Test
    void buttonContextAnswersAlertsAndPreventsDoubleAnswer() {
        var silent = new Contexts.Button(api, new Action.Button(1, ALICE, 2, "cb1", "ns", ""));
        assertFalse(silent.answered());
        silent.answer();
        assertTrue(silent.answered());
        assertThrows(IllegalStateException.class, silent::answer);
        assertThrows(IllegalStateException.class, () -> silent.answer("late"));
        assertThrows(IllegalStateException.class, () -> silent.alert("late"));

        var toast = new Contexts.Button(api, new Action.Button(1, ALICE, 2, "cb2", "ns", ""));
        toast.answer("Saved");

        var alert = new Contexts.Button(api, new Action.Button(1, ALICE, 2, "cb3", "ns", ""));
        alert.alert("Warning!");

        assertEquals(List.of("answerCallbackQuery", "answerCallbackQuery", "answerCallbackQuery"), api.methods);
        assertEquals(Map.of("callback_query_id", "cb1"), api.params.get(0));
        assertEquals(Map.of("callback_query_id", "cb2", "text", "Saved", "show_alert", false), api.params.get(1));
        assertEquals(Map.of("callback_query_id", "cb3", "text", "Warning!", "show_alert", true), api.params.get(2));
    }

    @Test
    void buttonContextEditsWithAndWithoutKeyboard() {
        var keyboard = InlineKeyboard.builder()
                .row(InlineButton.callback("Yes", "vote", "yes"), InlineButton.callback("No", "vote", "no"))
                .row(InlineButton.url("Info", "https://example.com"))
                .build();
        var ctx = new Contexts.Button(api, new Action.Button(10, ALICE, 20, "cb", "vote", "yes"));

        ctx.edit("Plain update");
        ctx.edit("With buttons", keyboard);

        assertEquals(List.of("editMessageText", "editMessageText"), api.methods);
        assertEquals(Map.of("chat_id", 10L, "message_id", 20L, "text", "Plain update"), api.params.get(0));
        assertEquals(10L, api.params.get(1).get("chat_id"));
        assertEquals(20L, api.params.get(1).get("message_id"));
        assertEquals("With buttons", api.params.get(1).get("text"));
        assertEquals(
                Map.of("inline_keyboard", List.of(
                        List.of(
                                Map.of("text", "Yes", "callback_data", "vote:yes"),
                                Map.of("text", "No", "callback_data", "vote:no")),
                        List.of(Map.of("text", "Info", "url", "https://example.com")))),
                Json.parse(Json.write(api.params.get(1).get("reply_markup"))));
    }
}
