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
    }

    private Path path(UUID id, String suffix) {
        return root.resolve(id.toString() + suffix);
    }
}
