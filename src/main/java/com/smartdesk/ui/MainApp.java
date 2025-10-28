package com.smartdesk.ui;

import javafx.application.Application;
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

/**
 * Main application entry point for SmartDesk.
 */
public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("SmartDesk");

        TabPane tabPane = new TabPane();
        tabPane.getTabs().add(createNotesTab());
        tabPane.getTabs().add(createTab("任务", "任务模块即将上线"));
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

    public static void main(String[] args) {
        launch(args);
    }
}
