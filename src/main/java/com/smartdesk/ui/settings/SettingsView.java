package com.smartdesk.ui.settings;

import com.smartdesk.core.config.AppConfig;
import com.smartdesk.core.config.ConfigManager;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.collections.FXCollections;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Presents the SmartDesk system settings allowing runtime configuration changes.
 */
public final class SettingsView extends BorderPane {

    private final ConfigManager configManager;

    private final ComboBox<AppConfig.AiMode> modeBox = new ComboBox<>();
    private final ComboBox<AppConfig.Provider> providerBox = new ComboBox<>();
    private final ComboBox<String> baseUrlBox = new ComboBox<>();
    private final ComboBox<String> modelBox = new ComboBox<>();
    private final PasswordField apiKeyField = new PasswordField();
    private final ComboBox<AppConfig.Theme> themeBox = new ComboBox<>();
    private final Label statusLabel = new Label();

    public SettingsView(final ConfigManager configManager) {
        this.configManager = Objects.requireNonNull(configManager, "configManager");
        getStyleClass().add("settings-view-root");
        setPadding(new Insets(24));
        setTop(buildHeader());
        setCenter(buildForm());
        setBottom(buildFooter());
        populateFields(configManager.getConfig());
        configManager.registerListener(config -> Platform.runLater(() -> populateFields(config)));
    }

    private VBox buildHeader() {
        Label title = new Label("系统设置");
        title.getStyleClass().add("settings-title");
        Label subtitle = new Label("配置聊天模式、API 接入及主题");
        subtitle.getStyleClass().add("settings-subtitle");
        VBox header = new VBox(6, title, subtitle);
        header.setPadding(new Insets(0, 0, 16, 0));
        return header;
    }

    private GridPane buildForm() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);

        modeBox.getItems().setAll(AppConfig.AiMode.values());
        providerBox.getItems().setAll(AppConfig.Provider.values());
        themeBox.getItems().setAll(AppConfig.Theme.values());

        baseUrlBox.setEditable(true);
        modelBox.setEditable(true);

        grid.add(label("AI 模式"), 0, 0);
        grid.add(modeBox, 1, 0);
        grid.add(label("在线提供方"), 0, 1);
        grid.add(providerBox, 1, 1);
        grid.add(label("接口地址"), 0, 2);
        grid.add(baseUrlBox, 1, 2);
        grid.add(label("模型名称"), 0, 3);
        grid.add(modelBox, 1, 3);
        grid.add(label("API Key"), 0, 4);
        grid.add(apiKeyField, 1, 4);
        grid.add(label("主题"), 0, 5);
        grid.add(themeBox, 1, 5);

        GridPane.setHgrow(baseUrlBox, Priority.ALWAYS);
        GridPane.setHgrow(modelBox, Priority.ALWAYS);
        GridPane.setHgrow(apiKeyField, Priority.ALWAYS);

        modeBox.valueProperty().addListener((obs, old, value) -> updateFieldState(value));
        providerBox.valueProperty().addListener((obs, old, value) ->
            updateProviderPresets(value, null, null, false));
        updateFieldState(modeBox.getValue());
        return grid;
    }

    private HBox buildFooter() {
        Button saveButton = new Button("保存");
        saveButton.getStyleClass().add("accent-button");
        saveButton.setOnAction(evt -> persist());
        HBox footer = new HBox(12, statusLabel, saveButton);
        footer.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(statusLabel, Priority.ALWAYS);
        statusLabel.getStyleClass().add("settings-status");
        return footer;
    }

    private void populateFields(final AppConfig config) {
        modeBox.setValue(config.getAiMode() == null ? AppConfig.AiMode.OFFLINE : config.getAiMode());
        providerBox.setValue(config.getProvider() == null ? AppConfig.Provider.CHATGPT : config.getProvider());
        updateProviderPresets(providerBox.getValue(), config.getBaseUrl(), config.getModel(), true);
        apiKeyField.setText(config.getApiKey());
        themeBox.setValue(config.getTheme() == null ? AppConfig.Theme.LIGHT : config.getTheme());
        updateFieldState(modeBox.getValue());
        statusLabel.setText("配置已同步");
    }

    private void updateFieldState(final AppConfig.AiMode aiMode) {
        boolean online = aiMode == AppConfig.AiMode.ONLINE;
        providerBox.setDisable(!online);
        baseUrlBox.setDisable(!online);
        modelBox.setDisable(!online);
        apiKeyField.setDisable(!online);
    }

    private void persist() {
        AppConfig config = new AppConfig();
        config.setAiMode(modeBox.getValue() == null ? AppConfig.AiMode.OFFLINE : modeBox.getValue());
        config.setProvider(providerBox.getValue() == null ? AppConfig.Provider.CHATGPT : providerBox.getValue());
        config.setBaseUrl(getComboValue(baseUrlBox));
        config.setModel(getComboValue(modelBox));
        config.setApiKey(apiKeyField.getText() == null ? "" : apiKeyField.getText().trim());
        config.setTheme(themeBox.getValue() == null ? AppConfig.Theme.LIGHT : themeBox.getValue());
        configManager.saveConfig(config);
        statusLabel.setText("已保存");
    }

    private Label label(final String text) {
        Label label = new Label(text);
        label.getStyleClass().add("settings-label");
        return label;
    }

    private void updateProviderPresets(final AppConfig.Provider provider, final String baseValue,
                                       final String modelValue, final boolean preserveExisting) {
        AppConfig.Provider resolved = provider == null ? AppConfig.Provider.CHATGPT : provider;
        List<String> baseOptions = BASE_URL_PRESETS.getOrDefault(resolved, List.of());
        List<String> modelOptions = MODEL_PRESETS.getOrDefault(resolved, List.of());
        applyPreset(baseUrlBox, baseOptions, baseValue, preserveExisting);
        applyPreset(modelBox, modelOptions, modelValue, preserveExisting);
    }

    private void applyPreset(final ComboBox<String> comboBox, final List<String> options,
                             final String override, final boolean preserveExisting) {
        String trimmedOverride = override == null ? "" : override.trim();
        String currentText = preserveExisting ? getComboValue(comboBox) : "";
        comboBox.setItems(FXCollections.observableArrayList(options));
        String target = !trimmedOverride.isEmpty() ? trimmedOverride : currentText;
        if (!target.isEmpty()) {
            comboBox.setValue(target);
            comboBox.getEditor().setText(target);
        } else if (!options.isEmpty()) {
            comboBox.setValue(options.get(0));
            comboBox.getEditor().setText(options.get(0));
        } else {
            comboBox.setValue(null);
            comboBox.getEditor().setText("");
        }
    }

    private String getComboValue(final ComboBox<String> comboBox) {
        String editorText = comboBox.getEditor().getText();
        if (editorText != null && !editorText.trim().isEmpty()) {
            return editorText.trim();
        }
        String value = comboBox.getValue();
        return value == null ? "" : value.trim();
    }

    private static final Map<AppConfig.Provider, List<String>> BASE_URL_PRESETS = Map.of(
        AppConfig.Provider.CHATGPT, List.of(
            "https://api.openai.com/v1/chat/completions",
            "https://api.openai.com/v1/responses",
            "https://api.openai.com/v1"
        ),
        AppConfig.Provider.DEEPSEEK, List.of(
            "https://api.deepseek.com/v1/chat/completions",
            "https://api.deepseek.com/v1",
            "https://api.deepseek.com"
        )
    );

    private static final Map<AppConfig.Provider, List<String>> MODEL_PRESETS = Map.of(
        AppConfig.Provider.CHATGPT, List.of(
            "gpt-4.1",
            "gpt-4.1-mini",
            "gpt-4o",
            "gpt-4o-mini",
            "gpt-3.5-turbo"
        ),
        AppConfig.Provider.DEEPSEEK, List.of(
            "deepseek-chat",
            "deepseek-reasoner"
        )
    );
}
