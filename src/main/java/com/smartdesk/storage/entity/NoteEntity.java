package com.smartdesk.storage.entity;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a persisted note record stored in the SmartDesk SQLite database.
 * <p>
 * The entity is intentionally lightweight and mirrors the structure of the
 * {@code notes} table managed by {@link com.smartdesk.storage.DatabaseManager}.
 * All mutator methods perform null validation in order to keep the domain state
 * consistent before it is handed over to the persistence layer.
 * </p>
 */
public class NoteEntity {

    private Long id;
    private String title;
    private String content;
    private String tag;
    private LocalDateTime date;

    /**
     * Creates an empty note entity. Fields can be set through the corresponding
     * setter methods before persisting the note.
     */
    public NoteEntity() {
        // Default constructor for frameworks and manual instantiation.
    }

    /**
     * Creates a fully populated note entity.
     *
     * @param id      the unique identifier as stored in the database
     * @param title   the note title, must not be {@code null}
     * @param content the note body text, may be {@code null}
     * @param tag     an optional tag used for categorisation, may be {@code null}
     * @param date    the timestamp representing when the note was created or last updated
     */
    public NoteEntity(final Long id, final String title, final String content,
                      final String tag, final LocalDateTime date) {
        this.id = id;
        this.title = Objects.requireNonNull(title, "title must not be null");
        this.content = content;
        this.tag = tag;
        this.date = Objects.requireNonNull(date, "date must not be null");
    }

    /**
     * Returns the database identifier.
     *
     * @return the note ID or {@code null} when the note has not yet been persisted
     */
    public Long getId() {
        return id;
    }

    /**
     * Updates the database identifier. This method should only be called by the
     * persistence layer after an insert operation has been executed successfully.
     *
     * @param id the newly assigned identifier
     */
    public void setId(final Long id) {
        this.id = id;
    }

    /**
     * Returns the human readable title of the note.
     *
     * @return non-null title string
     */
    public String getTitle() {
        return title;
    }

    /**
     * Updates the note title.
     *
     * @param title the new title, must not be {@code null}
     */
    public void setTitle(final String title) {
        this.title = Objects.requireNonNull(title, "title must not be null");
    }

    /**
     * Returns the textual content of the note.
     *
     * @return the content or {@code null} if the note currently only contains a title
     */
    public String getContent() {
        return content;
    }

    /**
     * Updates the textual content associated with this note.
     *
     * @param content the content text, may be {@code null}
     */
    public void setContent(final String content) {
        this.content = content;
    }

    /**
     * Returns the tag label that classifies the note.
     *
     * @return the tag or {@code null} if no tag is associated
     */
    public String getTag() {
        return tag;
    }

    /**
     * Updates the tag label assigned to the note.
     *
     * @param tag the tag text, may be {@code null}
     */
    public void setTag(final String tag) {
        this.tag = tag;
    }

    /**
     * Returns the timestamp that describes when the note was created or last updated.
     *
     * @return a non-null {@link LocalDateTime}
     */
    public LocalDateTime getDate() {
        return date;
    }

    /**
     * Updates the timestamp associated with the note.
     *
     * @param date the timestamp, must not be {@code null}
     */
    public void setDate(final LocalDateTime date) {
        this.date = Objects.requireNonNull(date, "date must not be null");
    }

    @Override
    public String toString() {
        return "NoteEntity{" +
            "id=" + id +
            ", title='" + title + '\'' +
            ", tag='" + tag + '\'' +
            ", date=" + date +
            '}';
    }
}
