package com.smartdesk.ui.chat;

import com.smartdesk.core.chat.ChatAssistant;
import com.smartdesk.core.chat.ChatAssistantFactory;
import com.smartdesk.core.chat.ChatHistory;
import com.smartdesk.core.chat.ChatMessage;
import com.smartdesk.core.chat.ChatSession;
import com.smartdesk.core.config.AppConfig;
import com.smartdesk.core.config.ConfigManager;
import com.smartdesk.core.config.ModelCatalog;
import com.smartdesk.ui.MainApp;
import com.smartdesk.ui.tasks.TaskViewModel;
import com.smartdesk.utils.MarkdownRenderer;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * JavaFX view encapsulating the chat assistant user interface.
 */
public final class ChatView extends BorderPane {

    private static final DateTimeFormatter SESSION_TIME_FORMAT = DateTimeFormatter.ofPattern("MM-dd HH:mm");
    private static final DateTimeFormatter MESSAGE_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ConfigManager configManager;
    private final ObservableList<MainApp.Note> notes;
    private final ObservableList<TaskViewModel> tasks;
    private final ChatHistory history = new ChatHistory();

    private final ObservableList<ChatSession> sessions = FXCollections.observableArrayList();
    private final ListView<ChatSession> sessionList = new ListView<>(sessions);
    private final ListView<ChatMessage> messageList = new ListView<>();
    private final TextArea composer = new TextArea();
    private final Button sendButton = new Button("发送");
    private final Button shareButton = new Button("插入资料");
    private final Button toggleHistoryButton = new Button("折叠历史");
    private final ComboBox<String> modelSelector = new ComboBox<>();
    private final Label modeLabel = new Label();
    private final Label statusLabel = new Label();

    private ChatAssistant assistant;
    private AppConfig baseConfig;
    private ChatSession activeSession;
    private String activeModel;
    private boolean updatingModel;
    private int sessionCounter = 1;

    private VBox sidebar;
    private boolean historyCollapsed;

    public ChatView(final ConfigManager configManager,
                    final ObservableList<MainApp.Note> notes,
                    final ObservableList<TaskViewModel> tasks) {
        this.configManager = Objects.requireNonNull(configManager, "configManager");
        this.notes = Objects.requireNonNull(notes, "notes");
        this.tasks = Objects.requireNonNull(tasks, "tasks");
        getStyleClass().add("chat-view-root");
        setPadding(new Insets(16));

        setLeft(buildSidebar());
        BorderPane conversationPane = new BorderPane();
        conversationPane.setTop(buildHeader());
        conversationPane.setCenter(buildMessagePane());
        conversationPane.setBottom(buildComposer());
        setCenter(conversationPane);

        configureSessionList();
        configureListView();
        configureComposer();

        applyConfig(configManager.getConfig());
        configManager.registerListener(config -> Platform.runLater(() -> applyConfig(config)));
    }

    private Node buildSidebar() {
        Label title = new Label("对话历史");
        title.getStyleClass().add("chat-session-header");

        Button newButton = new Button("新建对话");
        newButton.getStyleClass().add("accent-button");
        newButton.setMaxWidth(Double.MAX_VALUE);
        newButton.setOnAction(evt -> startNewSession());

        sessionList.setPlaceholder(new Label("暂无对话，点击上方新建"));

        sidebar = new VBox(12, title, newButton, sessionList);
        sidebar.getStyleClass().add("chat-sidebar");
        VBox.setVgrow(sessionList, Priority.ALWAYS);
        return sidebar;
    }

    private Node buildHeader() {
        Label title = new Label("聊天助理");
        title.getStyleClass().add("chat-view-title");
        modeLabel.getStyleClass().add("chat-view-mode");

        HBox titleRow = new HBox(8, title, modeLabel);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label modelLabel = new Label("模型");
        modelLabel.getStyleClass().add("chat-model-label");
        modelSelector.getStyleClass().add("chat-model-selector");
        modelSelector.setVisibleRowCount(10);
        modelSelector.setPromptText("请选择模型");
        modelSelector.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (updatingModel) {
                return;
            }
            if (newValue == null || newValue.isBlank()) {
                return;
            }
            activeModel = newValue;
            if (activeSession != null) {
                activeSession.setModelName(activeModel);
            }
            configureAssistant();
            updateModeLabel();
            updateStatus("已切换到模型：" + activeModel);
        });

        Region spacer = new Region();
        toggleHistoryButton.getStyleClass().add("chat-history-toggle");
        toggleHistoryButton.setOnAction(evt -> toggleHistory());

        HBox controls = new HBox(12, modelLabel, modelSelector, spacer, toggleHistoryButton);
        controls.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(spacer, Priority.ALWAYS);

        statusLabel.getStyleClass().add("chat-view-status");

        VBox header = new VBox(6, titleRow, controls, statusLabel);
        header.setPadding(new Insets(0, 0, 12, 0));
        return header;
    }

    private Node buildMessagePane() {
        messageList.setPadding(new Insets(12));
        messageList.setStyle("-fx-background-color: transparent;");
        return messageList;
    }

    private Node buildComposer() {
        VBox container = new VBox(8);
        container.setPadding(new Insets(12, 0, 0, 0));

        composer.setPromptText("输入问题，按 Ctrl+Enter 发送");
        composer.setWrapText(true);
        composer.getStyleClass().add("chat-view-composer");

        shareButton.getStyleClass().add("chat-share-button");
        shareButton.setOnAction(evt -> handleShareContext());

        sendButton.getStyleClass().add("accent-button");
        sendButton.setDefaultButton(true);
        sendButton.setOnAction(evt -> dispatchMessage());

        Region spacer = new Region();
        HBox actionBar = new HBox(12, shareButton, spacer, sendButton);
        actionBar.getStyleClass().add("chat-composer-actions");
        actionBar.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(spacer, Priority.ALWAYS);

        container.getChildren().addAll(composer, actionBar);
        VBox.setVgrow(composer, Priority.ALWAYS);
        return container;
    }

    private void configureSessionList() {
        sessionList.setCellFactory(list -> new ChatSessionCell());
        sessionList.getStyleClass().add("chat-session-list");
        sessionList.getSelectionModel().selectedItemProperty().addListener((obs, old, value) -> {
            if (value == null) {
                return;
            }
            if (value == activeSession) {
                messageList.scrollTo(Math.max(value.getMessages().size() - 1, 0));
            } else {
                openSession(value);
            }
        });
    }

    private void configureListView() {
        messageList.setCellFactory(list -> new ChatMessageCell());
        messageList.getStyleClass().add("chat-view-list");
        messageList.setFocusTraversable(false);
        messageList.setPlaceholder(new Label("向 AI 发送你的第一个问题吧！"));
    }

    private void configureComposer() {
        composer.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER && event.isControlDown()) {
                dispatchMessage();
                event.consume();
            }
        });
    }

    private void applyConfig(final AppConfig config) {
        baseConfig = (config == null ? new AppConfig() : config.copy());
        if (sessions.isEmpty()) {
            refreshModelSelector();
            startNewSession();
        } else {
            refreshModelSelector();
            if (activeSession == null && !sessions.isEmpty()) {
                sessionList.getSelectionModel().selectFirst();
            } else if (activeSession != null) {
                openSession(activeSession);
            }
        }
        updateModeLabel();
        updateStatus("配置已同步");
    }

    private void refreshModelSelector() {
        if (baseConfig == null) {
            return;
        }
        LinkedHashSet<String> options = new LinkedHashSet<>();
        options.addAll(ModelCatalog.getModelPresets(baseConfig.getProvider()));
        if (baseConfig.getCustomModels() != null) {
            for (String model : baseConfig.getCustomModels()) {
                if (model != null && !model.isBlank()) {
                    options.add(model.trim());
                }
            }
        }
        if (baseConfig.getModel() != null && !baseConfig.getModel().isBlank()) {
            options.add(baseConfig.getModel());
        }
        if (activeSession != null && activeSession.getModelName() != null
            && !activeSession.getModelName().isBlank()) {
            options.add(activeSession.getModelName());
        }
        ObservableList<String> items = FXCollections.observableArrayList(options);
        updatingModel = true;
        modelSelector.setItems(items);
        String target = activeSession != null && activeSession.getModelName() != null
            && !activeSession.getModelName().isBlank()
            ? activeSession.getModelName()
            : baseConfig.getModel();
        if (target != null && !target.isBlank()) {
            if (!items.contains(target)) {
                items.add(0, target);
                modelSelector.setItems(items);
            }
            modelSelector.getSelectionModel().select(target);
            activeModel = target;
        } else {
            modelSelector.getSelectionModel().clearSelection();
            activeModel = null;
        }
        updatingModel = false;
        modelSelector.setDisable(baseConfig.getAiMode() == AppConfig.AiMode.OFFLINE);
    }

    private void configureAssistant() {
        if (baseConfig == null) {
            return;
        }
        if (assistant != null) {
            assistant.shutdown();
        }
        AppConfig working = baseConfig.copy();
        if (activeModel != null && !activeModel.isBlank()) {
            working.setModel(activeModel);
        }
        assistant = ChatAssistantFactory.createAssistant(history, working);
    }

    private void updateModeLabel() {
        if (baseConfig == null) {
            modeLabel.setText("");
            return;
        }
        AppConfig.AiMode mode = baseConfig.getAiMode() == null ? AppConfig.AiMode.OFFLINE : baseConfig.getAiMode();
        if (mode == AppConfig.AiMode.OFFLINE) {
            modeLabel.setText("(离线模式)");
        } else {
            AppConfig.Provider provider = baseConfig.getProvider() == null
                ? AppConfig.Provider.CHATGPT
                : baseConfig.getProvider();
            String providerName = describeProvider(provider);
            String modelName = activeModel != null && !activeModel.isBlank()
                ? activeModel
                : (baseConfig.getModel() == null ? "未选择模型" : baseConfig.getModel());
            modeLabel.setText("(在线 · " + providerName + " · " + modelName + ")");
        }
    }

    private void startNewSession() {
        ChatSession session = new ChatSession("对话 " + sessionCounter++);
        session.setModelName(activeModel != null && !activeModel.isBlank()
            ? activeModel
            : baseConfig.getModel());
        sessions.add(0, session);
        sessionList.getSelectionModel().select(session);
        openSession(session);
        updateStatus("已开始新的对话");
    }

    private void openSession(final ChatSession session) {
        if (session == null) {
            return;
        }
        activeSession = session;
        if (messageList.getItems() != session.getMessages()) {
            messageList.setItems(session.getMessages());
        }
        ensureSessionGreeting(session);
        refreshModelSelector();
        session.setModelName(activeModel);
        syncHistoryFromSession(session);
        configureAssistant();
        updateModeLabel();
        messageList.scrollTo(Math.max(session.getMessages().size() - 1, 0));
    }

    private void ensureSessionGreeting(final ChatSession session) {
        if (session.getMessages().isEmpty()) {
            ChatMessage greeting = ChatMessage.of(ChatMessage.Sender.SYSTEM,
                "欢迎使用智能聊天助理，有任何问题都可以告诉我！");
            session.addMessage(greeting);
        }
    }

    private void syncHistoryFromSession(final ChatSession session) {
        List<ChatMessage> snapshot = new ArrayList<>(session.getMessages());
        history.replaceWith(snapshot);
    }

    private void dispatchMessage() {
        if (assistant == null) {
            updateStatus("助手尚未初始化，请检查设置");
            return;
        }
        String text = composer.getText();
        if (text == null || text.isBlank()) {
            return;
        }
        String content = text.trim();
        composer.clear();
        ChatMessage userMessage = ChatMessage.of(ChatMessage.Sender.USER, content);
        activeSession.addMessage(userMessage);
        refreshSessionOrder(activeSession);
        messageList.scrollTo(Math.max(activeSession.getMessages().size() - 1, 0));
        sendButton.setDisable(true);
        updateStatus("发送中...");
        assistant.sendMessage(userMessage, response -> Platform.runLater(() -> {
            activeSession.addMessage(response);
            refreshSessionOrder(activeSession);
            messageList.scrollTo(Math.max(activeSession.getMessages().size() - 1, 0));
            updateStatus("响应时间: " + response.getTimestamp().toLocalTime().format(MESSAGE_TIME_FORMAT));
            sendButton.setDisable(false);
        }), error -> Platform.runLater(() -> {
            updateStatus("发生错误: " + error.getMessage());
            sendButton.setDisable(false);
        }));
    }

    private void handleShareContext() {
        ShareContextDialog dialog = new ShareContextDialog(notes, tasks);
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(snippet -> {
            if (!snippet.isBlank()) {
                int position = composer.getCaretPosition();
                composer.insertText(position, snippet + System.lineSeparator());
                composer.requestFocus();
                composer.positionCaret(position + snippet.length() + 1);
            }
        });
    }

    private void refreshSessionOrder(final ChatSession session) {
        if (session == null) {
            return;
        }
        sessions.remove(session);
        sessions.add(0, session);
        sessionList.getSelectionModel().select(session);
    }

    private void updateStatus(final String text) {
        statusLabel.setText(text);
    }

    private void toggleHistory() {
        if (historyCollapsed) {
            setLeft(sidebar);
            toggleHistoryButton.setText("折叠历史");
        } else {
            setLeft(null);
            toggleHistoryButton.setText("展开历史");
        }
        historyCollapsed = !historyCollapsed;
    }

    public void shutdown() {
        if (assistant != null) {
            assistant.shutdown();
        }
    }

    private static String describeProvider(final AppConfig.Provider provider) {
        return switch (provider) {
            case CHATGPT -> "OpenAI";
            case DEEPSEEK -> "DeepSeek";
        };
    }

    private final class ChatSessionCell extends ListCell<ChatSession> {
        private final Label titleLabel = new Label();
        private final Label metaLabel = new Label();
        private final VBox container = new VBox(4, titleLabel, metaLabel);

        private ChatSessionCell() {
            container.getStyleClass().add("chat-session-cell");
            titleLabel.getStyleClass().add("chat-session-title");
            metaLabel.getStyleClass().add("chat-session-meta");
        }

        @Override
        protected void updateItem(final ChatSession item, final boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                return;
            }
            titleLabel.setText(item.getTitle());
            String timeText = item.getUpdatedAt() == null ? "刚刚" : item.getUpdatedAt().format(SESSION_TIME_FORMAT);
            String modelText = (item.getModelName() == null || item.getModelName().isBlank())
                ? "默认模型"
                : item.getModelName();
            metaLabel.setText(timeText + " · " + modelText + " · 消息数 " + item.getMessages().size());
            setGraphic(container);
        }
    }

    private final class ChatMessageCell extends ListCell<ChatMessage> {
        private final Label senderLabel = new Label();
        private final WebView markdownView = new WebView();
        private final Label timeLabel = new Label();
        private final VBox bubble = new VBox(6, senderLabel, markdownView, timeLabel);
        private final HBox wrapper = new HBox(bubble);

        private ChatMessageCell() {
            bubble.getStyleClass().add("chat-bubble");
            senderLabel.getStyleClass().add("chat-bubble-sender");
            timeLabel.getStyleClass().add("chat-bubble-time");
            timeLabel.setMaxWidth(Double.MAX_VALUE);
            timeLabel.setAlignment(Pos.CENTER_RIGHT);

            bubble.setMaxWidth(560);
            bubble.setFillWidth(true);
            bubble.setPadding(new Insets(12));

            wrapper.setPadding(new Insets(4, 8, 4, 8));
            wrapper.setFillHeight(true);

            markdownView.setContextMenuEnabled(false);
            markdownView.setZoom(1.0);
            markdownView.setMaxWidth(520);
            markdownView.setPrefWidth(520);
            markdownView.setMinHeight(0);
            markdownView.setStyle("-fx-background-color: transparent;");
            markdownView.getEngine().getLoadWorker().stateProperty().addListener((obs, old, state) -> {
                if (state == Worker.State.SUCCEEDED) {
                    Platform.runLater(() -> {
                        Object result = markdownView.getEngine().executeScript("document.body.scrollHeight + 16");
                        if (result instanceof Number number) {
                            markdownView.setPrefHeight(number.doubleValue());
                        }
                    });
                }
            });
        }

        @Override
        protected void updateItem(final ChatMessage item, final boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                return;
            }
            ChatMessage.Sender sender = item.getSender();
            String senderName = switch (sender) {
                case USER -> "我";
                case ASSISTANT -> "AI";
                case SYSTEM -> "系统";
            };
            senderLabel.setText(senderName);
            timeLabel.setText(item.getTimestamp().toLocalTime().format(MESSAGE_TIME_FORMAT));

            String textColor;
            String linkColor;
            String bubbleStyle;
            Pos alignment;
            if (sender == ChatMessage.Sender.USER) {
                textColor = "#ffffff";
                linkColor = "#d0d8ff";
                bubbleStyle = "chat-bubble-user";
                alignment = Pos.CENTER_RIGHT;
            } else if (sender == ChatMessage.Sender.ASSISTANT) {
                textColor = "#1f2a4a";
                linkColor = "#3f51b5";
                bubbleStyle = "chat-bubble-assistant";
                alignment = Pos.CENTER_LEFT;
            } else {
                textColor = "#253057";
                linkColor = "#3f51b5";
                bubbleStyle = "chat-bubble-system";
                alignment = Pos.CENTER_LEFT;
            }
            String html = MarkdownRenderer.toHtml(item.getContent(), textColor, linkColor);
            markdownView.getEngine().loadContent(html);

            bubble.getStyleClass().setAll("chat-bubble", bubbleStyle);
            wrapper.setAlignment(alignment);
            setGraphic(wrapper);
        }
    }
}
