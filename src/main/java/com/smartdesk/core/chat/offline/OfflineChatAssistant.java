package com.smartdesk.core.chat.offline;

import com.smartdesk.core.chat.ChatAssistant;
import com.smartdesk.core.chat.ChatHistory;
import com.smartdesk.core.chat.ChatMessage;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Chat assistant implementation backed by the {@link RuleBasedResponder} for offline use cases.
 */
public final class OfflineChatAssistant implements ChatAssistant {

    private final RuleBasedResponder responder;
    private final ChatHistory history;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "offline-assistant-thread");
        thread.setDaemon(true);
        return thread;
    });

    public OfflineChatAssistant(final ChatHistory history) {
        this.history = Objects.requireNonNull(history, "history");
        this.responder = new RuleBasedResponder();
    }

    @Override
    public void sendMessage(final ChatMessage userMessage, final Consumer<ChatMessage> onSuccess,
                            final Consumer<Throwable> onFailure) {
        Objects.requireNonNull(userMessage, "userMessage");
        history.add(userMessage);
        executor.submit(() -> {
            try {
                String reply = responder.respond(userMessage.getContent());
                ChatMessage response = ChatMessage.of(ChatMessage.Sender.ASSISTANT, reply);
                history.add(response);
                onSuccess.accept(response);
            } catch (Exception ex) {
                onFailure.accept(ex);
            }
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
