package com.smartdesk.core.task;

import com.smartdesk.core.task.model.Task;
import com.smartdesk.core.task.model.TaskBoardColumn;
import com.smartdesk.core.task.model.TaskDashboardSnapshot;
import com.smartdesk.core.task.model.TaskLane;
import com.smartdesk.core.task.model.TaskPriority;
import com.smartdesk.core.task.model.TaskStatus;
import com.smartdesk.core.task.model.TaskType;
import com.smartdesk.core.task.service.TaskQuerySupport;
import com.smartdesk.storage.DatabaseManager;
import com.smartdesk.storage.entity.TaskEntity;
import com.smartdesk.utils.DateTimeUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles task planning and scheduling logic and acts as the primary façade for
 * the Task module. The service exposes convenience methods for CRUD
 * operations, dashboard projections and reminder scheduling support.
 */
public class TaskService implements TaskQuerySupport {

    private static final Logger LOGGER = Logger.getLogger(TaskService.class.getName());
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private static final String INSERT_SQL = """
        INSERT INTO tasks (title, description, start_at, due_at, priority, type,
            reminder_enabled, reminder_lead_minutes, status, last_reminded_at,
            created_at, updated_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

    private static final String UPDATE_SQL = """
        UPDATE tasks SET title = ?, description = ?, start_at = ?, due_at = ?,
            priority = ?, type = ?, reminder_enabled = ?, reminder_lead_minutes = ?,
            status = ?, last_reminded_at = ?, created_at = ?, updated_at = ?
        WHERE id = ?
        """;

    private static final String DELETE_SQL = "DELETE FROM tasks WHERE id = ?";

    private static final String SELECT_BASE_SQL = """
        SELECT id, title, description, start_at, due_at, priority, type,
               reminder_enabled, reminder_lead_minutes, status, last_reminded_at,
               created_at, updated_at
          FROM tasks
        """;

    private final DatabaseManager databaseManager;

    public TaskService(final DatabaseManager databaseManager) {
        this.databaseManager = Objects.requireNonNull(databaseManager, "databaseManager");
    }

    /**
     * Creates a new task and persists it. Automatically sets the created/updated
     * timestamps and returns the fully initialised domain object.
     */
    public Task createTask(final Task task) {
        Objects.requireNonNull(task, "task");
        final LocalDateTime now = DateTimeUtils.now();
        final Task taskToPersist = normaliseForCreate(task, now);
        final TaskEntity entity = toEntity(taskToPersist);

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            bindEntity(statement, entity);
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    entity.setId(generatedKeys.getLong(1));
                }
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to insert task", ex);
            throw new IllegalStateException("Failed to insert task", ex);
        }

        return fromEntity(entity);
    }

    /**
     * Retrieves a task by its identifier.
     */
    public Optional<Task> findTaskById(final long id) {
        final String sql = SELECT_BASE_SQL + " WHERE id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to fetch task by id", ex);
        }
        return Optional.empty();
    }

    /**
     * Returns all tasks sorted by due date and priority. Tasks without a due
     * date are placed at the end of the list.
     */
    public List<Task> listAllTasks() {
        final List<Task> tasks = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BASE_SQL);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                tasks.add(mapRow(resultSet));
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to fetch tasks", ex);
        }

        tasks.sort(Comparator
            .comparing(Task::getDueDateTime, Comparator.nullsLast(LocalDateTime::compareTo))
            .thenComparing(task -> task.getPriority().getLevel(), Comparator.reverseOrder()));
        return tasks;
    }

    /**
     * Updates the persisted data for the supplied task. The {@code id} must not
     * be {@code null}.
     */
    public Task updateTask(final Task task) {
        Objects.requireNonNull(task, "task");
        if (task.getId() == null) {
            throw new IllegalArgumentException("Task id must be present for updates");
        }
        final LocalDateTime now = DateTimeUtils.now();
        final Task taskToPersist = task.toBuilder().withUpdatedAt(now).build();
        final TaskEntity entity = toEntity(taskToPersist);

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
            bindEntity(statement, entity);
            statement.setLong(13, entity.getId());
            statement.executeUpdate();
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to update task", ex);
            throw new IllegalStateException("Failed to update task", ex);
        }
        return fromEntity(entity);
    }

    /**
     * Deletes a task by id.
     */
    public boolean deleteTask(final long id) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to delete task", ex);
            throw new IllegalStateException("Failed to delete task", ex);
        }
    }

    /**
     * Marks a task as completed and clears future reminders.
     */
    public Optional<Task> markTaskCompleted(final long id) {
        final Optional<Task> taskOptional = findTaskById(id);
        if (taskOptional.isEmpty()) {
            return Optional.empty();
        }
        final Task task = taskOptional.get().toBuilder()
            .withStatus(TaskStatus.COMPLETED)
            .withReminderEnabled(false)
            .withLastRemindedAt(DateTimeUtils.now())
            .build();
        return Optional.of(updateTask(task));
    }

    /**
     * Moves a task into the IN_PROGRESS state.
     */
    public Optional<Task> startTask(final long id) {
        return findTaskById(id).map(existing -> updateTask(existing.toBuilder()
            .withStatus(TaskStatus.IN_PROGRESS)
            .build()));
    }

    /**
     * Snoozes a task by shifting its due date. If the task does not have a due
     * date yet, the method creates one relative to {@code DateTimeUtils#now()}.
     */
    public Optional<Task> snoozeTask(final long id, final Duration duration) {
        if (duration == null || duration.isNegative() || duration.isZero()) {
            throw new IllegalArgumentException("Duration must be positive");
        }
        return findTaskById(id).map(existing -> {
            LocalDateTime base = existing.getDueDateTime();
            if (base == null) {
                base = DateTimeUtils.now();
            }
            return updateTask(existing.toBuilder()
                .withDueDateTime(base.plus(duration))
                .withStatus(TaskStatus.PLANNED)
                .build());
        });
    }

    /**
     * Returns a dashboard snapshot grouping tasks into Today, Upcoming, Someday
     * etc. Completed tasks are presented in their own lane.
     */
    public TaskDashboardSnapshot buildDashboard(final LocalDate referenceDate, final int upcomingDays) {
        Objects.requireNonNull(referenceDate, "referenceDate");
        if (upcomingDays < 0) {
            throw new IllegalArgumentException("upcomingDays must not be negative");
        }

        final Map<TaskLane, List<Task>> lanes = new EnumMap<>(TaskLane.class);
        for (TaskLane lane : TaskLane.values()) {
            lanes.put(lane, new ArrayList<>());
        }

        final LocalDateTime todayStart = referenceDate.atStartOfDay();
        final LocalDateTime todayEnd = referenceDate.atTime(LocalTime.MAX);
        final LocalDateTime upcomingLimit = referenceDate.plusDays(upcomingDays).atTime(LocalTime.MAX);

        for (Task task : listAllTasks()) {
            if (task.getStatus() == TaskStatus.CANCELLED) {
                continue;
            }
            if (task.getStatus() == TaskStatus.COMPLETED) {
                lanes.get(TaskLane.COMPLETED).add(task);
                continue;
            }

            if (task.getType() == TaskType.COURSE) {
                lanes.get(TaskLane.COURSE).add(task);
                continue;
            }
            if (task.getType() == TaskType.ANNIVERSARY) {
                lanes.get(TaskLane.ANNIVERSARY).add(task);
                continue;
            }

            final LocalDateTime due = task.getDueDateTime();
            if (due != null) {
                if (due.isBefore(todayStart)) {
                    lanes.get(TaskLane.OVERDUE).add(task);
                } else if (!due.isAfter(todayEnd)) {
                    lanes.get(TaskLane.TODAY).add(task);
                } else if (!due.isAfter(upcomingLimit)) {
                    lanes.get(TaskLane.UPCOMING).add(task);
                } else {
                    lanes.get(TaskLane.SOMEDAY).add(task);
                }
            } else {
                lanes.get(TaskLane.SOMEDAY).add(task);
            }
        }

        lanes.replaceAll((lane, list) -> list.stream()
            .sorted(Comparator
                .comparing(Task::getDueDateTime, Comparator.nullsLast(LocalDateTime::compareTo))
                .thenComparing(task -> task.getPriority().getLevel(), Comparator.reverseOrder()))
            .toList());

        return new TaskDashboardSnapshot(referenceDate, lanes);
    }

    /**
     * Convenience wrapper that augments the dashboard data with board specific
     * metadata (标题、描述、配色) so the任务面板可以直接消费。
     */
    public List<TaskBoardColumn> buildBoard(final LocalDate referenceDate, final int upcomingDays) {
        return buildDashboard(referenceDate, upcomingDays).toBoardColumns();
    }

    /**
     * Builds a single board column for the specified lane which is handy when
     * the UI only needs to刷新某一列.
     */
    public TaskBoardColumn buildBoardLane(final TaskLane lane, final LocalDate referenceDate,
                                          final int upcomingDays) {
        Objects.requireNonNull(lane, "lane");
        return buildDashboard(referenceDate, upcomingDays).toBoardColumn(lane);
    }

    /**
     * Flexible filter that allows callers to constrain tasks by type, status,
     * minimum priority and date range.
     */
    public List<Task> filterTasks(final TaskType type, final TaskStatus status,
                                  final TaskPriority minimumPriority,
                                  final LocalDate from, final LocalDate to) {
        return listAllTasks().stream()
            .filter(task -> type == null || task.getType() == type)
            .filter(task -> status == null || task.getStatus() == status)
            .filter(task -> minimumPriority == null || task.getPriority().getLevel() >= minimumPriority.getLevel())
            .filter(task -> {
                if (from == null && to == null) {
                    return true;
                }
                final LocalDateTime due = task.getDueDateTime();
                if (due == null) {
                    return false;
                }
                final boolean afterFrom = from == null || !due.toLocalDate().isBefore(from);
                final boolean beforeTo = to == null || !due.toLocalDate().isAfter(to);
                return afterFrom && beforeTo;
            })
            .toList();
    }

    @Override
    public List<Task> fetchTasksRequiringReminder(final LocalDateTime referenceTime) {
        Objects.requireNonNull(referenceTime, "referenceTime");
        final List<Task> candidates = new ArrayList<>();
        final String sql = SELECT_BASE_SQL + " WHERE reminder_enabled = 1 AND due_at IS NOT NULL";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                final Task task = mapRow(resultSet);
                if (shouldTriggerReminder(task, referenceTime)) {
                    candidates.add(task);
                }
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to fetch reminders", ex);
        }
        return candidates;
    }

    @Override
    public void markReminderTriggered(final Task task, final LocalDateTime reminderTime) {
        Objects.requireNonNull(task, "task");
        if (task.getId() == null) {
            return;
        }
        final String sql = "UPDATE tasks SET last_reminded_at = ?, updated_at = ? WHERE id = ?";
        final String timestamp = format(reminderTime);
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, timestamp);
            statement.setString(2, timestamp);
            statement.setLong(3, task.getId());
            statement.executeUpdate();
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to mark reminder as triggered", ex);
        }
    }

    private boolean shouldTriggerReminder(final Task task, final LocalDateTime referenceTime) {
        if (!task.isReminderEnabled()) {
            return false;
        }
        if (task.getStatus() == TaskStatus.COMPLETED || task.getStatus() == TaskStatus.CANCELLED) {
            return false;
        }
        final LocalDateTime due = task.getDueDateTime();
        if (due == null) {
            return false;
        }
        final LocalDateTime reminderWindowStart = due.minusMinutes(task.getReminderLeadMinutes());
        if (referenceTime.isBefore(reminderWindowStart)) {
            return false;
        }
        final LocalDateTime lastReminded = task.getLastRemindedAt();
        return lastReminded == null || lastReminded.isBefore(reminderWindowStart);
    }

    private TaskEntity toEntity(final Task task) {
        final TaskEntity entity = new TaskEntity();
        entity.setId(task.getId());
        entity.setTitle(task.getTitle());
        entity.setDescription(task.getDescription());
        entity.setStartAt(format(task.getStartDateTime()));
        entity.setDueAt(format(task.getDueDateTime()));
        entity.setPriority(task.getPriority().getLevel());
        entity.setType(task.getType().name());
        entity.setReminderEnabled(task.isReminderEnabled());
        entity.setReminderLeadMinutes(task.getReminderLeadMinutes());
        entity.setStatus(task.getStatus().name());
        entity.setLastRemindedAt(format(task.getLastRemindedAt()));
        entity.setCreatedAt(format(task.getCreatedAt()));
        entity.setUpdatedAt(format(task.getUpdatedAt()));
        return entity;
    }

    private Task fromEntity(final TaskEntity entity) {
        return Task.builder()
            .withId(entity.getId())
            .withTitle(entity.getTitle())
            .withDescription(entity.getDescription())
            .withStartDateTime(parse(entity.getStartAt()))
            .withDueDateTime(parse(entity.getDueAt()))
            .withPriority(TaskPriority.fromLevel(entity.getPriority()))
            .withType(TaskType.valueOf(entity.getType()))
            .withReminderEnabled(entity.isReminderEnabled())
            .withReminderLeadMinutes(entity.getReminderLeadMinutes())
            .withStatus(TaskStatus.valueOf(entity.getStatus()))
            .withLastRemindedAt(parse(entity.getLastRemindedAt()))
            .withCreatedAt(parse(entity.getCreatedAt()))
            .withUpdatedAt(parse(entity.getUpdatedAt()))
            .build();
    }

    private Task mapRow(final ResultSet resultSet) throws SQLException {
        final TaskEntity entity = new TaskEntity();
        entity.setId(resultSet.getLong("id"));
        entity.setTitle(resultSet.getString("title"));
        entity.setDescription(resultSet.getString("description"));
        entity.setStartAt(resultSet.getString("start_at"));
        entity.setDueAt(resultSet.getString("due_at"));
        entity.setPriority(resultSet.getInt("priority"));
        entity.setType(resultSet.getString("type"));
        entity.setReminderEnabled(resultSet.getInt("reminder_enabled") == 1);
        entity.setReminderLeadMinutes(resultSet.getInt("reminder_lead_minutes"));
        entity.setStatus(resultSet.getString("status"));
        entity.setLastRemindedAt(resultSet.getString("last_reminded_at"));
        entity.setCreatedAt(resultSet.getString("created_at"));
        entity.setUpdatedAt(resultSet.getString("updated_at"));
        return fromEntity(entity);
    }

    private void bindEntity(final PreparedStatement statement, final TaskEntity entity) throws SQLException {
        statement.setString(1, entity.getTitle());
        statement.setString(2, entity.getDescription());
        statement.setString(3, entity.getStartAt());
        statement.setString(4, entity.getDueAt());
        statement.setInt(5, entity.getPriority());
        statement.setString(6, entity.getType());
        statement.setInt(7, entity.isReminderEnabled() ? 1 : 0);
        statement.setInt(8, entity.getReminderLeadMinutes());
        statement.setString(9, entity.getStatus());
        statement.setString(10, entity.getLastRemindedAt());
        statement.setString(11, entity.getCreatedAt());
        statement.setString(12, entity.getUpdatedAt());
    }

    private Task normaliseForCreate(final Task task, final LocalDateTime now) {
        final TaskStatus status = task.getStatus() == null ? TaskStatus.PLANNED : task.getStatus();
        return task.toBuilder()
            .withStatus(status)
            .withCreatedAt(task.getCreatedAt() == null ? now : task.getCreatedAt())
            .withUpdatedAt(now)
            .build();
    }

    private static String format(final LocalDateTime value) {
        return value == null ? null : DATE_TIME_FORMATTER.format(value);
    }

    private static LocalDateTime parse(final String value) {
        return value == null ? null : LocalDateTime.parse(value, DATE_TIME_FORMATTER);
    }
}
