package com.smartdesk.storage;

import java.sql.Connection;
import java.sql.DriverManager;
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
        this(SQLITE_JDBC_PREFIX + DEFAULT_DATABASE_FILE);
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
            LOGGER.log(Level.INFO, "Database initialised using URL: {0}", databaseUrl);
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to initialise SQLite database", ex);
            throw new IllegalStateException("Failed to initialise SQLite database", ex);
        }
    }
}
