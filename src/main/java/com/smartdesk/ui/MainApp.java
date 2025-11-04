package com.smartdesk.ui;

import com.smartdesk.core.chat.ChatHistoryService;
import com.smartdesk.core.config.AppConfig;
import com.smartdesk.core.config.ConfigManager;
import com.smartdesk.core.note.NoteService;
import com.smartdesk.core.task.TaskService;
import com.smartdesk.core.task.model.Task;
import com.smartdesk.core.task.model.TaskPriority;
import com.smartdesk.core.task.model.TaskStatus;
import com.smartdesk.core.task.model.TaskType;
import com.smartdesk.ui.chat.ChatView;
import com.smartdesk.ui.settings.SettingsView;
import com.smartdesk.ui.tasks.TaskDashboardView;
import com.smartdesk.ui.tasks.TaskViewModel;
import com.smartdesk.storage.DatabaseManager;
import com.smartdesk.storage.entity.NoteEntity;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Main application entry point for SmartDesk.
 */
public class MainApp extends Application {

    private static final Logger LOGGER = Logger.getLogger(MainApp.class.getName());

    private ConfigManager configManager;
    private ChatView chatView;
    private SettingsView settingsView;
    private TaskDashboardView taskDashboardView;
    private Scene scene;
    private ObservableList<Note> notes;
    private ObservableList<TaskViewModel> tasks;
    private DatabaseManager databaseManager;
    private NoteService noteService;
    private TaskService taskService;
    private ChatHistoryService chatHistoryService;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("SmartDesk");

        configManager = new ConfigManager();

        databaseManager = new DatabaseManager();
        noteService = new NoteService(databaseManager);
        taskService = new TaskService(databaseManager);
        chatHistoryService = new ChatHistoryService(databaseManager);

        notes = loadNotes();
        tasks = loadTasks();

        TabPane tabPane = new TabPane();
        tabPane.getTabs().add(createNotesTab());
        tabPane.getTabs().add(createTaskTab());
        tabPane.getTabs().add(createChatTab());
        tabPane.getTabs().add(createTab("总结", "总结模块即将上线"));
        tabPane.getTabs().add(createSettingsTab());

        BorderPane root = new BorderPane(tabPane);
        root.getStyleClass().add("app-root");
        scene = new Scene(root, 960, 600);
        scene.getStylesheets().add(
                getClass().getResource("/com/smartdesk/resources/application.css").toExternalForm()
        );
        applyTheme(configManager.getConfig().getTheme());
        configManager.registerListener(config -> Platform.runLater(() -> applyTheme(config.getTheme())));

        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
        primaryStage.show();
    }

    @Override
    public void stop() {
        if (taskDashboardView != null) {
            taskDashboardView.getReminderManager().shutdown();
        }
        if (chatView != null) {
            chatView.shutdown();
        }
    }

    private Tab createNotesTab() {
        Tab tab = new Tab("笔记");
        tab.setClosable(false);

        BorderPane notesLayout = new BorderPane();
        notesLayout.getStyleClass().add("notes-root");

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
            NoteEntity entity = new NoteEntity();
            entity.setTitle("未命名笔记");
            entity.setContent("");
            entity.setTag(null);
            entity.setDate(LocalDateTime.now());
            try {
                NoteEntity persisted = noteService.createNote(entity);
                Note newNote = Note.fromEntity(persisted);
                notes.add(0, newNote);
                noteListView.getSelectionModel().select(newNote);
                statusLabel.setText("已创建新笔记");
                titleField.requestFocus();
            } catch (IllegalStateException ex) {
                LOGGER.log(Level.SEVERE, "Failed to create note", ex);
                statusLabel.setText("新建笔记失败");
            }
        });

        deleteButton.setOnAction(event -> {
            Note selectedNote = noteListView.getSelectionModel().getSelectedItem();
            if (selectedNote != null) {
                int index = noteListView.getSelectionModel().getSelectedIndex();
                try {
                    if (selectedNote.getId() != null) {
                        noteService.deleteNote(selectedNote.getId());
                    }
                    notes.remove(selectedNote);
                    statusLabel.setText("已删除笔记");
                    if (!notes.isEmpty()) {
                        noteListView.getSelectionModel().select(Math.min(index, notes.size() - 1));
                    }
                } catch (IllegalStateException ex) {
                    LOGGER.log(Level.SEVERE, "Failed to delete note", ex);
                    statusLabel.setText("删除笔记失败");
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

        taskDashboardView = new TaskDashboardView(tasks, taskService);
        tab.setContent(taskDashboardView);
        return tab;
    }

    private Tab createChatTab() {
        Tab tab = new Tab("聊天");
        tab.setClosable(false);
        chatView = new ChatView(configManager, notes, tasks, chatHistoryService);
        tab.setContent(chatView);
        return tab;
    }

    private Tab createSettingsTab() {
        Tab tab = new Tab("设置");
        tab.setClosable(false);
        settingsView = new SettingsView(configManager);
        tab.setContent(settingsView);
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
            try {
                noteService.updateNote(selectedNote.toEntity());
                lastUpdatedLabel.setText("最后编辑：" + selectedNote.getFormattedTimestamp());
                statusLabel.setText("已保存笔记");
                noteListView.refresh();
            } catch (IllegalStateException ex) {
                LOGGER.log(Level.SEVERE, "Failed to update note", ex);
                statusLabel.setText("保存失败，请稍后再试");
            }
        }
    }

    private Tab createTab(String title, String placeholderText) {
        Tab tab = new Tab(title);
        tab.setContent(new Label(placeholderText));
        tab.setClosable(false);
        return tab;
    }

    private void applyTheme(AppConfig.Theme theme) {
        if (scene == null) {
            return;
        }
        var root = scene.getRoot();
        root.getStyleClass().removeAll("theme-light", "theme-dark");
        String styleClass = theme == AppConfig.Theme.DARK ? "theme-dark" : "theme-light";
        if (!root.getStyleClass().contains(styleClass)) {
            root.getStyleClass().add(styleClass);
        }
    }

    private ObservableList<TaskViewModel> loadTasks() {
        List<Task> persistedTasks = taskService.listAllTasks();
        if (persistedTasks.isEmpty()) {
            seedDefaultTasks();
            persistedTasks = taskService.listAllTasks();
        }
        List<TaskViewModel> models = persistedTasks.stream()
            .map(TaskViewModel::fromDomain)
            .collect(Collectors.toList());
        return FXCollections.observableArrayList(models);
    }

    private ObservableList<Note> loadNotes() {
        List<NoteEntity> persistedNotes = noteService.getAllNotes();
        if (persistedNotes.isEmpty()) {
            seedDefaultNotes();
            persistedNotes = noteService.getAllNotes();
        }
        List<Note> models = persistedNotes.stream()
            .map(Note::fromEntity)
            .collect(Collectors.toList());
        return FXCollections.observableArrayList(models);
    }

    private void seedDefaultNotes() {
        createNote("会议记录", "讨论项目进度、风险以及下周的里程碑。", LocalDateTime.now().minusDays(1));
        createNote("灵感捕捉", "重新设计仪表盘配色，突出重点指标。", LocalDateTime.now().minusHours(6));
        createNote("阅读摘录", "《用户体验要素》关于信息架构的章节值得复盘。", LocalDateTime.now().minusDays(3));
    }

    private void createNote(final String title, final String content, final LocalDateTime timestamp) {
        NoteEntity entity = new NoteEntity();
        entity.setTitle(title);
        entity.setContent(content);
        entity.setTag(null);
        entity.setDate(timestamp);
        try {
            noteService.createNote(entity);
        } catch (IllegalStateException ex) {
            LOGGER.log(Level.SEVERE, "Failed to seed default note", ex);
        }
    }

    private void seedDefaultTasks() {
        LocalDateTime now = LocalDateTime.now();
        createTask(
            Task.builder()
                .withTitle("设计评审")
                .withDescription("准备周四的设计评审资料，突出关键交互流程。")
                .withType(TaskType.EVENT)
                .withPriority(TaskPriority.HIGH)
                .withStatus(TaskStatus.IN_PROGRESS)
                .withStartDateTime(now.minusDays(1))
                .withDueDateTime(now.plusDays(1).withHour(15).withMinute(0))
                .withReminderEnabled(true)
                .withReminderLeadMinutes(30)
                .build());

        createTask(
            Task.builder()
                .withTitle("需求梳理")
                .withDescription("梳理 V2.3 版本需求并输出原型草稿。")
                .withType(TaskType.TODO)
                .withPriority(TaskPriority.NORMAL)
                .withStatus(TaskStatus.PLANNED)
                .withDueDateTime(now.plusDays(3).withHour(11).withMinute(0))
                .withReminderEnabled(true)
                .withReminderLeadMinutes(60)
                .build());

        createTask(
            Task.builder()
                .withTitle("BUG 回归")
                .withDescription("回归测试登录流程相关缺陷并记录测试结果。")
                .withType(TaskType.TODO)
                .withPriority(TaskPriority.URGENT)
                .withStatus(TaskStatus.IN_PROGRESS)
                .withDueDateTime(now.plusHours(6))
                .withReminderEnabled(true)
                .withReminderLeadMinutes(20)
                .build());

        createTask(
            Task.builder()
                .withTitle("团队同步")
                .withDescription("整理本周进展并准备周会同步材料。")
                .withType(TaskType.EVENT)
                .withPriority(TaskPriority.LOW)
                .withStatus(TaskStatus.COMPLETED)
                .withDueDateTime(now.minusDays(1))
                .withReminderEnabled(false)
                .build());

        createTask(
            Task.builder()
                .withTitle("UI 设计进阶课")
                .withDescription("第 6 周：交互流设计复盘与作业提交。")
                .withType(TaskType.COURSE)
                .withPriority(TaskPriority.NORMAL)
                .withStatus(TaskStatus.IN_PROGRESS)
                .withDueDateTime(now.plusDays(5).withHour(20))
                .withReminderEnabled(true)
                .withReminderLeadMinutes(90)
                .build());

        createTask(
            Task.builder()
                .withTitle("团队成立纪念日")
                .withDescription("准备庆祝活动并发布内部分享。")
                .withType(TaskType.ANNIVERSARY)
                .withPriority(TaskPriority.HIGH)
                .withStatus(TaskStatus.PLANNED)
                .withDueDateTime(now.plusDays(10).withHour(9))
                .withReminderEnabled(true)
                .withReminderLeadMinutes(120)
                .build());
    }

    private void createTask(final Task task) {
        try {
            taskService.createTask(task);
        } catch (IllegalStateException ex) {
            LOGGER.log(Level.SEVERE, "Failed to seed default task", ex);
        }
    }

    public static class Note {
        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        private Long id;
        private String title;
        private String content;
        private LocalDateTime lastUpdated;

        private Note(Long id, String title, String content, LocalDateTime lastUpdated) {
            this.id = id;
            this.title = title;
            this.content = content;
            this.lastUpdated = lastUpdated;
        }

        public static Note fromEntity(final NoteEntity entity) {
            return new Note(entity.getId(), entity.getTitle(), entity.getContent(), entity.getDate());
        }

        public NoteEntity toEntity() {
            NoteEntity entity = new NoteEntity();
            entity.setId(id);
            entity.setTitle(title);
            entity.setContent(content);
            entity.setTag(null);
            entity.setDate(lastUpdated != null ? lastUpdated : LocalDateTime.now());
            return entity;
        }

        public Long getId() {
            return id;
        }

        public void setId(final Long id) {
            this.id = id;
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

    public static void main(String[] args) {
        launch(args);
    }
}
