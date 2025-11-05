package com.smartdesk.core.chat.online;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.smartdesk.core.chat.ChatAttachment;
import com.smartdesk.core.chat.ChatHistoryService;
import com.smartdesk.core.chat.ChatMessage;
import com.smartdesk.core.config.AppConfig;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Client implementation targeting OpenAI providers with support for the Files API.
 */
public final class OpenAiClient implements AiModelClient {

    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .build();
    private final Gson gson = new Gson();
    private final ChatHistoryService historyService;

    public OpenAiClient(final ChatHistoryService historyService) {
        this.historyService = Objects.requireNonNull(historyService, "historyService");
    }

    @Override
    public String sendMessage(final AppConfig config, final List<ChatMessage> history,
                              final ChatMessage userMessage) throws AiClientException {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(history, "history");
        if (config.getModel() == null || config.getModel().isBlank()) {
            throw new AiClientException("未配置 OpenAI 模型");
        }
        ensureFileIds(config, userMessage);
        String endpoint = requireEndpoint(config.getBaseUrl(), "/responses");
        String payload = buildPayload(config, history);
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(endpoint))
            .timeout(Duration.ofSeconds(120))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + config.getApiKey())
            .header("OpenAI-Beta", "assistants=v2")
            .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
            .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return parseResponse(response.body());
            }
            throw new AiClientException("OpenAI 响应异常: " + response.statusCode() + ": " + response.body());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AiClientException("请求被中断", ex);
        } catch (IOException ex) {
            throw new AiClientException("调用 OpenAI 接口失败", ex);
        }
    }

    private void ensureFileIds(final AppConfig config, final ChatMessage userMessage) throws AiClientException {
        if (userMessage == null || !userMessage.hasAttachments()) {
            return;
        }
        for (ChatAttachment attachment : userMessage.getAttachments()) {
            if (attachment.getProviderFileId().isPresent()) {
                continue;
            }
            String fileId = uploadAttachment(config, attachment);
            attachment.setProviderFileId(fileId);
            attachment.getDatabaseId().ifPresent(id -> historyService.updateAttachmentFileId(id, fileId));
        }
    }

    private String uploadAttachment(final AppConfig config, final ChatAttachment attachment) throws AiClientException {
        String boundary = "----SmartDeskBoundary" + System.currentTimeMillis();
        byte[] fileBytes;
        try {
            fileBytes = Files.readAllBytes(attachment.getFilePath());
        } catch (IOException ex) {
            throw new AiClientException("读取附件失败: " + attachment.getFileName(), ex);
        }
        byte[] purposeBlock = buildPurposeBlock(boundary);
        byte[] fileBlock = buildFileBlock(boundary, attachment, fileBytes);
        byte[] closing = ("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
        String endpoint = requireEndpoint(config.getBaseUrl(), "/files");
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(endpoint))
            .timeout(Duration.ofSeconds(120))
            .header("Authorization", "Bearer " + config.getApiKey())
            .header("OpenAI-Beta", "assistants=v2")
            .header("Content-Type", "multipart/form-data; boundary=" + boundary)
            .POST(HttpRequest.BodyPublishers.ofByteArrays(List.of(purposeBlock, fileBlock, closing)))
            .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonObject parsed = gson.fromJson(response.body(), JsonObject.class);
                if (parsed == null || !parsed.has("id")) {
                    throw new AiClientException("OpenAI 未返回文件 ID");
                }
                return parsed.get("id").getAsString();
            }
            throw new AiClientException("上传附件失败: " + response.statusCode() + " - " + response.body());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AiClientException("上传附件被中断", ex);
        } catch (IOException ex) {
            throw new AiClientException("上传附件失败", ex);
        }
    }

    private byte[] buildPurposeBlock(final String boundary) {
        String block = "--" + boundary + "\r\n"
            + "Content-Disposition: form-data; name=\"purpose\"\r\n\r\n"
            + "assistants\r\n";
        return block.getBytes(StandardCharsets.UTF_8);
    }

    private byte[] buildFileBlock(final String boundary, final ChatAttachment attachment, final byte[] data) {
        String mime = attachment.getMimeType() == null ? "application/octet-stream" : attachment.getMimeType();
        StringBuilder builder = new StringBuilder();
        String safeName = attachment.getFileName() == null ? "attachment" : attachment.getFileName().replace('"', '_');
        builder.append("--").append(boundary).append("\r\n")
            .append("Content-Disposition: form-data; name=\"file\"; filename=\"")
            .append(safeName)
            .append("\"\r\n")
            .append("Content-Type: ").append(mime).append("\r\n\r\n");
        byte[] header = builder.toString().getBytes(StandardCharsets.UTF_8);
        byte[] footer = "\r\n".getBytes(StandardCharsets.UTF_8);
        byte[] block = new byte[header.length + data.length + footer.length];
        System.arraycopy(header, 0, block, 0, header.length);
        System.arraycopy(data, 0, block, header.length, data.length);
        System.arraycopy(footer, 0, block, header.length + data.length, footer.length);
        return block;
    }

    private String buildPayload(final AppConfig config, final List<ChatMessage> history) {
        JsonObject payload = new JsonObject();
        payload.addProperty("model", config.getModel());
        JsonArray input = new JsonArray();
        for (ChatMessage message : history) {
            JsonObject block = new JsonObject();
            block.addProperty("role", mapRole(message));
            JsonArray content = new JsonArray();
            JsonObject text = new JsonObject();
            text.addProperty("type", "input_text");
            text.addProperty("text", message.getContent() == null ? "" : message.getContent());
            content.add(text);
            block.add("content", content);
            JsonArray attachments = buildAttachmentArray(message);
            if (attachments != null) {
                block.add("attachments", attachments);
            }
            input.add(block);
        }
        payload.add("input", input);
        return gson.toJson(payload);
    }

    private JsonArray buildAttachmentArray(final ChatMessage message) {
        if (message == null || !message.hasAttachments()) {
            return null;
        }
        JsonArray attachments = new JsonArray();
        for (ChatAttachment attachment : message.getAttachments()) {
            attachment.getProviderFileId().ifPresent(fileId -> {
                JsonObject reference = new JsonObject();
                reference.addProperty("file_id", fileId);
                attachments.add(reference);
            });
        }
        return attachments.size() == 0 ? null : attachments;
    }

    private String parseResponse(final String body) throws AiClientException {
        JsonObject root = gson.fromJson(body, JsonObject.class);
        if (root == null) {
            throw new AiClientException("OpenAI 返回空响应");
        }
        JsonArray output = root.getAsJsonArray("output");
        if (output == null || output.size() == 0) {
            throw new AiClientException("OpenAI 响应缺少内容");
        }
        JsonObject first = output.get(0).getAsJsonObject();
        JsonArray content = first.getAsJsonArray("content");
        if (content == null) {
            throw new AiClientException("OpenAI 响应缺少 content 字段");
        }
        for (int i = 0; i < content.size(); i++) {
            JsonObject item = content.get(i).getAsJsonObject();
            String type = item.get("type").getAsString();
            if ("output_text".equals(type) || "text".equals(type)) {
                return item.get("text").getAsString();
            }
        }
        throw new AiClientException("OpenAI 响应未包含文本内容");
    }

    private String mapRole(final ChatMessage message) {
        return switch (message.getSender()) {
            case USER -> "user";
            case ASSISTANT -> "assistant";
            case SYSTEM -> "system";
        };
    }

    private String resolveEndpoint(final String baseUrl, final String resourcePath) {
        String base = resolveApiBase(baseUrl);
        if (base.endsWith("/") && resourcePath.startsWith("/")) {
            return base + resourcePath.substring(1);
        }
        if (!base.endsWith("/") && !resourcePath.startsWith("/")) {
            return base + "/" + resourcePath;
        }
        return base + resourcePath;
    }

    private String resolveApiBase(final String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "";
        }
        String trimmed = baseUrl.trim();
        try {
            URI uri = URI.create(trimmed);
            String scheme = uri.getScheme() == null ? "https" : uri.getScheme();
            String authority = uri.getAuthority();
            if (authority == null || authority.isBlank()) {
                return trimmed;
            }
            String path = uri.getPath() == null ? "" : uri.getPath();
            String lower = path.toLowerCase(Locale.ROOT);
            int idx = lower.indexOf("/v1");
            String basePath;
            if (idx >= 0) {
                basePath = path.substring(0, idx + 3);
            } else if (path.isBlank() || "/".equals(path)) {
                basePath = "/v1";
            } else if (path.endsWith("/")) {
                basePath = path + "v1";
            } else {
                basePath = path + "/v1";
            }
            if (!basePath.startsWith("/")) {
                basePath = "/" + basePath;
            }
            return scheme + "://" + authority + basePath;
        } catch (IllegalArgumentException ex) {
            return trimmed;
        }
    }

    private String requireEndpoint(final String baseUrl, final String resourcePath) throws AiClientException {
        String endpoint = resolveEndpoint(baseUrl, resourcePath);
        if (endpoint == null || endpoint.isBlank()) {
            throw new AiClientException("AI 接口地址未配置");
        }
        return endpoint;
    }
}
