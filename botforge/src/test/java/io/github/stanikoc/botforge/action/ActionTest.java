package io.github.stanikoc.botforge.action;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActionTest {
    private static final User ALICE = new User(42, "Alice", "Smith", "alice", false);

    @Test
    void returnsUserIdForAllActionTypes() {
        assertEquals(42, new Action.Command(1, ALICE, 2, "start", "").userId());
        assertEquals(42, new Action.Button(1, ALICE, 2, "cb", "vote", "up").userId());
        assertEquals(42, new Action.Text(1, ALICE, 2, "hi").userId());
    }

    @Test
    void splitsCommandArgumentsOnWhitespace() {
        var empty = new Action.Command(1, ALICE, 2, "start", "");
        var single = new Action.Command(1, ALICE, 2, "echo", "hello");
        var multiple = new Action.Command(1, ALICE, 2, "echo", "one  two\tthree");

        assertEquals(List.of(), empty.args());
        assertEquals(List.of("hello"), single.args());
        assertEquals(List.of("one", "two", "three"), multiple.args());
    }

    @Test
    void userStoresNullableAndBotFields() {
        var bot = new User(99, "ForgeBot", null, null, true);

        assertEquals(99, bot.id());
        assertEquals("ForgeBot", bot.firstName());
        assertNull(bot.lastName());
        assertNull(bot.username());
        assertTrue(bot.isBot());
    }
}
