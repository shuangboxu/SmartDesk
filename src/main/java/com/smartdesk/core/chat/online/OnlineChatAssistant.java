package com.smartdesk.core.chat.online;

import com.smartdesk.core.chat.ChatAssistant;
import com.smartdesk.core.chat.ChatHistory;
import com.smartdesk.core.chat.ChatMessage;
import com.smartdesk.core.config.AppConfig;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Online chat assistant delegating to a configured {@link AiModelClient} implementation.
 */
public final class OnlineChatAssistant implements ChatAssistant {

    private final ChatHistory history;
    private final AppConfig config;
    private final AiModelClient client;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "online-assistant-thread");
        thread.setDaemon(true);
        return thread;
    });

    public OnlineChatAssistant(final ChatHistory history, final AppConfig config,
                               final AiModelClient client) {
        this.history = Objects.requireNonNull(history, "history");
        this.config = Objects.requireNonNull(config, "config");
        this.client = Objects.requireNonNull(client, "client");
    }

    @Override
    public void sendMessage(final ChatMessage userMessage, final Consumer<ChatMessage> onSuccess,
                            final Consumer<Throwable> onFailure) {
        Objects.requireNonNull(userMessage, "userMessage");
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            onFailure.accept(new IllegalStateException("请先在设置中配置 API Key"));
            return;
        }
        if (config.getBaseUrl() == null || config.getBaseUrl().isBlank()) {
            onFailure.accept(new IllegalStateException("请先在设置中配置接口地址"));
            return;
        }
        history.add(userMessage);
        CompletableFuture.supplyAsync(() -> {
            try {
                return client.sendMessage(config, history.getMessages(), userMessage);
            } catch (AiClientException ex) {
                throw new RuntimeException(ex);
            }
        }, executor).whenComplete((reply, throwable) -> {
            if (throwable != null) {
                Throwable cause = throwable.getCause() == null ? throwable : throwable.getCause();
                onFailure.accept(cause);
                return;
            }
            ChatMessage response = ChatMessage.of(ChatMessage.Sender.ASSISTANT, reply);
            history.add(response);
            onSuccess.accept(response);
        });
    }

    @Override
    public void shutdown() {
        executor.shutdown();
        try {
            executor.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
