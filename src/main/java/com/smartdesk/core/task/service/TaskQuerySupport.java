package com.smartdesk.core.task.service;

import com.smartdesk.core.task.model.Task;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Small interface that exposes only the reminder-related query operations.
 * {@link com.smartdesk.core.task.scheduler.ReminderScheduler} depends on this to
 * keep the scheduler loosely coupled with the full service implementation.
 */
public interface TaskQuerySupport {

    List<Task> fetchTasksRequiringReminder(LocalDateTime referenceTime);

    void markReminderTriggered(Task task, LocalDateTime reminderTime);
}
