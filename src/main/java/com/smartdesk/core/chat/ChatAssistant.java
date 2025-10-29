package com.smartdesk.core.chat;

import java.util.function.Consumer;

/**
 * Defines operations that every chat assistant implementation must support.
 */
public interface ChatAssistant {

    /**
     * Sends the user message to the assistant.
     *
     * @param userMessage message object authored by the user and already persisted in history
     * @param onSuccess   consumer invoked with the assistant reply when available
     * @param onFailure   consumer invoked with the error raised while generating the reply
     */
    void sendMessage(ChatMessage userMessage, Consumer<ChatMessage> onSuccess, Consumer<Throwable> onFailure);

    /**
     * Releases any resources held by the assistant instance.
     */
    void shutdown();
}
