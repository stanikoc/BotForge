package io.github.stanikoc.botforge.internal;

import io.github.stanikoc.botforge.action.Action;
import io.github.stanikoc.botforge.action.User;
import io.github.stanikoc.botforge.internal.json.Json;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UpdateParserTest {
    private static final String BOT = "ForgeBot";
    private static final User ALICE = new User(7, "Alice", null, "alice", false);

    private static Action parse(String json) {
        return UpdateParser.parse(Json.parseObject(json), BOT);
    }

    private static String message(String text, int commandLength) {
        String entities = commandLength > 0
                ? ", \"entities\": [{\"type\": \"bot_command\", \"offset\": 0, \"length\": " + commandLength + "}]"
                : "";
        return """
                {"update_id": 1, "message": {"message_id": 5, "chat": {"id": -100}, "text": "%s",
                 "from": {"id": 7, "is_bot": false, "first_name": "Alice", "username": "alice"}%s}}
                """.formatted(text, entities);
    }

    @Test
    void parsesCommandWithArguments() {
        Action.Command command = (Action.Command) parse(message("/Echo  hello   world ", 5));
        assertEquals(new Action.Command(-100, ALICE, 5, "echo", "hello   world"), command);
        assertEquals(List.of("hello", "world"), command.args());
    }

    @Test
    void parsesCommandWithoutArguments() {
        Action.Command command = (Action.Command) parse(message("/start", 6));
        assertEquals("", command.argument());
        assertEquals(List.of(), command.args());
    }

    @Test
    void acceptsCommandsAddressedToThisBotCaseInsensitively() {
        Action.Command command = (Action.Command) parse(message("/Start@FORGEBOT now", 15));
        assertEquals("start", command.name());
        assertEquals("now", command.argument());
    }

    @Test
    void ignoresCommandsAddressedToOtherBots() {
        assertNull(parse(message("/start@OtherBot", 15)));
    }

    @Test
    void treatsSlashWithoutLeadingCommandEntityAsText() {
        assertEquals(new Action.Text(-100, ALICE, 5, "/ not a command"), parse(message("/ not a command", 0)));
        assertEquals(new Action.Text(-100, ALICE, 5, "/"), parse(message("/", 1)));
        assertEquals(
                new Action.Text(-100, ALICE, 5, "say /start"),
                parse("""
                        {"update_id": 1, "message": {"message_id": 5, "chat": {"id": -100}, "text": "say /start",
                         "from": {"id": 7, "is_bot": false, "first_name": "Alice", "username": "alice"},
                         "entities": [{"type": "bot_command", "offset": 4, "length": 6}]}}
                        """));
        assertEquals(
                new Action.Text(-100, ALICE, 5, "/bold"),
                parse("""
                        {"update_id": 1, "message": {"message_id": 5, "chat": {"id": -100}, "text": "/bold",
                         "from": {"id": 7, "is_bot": false, "first_name": "Alice", "username": "alice"},
                         "entities": [{"type": "bold", "offset": 0, "length": 5}]}}
                        """));
    }

    @Test
    void clampsCommandLengthExceedingTextLength() {
        Action.Command command = (Action.Command) parse(message("/hi", 99));
        assertEquals("hi", command.name());
        assertEquals("", command.argument());
    }

    @Test
    void parsesPlainTextAndAllUserFields() {
        assertEquals(new Action.Text(-100, ALICE, 5, "hello"), parse(message("hello", 0)));

        Action action = parse("""
                {"update_id": 1, "message": {"message_id": 9, "chat": {"id": 12}, "text": "yo",
                 "from": {"id": 8, "is_bot": true, "first_name": "Helper", "last_name": "Bot"}}}
                """);
        assertEquals(new Action.Text(12, new User(8, "Helper", "Bot", null, true), 9, "yo"), action);
    }

    @Test
    void ignoresMessagesWithoutTextOrFromAndUnknownUpdates() {
        assertNull(parse("""
                {"update_id": 1, "message": {"message_id": 5, "chat": {"id": 1},
                 "from": {"id": 7, "first_name": "Alice"}, "photo": []}}
                """));
        assertNull(parse("""
                {"update_id": 1, "message": {"message_id": 5, "chat": {"id": 1}, "text": "channel post"}}
                """));
        assertNull(parse("""
                {"update_id": 1, "edited_message": {"message_id": 5, "chat": {"id": 1}, "text": "edited"}}
                """));
    }

    @Test
    void parsesCallbackWithAndWithoutPayload() {
        assertEquals(
                new Action.Button(-100, ALICE, 5, "cb1", "vote", "up:extra"),
                parse(callback("vote:up:extra")));
        assertEquals(new Action.Button(-100, ALICE, 5, "cb1", "refresh", ""), parse(callback("refresh")));
        assertEquals(new Action.Button(-100, ALICE, 5, "cb1", "vote", ""), parse(callback("vote:")));
    }

    @Test
    void ignoresInlineModeAndDataLessCallbacks() {
        assertNull(parse("""
                {"update_id": 1, "callback_query": {"id": "cb1", "inline_message_id": "x", "data": "vote:up",
                 "from": {"id": 7, "first_name": "Alice"}}}
                """));
        assertNull(parse("""
                {"update_id": 1, "callback_query": {"id": "cb1",
                 "from": {"id": 7, "first_name": "Alice"},
                 "message": {"message_id": 5, "chat": {"id": -100}}}}
                """));
    }

    private static String callback(String data) {
        return """
                {"update_id": 1, "callback_query": {"id": "cb1", "data": "%s",
                 "from": {"id": 7, "is_bot": false, "first_name": "Alice", "username": "alice"},
                 "message": {"message_id": 5, "chat": {"id": -100}}}}
                """.formatted(data);
    }
}
