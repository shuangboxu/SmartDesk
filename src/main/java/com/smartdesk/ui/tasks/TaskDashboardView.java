package com.smartdesk.ui.tasks;

import com.smartdesk.core.task.TaskService;
import com.smartdesk.core.task.model.Task;
import com.smartdesk.core.task.model.TaskLane;
import com.smartdesk.core.task.model.TaskPriority;
import com.smartdesk.core.task.model.TaskStatus;
import com.smartdesk.core.task.model.TaskType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.control.Alert;

/**
 * Rich dashboard view dedicated to the SmartDesk task module.
 */
public final class TaskDashboardView extends BorderPane {

    private static final Logger LOGGER = Logger.getLogger(TaskDashboardView.class.getName());

    private final ObservableList<TaskViewModel> tasks;
    private final TaskService taskService;
    private final ObjectProperty<TaskType> typeFilter = new SimpleObjectProperty<>(null);
    private final ObjectProperty<TaskStatus> statusFilter = new SimpleObjectProperty<>(null);
    private final ObjectProperty<TaskPriority> minimumPriorityFilter = new SimpleObjectProperty<>(null);
    private final TextField searchField = new TextField();
    private final DatePicker calendarView = new DatePicker(LocalDate.now());
    private final ListView<TaskViewModel> upcomingList = new ListView<>();
    private final Label summaryLabel = new Label();
    private final Label reminderLabel = new Label();
    private final Map<TaskLane, TaskSectionPane> laneSections = new EnumMap<>(TaskLane.class);
    private final TaskReminderManager reminderManager;
    private final Map<TaskViewModel, List<Observable>> observedTaskProperties = new IdentityHashMap<>();
    private final InvalidationListener taskPropertyListener = obs -> refresh();

    public TaskDashboardView(final ObservableList<TaskViewModel> tasks, final TaskService taskService) {
        this.tasks = Objects.requireNonNull(tasks, "tasks");
        this.taskService = Objects.requireNonNull(taskService, "taskService");
        getStyleClass().add("task-dashboard-root");

        setPadding(new Insets(16));
        setTop(buildHeaderBar());
        setLeft(buildCalendarPane());
        setCenter(buildSections());

        tasks.forEach(this::registerTaskObservers);
        tasks.addListener((ListChangeListener<TaskViewModel>) change -> {
            while (change.next()) {
                if (change.wasRemoved()) {
                    change.getRemoved().forEach(this::unregisterTaskObservers);
                }
                if (change.wasAdded()) {
                    change.getAddedSubList().forEach(this::registerTaskObservers);
                }
            }
            refresh();
        });
        calendarView.valueProperty().addListener((obs, oldValue, newValue) -> refresh());
        typeFilter.addListener((obs, oldValue, newValue) -> refresh());
        statusFilter.addListener((obs, oldValue, newValue) -> refresh());
        minimumPriorityFilter.addListener((obs, oldValue, newValue) -> refresh());
        searchField.textProperty().addListener((obs, oldText, newText) -> refresh());

        reminderManager = new TaskReminderManager(tasks, taskService);

        refresh();
    }

    public TaskReminderManager getReminderManager() {
        return reminderManager;
    }

    private void registerTaskObservers(final TaskViewModel task) {
        if (task == null || observedTaskProperties.containsKey(task)) {
            return;
        }
        List<Observable> observables = List.of(
            task.titleProperty(),
            task.descriptionProperty(),
            task.typeProperty(),
            task.statusProperty(),
            task.priorityProperty(),
            task.startDateTimeProperty(),
            task.dueDateTimeProperty(),
            task.reminderEnabledProperty(),
            task.reminderLeadMinutesProperty(),
            task.lastRemindedAtProperty(),
            task.reminderTriggeredProperty(),
            task.updatedAtProperty()
        );
        observables.forEach(observable -> observable.addListener(taskPropertyListener));
        observedTaskProperties.put(task, observables);
    }

    private void unregisterTaskObservers(final TaskViewModel task) {
        List<Observable> observables = observedTaskProperties.remove(task);
        if (observables == null) {
            return;
        }
        observables.forEach(observable -> observable.removeListener(taskPropertyListener));
    }

    private Node buildHeaderBar() {
        VBox wrapper = new VBox(12);

        Label title = new Label("任务调度中心");
        title.getStyleClass().add("task-dashboard-title");

        summaryLabel.getStyleClass().add("task-dashboard-summary");

        HBox filters = new HBox(12);
        filters.setAlignment(Pos.CENTER_LEFT);

        Button addButton = new Button("新建任务");
        addButton.getStyleClass().add("accent-button");
        addButton.setOnAction(evt -> openEditor(null));

        ComboBox<TaskType> typeCombo = new ComboBox<>();
        typeCombo.getItems().add(null);
        typeCombo.getItems().addAll(TaskType.values());
        typeCombo.setValue(null);
        typeCombo.setPromptText("任务类型");
        typeCombo.setButtonCell(new FilterCell<>("全部类型"));
        typeCombo.setCellFactory(list -> new FilterCell<>("全部类型"));
        typeFilter.bind(typeCombo.valueProperty());

        ComboBox<TaskStatus> statusCombo = new ComboBox<>();
        statusCombo.getItems().add(null);
        statusCombo.getItems().addAll(TaskStatus.values());
        statusCombo.setPromptText("任务状态");
        statusCombo.setButtonCell(new FilterCell<>("全部状态"));
        statusCombo.setCellFactory(list -> new FilterCell<>("全部状态"));
        statusFilter.bind(statusCombo.valueProperty());

        ComboBox<TaskPriority> priorityCombo = new ComboBox<>();
        priorityCombo.getItems().add(null);
        priorityCombo.getItems().addAll(TaskPriority.values());
        priorityCombo.setPromptText("最低优先级");
        priorityCombo.setButtonCell(new FilterCell<>("不限"));
        priorityCombo.setCellFactory(list -> new FilterCell<>("不限"));
        minimumPriorityFilter.bind(priorityCombo.valueProperty());

        searchField.setPromptText("搜索标题或描述");
        searchField.getStyleClass().add("task-dashboard-search");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        filters.getChildren().addAll(addButton, new Separator(), typeCombo, statusCombo,
            priorityCombo, searchField);

        wrapper.getChildren().addAll(title, summaryLabel, filters);
        return wrapper;
    }

    private Node buildCalendarPane() {
        VBox left = new VBox(16);
        left.setPadding(new Insets(12));
        left.getStyleClass().add("task-dashboard-side");

        Label calendarTitle = new Label("日程视图");
        calendarTitle.getStyleClass().add("task-side-title");

        calendarView.setShowWeekNumbers(false);
        calendarView.setDayCellFactory(picker -> new CalendarDayCell());

        VBox calendarBox = new VBox(8, calendarTitle, calendarView);

        Label reminderTitle = new Label("即将提醒");
        reminderTitle.getStyleClass().add("task-side-title");

        upcomingList.setPlaceholder(new Label("暂无即将到期的任务"));
        upcomingList.setCellFactory(list -> new UpcomingTaskCell());
        upcomingList.prefHeightProperty().bind(heightProperty().multiply(0.45));

        reminderLabel.getStyleClass().add("task-side-hint");

        left.getChildren().addAll(calendarBox, reminderTitle, upcomingList, reminderLabel);
        VBox.setVgrow(upcomingList, Priority.ALWAYS);
        return left;
    }

    private Node buildSections() {
        VBox container = new VBox(18);
        container.setPadding(new Insets(0, 0, 0, 16));

        for (TaskLane lane : List.of(TaskLane.OVERDUE, TaskLane.TODAY, TaskLane.UPCOMING,
            TaskLane.SOMEDAY, TaskLane.COURSE, TaskLane.ANNIVERSARY, TaskLane.COMPLETED)) {
            TaskSectionPane pane = new TaskSectionPane(lane);
            laneSections.put(lane, pane);
            container.getChildren().add(pane);
        }

        ScrollPane scrollPane = new ScrollPane(container);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("task-dashboard-scroll");
        return scrollPane;
    }

    private void refresh() {
        LocalDate referenceDate = calendarView.getValue() == null ? LocalDate.now() : calendarView.getValue();
        LocalDateTime now = LocalDateTime.now();
        Predicate<TaskViewModel> predicate = task -> {
            if (typeFilter.get() != null && task.getType() != typeFilter.get()) {
                return false;
            }
            if (statusFilter.get() != null && task.getStatus() != statusFilter.get()) {
                return false;
            }
            if (minimumPriorityFilter.get() != null
                && task.getPriority().getLevel() < minimumPriorityFilter.get().getLevel()) {
                return false;
            }
            String keyword = searchField.getText();
            if (keyword != null && !keyword.isBlank()) {
                String lower = keyword.toLowerCase();
                if (!(task.getTitle().toLowerCase().contains(lower)
                    || task.getDescription().toLowerCase().contains(lower))) {
                    return false;
                }
            }
            return true;
        };

        List<TaskViewModel> filtered = tasks.stream()
            .filter(predicate)
            .sorted(Comparator.comparing(TaskViewModel::getDueDateTime,
                Comparator.nullsLast(LocalDateTime::compareTo))
                .thenComparing(task -> task.getPriority().getLevel(), Comparator.reverseOrder()))
            .collect(Collectors.toList());

        Map<TaskLane, List<TaskViewModel>> laneMap = new EnumMap<>(TaskLane.class);
        for (TaskLane lane : laneSections.keySet()) {
            laneMap.put(lane, new ArrayList<>());
        }

        for (TaskViewModel task : filtered) {
            if (task.getStatus() == TaskStatus.COMPLETED) {
                laneMap.get(TaskLane.COMPLETED).add(task);
                continue;
            }
            if (task.getStatus() == TaskStatus.CANCELLED) {
                laneMap.get(TaskLane.SOMEDAY).add(task);
                continue;
            }
            if (task.isOverdue(now)) {
                laneMap.get(TaskLane.OVERDUE).add(task);
                continue;
            }
            if (task.getType() == TaskType.COURSE) {
                laneMap.get(TaskLane.COURSE).add(task);
                continue;
            }
            if (task.getType() == TaskType.ANNIVERSARY) {
                laneMap.get(TaskLane.ANNIVERSARY).add(task);
                continue;
            }
            LocalDateTime due = task.getDueDateTime();
            if (due == null) {
                laneMap.get(TaskLane.SOMEDAY).add(task);
                continue;
            }
            LocalDate dueDate = due.toLocalDate();
            if (dueDate.isEqual(referenceDate)) {
                laneMap.get(TaskLane.TODAY).add(task);
            } else if (!dueDate.isBefore(referenceDate) && !dueDate.isAfter(referenceDate.plusDays(5))) {
                laneMap.get(TaskLane.UPCOMING).add(task);
            } else if (dueDate.isAfter(referenceDate.plusDays(5))) {
                laneMap.get(TaskLane.SOMEDAY).add(task);
            }
        }

        int total = filtered.size();
        long completed = filtered.stream().filter(task -> task.getStatus() == TaskStatus.COMPLETED).count();
        long inProgress = filtered.stream().filter(task -> task.getStatus() == TaskStatus.IN_PROGRESS).count();
        long planned = filtered.stream().filter(task -> task.getStatus() == TaskStatus.PLANNED).count();
        summaryLabel.setText(String.format("共 %d 项 · 进行中 %d · 计划中 %d · 已完成 %d",
            total, inProgress, planned, completed));

        laneSections.forEach((lane, pane) -> pane.updateTasks(laneMap.getOrDefault(lane, List.of())));

        List<TaskViewModel> upcoming = filtered.stream()
            .filter(task -> task.getDueDateTime() != null)
            .filter(task -> task.getStatus() != TaskStatus.COMPLETED)
            .filter(task -> !task.isOverdue(now))
            .sorted(Comparator.comparing(TaskViewModel::getDueDateTime))
            .limit(5)
            .collect(Collectors.toList());
        upcomingList.getItems().setAll(upcoming);

        long reminderEnabledCount = filtered.stream().filter(TaskViewModel::isReminderEnabled).count();
        reminderLabel.setText("已开启提醒的任务：" + reminderEnabledCount + " 条");

        calendarView.setDayCellFactory(picker -> new CalendarDayCell());
    }

    private void openEditor(final TaskViewModel taskToEdit) {
        TaskEditorDialog dialog = new TaskEditorDialog(taskToEdit);
        dialog.showAndAwaitResult().ifPresent(updated -> {
            try {
                if (taskToEdit == null) {
                    Task created = taskService.createTask(updated.toDomain());
                    TaskViewModel persisted = TaskViewModel.fromDomain(created);
                    tasks.add(persisted);
                } else {
                    Task persisted = taskService.updateTask(updated.toDomain());
                    taskToEdit.applyDomain(persisted);
                }
                refresh();
            } catch (IllegalStateException ex) {
                LOGGER.log(Level.SEVERE, "Failed to persist task changes", ex);
                showError("保存任务失败", "请稍后再试或检查日志。");
            }
        });
    }

    private void showError(final String title, final String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private final class TaskSectionPane extends VBox {
        private final TaskLane lane;
        private final Label header = new Label();
        private final FlowPane tags = new FlowPane(8, 4);
        private final ListView<TaskViewModel> listView = new ListView<>();

        TaskSectionPane(final TaskLane lane) {
            this.lane = lane;
            getStyleClass().add("task-section");
            setSpacing(12);

            header.getStyleClass().add("task-section-header");
            tags.getStyleClass().add("task-section-tags");

            listView.setPlaceholder(new Label("暂无任务"));
            listView.setCellFactory(list -> new TaskCardCell());
            listView.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) {
                    TaskViewModel selected = listView.getSelectionModel().getSelectedItem();
                    if (selected != null) {
                        openEditor(selected);
                    }
                }
            });
            VBox.setVgrow(listView, Priority.ALWAYS);

            getChildren().addAll(header, tags, listView);
        }

        void updateTasks(final List<TaskViewModel> data) {
            header.setText(lane.getDisplayName() + " · " + data.size());
            Label info = new Label(lane.getDescription());
            info.getStyleClass().add("task-section-tag-text");
            tags.getChildren().setAll(info);
            tags.setStyle("-fx-border-color: " + lane.getAccentColor() + ";");
            listView.getItems().setAll(data);
        }
    }

    private final class TaskCardCell extends ListCell<TaskViewModel> {
        private final Label title = new Label();
        private final Label meta = new Label();
        private final Label description = new Label();
        private final HBox actions = new HBox(8);
        private final Button editButton = new Button("编辑");
        private final Button completeButton = new Button("完成");
        private final VBox container = new VBox(6);

        TaskCardCell() {
            getStyleClass().add("task-card-cell");
            title.getStyleClass().add("task-card-title");
            meta.getStyleClass().add("task-card-meta");
            description.getStyleClass().add("task-card-description");
            actions.getStyleClass().add("task-card-actions");

            editButton.getStyleClass().add("task-card-button");
            editButton.setOnAction(evt -> openEditor(getItem()));

            completeButton.getStyleClass().add("task-card-button");
            completeButton.setOnAction(evt -> {
                TaskViewModel item = getItem();
                if (item != null && item.isPersisted()) {
                    try {
                        Optional<Task> updated = taskService.markTaskCompleted(item.getId());
                        updated.ifPresent(item::applyDomain);
                        TaskDashboardView.this.refresh();
                    } catch (IllegalStateException ex) {
                        LOGGER.log(Level.SEVERE, "Failed to mark task completed", ex);
                        showError("更新任务失败", "无法标记任务完成，请稍后再试。");
                    }
                }
            });

            actions.getChildren().addAll(editButton, completeButton);

            container.getChildren().addAll(title, meta, description, actions);
            container.getStyleClass().add("task-card-container");
        }

        @Override
        protected void updateItem(final TaskViewModel item, final boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                completeButton.disableProperty().unbind();
                setGraphic(null);
            } else {
                title.setText(item.getTitle());
                String priority = switch (item.getPriority()) {
                    case LOW -> "优先级 1";
                    case NORMAL -> "优先级 2";
                    case HIGH -> "优先级 3";
                    case URGENT -> "优先级 4";
                    case CRITICAL -> "优先级 5";
                };
                meta.setText(item.getFormattedDueDate() + " · " + priority + " · " + statusLabel(item));
                description.setText(item.getDescription() == null || item.getDescription().isBlank()
                    ? "暂无描述" : item.getDescription());
                completeButton.disableProperty().unbind();
                completeButton.disableProperty().bind(Bindings.createBooleanBinding(
                    () -> item.getStatus() == TaskStatus.COMPLETED,
                    item.statusProperty()));
                setGraphic(container);
            }
        }

        private String statusLabel(final TaskViewModel item) {
            return switch (item.getStatus()) {
                case PLANNED -> "计划中";
                case IN_PROGRESS -> "进行中";
                case COMPLETED -> "已完成";
                case CANCELLED -> "已取消";
            };
        }
    }

    private final class UpcomingTaskCell extends ListCell<TaskViewModel> {
        UpcomingTaskCell() {
            getStyleClass().add("task-upcoming-cell");
        }

        @Override
        protected void updateItem(final TaskViewModel item, final boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
            } else {
                setText(item.getFormattedDueDate() + "  ·  " + item.getTitle());
            }
        }
    }

    private final class CalendarDayCell extends DateCell {
        @Override
        public void updateItem(final LocalDate item, final boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setStyle("");
                setTooltip(null);
                return;
            }
            long count = tasks.stream().filter(task -> task.occursOn(item)).count();
            if (count > 0) {
                setStyle("-fx-background-color: rgba(63, 81, 181, 0.15);");
                setTooltip(new javafx.scene.control.Tooltip("共有 " + count + " 个任务"));
            } else {
                setStyle("");
                setTooltip(null);
            }
        }
    }

    private static final class FilterCell<T> extends ListCell<T> {
        private final String placeholder;

        private FilterCell(final String placeholder) {
            this.placeholder = placeholder;
        }

        @Override
        protected void updateItem(final T item, final boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setText(null);
            } else if (item == null) {
                setText(placeholder);
            } else {
                if (item instanceof TaskType taskType) {
                    setText(switch (taskType) {
                        case TODO -> "待办任务";
                        case COURSE -> "课程计划";
                        case ANNIVERSARY -> "纪念日";
                        case EVENT -> "事件/日程";
                    });
                } else if (item instanceof TaskStatus taskStatus) {
                    setText(switch (taskStatus) {
                        case PLANNED -> "计划中";
                        case IN_PROGRESS -> "进行中";
                        case COMPLETED -> "已完成";
                        case CANCELLED -> "已取消";
                    });
                } else if (item instanceof TaskPriority taskPriority) {
                    setText("≥ 优先级 " + taskPriority.getLevel());
                } else {
                    setText(item.toString());
                }
            }
        }
    }
}
