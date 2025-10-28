package com.smartdesk.core.note;

import com.smartdesk.storage.DatabaseManager;
import com.smartdesk.storage.entity.NoteEntity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides CRUD (Create, Read, Update, Delete) operations for {@link NoteEntity}
 * instances. The service acts as the boundary between UI/business logic and the
 * persistence layer managed by {@link DatabaseManager}.
 */
public class NoteService {

    private static final Logger LOGGER = Logger.getLogger(NoteService.class.getName());

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private static final String INSERT_NOTE_SQL = "INSERT INTO notes (title, content, tag, date) VALUES (?, ?, ?, ?)";
    private static final String SELECT_NOTE_SQL = "SELECT id, title, content, tag, date FROM notes WHERE id = ?";
    private static final String SELECT_ALL_NOTES_SQL = "SELECT id, title, content, tag, date FROM notes ORDER BY date DESC";
    private static final String UPDATE_NOTE_SQL = "UPDATE notes SET title = ?, content = ?, tag = ?, date = ? WHERE id = ?";
    private static final String DELETE_NOTE_SQL = "DELETE FROM notes WHERE id = ?";

    private final DatabaseManager databaseManager;

    /**
     * Creates a new note service that uses the provided {@link DatabaseManager}
     * for all persistence related tasks.
     *
     * @param databaseManager the database manager, must not be {@code null}
     */
    public NoteService(final DatabaseManager databaseManager) {
        this.databaseManager = Objects.requireNonNull(databaseManager, "databaseManager must not be null");
    }

    /**
     * Persists a new note in the database.
     *
     * @param note the note to create; the title must not be {@code null} or blank
     * @return the persisted note with its database identifier populated
     */
    public NoteEntity createNote(final NoteEntity note) {
        validateNoteForCreate(note);

        final LocalDateTime timestamp = note.getDate() != null ? note.getDate() : LocalDateTime.now();
        note.setDate(timestamp);

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_NOTE_SQL, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, note.getTitle());
            statement.setString(2, note.getContent());
            statement.setString(3, note.getTag());
            statement.setString(4, DATE_FORMATTER.format(timestamp));
            final int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                throw new IllegalStateException("Creating note failed, no rows affected.");
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    note.setId(generatedKeys.getLong(1));
                } else {
                    throw new IllegalStateException("Creating note failed, no ID obtained.");
                }
            }

            return note;
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to create note", ex);
            throw new IllegalStateException("Failed to create note", ex);
        }
    }

    /**
     * Retrieves a note for the given identifier.
     *
     * @param noteId the database identifier
     * @return an {@link Optional} describing the note or empty if no note exists for the ID
     */
    public Optional<NoteEntity> getNoteById(final long noteId) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_NOTE_SQL)) {

            statement.setLong(1, noteId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
            }
            return Optional.empty();
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to fetch note with id=" + noteId, ex);
            throw new IllegalStateException("Failed to fetch note with id=" + noteId, ex);
        }
    }

    /**
     * Loads all notes stored in the database ordered by the last modification date.
     *
     * @return immutable list containing all persisted notes
     */
    public List<NoteEntity> getAllNotes() {
        final List<NoteEntity> notes = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_ALL_NOTES_SQL);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                notes.add(mapRow(resultSet));
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to fetch all notes", ex);
            throw new IllegalStateException("Failed to fetch all notes", ex);
        }
        return List.copyOf(notes);
    }

    /**
     * Updates an existing note in the database.
     *
     * @param note the note to update; must already possess a database identifier
     * @return {@code true} when a row was updated, {@code false} otherwise
     */
    public boolean updateNote(final NoteEntity note) {
        validateNoteForUpdate(note);

        final LocalDateTime timestamp = note.getDate() != null ? note.getDate() : LocalDateTime.now();
        note.setDate(timestamp);

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_NOTE_SQL)) {

            statement.setString(1, note.getTitle());
            statement.setString(2, note.getContent());
            statement.setString(3, note.getTag());
            statement.setString(4, DATE_FORMATTER.format(timestamp));
            statement.setLong(5, note.getId());

            return statement.executeUpdate() > 0;
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to update note with id=" + note.getId(), ex);
            throw new IllegalStateException("Failed to update note with id=" + note.getId(), ex);
        }
    }

    /**
     * Deletes a note with the supplied identifier.
     *
     * @param noteId the identifier to delete
     * @return {@code true} if the note existed and was removed
     */
    public boolean deleteNote(final long noteId) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_NOTE_SQL)) {

            statement.setLong(1, noteId);
            return statement.executeUpdate() > 0;
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to delete note with id=" + noteId, ex);
            throw new IllegalStateException("Failed to delete note with id=" + noteId, ex);
        }
    }

    private void validateNoteForCreate(final NoteEntity note) {
        Objects.requireNonNull(note, "note must not be null");
        if (note.getTitle() == null || note.getTitle().isBlank()) {
            throw new IllegalArgumentException("A note must contain a non-empty title");
        }
    }

    private void validateNoteForUpdate(final NoteEntity note) {
        validateNoteForCreate(note);
        if (note.getId() == null) {
            throw new IllegalArgumentException("Cannot update a note without an identifier");
        }
    }

    private NoteEntity mapRow(final ResultSet resultSet) throws SQLException {
        final NoteEntity note = new NoteEntity();
        note.setId(resultSet.getLong("id"));
        note.setTitle(resultSet.getString("title"));
        note.setContent(resultSet.getString("content"));
        note.setTag(resultSet.getString("tag"));
        final String dateValue = resultSet.getString("date");
        if (dateValue != null && !dateValue.isBlank()) {
            note.setDate(LocalDateTime.parse(dateValue, DATE_FORMATTER));
        } else {
            note.setDate(LocalDateTime.now());
        }
        return note;
    }
}
