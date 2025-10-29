package com.smartdesk.core.chat.online;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.smartdesk.core.chat.ChatMessage;
import com.smartdesk.core.config.AppConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * Convenience base class encapsulating the HTTP/JSON interaction pattern for chat completion APIs.
 */
abstract class AbstractJsonAiClient implements AiModelClient {

    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .build();
    private final Gson gson = new Gson();

    @Override
    public String sendMessage(final AppConfig config, final List<ChatMessage> history,
                              final String userMessage) throws AiClientException {
        Objects.requireNonNull(userMessage, "userMessage");
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.getBaseUrl()))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + config.getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(buildPayload(config, history)))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return parseContent(response.body());
            }
            throw new AiClientException("AI provider returned status " + response.statusCode() + ": " + response.body());
        } catch (AiClientException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AiClientException("Failed to contact AI provider", ex);
        }
    }

    private String buildPayload(final AppConfig config, final List<ChatMessage> history) {
        JsonObject payload = new JsonObject();
        payload.addProperty("model", config.getModel());
        JsonArray messages = new JsonArray();
        for (ChatMessage message : history) {
            JsonObject jsonMessage = new JsonObject();
            jsonMessage.addProperty("role", mapRole(message));
            jsonMessage.addProperty("content", message.getContent());
            messages.add(jsonMessage);
        }
        customisePayload(payload);
        payload.add("messages", messages);
        return gson.toJson(payload);
    }

    private String mapRole(final ChatMessage message) {
        return switch (message.getSender()) {
            case USER -> "user";
            case ASSISTANT -> "assistant";
            case SYSTEM -> "system";
        };
    }

    protected abstract void customisePayload(JsonObject payload);

    protected String parseContent(final String rawBody) throws AiClientException {
        JsonElement parsed = gson.fromJson(rawBody, JsonElement.class);
        if (!parsed.isJsonObject()) {
            throw new AiClientException("Unexpected response from AI provider");
        }
        JsonObject object = parsed.getAsJsonObject();
        JsonArray choices = object.getAsJsonArray("choices");
        if (choices == null || choices.size() == 0) {
            throw new AiClientException("AI provider response does not contain choices");
        }
        JsonObject choice = choices.get(0).getAsJsonObject();
        JsonObject message = choice.getAsJsonObject("message");
        if (message == null) {
            throw new AiClientException("AI provider response does not contain message content");
        }
        JsonElement content = message.get("content");
        if (content == null) {
            throw new AiClientException("AI provider response does not contain content field");
        }
        return content.getAsString();
    }
}
