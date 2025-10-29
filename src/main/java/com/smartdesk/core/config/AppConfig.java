package com.smartdesk.core.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents the runtime configuration for SmartDesk.
 */
public final class AppConfig {

    /**
     * Available AI operation modes.
     */
    public enum AiMode {
        OFFLINE,
        ONLINE
    }

    /**
     * Providers supported while operating in {@link AiMode#ONLINE}.
     */
    public enum Provider {
        CHATGPT,
        DEEPSEEK
    }

    private AiMode aiMode = AiMode.OFFLINE;
    private Provider provider = Provider.CHATGPT;
    private String apiKey = "";
    private String baseUrl = "";
    private String model = "";
    private List<String> customModels = new ArrayList<>();

    public AppConfig() {
    }

    public AppConfig(final AiMode aiMode, final Provider provider, final String apiKey,
                     final String baseUrl, final String model) {
        this.aiMode = Objects.requireNonNull(aiMode, "aiMode");
        this.provider = Objects.requireNonNull(provider, "provider");
        this.apiKey = Objects.requireNonNull(apiKey, "apiKey");
        this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl");
        this.model = Objects.requireNonNull(model, "model");
        this.customModels = new ArrayList<>();
    }

    public AppConfig copy() {
        AppConfig clone = new AppConfig(aiMode, provider, apiKey, baseUrl, model);
        clone.setCustomModels(new ArrayList<>(getCustomModels()));
        return clone;
    }

    public AiMode getAiMode() {
        return aiMode;
    }

    public void setAiMode(final AiMode aiMode) {
        this.aiMode = Objects.requireNonNull(aiMode, "aiMode");
    }

    public Provider getProvider() {
        return provider;
    }

    public void setProvider(final Provider provider) {
        this.provider = Objects.requireNonNull(provider, "provider");
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(final String apiKey) {
        this.apiKey = Objects.requireNonNull(apiKey, "apiKey");
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(final String baseUrl) {
        this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl");
    }

    public String getModel() {
        return model;
    }

    public void setModel(final String model) {
        this.model = Objects.requireNonNull(model, "model");
    }

    public List<String> getCustomModels() {
        return customModels;
    }

    public void setCustomModels(final List<String> customModels) {
        this.customModels = customModels == null ? new ArrayList<>() : new ArrayList<>(customModels);
    }
}
