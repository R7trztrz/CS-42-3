package com.cs_42_3.surveyplatformbackend.export.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.*;
import java.nio.file.*;
import java.util.UUID;

/**
 * Writes only server-generated UUID filenames outside static web resources.
 *
 * @author Simon Tian
 */
@Component
public class LocalExportStorage implements ExportStorage {
    private final Path root;

    public LocalExportStorage(@Value("${app.exports.storage-directory:./data/exports}") String directory) {
        root = Path.of(directory).toAbsolutePath().normalize();
    }

    @Override
    public OutputStream createTemporary(UUID id) throws IOException {
        Files.createDirectories(root);
        return Files.newOutputStream(path(id, ".part"));
    }

    @Override
    public Path createWorkspace(UUID id) throws IOException {
        clearWorkspace(path(id, ".work"));
        return Files.createDirectories(path(id, ".work"));
    }

    /** Removes only flat temporary files inside this server-generated job directory. */
    public static void clearWorkspace(Path directory) throws IOException {
        if (directory.getFileName() == null || !directory.getFileName().toString()
                .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.work")) {
            throw new IOException("Refusing to clean an unmanaged export workspace.");
        }
        if (Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) {
            try (var files = Files.newDirectoryStream(directory)) {
                for (Path file : files) {
                    if (Files.isDirectory(file, LinkOption.NOFOLLOW_LINKS)) {
                        throw new IOException("Unexpected nested export workspace directory.");
                    }
                    Files.deleteIfExists(file);
                }
            }
        }
        // Delete a symbolic link itself instead of following it into another directory.
        Files.deleteIfExists(directory);
    }

    @Override
    public void publish(UUID id) throws IOException {
        // Readers cannot access an incomplete ZIP; task completion is committed afterwards.
        try {
            Files.move(path(id, ".part"), path(id, ".zip"),
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException unsupported) {
            Files.move(path(id, ".part"), path(id, ".zip"), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Override
    public InputStream open(UUID id) throws IOException {
        return Files.newInputStream(path(id, ".zip"));
    }

    @Override
    public void delete(UUID id) throws IOException {
        Files.deleteIfExists(path(id, ".part"));
        Files.deleteIfExists(path(id, ".zip"));
        clearWorkspace(path(id, ".work"));
    }

    private Path path(UUID id, String suffix) {
        return root.resolve(id.toString() + suffix);
    }
}
