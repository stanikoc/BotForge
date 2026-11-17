package io.github.stanikoc.botforge.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValidationTest {
    @Test
    void acceptsValidCommandNames() {
        assertEquals("a", Validation.commandName("a"));
        assertEquals("start_2", Validation.commandName("start_2"));
        assertEquals("a".repeat(32), Validation.commandName("a".repeat(32)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "/start", "Start", "a-b", "a b"})
    void rejectsInvalidCommandNames(String name) {
        assertThrows(IllegalArgumentException.class, () -> Validation.commandName(name));
    }

    @Test
    void rejectsTooLongCommandName() {
        assertThrows(IllegalArgumentException.class, () -> Validation.commandName("a".repeat(33)));
    }

    @Test
    void validatesCommandDescriptions() {
        assertEquals("x", Validation.commandDescription("x"));
        assertEquals("d".repeat(256), Validation.commandDescription("d".repeat(256)));

        assertThrows(IllegalArgumentException.class, () -> Validation.commandDescription(""));
        assertThrows(IllegalArgumentException.class, () -> Validation.commandDescription("   "));
        assertThrows(IllegalArgumentException.class, () -> Validation.commandDescription("d".repeat(257)));
    }

    @Test
    void validatesButtonNamespaces() {
        assertEquals("vote", Validation.namespace("vote"));
        assertEquals("n".repeat(64), Validation.namespace("n".repeat(64)));

        assertThrows(IllegalArgumentException.class, () -> Validation.namespace(""));
        assertThrows(IllegalArgumentException.class, () -> Validation.namespace("a:b"));
        assertThrows(IllegalArgumentException.class, () -> Validation.namespace(":a"));
        assertThrows(IllegalArgumentException.class, () -> Validation.namespace("n".repeat(65)));
        assertThrows(IllegalArgumentException.class, () -> Validation.namespace("😀".repeat(17)));
    }

    @Test
    void validatesCallbackDataUtf8ByteLength() {
        assertDoesNotThrow(() -> Validation.callbackData("a"));
        assertDoesNotThrow(() -> Validation.callbackData("a".repeat(64)));
        assertDoesNotThrow(() -> Validation.callbackData("é".repeat(32))); // 64 UTF-8 bytes

        assertThrows(IllegalArgumentException.class, () -> Validation.callbackData(""));
        assertThrows(IllegalArgumentException.class, () -> Validation.callbackData("a".repeat(65)));
        assertThrows(IllegalArgumentException.class, () -> Validation.callbackData("é".repeat(33))); // 66 UTF-8 bytes
    }
}
