package com.smartdesk.core.task.model;

import java.util.List;
import java.util.Objects;

/**
 * Immutable view model describing one column on the task board. In addition to
 * the raw task list, the column carries display metadata such as the title,
 * colour accent and icon key so that the JavaFX 层可以直接映射到 UI 控件。
 */
public record TaskBoardColumn(
    TaskLane lane,
    String title,
    String description,
    String accentColor,
    String icon,
    List<Task> tasks
) {

    public TaskBoardColumn {
        Objects.requireNonNull(lane, "lane must not be null");
        title = title == null ? lane.getDisplayName() : title;
        description = description == null ? "" : description;
        accentColor = accentColor == null ? lane.getAccentColor() : accentColor;
        icon = icon == null ? lane.getIcon() : icon;
        tasks = tasks == null ? List.of() : List.copyOf(tasks);
    }

    /**
     * Convenience factory that fills the column metadata directly from the lane
     * definition.
     */
    public static TaskBoardColumn fromLane(final TaskLane lane, final List<Task> tasks) {
        return new TaskBoardColumn(lane, lane.getDisplayName(), lane.getDescription(),
            lane.getAccentColor(), lane.getIcon(), tasks);
    }

    /**
     * Total number of tasks present in the column.
     */
    public int totalTasksCount() {
        return tasks.size();
    }

    /**
     * Number of tasks that are already marked as completed within the column.
     */
    public long completedTasksCount() {
        return tasks.stream()
            .filter(task -> task.getStatus() == TaskStatus.COMPLETED)
            .count();
    }

    /**
     * Number of active (i.e. not completed/cancelled) tasks.
     */
    public long activeTasksCount() {
        return tasks.stream()
            .filter(task -> task.getStatus() != TaskStatus.COMPLETED)
            .filter(task -> task.getStatus() != TaskStatus.CANCELLED)
            .count();
    }

    /**
     * Completion ratio expressed as {@code 0.0 .. 1.0}. When the column is
     * empty, the ratio defaults to {@code 0.0} to avoid NaN values in the UI.
     */
    public double completionRatio() {
        final int total = totalTasksCount();
        return total == 0 ? 0D : (double) completedTasksCount() / total;
    }
}
