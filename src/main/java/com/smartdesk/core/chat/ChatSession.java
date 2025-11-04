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

    private final UUID id;
    private final ObservableList<ChatMessage> messages = FXCollections.observableArrayList();
    private final String defaultTitle;
    private String title;
    private boolean autoTitle;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String modelName;

    public ChatSession(final String defaultTitle) {
        this(UUID.randomUUID(), defaultTitle, defaultTitle, true, LocalDateTime.now(), LocalDateTime.now(), "");
    }

    public ChatSession(final UUID id,
                       final String defaultTitle,
                       final String title,
                       final boolean autoTitle,
                       final LocalDateTime createdAt,
                       final LocalDateTime updatedAt,
                       final String modelName) {
        this.id = Objects.requireNonNull(id, "id");
        this.defaultTitle = Objects.requireNonNull(defaultTitle, "defaultTitle");
        this.title = title == null || title.isBlank() ? defaultTitle : title;
        this.autoTitle = autoTitle;
        this.createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
        this.updatedAt = updatedAt == null ? this.createdAt : updatedAt;
        this.modelName = modelName == null ? "" : modelName;
    }

    public UUID getId() {
        return id;
    }

    public String getDefaultTitle() {
        return defaultTitle;
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

    public boolean isAutoTitle() {
        return autoTitle;
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

    public void setUpdatedAt(final LocalDateTime updatedAt) {
        this.updatedAt = updatedAt == null ? LocalDateTime.now() : updatedAt;
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

    public void setAutoTitle(final boolean autoTitle) {
        this.autoTitle = autoTitle;
    }
}
