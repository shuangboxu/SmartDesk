package com.smartdesk.core.chat.online;

import com.google.gson.JsonObject;

/**
 * Client implementation targeting the DeepSeek chat completion API.
 */
public final class DeepSeekClient extends AbstractJsonAiClient {

    @Override
    protected void customisePayload(final JsonObject payload) {
        payload.addProperty("stream", false);
    }
}
