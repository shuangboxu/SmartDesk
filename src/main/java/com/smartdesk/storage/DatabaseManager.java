package com.smartdesk.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Centralised management of the SQLite database connection used by SmartDesk.
 * <p>
 * The manager is responsible for initialising the schema and for handing out
 * short-lived JDBC connections to callers. Each method returns a new
 * {@link Connection} instance; it is therefore the caller's responsibility to
 * close the connection using try-with-resources in order to keep database
 * resources under control.
 * </p>
 */
public class DatabaseManager {

    /** Logger used to report initialisation and connection issues. */
    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());

    /** Default file name for the SQLite database if no explicit URL is provided. */
    public static final String DEFAULT_DATABASE_FILE = "smartdesk.db";

    /** Directory (relative to the user home) that stores persistent application data. */
    private static final String DEFAULT_DATA_DIRECTORY = ".smartdesk";

    /** JDBC connection prefix for SQLite databases. */
    private static final String SQLITE_JDBC_PREFIX = "jdbc:sqlite:";

    /**
     * DDL statement that creates the {@code notes} table. The table mirrors the
     * state of {@link com.smartdesk.storage.entity.NoteEntity}.
     */
    public static final String CREATE_NOTES_TABLE_SQL = """
        CREATE TABLE IF NOT EXISTS notes (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            title TEXT NOT NULL,
            content TEXT,
            tag TEXT,
            date TEXT NOT NULL
        )
        """;

    /**
     * DDL statement that creates the {@code tasks} table. The schema supports a
     * rich set of scheduling attributes including reminders and status
     * tracking.
     */
    public static final String CREATE_TASKS_TABLE_SQL = """
        CREATE TABLE IF NOT EXISTS tasks (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            title TEXT NOT NULL,
            description TEXT,
            start_at TEXT,
            due_at TEXT,
            priority INTEGER NOT NULL,
            type TEXT NOT NULL,
            reminder_enabled INTEGER NOT NULL,
            reminder_lead_minutes INTEGER NOT NULL,
            status TEXT NOT NULL,
            last_reminded_at TEXT,
            created_at TEXT NOT NULL,
            updated_at TEXT NOT NULL
        )
        """;

    /** Creates an index that accelerates reminder lookups. */
    public static final String CREATE_TASKS_REMINDER_INDEX_SQL = """
        CREATE INDEX IF NOT EXISTS idx_tasks_reminder
            ON tasks (reminder_enabled, status, due_at)
        """;

    /** DDL statement creating the {@code chat_sessions} table. */
    public static final String CREATE_CHAT_SESSIONS_TABLE_SQL = """
        CREATE TABLE IF NOT EXISTS chat_sessions (
            id TEXT PRIMARY KEY,
            default_title TEXT NOT NULL,
            title TEXT NOT NULL,
            auto_title INTEGER NOT NULL,
            model_name TEXT,
            created_at TEXT NOT NULL,
            updated_at TEXT NOT NULL
        )
        """;

    /** DDL statement creating the {@code chat_messages} table. */
    public static final String CREATE_CHAT_MESSAGES_TABLE_SQL = """
        CREATE TABLE IF NOT EXISTS chat_messages (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            session_id TEXT NOT NULL,
            sender TEXT NOT NULL,
            content TEXT NOT NULL,
            timestamp TEXT NOT NULL,
            FOREIGN KEY (session_id) REFERENCES chat_sessions(id) ON DELETE CASCADE
        )
        """;

    /** DDL statement creating the attachment table backing uploaded files. */
    public static final String CREATE_CHAT_ATTACHMENTS_TABLE_SQL = """
        CREATE TABLE IF NOT EXISTS chat_attachments (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            message_id INTEGER NOT NULL,
            file_name TEXT NOT NULL,
            mime_type TEXT,
            data BLOB NOT NULL,
            file_id TEXT,
            FOREIGN KEY (message_id) REFERENCES chat_messages(id) ON DELETE CASCADE
        )
        """;

    /** Index accelerating chat history retrieval ordered by timestamp. */
    public static final String CREATE_CHAT_MESSAGES_INDEX_SQL = """
        CREATE INDEX IF NOT EXISTS idx_chat_messages_session
            ON chat_messages (session_id, timestamp)
        """;

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("SQLite JDBC driver not found on the classpath", ex);
        }
    }

    private final String databaseUrl;

    /**
     * Creates a database manager using the default database file located in the
     * working directory ({@value #DEFAULT_DATABASE_FILE}).
     */
    public DatabaseManager() {
        this(resolveDefaultDatabaseUrl());
    }

    /**
     * Creates a database manager for a specific SQLite connection URL.
     *
     * @param databaseUrl the JDBC connection string, must not be {@code null}
     */
    public DatabaseManager(final String databaseUrl) {
        this.databaseUrl = Objects.requireNonNull(databaseUrl, "databaseUrl must not be null");
        initializeDatabase();
    }

    private static String resolveDefaultDatabaseUrl() {
        final String userHome = System.getProperty("user.home");
        if (userHome == null || userHome.isBlank()) {
            LOGGER.log(Level.WARNING, "User home directory is undefined; falling back to working directory for database storage.");
            return SQLITE_JDBC_PREFIX + DEFAULT_DATABASE_FILE;
        }

        final Path dataDirectory = Path.of(userHome, DEFAULT_DATA_DIRECTORY);
        try {
            Files.createDirectories(dataDirectory);
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Failed to create SmartDesk data directory at {0}, falling back to working directory.", dataDirectory);
            LOGGER.log(Level.FINE, "Directory creation failure", ex);
            return SQLITE_JDBC_PREFIX + DEFAULT_DATABASE_FILE;
        }

        final Path databaseFile = dataDirectory.resolve(DEFAULT_DATABASE_FILE);
        return SQLITE_JDBC_PREFIX + databaseFile.toAbsolutePath();
    }

    /**
     * Returns a fresh JDBC connection. Callers should use try-with-resources in
     * order to close it after usage.
     *
     * @return a new {@link Connection}
     * @throws SQLException if the underlying JDBC driver cannot establish a connection
     */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(databaseUrl);
    }

    /**
     * Returns the JDBC URL the manager uses for connections. Exposed primarily
     * for diagnostic purposes.
     *
     * @return the JDBC connection URL
     */
    public String getDatabaseUrl() {
        return databaseUrl;
    }

    /**
     * Initialises the SQLite database by creating the required tables. The
     * method is idempotent and can safely be called multiple times.
     */
    public final void initializeDatabase() {
        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            statement.execute(CREATE_NOTES_TABLE_SQL);
            statement.execute(CREATE_TASKS_TABLE_SQL);
            statement.execute(CREATE_TASKS_REMINDER_INDEX_SQL);
            statement.execute(CREATE_CHAT_SESSIONS_TABLE_SQL);
            statement.execute(CREATE_CHAT_MESSAGES_TABLE_SQL);
            statement.execute(CREATE_CHAT_ATTACHMENTS_TABLE_SQL);
            statement.execute(CREATE_CHAT_MESSAGES_INDEX_SQL);
            upgradeChatAttachmentsTable(connection);
            LOGGER.log(Level.INFO, "Database initialised using URL: {0}", databaseUrl);
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to initialise SQLite database", ex);
            throw new IllegalStateException("Failed to initialise SQLite database", ex);
        }
    }

    private void upgradeChatAttachmentsTable(final Connection connection) throws SQLException {
        ensureColumn(connection, "chat_attachments", "file_id TEXT", "file_id");
    }

    private void ensureColumn(final Connection connection, final String table, final String columnDefinition,
                              final String columnName) throws SQLException {
        boolean present = false;
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) {
                if (columnName.equalsIgnoreCase(rs.getString("name"))) {
                    present = true;
                    break;
                }
            }
        }
        if (!present) {
            try (Statement alter = connection.createStatement()) {
                alter.execute("ALTER TABLE " + table + " ADD COLUMN " + columnDefinition);
            }
        }
    }
}
