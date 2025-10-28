package com.smartdesk.core.task.scheduler;

import com.smartdesk.core.task.model.Task;
import com.smartdesk.core.task.service.TaskQuerySupport;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Background scheduler that polls the database for tasks that require a
 * reminder. The scheduler is lightweight and designed to run even when the UI
 * is closed; callers can listen for {@link ReminderListener} callbacks to show
 * notifications or trigger sounds.
 */
public class ReminderScheduler implements AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger(ReminderScheduler.class.getName());
    private static final Duration DEFAULT_SCAN_INTERVAL = Duration.ofMinutes(1);

    private final TaskQuerySupport taskQuerySupport;
    private final ScheduledExecutorService executor;
    private final Set<ReminderListener> listeners = new CopyOnWriteArraySet<>();

    private ScheduledFuture<?> scheduledFuture;
    private Duration scanInterval = DEFAULT_SCAN_INTERVAL;

    public ReminderScheduler(final TaskQuerySupport taskQuerySupport) {
        this.taskQuerySupport = Objects.requireNonNull(taskQuerySupport, "taskQuerySupport");
        this.executor = Executors.newSingleThreadScheduledExecutor(new DaemonThreadFactory());
    }

    /**
     * Configures how often the scheduler scans for due tasks. The value must be
     * at least one minute to prevent excessive database load.
     */
    public void setScanInterval(final Duration interval) {
        if (interval == null || interval.compareTo(Duration.ofMinutes(1)) < 0) {
            throw new IllegalArgumentException("Scan interval must be at least one minute");
        }
        this.scanInterval = interval;
        restart();
    }

    /**
     * Starts polling for reminders. Calling start multiple times is safe and
     * will simply restart the polling cycle.
     */
    public synchronized void start() {
        restart();
    }

    private synchronized void restart() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
        }
        scheduledFuture = executor.scheduleAtFixedRate(this::scanAndNotify, 0,
            scanInterval.toMinutes(), TimeUnit.MINUTES);
    }

    private void scanAndNotify() {
        try {
            final LocalDateTime now = LocalDateTime.now();
            final var dueTasks = taskQuerySupport.fetchTasksRequiringReminder(now);
            for (Task task : dueTasks) {
                final LocalDateTime dueDate = task.getDueDateTime();
                final Duration remaining = dueDate == null ? Duration.ZERO : Duration.between(now, dueDate);
                notifyListeners(task, remaining);
                taskQuerySupport.markReminderTriggered(task, now);
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Failed to execute reminder scan", ex);
        }
    }

    private void notifyListeners(final Task task, final Duration remaining) {
        for (ReminderListener listener : listeners) {
            try {
                listener.onReminder(task, remaining);
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Reminder listener threw exception", ex);
            }
        }
    }

    public void addReminderListener(final ReminderListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeReminderListener(final ReminderListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    @Override
    public void close() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
        }
        executor.shutdownNow();
    }

    /**
     * Listener interface that UI components can implement to receive reminder
     * notifications.
     */
    public interface ReminderListener {
        void onReminder(Task task, Duration remainingUntilDue);
    }

    private static final class DaemonThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(final Runnable r) {
            final Thread thread = new Thread(r, "smartdesk-reminder-scheduler");
            thread.setDaemon(true);
            return thread;
        }
    }
}
