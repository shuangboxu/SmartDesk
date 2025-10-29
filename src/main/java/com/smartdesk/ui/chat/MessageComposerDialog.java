package com.smartdesk.ui.chat;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Objects;

/**
 * Dialog used to compose chat messages in a lightweight sheet.
 */
final class MessageComposerDialog extends Dialog<String> {

    private final TextArea editor = new TextArea();

    MessageComposerDialog(final String initialText) {
        setTitle("撰写消息");
        setHeaderText(null);

        ButtonType sendType = new ButtonType("发送", ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("取消", ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(sendType, cancelType);
        getDialogPane().getStyleClass().add("chat-composer-dialog");
        getDialogPane().getStylesheets().add(Objects.requireNonNull(
            getClass().getResource("/com/smartdesk/resources/application.css"))
            .toExternalForm());

        Label title = new Label("写下你的想法");
        title.getStyleClass().add("chat-dialog-title");

        Label subtitle = new Label("AI 将根据内容快速给出建议或答案");
        subtitle.getStyleClass().add("chat-dialog-subtitle");

        editor.setWrapText(true);
        editor.getStyleClass().add("chat-dialog-editor");
        editor.setPromptText("在此处输入要向助理咨询的问题或上下文...");
        if (initialText != null && !initialText.isBlank()) {
            editor.setText(initialText);
            editor.positionCaret(editor.getText().length());
        }

        VBox container = new VBox(12, title, subtitle, editor);
        container.getStyleClass().add("chat-dialog-container");
        container.setPadding(new Insets(12, 4, 4, 4));
        VBox.setVgrow(editor, Priority.ALWAYS);

        getDialogPane().setContent(container);

        Node sendButton = getDialogPane().lookupButton(sendType);
        sendButton.disableProperty().bind(Bindings.createBooleanBinding(
            () -> editor.getText() == null || editor.getText().trim().isEmpty(),
            editor.textProperty()));

        setResultConverter(button -> button == sendType ? editor.getText().trim() : null);
    }
}

