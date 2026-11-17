package io.github.stanikoc.botforge;

/**
 * Thrown when a Telegram Bot API call fails, either because Telegram rejected it or because
 * the request could not be completed.
 */
public class TelegramException extends RuntimeException {
    private final int errorCode;

    public TelegramException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public TelegramException(int errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * The Telegram error code ,or {@code 0} if no response was received.
     */
    public int errorCode() {
        return errorCode;
    }
}
