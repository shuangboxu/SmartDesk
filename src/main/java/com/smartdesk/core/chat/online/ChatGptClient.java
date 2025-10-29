package com.smartdesk.core.chat.online;

import com.google.gson.JsonObject;

/**
 * Client implementation targeting the OpenAI ChatGPT API.
 */
public final class ChatGptClient extends AbstractJsonAiClient {

    @Override
    protected void customisePayload(final JsonObject payload) {
        payload.addProperty("temperature", 0.7);
    }
}
