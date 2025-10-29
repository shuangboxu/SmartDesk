package com.smartdesk.core.chat.online;

/**
 * Exception raised by online AI client implementations when requests fail.
 */
public final class AiClientException extends Exception {

    public AiClientException(final String message) {
        super(message);
    }

    public AiClientException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
