package io.github.stanikoc.botforge.keyboard;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InlineButtonTest {
    @Test
    void createsCallbackButtonWithAndWithoutPayload() {
        var withPayload = InlineButton.callback("Up", "vote", "up");
        var withoutPayload = InlineButton.callback("Refresh", "refresh");
        var emptyPayload = InlineButton.callback("Refresh", "refresh", "");

        assertEquals(new InlineButton.Callback("Up", "vote:up"), withPayload);
        assertEquals(new InlineButton.Callback("Refresh", "refresh"), withoutPayload);
        assertEquals(withoutPayload, emptyPayload);
        assertEquals("Up", withPayload.text());
    }

    @Test
    void rejectsInvalidCallbackNamespacesAndData() {
        assertThrows(IllegalArgumentException.class, () -> InlineButton.callback("x", "", "up"));
        assertThrows(IllegalArgumentException.class, () -> InlineButton.callback("x", "a:b", "up"));
        assertThrows(IllegalArgumentException.class, () -> InlineButton.callback("x", "ns", "a".repeat(62)));
        assertThrows(IllegalArgumentException.class, () -> InlineButton.callback("x", "ns", "😀".repeat(16)));
        assertThrows(IllegalArgumentException.class, () -> new InlineButton.Callback("x", ""));
        assertThrows(NullPointerException.class, () -> new InlineButton.Callback(null, "vote"));
    }

    @Test
    void acceptsExact64ByteCallbackData() {
        String data = "a".repeat(64);
        assertEquals(new InlineButton.Callback("Max", data), new InlineButton.Callback("Max", data));
    }

    @Test
    void createsAndValidatesUrlButton() {
        var button = InlineButton.url("Docs", "https://example.com");

        assertEquals(new InlineButton.Url("Docs", "https://example.com"), button);
        assertEquals("Docs", button.text());
        assertThrows(NullPointerException.class, () -> new InlineButton.Url(null, "https://example.com"));
        assertThrows(NullPointerException.class, () -> new InlineButton.Url("Docs", null));
    }
}
