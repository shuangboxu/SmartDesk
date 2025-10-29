package com.smartdesk.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javafx.geometry.Insets;

import com.smartdesk.core.config.AppConfig;
import com.smartdesk.core.config.ConfigManager;
import com.smartdesk.core.task.model.TaskPriority;
import com.smartdesk.core.task.model.TaskStatus;
import com.smartdesk.core.task.model.TaskType;
import com.smartdesk.ui.chat.ChatView;
import com.smartdesk.ui.settings.SettingsView;
import com.smartdesk.ui.tasks.TaskDashboardView;
import com.smartdesk.ui.tasks.TaskViewModel;

/**
 * Main application entry point for SmartDesk.
 */
public class MainApp extends Application {

    private ConfigManager configManager;
    private ChatView chatView;
    private SettingsView settingsView;
    private TaskDashboardView taskDashboardView;
    private Scene scene;
    private ObservableList<Note> notes;
    private ObservableList<TaskViewModel> tasks;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("SmartDesk");

        configManager = new ConfigManager();

        notes = createSampleNotes();
        tasks = createSampleTasks();

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

        taskDashboardView = new TaskDashboardView(tasks);
        tab.setContent(taskDashboardView);
        return tab;
    }

    private Tab createChatTab() {
        Tab tab = new Tab("聊天");
        tab.setClosable(false);
        chatView = new ChatView(configManager, notes, tasks);
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
            lastUpdatedLabel.setText("最后编辑：" + selectedNote.getFormattedTimestamp());
            statusLabel.setText("已保存笔记");
            noteListView.refresh();
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

    private ObservableList<TaskViewModel> createSampleTasks() {
        ObservableList<TaskViewModel> tasks = FXCollections.observableArrayList();

        TaskViewModel designReview = new TaskViewModel();
        designReview.setTitle("设计评审");
        designReview.setDescription("准备周四的设计评审资料，突出关键交互流程。");
        designReview.setType(TaskType.EVENT);
        designReview.setPriority(TaskPriority.HIGH);
        designReview.setStatus(TaskStatus.IN_PROGRESS);
        designReview.setStartDateTime(LocalDateTime.now().minusDays(1));
        designReview.setDueDateTime(LocalDateTime.now().plusDays(1).withHour(15).withMinute(0));
        designReview.setReminderLeadMinutes(30);
        tasks.add(designReview);

        TaskViewModel requirementAnalysis = new TaskViewModel();
        requirementAnalysis.setTitle("需求梳理");
        requirementAnalysis.setDescription("梳理 V2.3 版本需求并输出原型草稿。");
        requirementAnalysis.setType(TaskType.TODO);
        requirementAnalysis.setPriority(TaskPriority.NORMAL);
        requirementAnalysis.setStatus(TaskStatus.PLANNED);
        requirementAnalysis.setDueDateTime(LocalDateTime.now().plusDays(3).withHour(11).withMinute(0));
        requirementAnalysis.setReminderLeadMinutes(60);
        tasks.add(requirementAnalysis);

        TaskViewModel regression = new TaskViewModel();
        regression.setTitle("BUG 回归");
        regression.setDescription("回归测试登录流程相关缺陷并记录测试结果。");
        regression.setType(TaskType.TODO);
        regression.setPriority(TaskPriority.URGENT);
        regression.setStatus(TaskStatus.IN_PROGRESS);
        regression.setDueDateTime(LocalDateTime.now().plusHours(6));
        regression.setReminderLeadMinutes(20);
        tasks.add(regression);

        TaskViewModel teamSync = new TaskViewModel();
        teamSync.setTitle("团队同步");
        teamSync.setDescription("整理本周进展并准备周会同步材料。");
        teamSync.setType(TaskType.EVENT);
        teamSync.setPriority(TaskPriority.LOW);
        teamSync.setStatus(TaskStatus.COMPLETED);
        teamSync.setDueDateTime(LocalDateTime.now().minusDays(1));
        teamSync.setReminderEnabled(false);
        tasks.add(teamSync);

        TaskViewModel uiCourse = new TaskViewModel();
        uiCourse.setTitle("UI 设计进阶课");
        uiCourse.setDescription("第 6 周：交互流设计复盘与作业提交。");
        uiCourse.setType(TaskType.COURSE);
        uiCourse.setPriority(TaskPriority.NORMAL);
        uiCourse.setStatus(TaskStatus.IN_PROGRESS);
        uiCourse.setDueDateTime(LocalDateTime.now().plusDays(5).withHour(20));
        uiCourse.setReminderLeadMinutes(90);
        tasks.add(uiCourse);

        TaskViewModel anniversary = new TaskViewModel();
        anniversary.setTitle("团队成立纪念日");
        anniversary.setDescription("准备庆祝活动并发布内部分享。");
        anniversary.setType(TaskType.ANNIVERSARY);
        anniversary.setPriority(TaskPriority.HIGH);
        anniversary.setStatus(TaskStatus.PLANNED);
        anniversary.setDueDateTime(LocalDateTime.now().plusDays(10).withHour(9));
        anniversary.setReminderLeadMinutes(120);
        tasks.add(anniversary);

        return tasks;
    }

    private ObservableList<Note> createSampleNotes() {
        return FXCollections.observableArrayList(
            new Note("会议记录", "讨论项目进度、风险以及下周的里程碑。", LocalDateTime.now().minusDays(1)),
            new Note("灵感捕捉", "重新设计仪表盘配色，突出重点指标。", LocalDateTime.now().minusHours(6)),
            new Note("阅读摘录", "《用户体验要素》关于信息架构的章节值得复盘。", LocalDateTime.now().minusDays(3))
        );
    }

    public static class Note {
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

    public static void main(String[] args) {
        launch(args);
    }
}
