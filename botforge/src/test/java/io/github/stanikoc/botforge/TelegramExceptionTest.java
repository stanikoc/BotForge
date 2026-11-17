package io.github.stanikoc.botforge;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class TelegramExceptionTest {
    @Test
    void storesErrorCodeAndMessage() {
        var exception = new TelegramException(400, "Bad Request");

        assertEquals(400, exception.errorCode());
        assertEquals("Bad Request", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void storesErrorCodeMessageAndCause() {
        var cause = new RuntimeException("boom");
        var exception = new TelegramException(0, "Request failed", cause);

        assertEquals(0, exception.errorCode());
        assertEquals("Request failed", exception.getMessage());
        assertSame(cause, exception.getCause());
    }
}
