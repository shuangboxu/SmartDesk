package com.smartdesk.ui.tasks;

import com.smartdesk.core.task.model.TaskPriority;
import com.smartdesk.core.task.model.TaskStatus;
import com.smartdesk.core.task.model.TaskType;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * View model used by the JavaFX task dashboard. It mirrors the structure of the
 * core task domain while exposing JavaFX properties for easy binding in the UI.
 */
public final class TaskViewModel {

    private static final AtomicLong ID_GENERATOR = new AtomicLong(1000);
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("MM-dd HH:mm");

    private final LongProperty id = new SimpleLongProperty(this, "id", ID_GENERATOR.getAndIncrement());
    private final StringProperty title = new SimpleStringProperty(this, "title", "");
    private final StringProperty description = new SimpleStringProperty(this, "description", "");
    private final ObjectProperty<LocalDateTime> startDateTime =
        new SimpleObjectProperty<>(this, "startDateTime", null);
    private final ObjectProperty<LocalDateTime> dueDateTime =
        new SimpleObjectProperty<>(this, "dueDateTime", null);
    private final ObjectProperty<TaskPriority> priority =
        new SimpleObjectProperty<>(this, "priority", TaskPriority.NORMAL);
    private final ObjectProperty<TaskType> type =
        new SimpleObjectProperty<>(this, "type", TaskType.TODO);
    private final ObjectProperty<TaskStatus> status =
        new SimpleObjectProperty<>(this, "status", TaskStatus.PLANNED);
    private final BooleanProperty reminderEnabled =
        new SimpleBooleanProperty(this, "reminderEnabled", true);
    private final IntegerProperty reminderLeadMinutes =
        new SimpleIntegerProperty(this, "reminderLeadMinutes", 15);
    private final ObjectProperty<LocalDateTime> lastRemindedAt =
        new SimpleObjectProperty<>(this, "lastRemindedAt", null);
    private final BooleanProperty reminderTriggered =
        new SimpleBooleanProperty(this, "reminderTriggered", false);

    public TaskViewModel() {
    }

    public TaskViewModel copy() {
        TaskViewModel copy = new TaskViewModel();
        copy.setId(getId());
        copy.setTitle(getTitle());
        copy.setDescription(getDescription());
        copy.setStartDateTime(getStartDateTime());
        copy.setDueDateTime(getDueDateTime());
        copy.setPriority(getPriority());
        copy.setType(getType());
        copy.setStatus(getStatus());
        copy.setReminderEnabled(isReminderEnabled());
        copy.setReminderLeadMinutes(getReminderLeadMinutes());
        copy.setLastRemindedAt(getLastRemindedAt());
        copy.setReminderTriggered(isReminderTriggered());
        return copy;
    }

    public long getId() {
        return id.get();
    }

    public void setId(final long value) {
        id.set(value);
    }

    public LongProperty idProperty() {
        return id;
    }

    public String getTitle() {
        return title.get();
    }

    public void setTitle(final String value) {
        title.set(value);
    }

    public StringProperty titleProperty() {
        return title;
    }

    public String getDescription() {
        return description.get();
    }

    public void setDescription(final String value) {
        description.set(value);
    }

    public StringProperty descriptionProperty() {
        return description;
    }

    public LocalDateTime getStartDateTime() {
        return startDateTime.get();
    }

    public void setStartDateTime(final LocalDateTime value) {
        startDateTime.set(value);
    }

    public ObjectProperty<LocalDateTime> startDateTimeProperty() {
        return startDateTime;
    }

    public LocalDateTime getDueDateTime() {
        return dueDateTime.get();
    }

    public void setDueDateTime(final LocalDateTime value) {
        dueDateTime.set(value);
    }

    public ObjectProperty<LocalDateTime> dueDateTimeProperty() {
        return dueDateTime;
    }

    public TaskPriority getPriority() {
        return priority.get();
    }

    public void setPriority(final TaskPriority value) {
        priority.set(value);
    }

    public ObjectProperty<TaskPriority> priorityProperty() {
        return priority;
    }

    public TaskType getType() {
        return type.get();
    }

    public void setType(final TaskType value) {
        type.set(value);
    }

    public ObjectProperty<TaskType> typeProperty() {
        return type;
    }

    public TaskStatus getStatus() {
        return status.get();
    }

    public void setStatus(final TaskStatus value) {
        status.set(value);
    }

    public ObjectProperty<TaskStatus> statusProperty() {
        return status;
    }

    public boolean isReminderEnabled() {
        return reminderEnabled.get();
    }

    public void setReminderEnabled(final boolean value) {
        reminderEnabled.set(value);
    }

    public BooleanProperty reminderEnabledProperty() {
        return reminderEnabled;
    }

    public int getReminderLeadMinutes() {
        return reminderLeadMinutes.get();
    }

    public void setReminderLeadMinutes(final int value) {
        reminderLeadMinutes.set(value);
    }

    public IntegerProperty reminderLeadMinutesProperty() {
        return reminderLeadMinutes;
    }

    public LocalDateTime getLastRemindedAt() {
        return lastRemindedAt.get();
    }

    public void setLastRemindedAt(final LocalDateTime value) {
        lastRemindedAt.set(value);
    }

    public ObjectProperty<LocalDateTime> lastRemindedAtProperty() {
        return lastRemindedAt;
    }

    public boolean isReminderTriggered() {
        return reminderTriggered.get();
    }

    public void setReminderTriggered(final boolean value) {
        reminderTriggered.set(value);
    }

    public BooleanProperty reminderTriggeredProperty() {
        return reminderTriggered;
    }

    public String getFormattedDueDate() {
        LocalDateTime due = getDueDateTime();
        if (due == null) {
            return "未设置截止时间";
        }
        return due.format(DATE_TIME_FORMATTER);
    }

    public boolean occursOn(final LocalDate date) {
        LocalDateTime due = getDueDateTime();
        if (due != null && due.toLocalDate().isEqual(date)) {
            return true;
        }
        LocalDateTime start = getStartDateTime();
        return start != null && start.toLocalDate().isEqual(date);
    }

    public boolean isOverdue(final LocalDateTime now) {
        LocalDateTime due = getDueDateTime();
        return due != null && due.isBefore(now) && getStatus() != TaskStatus.COMPLETED;
    }

    public boolean isDueWithin(final LocalDateTime now, final Duration duration) {
        LocalDateTime due = getDueDateTime();
        if (due == null) {
            return false;
        }
        if (due.isBefore(now)) {
            return false;
        }
        Duration remaining = Duration.between(now, due);
        return !remaining.isNegative() && remaining.compareTo(duration) <= 0;
    }

    public void markReminderTriggered(final LocalDateTime reminderTime) {
        setReminderTriggered(true);
        setLastRemindedAt(reminderTime);
    }

    public void resetReminderState() {
        setReminderTriggered(false);
        setLastRemindedAt(null);
    }

    @Override
    public String toString() {
        return getTitle();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TaskViewModel other)) {
            return false;
        }
        return getId() == other.getId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
}
