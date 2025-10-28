package com.smartdesk.ui.tasks;

import java.io.IOException;
import java.util.Optional;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.stage.Modality;

/**
 * Dialog used to create or edit a task.
 */
public class TaskEditorDialog extends Dialog<TaskViewModel> {

    private final TaskEditorController controller;

    public TaskEditorDialog(final TaskViewModel initialTask) {
        setTitle(initialTask == null ? "新建任务" : "编辑任务");
        setHeaderText("设置任务信息");
        initModality(Modality.APPLICATION_MODAL);

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/smartdesk/resources/fxml/task-editor-dialog.fxml"));
            DialogPane pane = loader.load();
            controller = loader.getController();
            controller.setTask(initialTask == null ? new TaskViewModel() : initialTask.copy());
            setDialogPane(pane);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load task editor dialog", e);
        }

        getDialogPane().lookupButton(ButtonType.OK).getStyleClass().add("accent-button");

        setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                return controller.buildUpdatedTask();
            }
            return null;
        });
    }

    public Optional<TaskViewModel> showAndAwaitResult() {
        return Optional.ofNullable(showAndWait().orElse(null));
    }
}
