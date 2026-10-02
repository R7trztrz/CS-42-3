package com.cs_42_3.surveyplatformbackend.asset.storage;

import com.cs_42_3.surveyplatformbackend.asset.exception.AssetException;
import com.cs_42_3.surveyplatformbackend.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.file.*;

/**
 * Stores server-generated filenames in a dedicated persistent directory.
 *
 * @author Simon Tian
 */
@Component
public class LocalAssetStorage implements AssetStorage {
    private final Path root;

    public LocalAssetStorage(@Value("${app.assets.storage-directory:./data/assets}") String directory) {
        root = Path.of(directory).toAbsolutePath().normalize();
    }

    private Path path(String key) {
        if (!key.matches("[0-9a-f-]{36}\\.(jpg|png|webp)")) {
            throw new IllegalArgumentException("Invalid internal asset key.");
        }
        return root.resolve(key);
    }

    @Override
    public void write(String key, byte[] bytes) {
        Path file = path(key);
        boolean created = false;
        try {
            Files.createDirectories(root);
            // CREATE_NEW never overwrites a previously stored image.
            try (var output = Files.newOutputStream(file, StandardOpenOption.CREATE_NEW)) {
                created = true;
                output.write(bytes);
            }
        } catch (IOException exception) {
            if (created) {
                try {
                    Files.deleteIfExists(file);
                } catch (IOException cleanup) {
                    exception.addSuppressed(cleanup);
                }
            }
            throw storageFailure();
        }
    }

    @Override
    public byte[] read(String key) {
        Path file = path(key);
        if (!exists(key)) throw AssetException.notFound();
        try {
            if (Files.size(file) > 5L * 1024 * 1024) throw storageFailure();
            return Files.readAllBytes(file);
        } catch (IOException exception) {
            throw storageFailure();
        }
    }

    @Override
    public boolean exists(String key) {
        return Files.isRegularFile(path(key), LinkOption.NOFOLLOW_LINKS);
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(path(key));
        } catch (IOException exception) {
            throw storageFailure();
        }
    }

    private AssetException storageFailure() {
        return new AssetException(ErrorCode.ASSET_STORAGE_FAILED, HttpStatus.INTERNAL_SERVER_ERROR,
                "Image storage is temporarily unavailable.");
    }
}
