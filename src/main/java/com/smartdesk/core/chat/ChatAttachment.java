package com.smartdesk.core.chat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Represents a binary asset shared with the chat assistant (e.g. images or documents).
 */
public final class ChatAttachment {

    private static final DecimalFormat SIZE_FORMAT = new DecimalFormat("#,##0.#");

    private final String fileName;
    private final String mimeType;
    private final Path filePath;
    private final long size;
    private volatile Long databaseId;
    private volatile String providerFileId;

    private ChatAttachment(final String fileName,
                           final String mimeType,
                           final Path filePath,
                           final long size,
                           final Long databaseId,
                           final String providerFileId) {
        this.fileName = Objects.requireNonNull(fileName, "fileName");
        this.mimeType = mimeType == null ? "application/octet-stream" : mimeType;
        this.filePath = Objects.requireNonNull(filePath, "filePath");
        this.size = size;
        this.databaseId = databaseId;
        this.providerFileId = providerFileId;
    }

    /**
     * Builds an attachment from a local file. The method reads the full content eagerly.
     */
    public static ChatAttachment fromFile(final Path file) throws IOException {
        Objects.requireNonNull(file, "file");
        String name = file.getFileName() == null ? "上传文件" : file.getFileName().toString();
        String mime = Files.probeContentType(file);
        Path stored = AttachmentStorage.getInstance().store(file);
        long size = Files.size(stored);
        return new ChatAttachment(name, mime, stored, size, null, null);
    }

    /**
     * Recreates an attachment from database payload.
     */
    public static ChatAttachment fromDatabase(final long id,
                                              final String fileName,
                                              final String mimeType,
                                              final byte[] data,
                                              final String providerFileId) throws IOException {
        Path restored = AttachmentStorage.getInstance().storeBytes(fileName, data);
        return new ChatAttachment(fileName,
            mimeType,
            restored,
            Files.size(restored),
            id,
            providerFileId);
    }

    public String getFileName() {
        return fileName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public Path getFilePath() {
        return filePath;
    }

    public long getSize() {
        return size;
    }

    public OptionalLong getDatabaseId() {
        return databaseId == null ? OptionalLong.empty() : OptionalLong.of(databaseId);
    }

    public void setDatabaseId(final long id) {
        this.databaseId = id;
    }

    public Optional<String> getProviderFileId() {
        return Optional.ofNullable(providerFileId);
    }

    public void setProviderFileId(final String providerFileId) {
        this.providerFileId = providerFileId;
    }

    public long size() {
        return size;
    }

    public String describe() {
        return fileName + " (" + mimeType + ", " + humanReadableSize(size()) + ")";
    }

    public byte[] readAllBytes() throws IOException {
        return Files.readAllBytes(filePath);
    }

    private static String humanReadableSize(final long bytes) {
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024d;
        if (kb < 1024) return SIZE_FORMAT.format(kb) + " KB";
        double mb = kb / 1024d;
        if (mb < 1024) return SIZE_FORMAT.format(mb) + " MB";
        double gb = mb / 1024d;
        return SIZE_FORMAT.format(gb) + " GB";
    }
}
