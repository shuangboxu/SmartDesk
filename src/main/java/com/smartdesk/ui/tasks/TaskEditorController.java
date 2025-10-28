package com.smartdesk.ui.tasks;

import com.smartdesk.core.task.model.TaskPriority;
import com.smartdesk.core.task.model.TaskStatus;
import com.smartdesk.core.task.model.TaskType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumMap;
import java.util.Map;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

/**
 * Controller backing the task editor dialog.
 */
public class TaskEditorController {

    private static final Map<TaskPriority, String> PRIORITY_LABELS = new EnumMap<>(TaskPriority.class);

    static {
        PRIORITY_LABELS.put(TaskPriority.LOW, "1 · 轻松");
        PRIORITY_LABELS.put(TaskPriority.NORMAL, "2 · 常规");
        PRIORITY_LABELS.put(TaskPriority.HIGH, "3 · 紧急");
        PRIORITY_LABELS.put(TaskPriority.URGENT, "4 · 很紧急");
        PRIORITY_LABELS.put(TaskPriority.CRITICAL, "5 · 紧迫");
    }

    @FXML
    private TextField titleField;
    @FXML
    private ComboBox<TaskType> typeCombo;
    @FXML
    private ComboBox<TaskPriority> priorityCombo;
    @FXML
    private ComboBox<TaskStatus> statusCombo;
    @FXML
    private DatePicker startDatePicker;
    @FXML
    private DatePicker dueDatePicker;
    @FXML
    private Spinner<Integer> dueHourSpinner;
    @FXML
    private Spinner<Integer> dueMinuteSpinner;
    @FXML
    private CheckBox reminderToggle;
    @FXML
    private Spinner<Integer> leadMinutesSpinner;
    @FXML
    private TextArea descriptionArea;

    private TaskViewModel task;

    @FXML
    private void initialize() {
        typeCombo.setItems(FXCollections.observableArrayList(TaskType.values()));
        typeCombo.setButtonCell(TaskCellFactories.createTypeCell());
        typeCombo.setCellFactory(list -> TaskCellFactories.createTypeCell());

        priorityCombo.setItems(FXCollections.observableArrayList(TaskPriority.values()));
        priorityCombo.setButtonCell(TaskCellFactories.createPriorityCell(PRIORITY_LABELS));
        priorityCombo.setCellFactory(list -> TaskCellFactories.createPriorityCell(PRIORITY_LABELS));

        statusCombo.setItems(FXCollections.observableArrayList(TaskStatus.values()));
        statusCombo.setButtonCell(TaskCellFactories.createStatusCell());
        statusCombo.setCellFactory(list -> TaskCellFactories.createStatusCell());

        dueHourSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 9));
        dueMinuteSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0, 5));
        leadMinutesSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(5, 240, 30, 5));

        reminderToggle.selectedProperty().addListener((obs, oldValue, newValue) -> {
            leadMinutesSpinner.setDisable(!newValue);
        });
    }

    public void setTask(final TaskViewModel task) {
        this.task = task;
        populateFields();
    }

    private void populateFields() {
        if (task == null) {
            return;
        }
        titleField.setText(task.getTitle());
        typeCombo.getSelectionModel().select(task.getType());
        priorityCombo.getSelectionModel().select(task.getPriority());
        statusCombo.getSelectionModel().select(task.getStatus());
        reminderToggle.setSelected(task.isReminderEnabled());
        leadMinutesSpinner.getValueFactory().setValue(task.getReminderLeadMinutes());
        descriptionArea.setText(task.getDescription());

        LocalDateTime start = task.getStartDateTime();
        startDatePicker.setValue(start == null ? null : start.toLocalDate());

        LocalDateTime due = task.getDueDateTime();
        if (due != null) {
            dueDatePicker.setValue(due.toLocalDate());
            dueHourSpinner.getValueFactory().setValue(due.getHour());
            dueMinuteSpinner.getValueFactory().setValue(due.getMinute());
        } else {
            dueDatePicker.setValue(null);
        }

        leadMinutesSpinner.setDisable(!task.isReminderEnabled());
    }

    public TaskViewModel buildUpdatedTask() {
        if (task == null) {
            task = new TaskViewModel();
        }
        task.setTitle(titleField.getText().isBlank() ? "未命名任务" : titleField.getText().trim());
        task.setType(typeCombo.getValue() == null ? TaskType.TODO : typeCombo.getValue());
        task.setPriority(priorityCombo.getValue() == null ? TaskPriority.NORMAL : priorityCombo.getValue());
        task.setStatus(statusCombo.getValue() == null ? TaskStatus.PLANNED : statusCombo.getValue());
        task.setReminderEnabled(reminderToggle.isSelected());
        task.setReminderLeadMinutes(leadMinutesSpinner.getValue());
        task.setDescription(descriptionArea.getText());

        LocalDate startDate = startDatePicker.getValue();
        task.setStartDateTime(startDate == null ? null : startDate.atStartOfDay());

        LocalDate dueDate = dueDatePicker.getValue();
        if (dueDate != null) {
            LocalTime time = LocalTime.of(dueHourSpinner.getValue(), dueMinuteSpinner.getValue());
            task.setDueDateTime(LocalDateTime.of(dueDate, time));
        } else {
            task.setDueDateTime(null);
        }

        if (!task.isReminderEnabled()) {
            task.resetReminderState();
        }
        return task;
    }
}
