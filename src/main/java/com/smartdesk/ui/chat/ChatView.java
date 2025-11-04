package com.smartdesk.ui.chat;

import com.smartdesk.core.chat.*;
import com.smartdesk.core.config.AppConfig;
import com.smartdesk.core.config.ConfigManager;
import com.smartdesk.core.config.ModelCatalog;
import com.smartdesk.ui.MainApp;
import com.smartdesk.ui.tasks.TaskViewModel;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 精简版 ChatView：去除所有装饰，用简单 Label 显示消息，
 * 消息宽度绑定到消息列表宽度，保证随窗口放大。
 */
public final class ChatView extends BorderPane {

    private static final DateTimeFormatter SESSION_TIME_FORMAT = DateTimeFormatter.ofPattern("MM-dd HH:mm");
    private static final DateTimeFormatter MESSAGE_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final double MIN_MESSAGE_WIDTH = 160;
    private static final double MIN_COMPOSER_HEIGHT = 96;
    private static final double MAX_COMPOSER_HEIGHT = 260;

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
    private final Text composerSizer = new Text();

    public ChatView(final ConfigManager configManager,
                    final ObservableList<MainApp.Note> notes,
                    final ObservableList<TaskViewModel> tasks) {
        this.configManager = Objects.requireNonNull(configManager, "configManager");
        this.notes = Objects.requireNonNull(notes, "notes");
        this.tasks = Objects.requireNonNull(tasks, "tasks");

        // 简化外层边距
        setPadding(new Insets(4));

        setLeft(buildSidebar());
        BorderPane conversationPane = new BorderPane();
        conversationPane.setTop(buildHeader());
        conversationPane.setCenter(buildMessagePane());
        conversationPane.setBottom(buildComposer());
        setCenter(conversationPane);
        VBox.setVgrow(conversationPane, Priority.ALWAYS);

        configureSessionList();
        configureListView();
        configureComposer();

// 关键：监听窗口变化，强制重新布局（修复放大后留白问题）
        widthProperty().addListener((obs, oldVal, newVal) -> Platform.runLater(this::requestLayout));
        heightProperty().addListener((obs, oldVal, newVal) -> Platform.runLater(this::requestLayout));

        applyConfig(configManager.getConfig());
        configManager.registerListener(config -> Platform.runLater(() -> applyConfig(config)));

    }

    private Node buildSidebar() {
        Label title = new Label("对话历史");

        Button newButton = new Button("新建对话");
        newButton.setMaxWidth(Double.MAX_VALUE);
        newButton.setOnAction(evt -> startNewSession());

        sessionList.setPlaceholder(new Label("暂无对话"));

        sidebar = new VBox(8, title, newButton, sessionList);
        VBox.setVgrow(sessionList, Priority.ALWAYS);
        sidebar.setPadding(new Insets(6));
        sidebar.setMinWidth(160);
        sidebar.setPrefWidth(200);
        return sidebar;
    }

    private Node buildHeader() {
        Label title = new Label("聊天助理");
        HBox titleRow = new HBox(8, title, modeLabel);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label modelLabel = new Label("模型");
        modelSelector.setVisibleRowCount(10);
        modelSelector.setPromptText("请选择模型");
        modelSelector.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (updatingModel) return;
            if (newValue == null || newValue.isBlank()) return;
            activeModel = newValue;
            if (activeSession != null) activeSession.setModelName(activeModel);
            configureAssistant();
            updateModeLabel();
            updateStatus("已切换到模型：" + activeModel);
        });

        Region spacer = new Region();
        toggleHistoryButton.setOnAction(evt -> toggleHistory());

        HBox controls = new HBox(8, modelLabel, modelSelector, spacer, toggleHistoryButton);
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox header = new VBox(6, titleRow, controls, statusLabel);
        header.setPadding(new Insets(6, 6, 12, 6));
        return header;
    }

//    private Node buildMessagePane() {
//        messageList.setPadding(new Insets(8));
//        messageList.setStyle("-fx-background-color: white;");
//
//        ScrollPane scroll = new ScrollPane(messageList);
//        scroll.setFitToWidth(true);
//        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
//        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
//        scroll.setStyle("-fx-background-color: transparent; -fx-padding:0;");
//
//        // 关键：让聊天区自动拉伸，占满剩余空间
//        VBox.setVgrow(scroll, Priority.ALWAYS);
//
//        return scroll;
//    }


    private Node buildMessagePane() {
        messageList.setPadding(new Insets(8));
        messageList.setStyle("-fx-background-color: white;");
        messageList.setFocusTraversable(false);
        messageList.setMinHeight(0);
        messageList.setPrefHeight(Double.MAX_VALUE);
        // 关键：直接使用 ListView，让 BorderPane 中心区域自动铺满剩余高度
        VBox.setVgrow(messageList, Priority.ALWAYS);
        BorderPane.setMargin(messageList, Insets.EMPTY);
        return messageList;
    }

    private Node buildComposer() {
        VBox container = new VBox(6);
        container.setPadding(new Insets(8, 6, 8, 6));

        composer.setPromptText("输入问题，按 Ctrl+Enter 发送");
        composer.setWrapText(true);
        composer.setPrefHeight(MIN_COMPOSER_HEIGHT);
        composer.setMinHeight(MIN_COMPOSER_HEIGHT);
        composer.setMaxHeight(MAX_COMPOSER_HEIGHT);

        shareButton.setOnAction(evt -> handleShareContext());

        sendButton.setDefaultButton(true);
        sendButton.setOnAction(evt -> dispatchMessage());

        HBox actionBar = new HBox(8, shareButton, new Region(), sendButton);
        HBox.setHgrow(actionBar.getChildren().get(1), Priority.ALWAYS);
        actionBar.setAlignment(Pos.CENTER_RIGHT);

        container.getChildren().addAll(composer, actionBar);
        VBox.setVgrow(composer, Priority.ALWAYS);
        return container;
    }

    private void configureSessionList() {
        sessionList.setCellFactory(list -> new ChatSessionCell());
        sessionList.getSelectionModel().selectedItemProperty().addListener((obs, old, value) -> {
            if (value == null) return;
            if (value == activeSession) {
                messageList.scrollTo(Math.max(value.getMessages().size() - 1, 0));
            } else {
                openSession(value);
            }
        });
    }

    private void configureListView() {
        messageList.setCellFactory(list -> new ChatMessageCell());
        messageList.setFocusTraversable(false);
        messageList.setPlaceholder(new Label("向 AI 发送你的第一个问题吧！"));
        messageList.setPrefWidth(Double.MAX_VALUE);
    }

    private void configureComposer() {
        composer.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER && event.isControlDown()) {
                dispatchMessage();
                event.consume();
            }
        });
        composer.textProperty().addListener((obs, oldValue, newValue) -> scheduleComposerResize());
        composer.widthProperty().addListener((obs, oldValue, newValue) -> scheduleComposerResize());
        Platform.runLater(this::resizeComposerToContent);
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

    private void scheduleComposerResize() {
        Platform.runLater(this::resizeComposerToContent);
    }

    private void resizeComposerToContent() {
        double availableWidth = composer.getWidth();
        if (availableWidth <= 0) return;
        composerSizer.setFont(composer.getFont());
        String content = composer.getText();
        if (content == null || content.isBlank()) {
            composer.setPrefHeight(MIN_COMPOSER_HEIGHT);
            composer.setMinHeight(MIN_COMPOSER_HEIGHT);
            return;
        }
        Insets padding = composer.getPadding();
        double horizontalPadding = padding == null ? 0 : padding.getLeft() + padding.getRight();
        double verticalPadding = padding == null ? 0 : padding.getTop() + padding.getBottom();
        composerSizer.setWrappingWidth(Math.max(0, availableWidth - horizontalPadding - 18));
        composerSizer.setText(content + "\n");
        double height = composerSizer.getLayoutBounds().getHeight() + verticalPadding + 20;
        double clamped = Math.max(MIN_COMPOSER_HEIGHT, Math.min(MAX_COMPOSER_HEIGHT, height));
        composer.setPrefHeight(clamped);
        composer.setMinHeight(clamped);
    }

    private void refreshModelSelector() {
        if (baseConfig == null) return;
        LinkedHashSet<String> options = new LinkedHashSet<>();
        options.addAll(ModelCatalog.getModelPresets(baseConfig.getProvider()));
        if (baseConfig.getCustomModels() != null) {
            for (String model : baseConfig.getCustomModels()) {
                if (model != null && !model.isBlank()) options.add(model.trim());
            }
        }
        if (baseConfig.getModel() != null && !baseConfig.getModel().isBlank()) options.add(baseConfig.getModel());
        if (activeSession != null && activeSession.getModelName() != null && !activeSession.getModelName().isBlank()) {
            options.add(activeSession.getModelName());
        }
        ObservableList<String> items = FXCollections.observableArrayList(options);
        updatingModel = true;
        modelSelector.setItems(items);
        String target = activeSession != null && activeSession.getModelName() != null && !activeSession.getModelName().isBlank()
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
        if (baseConfig == null) return;
        if (assistant != null) assistant.shutdown();
        AppConfig working = baseConfig.copy();
        if (activeModel != null && !activeModel.isBlank()) working.setModel(activeModel);
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
            AppConfig.Provider provider = baseConfig.getProvider() == null ? AppConfig.Provider.CHATGPT : baseConfig.getProvider();
            String providerName = describeProvider(provider);
            String modelName = activeModel != null && !activeModel.isBlank() ? activeModel : (baseConfig.getModel() == null ? "未选择模型" : baseConfig.getModel());
            modeLabel.setText("(在线 · " + providerName + " · " + modelName + ")");
        }
    }

    private void startNewSession() {
        ChatSession session = new ChatSession("对话 " + sessionCounter++);
        session.setModelName(activeModel != null && !activeModel.isBlank() ? activeModel : baseConfig.getModel());
        sessions.add(0, session);
        sessionList.getSelectionModel().select(session);
        openSession(session);
        updateStatus("已开始新的对话");
    }

    private void openSession(final ChatSession session) {
        if (session == null) return;
        activeSession = session;
        if (messageList.getItems() != session.getMessages()) messageList.setItems(session.getMessages());
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
            ChatMessage greeting = ChatMessage.of(ChatMessage.Sender.SYSTEM, "欢迎使用智能聊天助理，有任何问题都可以告诉我！");
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
        if (text == null || text.isBlank()) return;
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
        if (session == null) return;
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
        if (assistant != null) assistant.shutdown();
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
            container.setPadding(new Insets(6));
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
            String modelText = (item.getModelName() == null || item.getModelName().isBlank()) ? "默认模型" : item.getModelName();
            metaLabel.setText(timeText + " · " + modelText + " · 消息数 " + item.getMessages().size());
            setGraphic(container);
        }
    }

    private final class ChatMessageCell extends ListCell<ChatMessage> {
        private final Label senderLabel = new Label();
        private final Label contentLabel = new Label();
        private final Label timeLabel = new Label();
        private final VBox bubble = new VBox(6, senderLabel, contentLabel, timeLabel);
        private final HBox wrapper = new HBox(bubble);

        private ChatMessageCell() {
            // 简单内联样式（无外部 CSS）
            bubble.setPadding(new Insets(8));
            bubble.setSpacing(6);
            bubble.setStyle("-fx-background-color: #ffffff; -fx-border-color: #e0e0e0; -fx-background-radius: 6; -fx-border-radius: 6;");
            contentLabel.setWrapText(true);
            contentLabel.setTextAlignment(TextAlignment.LEFT);
            // 关键：让气泡宽度随 messageList 宽度变化
            bubble.maxWidthProperty().bind(messageList.widthProperty().subtract(80));
            contentLabel.maxWidthProperty().bind(bubble.maxWidthProperty().subtract(16));
            senderLabel.setStyle("-fx-font-weight: bold;");
            timeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");

            wrapper.setPadding(new Insets(4, 8, 4, 8));
            wrapper.setFillHeight(true);
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

            // 简单颜色区分，用户右对齐，AI/系统左对齐
            if (sender == ChatMessage.Sender.USER) {
                bubble.setStyle("-fx-background-color: #d7ecff; -fx-border-color: #a7c5e3; -fx-background-radius: 6; -fx-border-radius: 6;");
                wrapper.setAlignment(Pos.CENTER_RIGHT);
            } else if (sender == ChatMessage.Sender.ASSISTANT) {
                bubble.setStyle("-fx-background-color: #ffffff; -fx-border-color: #e0e0e0; -fx-background-radius: 6; -fx-border-radius: 6;");
                wrapper.setAlignment(Pos.CENTER_LEFT);
            } else {
                bubble.setStyle("-fx-background-color: #f1f4ff; -fx-border-color: #d6dbe8; -fx-background-radius: 6; -fx-border-radius: 6;");
                wrapper.setAlignment(Pos.CENTER_LEFT);
            }

            // 直接显示原始文本（不渲染 Markdown）
            contentLabel.setText(item.getContent());

            setGraphic(wrapper);
        }
    }
}
