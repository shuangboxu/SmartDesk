package com.smartdesk.ui.chat;

import com.smartdesk.core.chat.ChatAssistant;
import com.smartdesk.core.chat.ChatAssistantFactory;
import com.smartdesk.core.chat.ChatHistory;
import com.smartdesk.core.chat.ChatMessage;
import com.smartdesk.core.config.AppConfig;
import com.smartdesk.core.config.ConfigManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Objects;

/**
 * JavaFX view encapsulating the chat assistant user interface.
 */
public final class ChatView extends BorderPane {

    private final ConfigManager configManager;
    private final ChatHistory history = new ChatHistory();
    private final ObservableList<ChatMessage> messages = FXCollections.observableArrayList();

    private ChatAssistant assistant;
    private final ListView<ChatMessage> messageList = new ListView<>(messages);
    private final TextArea composer = new TextArea();
    private final Button sendButton = new Button("发送");
    private final Label modeLabel = new Label();
    private final Label statusLabel = new Label();

    public ChatView(final ConfigManager configManager) {
        this.configManager = Objects.requireNonNull(configManager, "configManager");
        getStyleClass().add("chat-view-root");
        setPadding(new Insets(16));
        setCenter(buildMessagePane());
        setBottom(buildComposer());
        setTop(buildHeader());
        configureListView();
        configureComposer();
        applyConfig(configManager.getConfig());
        configManager.registerListener(config -> Platform.runLater(() -> applyConfig(config)));
    }

    private Node buildHeader() {
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("聊天助理");
        title.getStyleClass().add("chat-view-title");
        modeLabel.getStyleClass().add("chat-view-mode");
        statusLabel.getStyleClass().add("chat-view-status");
        header.getChildren().addAll(title, modeLabel, statusLabel);
        return header;
    }

    private Node buildMessagePane() {
        ScrollPane scrollPane = new ScrollPane(messageList);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.getStyleClass().add("chat-view-scroll");
        return scrollPane;
    }

    private Node buildComposer() {
        VBox container = new VBox(8);
        container.setPadding(new Insets(12, 0, 0, 0));
        composer.setPromptText("输入问题，按 Ctrl+Enter 发送");
        composer.setWrapText(true);
        composer.getStyleClass().add("chat-view-composer");
        sendButton.getStyleClass().add("accent-button");
        sendButton.setDefaultButton(true);
        sendButton.setOnAction(evt -> dispatchMessage());
        HBox actionBar = new HBox(12, sendButton);
        actionBar.setAlignment(Pos.CENTER_RIGHT);
        container.getChildren().addAll(composer, actionBar);
        VBox.setVgrow(composer, Priority.ALWAYS);
        return container;
    }

    private void configureListView() {
        messageList.setCellFactory(list -> new ChatMessageCell());
        messageList.getStyleClass().add("chat-view-list");
    }

    private void configureComposer() {
        composer.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER && event.isControlDown()) {
                dispatchMessage();
                event.consume();
            }
        });
    }

    private void dispatchMessage() {
        String text = composer.getText();
        if (text == null || text.isBlank()) {
            return;
        }
        composer.clear();
        ChatMessage userMessage = ChatMessage.of(ChatMessage.Sender.USER, text.trim());
        messages.add(userMessage);
        messageList.scrollTo(messages.size() - 1);
        sendButton.setDisable(true);
        statusLabel.setText("发送中...");
        assistant.sendMessage(userMessage, response -> Platform.runLater(() -> {
            messages.add(response);
            messageList.scrollTo(messages.size() - 1);
            statusLabel.setText("响应时间: " + response.getTimestamp().toLocalTime().withNano(0));
            sendButton.setDisable(false);
        }), error -> Platform.runLater(() -> {
            statusLabel.setText("发生错误: " + error.getMessage());
            sendButton.setDisable(false);
        }));
    }

    private void applyConfig(final AppConfig config) {
        if (assistant != null) {
            assistant.shutdown();
        }
        history.clear();
        messages.clear();
        assistant = ChatAssistantFactory.createAssistant(history, config);
        String modeText = config.getAiMode() == AppConfig.AiMode.OFFLINE ? "离线模式" : "在线模式 - " + config.getProvider();
        modeLabel.setText("(" + modeText + ")");
        statusLabel.setText("配置已更新，开始新的对话");
        ChatMessage greeting = ChatMessage.of(ChatMessage.Sender.SYSTEM, "欢迎使用智能聊天助理，有任何问题都可以告诉我！");
        history.add(greeting);
        messages.add(greeting);
    }

    public void shutdown() {
        if (assistant != null) {
            assistant.shutdown();
        }
    }

    private static final class ChatMessageCell extends ListCell<ChatMessage> {
        @Override
        protected void updateItem(final ChatMessage item, final boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            Label bubble = new Label(item.getContent());
            bubble.setWrapText(true);
            bubble.setMaxWidth(520);
            bubble.getStyleClass().add("chat-bubble");
            bubble.getStyleClass().add(switch (item.getSender()) {
                case USER -> "chat-bubble-user";
                case ASSISTANT -> "chat-bubble-assistant";
                case SYSTEM -> "chat-bubble-system";
            });

            Label time = new Label(item.getTimestamp().toLocalTime().withNano(0).toString());
            time.getStyleClass().add("chat-bubble-time");
            VBox wrapper = new VBox(4, bubble, time);
            wrapper.setFillWidth(true);
            wrapper.getStyleClass().add("chat-bubble-wrapper");

            HBox container = new HBox(wrapper);
            container.setFillHeight(true);
            container.setPadding(new Insets(8));
            if (item.getSender() == ChatMessage.Sender.USER) {
                container.setAlignment(Pos.CENTER_RIGHT);
            } else {
                container.setAlignment(Pos.CENTER_LEFT);
            }
            setGraphic(container);
        }
    }
}
