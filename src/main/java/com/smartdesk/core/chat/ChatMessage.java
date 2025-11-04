package com.smartdesk.core.chat;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a single chat message exchanged between the user and the assistant.
 */
public final class ChatMessage {

    public enum Sender {
        USER,
        ASSISTANT,
        SYSTEM
    }

    private final Sender sender;
    private final String content;
    private final LocalDateTime timestamp;

    private ChatMessage(final Sender sender, final String content, final LocalDateTime timestamp) {
        this.sender = Objects.requireNonNull(sender, "sender");
        this.content = Objects.requireNonNull(content, "content");
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp");
    }

    public static ChatMessage of(final Sender sender, final String content) {
        return new ChatMessage(sender, content, LocalDateTime.now());
    }

    /**
     * Creates a chat message using an explicit timestamp. Primarily used when
     * reconstructing persisted chat history from the database.
     *
     * @param sender    the message sender
     * @param content   the message text
     * @param timestamp the timestamp associated with the message
     * @return a new {@link ChatMessage} instance populated with the supplied data
     */
    public static ChatMessage of(final Sender sender, final String content, final LocalDateTime timestamp) {
        return new ChatMessage(sender, content, timestamp);
    }

    public Sender getSender() {
        return sender;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
