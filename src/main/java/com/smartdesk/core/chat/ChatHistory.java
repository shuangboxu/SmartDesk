package com.smartdesk.core.chat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Maintains the ordered history of messages exchanged with the assistant.
 */
public final class ChatHistory {

    private final List<ChatMessage> messages = new ArrayList<>();

    public void add(final ChatMessage message) {
        messages.add(message);
    }

    public void replaceWith(final List<ChatMessage> snapshot) {
        messages.clear();
        if (snapshot != null) {
            messages.addAll(snapshot);
        }
    }

    public List<ChatMessage> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    public void clear() {
        messages.clear();
    }
}
