package com.smartdesk.core.task.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Immutable domain model describing a task surfaced in the UI.
 */
public final class Task {

    private final Long id;
    private final String title;
    private final String description;
    private final LocalDateTime startDateTime;
    private final LocalDateTime dueDateTime;
    private final TaskPriority priority;
    private final TaskType type;
    private final boolean reminderEnabled;
    private final int reminderLeadMinutes;
    private final TaskStatus status;
    private final LocalDateTime lastRemindedAt;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private Task(final Builder builder) {
        this.id = builder.id;
        this.title = builder.title;
        this.description = builder.description;
        this.startDateTime = builder.startDateTime;
        this.dueDateTime = builder.dueDateTime;
        this.priority = builder.priority;
        this.type = builder.type;
        this.reminderEnabled = builder.reminderEnabled;
        this.reminderLeadMinutes = builder.reminderLeadMinutes;
        this.status = builder.status;
        this.lastRemindedAt = builder.lastRemindedAt;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getStartDateTime() {
        return startDateTime;
    }

    public LocalDateTime getDueDateTime() {
        return dueDateTime;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public TaskType getType() {
        return type;
    }

    public boolean isReminderEnabled() {
        return reminderEnabled;
    }

    public int getReminderLeadMinutes() {
        return reminderLeadMinutes;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public LocalDateTime getLastRemindedAt() {
        return lastRemindedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Returns a new builder initialised with the current task's data. Useful
     * when updating a single property.
     */
    public Builder toBuilder() {
        return new Builder()
            .withId(id)
            .withTitle(title)
            .withDescription(description)
            .withStartDateTime(startDateTime)
            .withDueDateTime(dueDateTime)
            .withPriority(priority)
            .withType(type)
            .withReminderEnabled(reminderEnabled)
            .withReminderLeadMinutes(reminderLeadMinutes)
            .withStatus(status)
            .withLastRemindedAt(lastRemindedAt)
            .withCreatedAt(createdAt)
            .withUpdatedAt(updatedAt);
    }

    @Override
    public String toString() {
        return "Task{"
            + "id=" + id
            + ", title='" + title + '\''
            + ", dueDateTime=" + dueDateTime
            + ", priority=" + priority
            + ", type=" + type
            + ", status=" + status
            + '}';
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Task other)) {
            return false;
        }
        return Objects.equals(id, other.id)
            && Objects.equals(title, other.title)
            && Objects.equals(description, other.description)
            && Objects.equals(startDateTime, other.startDateTime)
            && Objects.equals(dueDateTime, other.dueDateTime)
            && priority == other.priority
            && type == other.type
            && reminderEnabled == other.reminderEnabled
            && reminderLeadMinutes == other.reminderLeadMinutes
            && status == other.status
            && Objects.equals(lastRemindedAt, other.lastRemindedAt)
            && Objects.equals(createdAt, other.createdAt)
            && Objects.equals(updatedAt, other.updatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, description, startDateTime, dueDateTime,
            priority, type, reminderEnabled, reminderLeadMinutes, status,
            lastRemindedAt, createdAt, updatedAt);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder used to create immutable {@link Task} instances.
     */
    public static final class Builder {
        private Long id;
        private String title;
        private String description;
        private LocalDateTime startDateTime;
        private LocalDateTime dueDateTime;
        private TaskPriority priority = TaskPriority.NORMAL;
        private TaskType type = TaskType.TODO;
        private boolean reminderEnabled;
        private int reminderLeadMinutes = 15;
        private TaskStatus status = TaskStatus.PLANNED;
        private LocalDateTime lastRemindedAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        private Builder() {
        }

        public Builder withId(final Long id) {
            this.id = id;
            return this;
        }

        public Builder withTitle(final String title) {
            this.title = title;
            return this;
        }

        public Builder withDescription(final String description) {
            this.description = description;
            return this;
        }

        public Builder withStartDateTime(final LocalDateTime startDateTime) {
            this.startDateTime = startDateTime;
            return this;
        }

        public Builder withDueDateTime(final LocalDateTime dueDateTime) {
            this.dueDateTime = dueDateTime;
            return this;
        }

        public Builder withPriority(final TaskPriority priority) {
            if (priority != null) {
                this.priority = priority;
            }
            return this;
        }

        public Builder withType(final TaskType type) {
            if (type != null) {
                this.type = type;
            }
            return this;
        }

        public Builder withReminderEnabled(final boolean reminderEnabled) {
            this.reminderEnabled = reminderEnabled;
            return this;
        }

        public Builder withReminderLeadMinutes(final int reminderLeadMinutes) {
            if (reminderLeadMinutes >= 0) {
                this.reminderLeadMinutes = reminderLeadMinutes;
            }
            return this;
        }

        public Builder withStatus(final TaskStatus status) {
            if (status != null) {
                this.status = status;
            }
            return this;
        }

        public Builder withLastRemindedAt(final LocalDateTime lastRemindedAt) {
            this.lastRemindedAt = lastRemindedAt;
            return this;
        }

        public Builder withCreatedAt(final LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder withUpdatedAt(final LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Task build() {
            Objects.requireNonNull(title, "Task title must not be null");
            if (title.isBlank()) {
                throw new IllegalArgumentException("Task title must not be blank");
            }
            if (dueDateTime != null && startDateTime != null && dueDateTime.isBefore(startDateTime)) {
                throw new IllegalArgumentException("Due date must be after start date");
            }
            return new Task(this);
        }
    }
}
