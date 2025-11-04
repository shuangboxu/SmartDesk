package com.smartdesk.core.chat;

import com.smartdesk.storage.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides persistence utilities for chat sessions and their messages.
 */
public final class ChatHistoryService {

    private static final Logger LOGGER = Logger.getLogger(ChatHistoryService.class.getName());

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private static final String INSERT_SESSION_SQL = """
        INSERT INTO chat_sessions (id, default_title, title, auto_title, model_name, created_at, updated_at)
        VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

    private static final String UPDATE_SESSION_SQL = """
        UPDATE chat_sessions
           SET title = ?, auto_title = ?, model_name = ?, updated_at = ?
         WHERE id = ?
        """;

    private static final String SELECT_SESSIONS_SQL = """
        SELECT id, default_title, title, auto_title, model_name, created_at, updated_at
          FROM chat_sessions
         ORDER BY datetime(updated_at) DESC
        """;

    private static final String INSERT_MESSAGE_SQL = """
        INSERT INTO chat_messages (session_id, sender, content, timestamp)
        VALUES (?, ?, ?, ?)
        """;

    private static final String SELECT_MESSAGES_FOR_SESSION_SQL = """
        SELECT id, sender, content, timestamp
          FROM chat_messages
         WHERE session_id = ?
         ORDER BY datetime(timestamp), id
        """;

    private final DatabaseManager databaseManager;

    public ChatHistoryService(final DatabaseManager databaseManager) {
        this.databaseManager = Objects.requireNonNull(databaseManager, "databaseManager");
    }

    /**
     * Loads all persisted chat sessions including their messages.
     *
     * @return list of {@link ChatSession} instances ordered by last update time
     */
    public List<ChatSession> loadAllSessions() {
        final List<ChatSession> sessions = new ArrayList<>();
        final Map<String, ChatSession> sessionMap = new LinkedHashMap<>();
        final Map<String, LocalDateTime> persistedUpdatedAt = new LinkedHashMap<>();

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement sessionStatement = connection.prepareStatement(SELECT_SESSIONS_SQL);
             ResultSet sessionResult = sessionStatement.executeQuery()) {

            while (sessionResult.next()) {
                final String id = sessionResult.getString("id");
                final String defaultTitle = sessionResult.getString("default_title");
                final String title = sessionResult.getString("title");
                final boolean autoTitle = sessionResult.getInt("auto_title") == 1;
                final String modelName = sessionResult.getString("model_name");
                final LocalDateTime createdAt = LocalDateTime.parse(sessionResult.getString("created_at"), FORMATTER);
                final LocalDateTime updatedAt = LocalDateTime.parse(sessionResult.getString("updated_at"), FORMATTER);

                final ChatSession session = new ChatSession(UUID.fromString(id), defaultTitle, title,
                    autoTitle, createdAt, updatedAt, modelName);
                sessionMap.put(id, session);
                persistedUpdatedAt.put(id, updatedAt);
                sessions.add(session);
            }

            try (PreparedStatement messageStatement = connection.prepareStatement(SELECT_MESSAGES_FOR_SESSION_SQL)) {
                for (Map.Entry<String, ChatSession> entry : sessionMap.entrySet()) {
                    messageStatement.setString(1, entry.getKey());
                    try (ResultSet messageResult = messageStatement.executeQuery()) {
                        while (messageResult.next()) {
                            final ChatMessage.Sender sender = ChatMessage.Sender.valueOf(messageResult.getString("sender"));
                            final String content = messageResult.getString("content");
                            final LocalDateTime timestamp = LocalDateTime.parse(messageResult.getString("timestamp"), FORMATTER);
                            entry.getValue().addMessage(ChatMessage.of(sender, content, timestamp));
                        }
                    }
                }
            }

            persistedUpdatedAt.forEach((sessionId, timestamp) -> {
                ChatSession session = sessionMap.get(sessionId);
                if (session != null) {
                    session.setUpdatedAt(timestamp);
                }
            });
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to load chat sessions", ex);
            throw new IllegalStateException("Failed to load chat sessions", ex);
        }

        return sessions;
    }

    /**
     * Creates and persists a new chat session.
     */
    public ChatSession createSession(final String defaultTitle, final String modelName) {
        final LocalDateTime now = LocalDateTime.now();
        final ChatSession session = new ChatSession(UUID.randomUUID(),
            defaultTitle,
            defaultTitle,
            true,
            now,
            now,
            modelName);

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_SESSION_SQL)) {

            statement.setString(1, session.getId().toString());
            statement.setString(2, session.getDefaultTitle());
            statement.setString(3, session.getTitle());
            statement.setInt(4, session.isAutoTitle() ? 1 : 0);
            statement.setString(5, session.getModelName());
            statement.setString(6, FORMATTER.format(now));
            statement.setString(7, FORMATTER.format(now));
            statement.executeUpdate();
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to create chat session", ex);
            throw new IllegalStateException("Failed to create chat session", ex);
        }

        return session;
    }

    /**
     * Persists the supplied message and updates the session metadata atomically.
     */
    public void persistMessage(final ChatSession session, final ChatMessage message) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(message, "message");

        try (Connection connection = databaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement insertMessage = connection.prepareStatement(INSERT_MESSAGE_SQL);
                 PreparedStatement updateSession = connection.prepareStatement(UPDATE_SESSION_SQL)) {

                insertMessage.setString(1, session.getId().toString());
                insertMessage.setString(2, message.getSender().name());
                insertMessage.setString(3, message.getContent());
                insertMessage.setString(4, FORMATTER.format(message.getTimestamp()));
                insertMessage.executeUpdate();

                updateSession.setString(1, session.getTitle());
                updateSession.setInt(2, session.isAutoTitle() ? 1 : 0);
                updateSession.setString(3, session.getModelName());
                updateSession.setString(4, FORMATTER.format(session.getUpdatedAt()));
                updateSession.setString(5, session.getId().toString());
                updateSession.executeUpdate();

                connection.commit();
            } catch (SQLException inner) {
                connection.rollback();
                throw inner;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to persist chat message", ex);
            throw new IllegalStateException("Failed to persist chat message", ex);
        }
    }

    /**
     * Updates session metadata such as the associated model. The method leaves
     * chat messages untouched.
     */
    public void updateSessionMetadata(final ChatSession session) {
        Objects.requireNonNull(session, "session");

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_SESSION_SQL)) {

            statement.setString(1, session.getTitle());
            statement.setInt(2, session.isAutoTitle() ? 1 : 0);
            statement.setString(3, session.getModelName());
            statement.setString(4, FORMATTER.format(session.getUpdatedAt()));
            statement.setString(5, session.getId().toString());
            statement.executeUpdate();
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to update chat session metadata", ex);
            throw new IllegalStateException("Failed to update chat session metadata", ex);
        }
    }
}
