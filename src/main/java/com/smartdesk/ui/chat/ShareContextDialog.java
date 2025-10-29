package com.smartdesk.ui.chat;

import com.smartdesk.ui.MainApp;
import com.smartdesk.ui.tasks.TaskViewModel;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Dialog used for inserting contextual note or task information into the chat composer.
 */
public final class ShareContextDialog extends Dialog<String> {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ListView<MainApp.Note> noteListView = new ListView<>();
    private final CheckBox noteTitleCheck = new CheckBox("笔记标题");
    private final CheckBox noteContentCheck = new CheckBox("笔记正文");

    private final ListView<TaskViewModel> taskListView = new ListView<>();
    private final CheckBox taskTitleCheck = new CheckBox("任务标题");
    private final CheckBox taskDescriptionCheck = new CheckBox("任务描述");
    private final CheckBox taskScheduleCheck = new CheckBox("时间信息");
    private final CheckBox taskStatusCheck = new CheckBox("状态/优先级");

    public ShareContextDialog(final ObservableList<MainApp.Note> notes,
                              final ObservableList<TaskViewModel> tasks) {
        setTitle("选择要发送的资料");
        setHeaderText("可将笔记或任务的部分内容发送给 AI");
        getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, new ButtonType("插入", ButtonBar.ButtonData.OK_DONE));

        noteListView.setItems(Objects.requireNonNull(notes, "notes"));
        noteListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(final MainApp.Note item, final boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getTitle());
            }
        });
        noteListView.setPlaceholder(new Label("暂无笔记"));

        taskListView.setItems(Objects.requireNonNull(tasks, "tasks"));
        taskListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(final TaskViewModel item, final boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getTitle());
            }
        });
        taskListView.setPlaceholder(new Label("暂无任务"));

        TabPane tabPane = new TabPane(buildNotesTab(), buildTasksTab());
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        getDialogPane().setContent(tabPane);

        setResultConverter(button -> {
            if (button == null || button.getButtonData() != ButtonBar.ButtonData.OK_DONE) {
                return null;
            }
            Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
            if (selectedTab == null) {
                return null;
            }
            if ("notes".equals(selectedTab.getId())) {
                return buildNotePayload();
            }
            if ("tasks".equals(selectedTab.getId())) {
                return buildTaskPayload();
            }
            return null;
        });
    }

    private Tab buildNotesTab() {
        noteTitleCheck.setSelected(true);
        noteContentCheck.setSelected(true);

        VBox options = new VBox(8, noteTitleCheck, noteContentCheck);
        options.setPadding(new Insets(12, 0, 0, 0));

        BorderPane pane = new BorderPane();
        pane.setCenter(noteListView);
        pane.setBottom(options);
        BorderPane.setMargin(options, new Insets(12, 0, 0, 0));

        Tab tab = new Tab("笔记", pane);
        tab.setId("notes");
        return tab;
    }

    private Tab buildTasksTab() {
        taskTitleCheck.setSelected(true);
        taskDescriptionCheck.setSelected(true);

        GridPane options = new GridPane();
        options.setHgap(12);
        options.setVgap(8);
        options.add(taskTitleCheck, 0, 0);
        options.add(taskDescriptionCheck, 1, 0);
        options.add(taskScheduleCheck, 0, 1);
        options.add(taskStatusCheck, 1, 1);
        GridPane.setHgrow(taskDescriptionCheck, Priority.ALWAYS);

        BorderPane pane = new BorderPane();
        pane.setCenter(taskListView);
        pane.setBottom(options);
        BorderPane.setMargin(options, new Insets(12, 0, 0, 0));

        Tab tab = new Tab("任务", pane);
        tab.setId("tasks");
        return tab;
    }

    private String buildNotePayload() {
        MainApp.Note selected = noteListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder("以下是我笔记中的内容：\n");
        boolean hasContent = false;
        if (noteTitleCheck.isSelected()) {
            builder.append("• 标题：").append(selected.getTitle()).append('\n');
            hasContent = true;
        }
        if (noteContentCheck.isSelected()) {
            builder.append("• 正文：").append(selected.getContent()).append('\n');
            hasContent = true;
        }
        if (!hasContent) {
            return null;
        }
        String payload = builder.toString().trim();
        return payload.isEmpty() ? null : payload;
    }

    private String buildTaskPayload() {
        TaskViewModel selected = taskListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder("我有一条任务想请你协助：\n");
        boolean hasContent = false;
        if (taskTitleCheck.isSelected()) {
            builder.append("• 标题：").append(selected.getTitle()).append('\n');
            hasContent = true;
        }
        if (taskDescriptionCheck.isSelected() && selected.getDescription() != null) {
            builder.append("• 描述：").append(selected.getDescription()).append('\n');
            hasContent = true;
        }
        if (taskScheduleCheck.isSelected()) {
            builder.append("• 起止：");
            if (selected.getStartDateTime() != null) {
                builder.append(selected.getStartDateTime().format(DATE_TIME_FORMATTER)).append(" 起");
            }
            if (selected.getDueDateTime() != null) {
                if (selected.getStartDateTime() != null) {
                    builder.append("，");
                }
                builder.append("截止到 ").append(selected.getDueDateTime().format(DATE_TIME_FORMATTER));
            }
            builder.append('\n');
            hasContent = true;
        }
        if (taskStatusCheck.isSelected()) {
            builder.append("• 状态：").append(selected.getStatus());
            if (selected.getPriority() != null) {
                builder.append("，优先级：").append(selected.getPriority());
            }
            builder.append('\n');
            hasContent = true;
        }
        if (!hasContent) {
            return null;
        }
        String payload = builder.toString().trim();
        return payload.isEmpty() ? null : payload;
    }
}
