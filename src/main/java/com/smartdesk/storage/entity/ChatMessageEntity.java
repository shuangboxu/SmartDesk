package com.smartdesk.storage.entity;

import com.smartdesk.core.chat.ChatMessage;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a persisted chat message record. The entity mirrors the
 * {@code chat_messages} table managed by {@link com.smartdesk.storage.DatabaseManager}.
 */
public class ChatMessageEntity {

    private Long id;
    private String sessionId;
    private ChatMessage.Sender sender;
    private String content;
    private LocalDateTime timestamp;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(final String sessionId) {
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId");
    }

    public ChatMessage.Sender getSender() {
        return sender;
    }

    public void setSender(final ChatMessage.Sender sender) {
        this.sender = Objects.requireNonNull(sender, "sender");
    }

    public String getContent() {
        return content;
    }

    public void setContent(final String content) {
        this.content = Objects.requireNonNull(content, "content");
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(final LocalDateTime timestamp) {
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp");
    }
}
