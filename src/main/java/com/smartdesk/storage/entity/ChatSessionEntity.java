package com.smartdesk.storage.entity;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a persisted chat session record. Each session groups multiple chat
 * messages exchanged with the assistant.
 */
public class ChatSessionEntity {

    private String id;
    private String defaultTitle;
    private String title;
    private boolean autoTitle;
    private String modelName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = Objects.requireNonNull(id, "id");
    }

    public String getDefaultTitle() {
        return defaultTitle;
    }

    public void setDefaultTitle(final String defaultTitle) {
        this.defaultTitle = Objects.requireNonNull(defaultTitle, "defaultTitle");
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = Objects.requireNonNull(title, "title");
    }

    public boolean isAutoTitle() {
        return autoTitle;
    }

    public void setAutoTitle(final boolean autoTitle) {
        this.autoTitle = autoTitle;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(final String modelName) {
        this.modelName = modelName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(final LocalDateTime updatedAt) {
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }
}
