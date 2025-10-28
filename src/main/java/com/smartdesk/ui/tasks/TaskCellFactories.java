package com.smartdesk.ui.tasks;

import com.smartdesk.core.task.model.TaskPriority;
import com.smartdesk.core.task.model.TaskStatus;
import com.smartdesk.core.task.model.TaskType;
import java.util.Map;
import javafx.scene.control.ListCell;

/**
 * Utility class providing consistent ComboBox/ListView cell renderers.
 */
final class TaskCellFactories {

    private TaskCellFactories() {
    }

    static ListCell<TaskType> createTypeCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(final TaskType item, final boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : switch (item) {
                    case TODO -> "待办任务";
                    case COURSE -> "课程计划";
                    case ANNIVERSARY -> "纪念日";
                    case EVENT -> "事件/日程";
                });
            }
        };
    }

    static ListCell<TaskPriority> createPriorityCell(final Map<TaskPriority, String> labels) {
        return new ListCell<>() {
            @Override
            protected void updateItem(final TaskPriority item, final boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : labels.get(item));
            }
        };
    }

    static ListCell<TaskStatus> createStatusCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(final TaskStatus item, final boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : switch (item) {
                    case PLANNED -> "计划中";
                    case IN_PROGRESS -> "进行中";
                    case COMPLETED -> "已完成";
                    case CANCELLED -> "已取消";
                });
            }
        };
    }
}
