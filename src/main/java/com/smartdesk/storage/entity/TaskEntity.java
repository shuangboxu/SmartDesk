package com.smartdesk.storage.entity;

import java.util.Objects;

/**
 * Represents a persisted task record. The entity mirrors the {@code tasks}
 * table stored in SQLite and is intentionally mutable to simplify JDBC
 * integration.
 */
public class TaskEntity {

    private Long id;
    private String title;
    private String description;
    private String startAt;
    private String dueAt;
    private int priority;
    private String type;
    private boolean reminderEnabled;
    private int reminderLeadMinutes;
    private String status;
    private String lastRemindedAt;
    private String createdAt;
    private String updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = Objects.requireNonNull(title, "title");
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getStartAt() {
        return startAt;
    }

    public void setStartAt(final String startAt) {
        this.startAt = startAt;
    }

    public String getDueAt() {
        return dueAt;
    }

    public void setDueAt(final String dueAt) {
        this.dueAt = dueAt;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(final int priority) {
        this.priority = priority;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = Objects.requireNonNull(type, "type");
    }

    public boolean isReminderEnabled() {
        return reminderEnabled;
    }

    public void setReminderEnabled(final boolean reminderEnabled) {
        this.reminderEnabled = reminderEnabled;
    }

    public int getReminderLeadMinutes() {
        return reminderLeadMinutes;
    }

    public void setReminderLeadMinutes(final int reminderLeadMinutes) {
        this.reminderLeadMinutes = reminderLeadMinutes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = Objects.requireNonNull(status, "status");
    }

    public String getLastRemindedAt() {
        return lastRemindedAt;
    }

    public void setLastRemindedAt(final String lastRemindedAt) {
        this.lastRemindedAt = lastRemindedAt;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final String createdAt) {
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(final String updatedAt) {
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }
}
