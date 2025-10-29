package com.smartdesk.core.chat;

import com.smartdesk.core.chat.offline.OfflineChatAssistant;
import com.smartdesk.core.chat.online.AiModelClient;
import com.smartdesk.core.chat.online.ChatGptClient;
import com.smartdesk.core.chat.online.DeepSeekClient;
import com.smartdesk.core.chat.online.OnlineChatAssistant;
import com.smartdesk.core.config.AppConfig;

import java.util.Objects;

/**
 * Factory responsible for instantiating the proper {@link ChatAssistant} depending on configuration.
 */
public final class ChatAssistantFactory {

    private ChatAssistantFactory() {
    }

    public static ChatAssistant createAssistant(final ChatHistory history, final AppConfig config) {
        Objects.requireNonNull(history, "history");
        Objects.requireNonNull(config, "config");
        AppConfig.AiMode mode = config.getAiMode() == null ? AppConfig.AiMode.OFFLINE : config.getAiMode();
        if (mode == AppConfig.AiMode.OFFLINE) {
            return new OfflineChatAssistant(history);
        }
        AppConfig.Provider provider = config.getProvider() == null ? AppConfig.Provider.CHATGPT : config.getProvider();
        AiModelClient client = switch (provider) {
            case CHATGPT -> new ChatGptClient();
            case DEEPSEEK -> new DeepSeekClient();
        };
        return new OnlineChatAssistant(history, config, client);
    }
}
