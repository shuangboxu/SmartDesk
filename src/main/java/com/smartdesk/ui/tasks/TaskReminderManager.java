package com.smartdesk.ui.tasks;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;

/**
 * Background reminder manager that periodically scans tasks and surfaces
 * desktop notifications when a task is approaching its deadline.
 */
public final class TaskReminderManager {

    private final ObservableList<TaskViewModel> tasks;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "task-reminder-thread");
        thread.setDaemon(true);
        return thread;
    });

    public TaskReminderManager(final ObservableList<TaskViewModel> tasks) {
        this.tasks = tasks;
        tasks.addListener((ListChangeListener<TaskViewModel>) change -> resetReminderStateForRemoved(change));
        scheduler.scheduleAtFixedRate(this::scanAndNotify, 5, 30, TimeUnit.SECONDS);
    }

    private void resetReminderStateForRemoved(final ListChangeListener.Change<? extends TaskViewModel> change) {
        while (change.next()) {
            if (change.wasRemoved()) {
                change.getRemoved().forEach(TaskViewModel::resetReminderState);
            }
        }
    }

    private void scanAndNotify() {
        LocalDateTime now = LocalDateTime.now();
        List<TaskViewModel> dueSoon = new ArrayList<>();
        for (TaskViewModel task : tasks) {
            if (!task.isReminderEnabled()) {
                continue;
            }
            if (task.isReminderTriggered()) {
                continue;
            }
            if (task.getDueDateTime() == null) {
                continue;
            }
            if (task.getStatus() == com.smartdesk.core.task.model.TaskStatus.COMPLETED
                || task.getStatus() == com.smartdesk.core.task.model.TaskStatus.CANCELLED) {
                continue;
            }
            Duration window = Duration.ofMinutes(task.getReminderLeadMinutes());
            if (task.isDueWithin(now, window)) {
                dueSoon.add(task);
            }
        }
        if (!dueSoon.isEmpty()) {
            Platform.runLater(() -> dueSoon.forEach(task -> presentNotification(task, now)));
        }
    }

    private void presentNotification(final TaskViewModel task, final LocalDateTime now) {
        task.markReminderTriggered(now);
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("任务提醒");
        alert.setHeaderText(task.getTitle());
        alert.setContentText("任务将在 " + task.getReminderLeadMinutes() + " 分钟后到期：" + task.getFormattedDueDate());
        ButtonType completeButton = new ButtonType("标记完成", ButtonData.OK_DONE);
        ButtonType snoozeButton = new ButtonType("稍后提醒", ButtonData.OTHER);
        alert.getButtonTypes().setAll(completeButton, snoozeButton, ButtonType.CLOSE);
        alert.initModality(javafx.stage.Modality.NONE);
        alert.setOnHidden(evt -> {
            ButtonType result = alert.getResult();
            if (result == completeButton) {
                task.setStatus(com.smartdesk.core.task.model.TaskStatus.COMPLETED);
                task.setReminderEnabled(false);
            } else if (result == snoozeButton) {
                task.setReminderTriggered(false);
                task.setReminderLeadMinutes(Math.max(5, task.getReminderLeadMinutes() / 2));
            }
        });
        alert.show();
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }
}
