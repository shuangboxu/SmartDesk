package com.smartdesk.core.config;

import java.util.List;
import java.util.Map;

/**
 * Provides built-in presets for provider base URLs and model identifiers.
 */
public final class ModelCatalog {

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

    private ModelCatalog() {
    }

    public static List<String> getBaseUrlPresets(final AppConfig.Provider provider) {
        AppConfig.Provider resolved = provider == null ? AppConfig.Provider.CHATGPT : provider;
        return BASE_URL_PRESETS.getOrDefault(resolved, List.of());
    }

    public static List<String> getModelPresets(final AppConfig.Provider provider) {
        AppConfig.Provider resolved = provider == null ? AppConfig.Provider.CHATGPT : provider;
        return MODEL_PRESETS.getOrDefault(resolved, List.of());
    }
}
