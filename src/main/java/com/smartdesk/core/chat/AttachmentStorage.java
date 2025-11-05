package com.smartdesk.core.chat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

/**
 * Stores uploaded attachments on disk so they can be streamed when required by providers.
 */
public final class AttachmentStorage {

    private static final AttachmentStorage INSTANCE = new AttachmentStorage();

    private final Path storageDirectory;

    private AttachmentStorage() {
        this.storageDirectory = resolveStorageDirectory();
    }

    public static AttachmentStorage getInstance() {
        return INSTANCE;
    }

    /**
     * Copies the provided file into the SmartDesk attachment directory and returns the new path.
     */
    public Path store(final Path source) throws IOException {
        Objects.requireNonNull(source, "source");
        String fileName = source.getFileName() == null ? UUID.randomUUID() + "-attachment" : source.getFileName().toString();
        Path target = storageDirectory.resolve(UUID.randomUUID() + "-" + fileName);
        Files.createDirectories(storageDirectory);
        return Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Materialises a file from raw bytes (used when loading from the database).
     */
    public Path storeBytes(final String originalFileName, final byte[] data) throws IOException {
        Objects.requireNonNull(data, "data");
        String safeName = (originalFileName == null || originalFileName.isBlank())
            ? UUID.randomUUID() + "-attachment"
            : originalFileName;
        Path target = storageDirectory.resolve(UUID.randomUUID() + "-" + safeName);
        Files.createDirectories(storageDirectory);
        return Files.write(target, data);
    }

    private Path resolveStorageDirectory() {
        String userHome = System.getProperty("user.home");
        if (userHome == null || userHome.isBlank()) {
            return Path.of("attachments");
        }
        return Path.of(userHome, ".smartdesk", "attachments");
    }
}
