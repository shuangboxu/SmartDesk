package com.smartdesk.ui.settings;

import com.smartdesk.core.config.AppConfig;
import com.smartdesk.core.config.ConfigManager;
import com.smartdesk.core.config.ModelCatalog;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
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
    private final Label statusLabel = new Label();
    private final ListView<String> customModelList = new ListView<>();
    private final TextField customModelField = new TextField();

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
        Label subtitle = new Label("配置聊天模式和 API 接入");
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
        baseUrlBox.setEditable(true);
        modelBox.setEditable(true);

        customModelList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        customModelList.setPrefHeight(120);
        customModelList.setPlaceholder(new Label("暂无自定义模型"));

        int row = 0;
        grid.add(label("AI 模式"), 0, row);
        grid.add(modeBox, 1, row++);
        grid.add(label("在线提供方"), 0, row);
        grid.add(providerBox, 1, row++);
        grid.add(label("接口地址"), 0, row);
        grid.add(baseUrlBox, 1, row++);
        grid.add(label("默认模型"), 0, row);
        grid.add(modelBox, 1, row++);
        grid.add(label("自定义模型"), 0, row);
        grid.add(buildCustomModelEditor(), 1, row++);
        grid.add(label("API Key"), 0, row);
        grid.add(apiKeyField, 1, row++);
        GridPane.setHgrow(baseUrlBox, Priority.ALWAYS);
        GridPane.setHgrow(modelBox, Priority.ALWAYS);
        GridPane.setHgrow(apiKeyField, Priority.ALWAYS);
        GridPane.setHgrow(customModelList, Priority.ALWAYS);

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
        List<String> models = config.getCustomModels();
        customModelList.getItems().setAll(models == null ? List.of() : models);
        updateProviderPresets(providerBox.getValue(), config.getBaseUrl(), config.getModel(), true);
        apiKeyField.setText(config.getApiKey());
        updateFieldState(modeBox.getValue());
        statusLabel.setText("配置已同步");
    }

    private void updateFieldState(final AppConfig.AiMode aiMode) {
        boolean online = aiMode == AppConfig.AiMode.ONLINE;
        providerBox.setDisable(!online);
        baseUrlBox.setDisable(!online);
        modelBox.setDisable(!online);
        customModelList.setDisable(!online);
        customModelField.setDisable(!online);
        apiKeyField.setDisable(!online);
    }

    private void persist() {
        AppConfig config = new AppConfig();
        config.setAiMode(modeBox.getValue() == null ? AppConfig.AiMode.OFFLINE : modeBox.getValue());
        config.setProvider(providerBox.getValue() == null ? AppConfig.Provider.CHATGPT : providerBox.getValue());
        config.setBaseUrl(getComboValue(baseUrlBox));
        config.setModel(getComboValue(modelBox));
        config.setApiKey(apiKeyField.getText() == null ? "" : apiKeyField.getText().trim());
        config.setCustomModels(List.copyOf(customModelList.getItems()));
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
        List<String> baseOptions = ModelCatalog.getBaseUrlPresets(provider);
        applyPreset(baseUrlBox, baseOptions, baseValue, preserveExisting);
        refreshModelBox(ModelCatalog.getModelPresets(provider), modelValue, preserveExisting);
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

    private void refreshModelBox(final List<String> providerModels, final String override,
                                 final boolean preserveExisting) {
        ObservableList<String> combined = FXCollections.observableArrayList();
        if (providerModels != null) {
            combined.addAll(providerModels);
        }
        for (String model : customModelList.getItems()) {
            if (model != null) {
                String trimmed = model.trim();
                if (!trimmed.isEmpty() && !combined.contains(trimmed)) {
                    combined.add(trimmed);
                }
            }
        }
        String trimmedOverride = override == null ? "" : override.trim();
        String currentText = preserveExisting ? getComboValue(modelBox) : "";
        String target = !trimmedOverride.isEmpty() ? trimmedOverride : currentText;
        if (!target.isEmpty() && !combined.contains(target)) {
            combined.add(0, target);
        }
        applyPreset(modelBox, combined, target, false);
    }

    private String getComboValue(final ComboBox<String> comboBox) {
        String editorText = comboBox.getEditor().getText();
        if (editorText != null && !editorText.trim().isEmpty()) {
            return editorText.trim();
        }
        String value = comboBox.getValue();
        return value == null ? "" : value.trim();
    }

    private VBox buildCustomModelEditor() {
        customModelField.setPromptText("输入模型名称后点击添加");
        Button addButton = new Button("添加");
        Button removeButton = new Button("移除选中");
        addButton.getStyleClass().add("accent-button");
        removeButton.getStyleClass().add("task-card-button");

        addButton.setOnAction(evt -> {
            String candidate = customModelField.getText();
            if (candidate != null) {
                String trimmed = candidate.trim();
                if (!trimmed.isEmpty() && !customModelList.getItems().contains(trimmed)) {
                    customModelList.getItems().add(trimmed);
                    customModelField.clear();
                    refreshModelBox(ModelCatalog.getModelPresets(providerBox.getValue()), trimmed, true);
                }
            }
        });

        removeButton.setOnAction(evt -> {
            String selected = customModelList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                customModelList.getItems().remove(selected);
                refreshModelBox(ModelCatalog.getModelPresets(providerBox.getValue()), modelBox.getValue(), true);
            }
        });

        HBox controls = new HBox(8, customModelField, addButton, removeButton);
        controls.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(customModelField, Priority.ALWAYS);

        VBox container = new VBox(8, customModelList, controls);
        container.setFillWidth(true);
        return container;
    }
}
