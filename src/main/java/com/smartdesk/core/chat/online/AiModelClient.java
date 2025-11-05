package com.smartdesk.core.chat.online;

import com.smartdesk.core.chat.ChatMessage;
import com.smartdesk.core.config.AppConfig;

import java.util.List;

/**
 * Client abstraction responsible for invoking concrete large language models.
 */
public interface AiModelClient {

    /**
     * Sends the provided conversation context and returns the generated reply.
     *
     * @param config      runtime configuration containing credentials and endpoints
     * @param history     message history (excluding the message to send)
     * @param userMessage last user message to process
     * @return assistant reply text
     * @throws AiClientException when the remote provider fails or the response is invalid
     */
    String sendMessage(AppConfig config, List<ChatMessage> history, ChatMessage userMessage) throws AiClientException;
}
