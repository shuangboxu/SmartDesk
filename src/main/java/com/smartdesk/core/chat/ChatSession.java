package com.smartdesk.core.chat;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a single conversational session with the assistant.
 */
public final class ChatSession {

    private final UUID id = UUID.randomUUID();
    private final ObservableList<ChatMessage> messages = FXCollections.observableArrayList();
    private final String defaultTitle;
    private String title;
    private boolean autoTitle = true;
    private final LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = createdAt;
    private String modelName = "";

    public ChatSession(final String defaultTitle) {
        this.defaultTitle = Objects.requireNonNull(defaultTitle, "defaultTitle");
        this.title = defaultTitle;
    }

    public UUID getId() {
        return id;
    }

    public ObservableList<ChatMessage> getMessages() {
        return messages;
    }

    public void addMessage(final ChatMessage message) {
        Objects.requireNonNull(message, "message");
        messages.add(message);
        updatedAt = message.getTimestamp();
        if (autoTitle && message.getSender() == ChatMessage.Sender.USER) {
            title = summarise(message.getContent());
            autoTitle = false;
        }
    }

    private String summarise(final String content) {
        if (content == null || content.isBlank()) {
            return defaultTitle;
        }
        String trimmed = content.trim();
        return trimmed.length() <= 18 ? trimmed : trimmed.substring(0, 18) + "...";
    }

    public String getTitle() {
        return title;
    }

    public void resetTitle() {
        this.title = defaultTitle;
        this.autoTitle = true;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(final String modelName) {
        this.modelName = modelName == null ? "" : modelName;
    }

    public void setTitle(final String customTitle) {
        if (customTitle != null && !customTitle.isBlank()) {
            this.title = customTitle.trim();
            this.autoTitle = false;
        }
    }
}
