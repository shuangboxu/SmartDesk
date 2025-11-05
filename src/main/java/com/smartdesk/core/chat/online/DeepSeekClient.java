package com.smartdesk.core.chat.online;

import com.google.gson.JsonObject;
import com.smartdesk.core.chat.AttachmentPromptFormatter;
import com.smartdesk.core.chat.ChatMessage;

/**
 * Client implementation targeting the DeepSeek chat completion API.
 */
public final class DeepSeekClient extends AbstractJsonAiClient {

    @Override
    protected void customisePayload(final JsonObject payload) {
        payload.addProperty("stream", false);
    }

    @Override
    protected String formatContent(final ChatMessage message) {
        // Inline attachment text for DeepSeek while keeping UI-facing message untouched.
        return AttachmentPromptFormatter.buildContentWithAttachments(message);
    }
}
