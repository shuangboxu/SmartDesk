package com.smartdesk.ui;

import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javafx.geometry.Insets;

/**
 * Main application entry point for SmartDesk.
 */
public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("SmartDesk");

        TabPane tabPane = new TabPane();
        tabPane.getTabs().add(createNotesTab());
        tabPane.getTabs().add(createTaskTab());
        tabPane.getTabs().add(createTab("聊天", "聊天模块即将上线"));
        tabPane.getTabs().add(createTab("总结", "总结模块即将上线"));
        tabPane.getTabs().add(createTab("设置", "设置模块即将上线"));

        BorderPane root = new BorderPane(tabPane);
        Scene scene = new Scene(root, 960, 600);
        scene.getStylesheets().add(
                getClass().getResource("/com/smartdesk/resources/application.css").toExternalForm()
        );
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private Tab createNotesTab() {
        Tab tab = new Tab("笔记");
        tab.setClosable(false);

        BorderPane notesLayout = new BorderPane();
        notesLayout.getStyleClass().add("notes-root");

        ObservableList<Note> notes = FXCollections.observableArrayList(
                new Note("会议记录", "讨论项目进度、风险以及下周的里程碑。", LocalDateTime.now().minusDays(1)),
                new Note("灵感捕捉", "重新设计仪表盘配色，突出重点指标。", LocalDateTime.now().minusHours(6)),
                new Note("阅读摘录", "《用户体验要素》关于信息架构的章节值得复盘。", LocalDateTime.now().minusDays(3))
        );

        ListView<Note> noteListView = new ListView<>(notes);
        noteListView.getStyleClass().add("notes-list-view");
        noteListView.setCellFactory(new Callback<>() {
            @Override
            public ListCell<Note> call(ListView<Note> listView) {
                return new ListCell<>() {
                    @Override
                    protected void updateItem(Note item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            setGraphic(null);
                        } else {
                            setText(item.getTitle());
                        }
                    }
                };
            }
        });

        VBox listContainer = new VBox(12, new Label("全部笔记"), noteListView);
        listContainer.setPadding(new Insets(16));
        listContainer.getStyleClass().add("notes-list-container");
        noteListView.setPrefWidth(240);
        VBox.setVgrow(noteListView, Priority.ALWAYS);

        Button addButton = new Button("新建");
        Button deleteButton = new Button("删除");

        HBox buttonBar = new HBox(8, addButton, deleteButton);
        buttonBar.getStyleClass().add("notes-button-bar");

        listContainer.getChildren().add(buttonBar);

        VBox detailContainer = new VBox(12);
        detailContainer.setPadding(new Insets(24));
        detailContainer.getStyleClass().add("notes-detail-container");

        Label detailHeader = new Label("笔记详情");
        detailHeader.getStyleClass().add("notes-detail-header");

        TextField titleField = new TextField();
        titleField.setPromptText("输入标题");
        titleField.getStyleClass().add("notes-title-field");

        TextArea contentArea = new TextArea();
        contentArea.setPromptText("在这里书写你的想法、会议记录或灵感……");
        contentArea.setWrapText(true);
        contentArea.setPrefRowCount(18);
        contentArea.getStyleClass().add("notes-content-area");
        VBox.setVgrow(contentArea, Priority.ALWAYS);

        Label lastUpdatedLabel = new Label();
        lastUpdatedLabel.getStyleClass().add("notes-timestamp");

        Label statusLabel = new Label("选择一条笔记以开始");
        statusLabel.getStyleClass().add("notes-status-label");
        statusLabel.setMaxWidth(Double.MAX_VALUE);

        Button saveButton = new Button("保存");
        saveButton.getStyleClass().add("accent-button");

        HBox actionBar = new HBox(12, saveButton, statusLabel);
        actionBar.getStyleClass().add("notes-action-bar");
        actionBar.setPadding(new Insets(4, 0, 0, 0));
        HBox.setHgrow(statusLabel, Priority.ALWAYS);

        detailContainer.getChildren().addAll(detailHeader, titleField, contentArea, lastUpdatedLabel, actionBar);

        notesLayout.setLeft(listContainer);
        notesLayout.setCenter(detailContainer);

        noteListView.getSelectionModel().selectedItemProperty().addListener((obs, oldNote, newNote) -> {
            if (newNote == null) {
                titleField.clear();
                contentArea.clear();
                lastUpdatedLabel.setText("没有选中的笔记");
                statusLabel.setText("选择一条笔记以开始");
            } else {
                titleField.setText(newNote.getTitle());
                contentArea.setText(newNote.getContent());
                lastUpdatedLabel.setText("最后编辑：" + newNote.getFormattedTimestamp());
                statusLabel.setText("正在编辑 " + newNote.getTitle());
            }
        });

        saveButton.disableProperty().bind(Bindings.isNull(noteListView.getSelectionModel().selectedItemProperty()));
        deleteButton.disableProperty().bind(Bindings.isNull(noteListView.getSelectionModel().selectedItemProperty()));

        addButton.setOnAction(event -> {
            Note newNote = new Note("未命名笔记", "", LocalDateTime.now());
            notes.add(0, newNote);
            noteListView.getSelectionModel().select(newNote);
            statusLabel.setText("已创建新笔记");
            titleField.requestFocus();
        });

        deleteButton.setOnAction(event -> {
            Note selectedNote = noteListView.getSelectionModel().getSelectedItem();
            if (selectedNote != null) {
                int index = noteListView.getSelectionModel().getSelectedIndex();
                notes.remove(selectedNote);
                statusLabel.setText("已删除笔记");
                if (!notes.isEmpty()) {
                    noteListView.getSelectionModel().select(Math.min(index, notes.size() - 1));
                }
            }
        });

        saveButton.setOnAction(event -> saveNote(noteListView, titleField, contentArea, lastUpdatedLabel, statusLabel));

        contentArea.setOnKeyPressed(event -> {
            if (event.isShortcutDown() && event.getCode() == KeyCode.S) {
                saveNote(noteListView, titleField, contentArea, lastUpdatedLabel, statusLabel);
            }
        });

        noteListView.getSelectionModel().selectFirst();

        tab.setContent(notesLayout);
        return tab;
    }

    private Tab createTaskTab() {
        Tab tab = new Tab("任务");
        tab.setClosable(false);

        BorderPane taskLayout = new BorderPane();
        taskLayout.getStyleClass().add("tasks-root");

        ObservableList<Task> tasks = FXCollections.observableArrayList(
                new Task("设计评审", "准备周四的设计评审资料，突出关键交互流程。", LocalDate.now().plusDays(2), Task.Priority.HIGH, Task.Status.IN_PROGRESS),
                new Task("需求梳理", "梳理 V2.3 版本需求并输出原型草稿。", LocalDate.now().plusDays(5), Task.Priority.MEDIUM, Task.Status.TODO),
                new Task("BUG 回归", "回归测试登录流程相关缺陷并记录测试结果。", LocalDate.now().plusDays(1), Task.Priority.HIGH, Task.Status.TODO),
                new Task("团队同步", "整理本周进展并准备周会同步材料。", LocalDate.now().plusDays(3), Task.Priority.LOW, Task.Status.DONE)
        );

        ProgressBar progressBar = new ProgressBar();
        progressBar.setPrefWidth(Double.MAX_VALUE);

        Label summaryLabel = new Label();
        summaryLabel.getStyleClass().add("tasks-summary-label");

        VBox summaryBox = new VBox(8, summaryLabel, progressBar);
        summaryBox.setPadding(new Insets(16));
        summaryBox.getStyleClass().add("tasks-summary-box");
        taskLayout.setTop(summaryBox);

        ListView<Task> taskListView = new ListView<>(tasks);
        taskListView.getStyleClass().add("tasks-list-view");
        taskListView.setPlaceholder(new Label("暂无任务"));
        taskListView.setCellFactory(new Callback<>() {
            @Override
            public ListCell<Task> call(ListView<Task> listView) {
                return new ListCell<>() {
                    @Override
                    protected void updateItem(Task item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            setGraphic(null);
                        } else {
                            setText(item.getTitle() + " · " + item.getStatus().getDisplayName() + " · " + item.getFormattedDueDate());
                        }
                    }
                };
            }
        });

        Label listHeader = new Label("全部任务");
        listHeader.getStyleClass().add("tasks-list-header");

        Button addButton = new Button("新建");
        Button completeButton = new Button("标记完成");
        Button deleteButton = new Button("删除");

        HBox buttonBar = new HBox(8, addButton, completeButton, deleteButton);
        buttonBar.getStyleClass().add("tasks-button-bar");

        VBox listContainer = new VBox(12, listHeader, taskListView, buttonBar);
        listContainer.setPadding(new Insets(16));
        listContainer.getStyleClass().add("tasks-list-container");
        taskListView.setPrefWidth(280);
        VBox.setVgrow(taskListView, Priority.ALWAYS);

        VBox detailContainer = new VBox(12);
        detailContainer.setPadding(new Insets(24));
        detailContainer.getStyleClass().add("tasks-detail-container");

        Label detailHeader = new Label("任务详情");
        detailHeader.getStyleClass().add("tasks-detail-header");

        TextField titleField = new TextField();
        titleField.setPromptText("任务标题");

        DatePicker dueDatePicker = new DatePicker();
        dueDatePicker.setPromptText("截止日期");

        ComboBox<Task.Priority> priorityCombo = new ComboBox<>();
        priorityCombo.setPromptText("选择优先级");
        priorityCombo.getItems().setAll(Task.Priority.values());
        priorityCombo.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(Task.Priority item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getDisplayName());
            }
        });
        priorityCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Task.Priority item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getDisplayName());
            }
        });

        ComboBox<Task.Status> statusCombo = new ComboBox<>();
        statusCombo.setPromptText("任务状态");
        statusCombo.getItems().setAll(Task.Status.values());
        statusCombo.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(Task.Status item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getDisplayName());
            }
        });
        statusCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Task.Status item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getDisplayName());
            }
        });

        TextArea descriptionArea = new TextArea();
        descriptionArea.setPromptText("任务描述、关键步骤或备注……");
        descriptionArea.setWrapText(true);
        descriptionArea.setPrefRowCount(10);
        VBox.setVgrow(descriptionArea, Priority.ALWAYS);

        Label statusLabel = new Label("选择一个任务以查看详情");
        statusLabel.getStyleClass().add("tasks-status-label");
        statusLabel.setMaxWidth(Double.MAX_VALUE);

        Button saveButton = new Button("保存修改");
        saveButton.getStyleClass().add("accent-button");

        HBox actionBar = new HBox(12, saveButton, statusLabel);
        actionBar.getStyleClass().add("tasks-action-bar");
        actionBar.setPadding(new Insets(4, 0, 0, 0));
        HBox.setHgrow(statusLabel, Priority.ALWAYS);

        detailContainer.getChildren().addAll(detailHeader, titleField, dueDatePicker, priorityCombo, statusCombo, descriptionArea, actionBar);

        titleField.setDisable(true);
        dueDatePicker.setDisable(true);
        priorityCombo.setDisable(true);
        statusCombo.setDisable(true);
        descriptionArea.setDisable(true);

        taskLayout.setLeft(listContainer);
        taskLayout.setCenter(detailContainer);

        Runnable refreshSummary = () -> updateTaskSummary(tasks, summaryLabel, progressBar);
        refreshSummary.run();
        tasks.addListener((ListChangeListener<Task>) change -> refreshSummary.run());

        taskListView.getSelectionModel().selectedItemProperty().addListener((obs, oldTask, newTask) -> {
            boolean hasTask = newTask != null;
            titleField.setDisable(!hasTask);
            dueDatePicker.setDisable(!hasTask);
            priorityCombo.setDisable(!hasTask);
            statusCombo.setDisable(!hasTask);
            descriptionArea.setDisable(!hasTask);

            if (!hasTask) {
                titleField.clear();
                dueDatePicker.setValue(null);
                priorityCombo.getSelectionModel().clearSelection();
                statusCombo.getSelectionModel().clearSelection();
                descriptionArea.clear();
                statusLabel.setText("选择一个任务以查看详情");
            } else {
                titleField.setText(newTask.getTitle());
                dueDatePicker.setValue(newTask.getDueDate());
                priorityCombo.getSelectionModel().select(newTask.getPriority());
                statusCombo.getSelectionModel().select(newTask.getStatus());
                descriptionArea.setText(newTask.getDescription());
                statusLabel.setText("正在查看 " + newTask.getTitle());
            }
        });

        saveButton.disableProperty().bind(Bindings.isNull(taskListView.getSelectionModel().selectedItemProperty()));
        deleteButton.disableProperty().bind(Bindings.isNull(taskListView.getSelectionModel().selectedItemProperty()));
        completeButton.disableProperty().bind(Bindings.createBooleanBinding(() -> {
            Task selected = taskListView.getSelectionModel().getSelectedItem();
            return selected == null || selected.getStatus() == Task.Status.DONE;
        }, taskListView.getSelectionModel().selectedItemProperty()));

        addButton.setOnAction(event -> {
            Task newTask = new Task("未命名任务", "", LocalDate.now().plusDays(1), Task.Priority.MEDIUM, Task.Status.TODO);
            tasks.add(0, newTask);
            taskListView.getSelectionModel().select(newTask);
            statusLabel.setText("已创建新任务");
            titleField.requestFocus();
            refreshSummary.run();
        });

        deleteButton.setOnAction(event -> {
            Task selectedTask = taskListView.getSelectionModel().getSelectedItem();
            if (selectedTask != null) {
                int index = taskListView.getSelectionModel().getSelectedIndex();
                tasks.remove(selectedTask);
                statusLabel.setText("已删除任务");
                if (!tasks.isEmpty()) {
                    taskListView.getSelectionModel().select(Math.min(index, tasks.size() - 1));
                }
                refreshSummary.run();
            }
        });

        completeButton.setOnAction(event -> {
            Task selectedTask = taskListView.getSelectionModel().getSelectedItem();
            if (selectedTask != null) {
                selectedTask.setStatus(Task.Status.DONE);
                statusCombo.getSelectionModel().select(Task.Status.DONE);
                statusLabel.setText("任务已标记为完成");
                taskListView.refresh();
                refreshSummary.run();
            }
        });

        saveButton.setOnAction(event -> saveTask(taskListView, titleField, descriptionArea, dueDatePicker, priorityCombo, statusCombo, statusLabel, tasks, progressBar, summaryLabel));

        taskListView.getSelectionModel().selectFirst();

        tab.setContent(taskLayout);
        return tab;
    }

    private void saveNote(ListView<Note> noteListView,
                          TextField titleField,
                          TextArea contentArea,
                          Label lastUpdatedLabel,
                          Label statusLabel) {
        Note selectedNote = noteListView.getSelectionModel().getSelectedItem();
        if (selectedNote != null) {
            selectedNote.setTitle(titleField.getText().isBlank() ? "未命名笔记" : titleField.getText());
            selectedNote.setContent(contentArea.getText());
            selectedNote.setLastUpdated(LocalDateTime.now());
            lastUpdatedLabel.setText("最后编辑：" + selectedNote.getFormattedTimestamp());
            statusLabel.setText("已保存笔记");
            noteListView.refresh();
        }
    }

    private void saveTask(ListView<Task> taskListView,
                          TextField titleField,
                          TextArea descriptionArea,
                          DatePicker dueDatePicker,
                          ComboBox<Task.Priority> priorityCombo,
                          ComboBox<Task.Status> statusCombo,
                          Label statusLabel,
                          ObservableList<Task> tasks,
                          ProgressBar progressBar,
                          Label summaryLabel) {
        Task selectedTask = taskListView.getSelectionModel().getSelectedItem();
        if (selectedTask != null) {
            selectedTask.setTitle(titleField.getText().isBlank() ? "未命名任务" : titleField.getText());
            selectedTask.setDescription(descriptionArea.getText());
            selectedTask.setDueDate(dueDatePicker.getValue());
            selectedTask.setPriority(priorityCombo.getValue() == null ? Task.Priority.MEDIUM : priorityCombo.getValue());
            selectedTask.setStatus(statusCombo.getValue() == null ? Task.Status.TODO : statusCombo.getValue());
            taskListView.refresh();
            statusLabel.setText("已保存任务");
            updateTaskSummary(tasks, summaryLabel, progressBar);
        }
    }

    private void updateTaskSummary(ObservableList<Task> tasks, Label summaryLabel, ProgressBar progressBar) {
        int total = tasks.size();
        int done = 0;
        int inProgress = 0;
        for (Task task : tasks) {
            if (task.getStatus() == Task.Status.DONE) {
                done++;
            } else if (task.getStatus() == Task.Status.IN_PROGRESS) {
                inProgress++;
            }
        }
        summaryLabel.setText(String.format("共 %d 项任务 · 进行中 %d · 已完成 %d", total, inProgress, done));
        progressBar.setProgress(total == 0 ? 0 : (double) done / total);
    }

    private Tab createTab(String title, String placeholderText) {
        Tab tab = new Tab(title);
        tab.setContent(new Label(placeholderText));
        tab.setClosable(false);
        return tab;
    }

    private static class Note {
        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        private String title;
        private String content;
        private LocalDateTime lastUpdated;

        private Note(String title, String content, LocalDateTime lastUpdated) {
            this.title = title;
            this.content = content;
            this.lastUpdated = lastUpdated;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public LocalDateTime getLastUpdated() {
            return lastUpdated;
        }

        public void setLastUpdated(LocalDateTime lastUpdated) {
            this.lastUpdated = lastUpdated;
        }

        public String getFormattedTimestamp() {
            return lastUpdated == null ? "" : lastUpdated.format(FORMATTER);
        }

        @Override
        public String toString() {
            return getTitle();
        }
    }

    private static class Task {
        enum Priority {
            HIGH("高优先级"),
            MEDIUM("中优先级"),
            LOW("低优先级");

            private final String displayName;

            Priority(String displayName) {
                this.displayName = displayName;
            }

            public String getDisplayName() {
                return displayName;
            }
        }

        enum Status {
            TODO("待开始"),
            IN_PROGRESS("进行中"),
            DONE("已完成");

            private final String displayName;

            Status(String displayName) {
                this.displayName = displayName;
            }

            public String getDisplayName() {
                return displayName;
            }
        }

        private String title;
        private String description;
        private LocalDate dueDate;
        private Priority priority;
        private Status status;

        private Task(String title, String description, LocalDate dueDate, Priority priority, Status status) {
            this.title = title;
            this.description = description;
            this.dueDate = dueDate;
            this.priority = priority;
            this.status = status;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public LocalDate getDueDate() {
            return dueDate;
        }

        public void setDueDate(LocalDate dueDate) {
            this.dueDate = dueDate;
        }

        public Priority getPriority() {
            return priority;
        }

        public void setPriority(Priority priority) {
            this.priority = priority;
        }

        public Status getStatus() {
            return status;
        }

        public void setStatus(Status status) {
            this.status = status;
        }

        public String getFormattedDueDate() {
            return dueDate == null ? "无截止日期" : dueDate.toString();
        }

        @Override
        public String toString() {
            return getTitle();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
