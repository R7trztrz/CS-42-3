package com.cs_42_3.surveyplatformbackend.export;

import com.cs_42_3.surveyplatformbackend.export.storage.LocalExportStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/** Private artifact paths and cleanup boundaries; no Docker or application context. */
class LocalExportStorageSafetyTest {
    @TempDir Path root;

    @Test
    void temporaryBytesCannotBeReadBeforePublishAndJobsStayIsolated() throws Exception {
        var storage = new LocalExportStorage(root.resolve("exports").toString());
        UUID first = UUID.randomUUID(), second = UUID.randomUUID();
        try (var out = storage.createTemporary(first)) { out.write("first".getBytes(StandardCharsets.UTF_8)); }
        assertThrows(IOException.class, () -> storage.open(first));
        storage.publish(first);
        try (var out = storage.createTemporary(second)) { out.write("second".getBytes(StandardCharsets.UTF_8)); }
        storage.publish(second);
        storage.delete(first);
        assertThrows(IOException.class, () -> storage.open(first));
        try (var in = storage.open(second)) {
            assertEquals("second", new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @Test
    void cleanupRefusesAnUnmanagedDirectoryWithoutChangingItsContents() throws Exception {
        Path unrelated = Files.createDirectory(root.resolve("unrelated.work"));
        Path marker = Files.writeString(unrelated.resolve("keep.txt"), "keep");
        assertThrows(IOException.class, () -> LocalExportStorage.clearWorkspace(unrelated));
        assertEquals("keep", Files.readString(marker));
    }

    @Test
    void cleanupRemovesAnOutsideSymlinkWithoutFollowingItsTarget() throws Exception {
        Path outside = Files.createDirectory(root.resolve("outside"));
        Path marker = Files.writeString(outside.resolve("keep.txt"), "keep");
        Path link = root.resolve(UUID.randomUUID() + ".work");
        Files.createSymbolicLink(link, outside);
        LocalExportStorage.clearWorkspace(link);
        assertFalse(Files.exists(link, LinkOption.NOFOLLOW_LINKS));
        assertEquals("keep", Files.readString(marker));
    }

    @Test
    void cleanupRefusesNestedDirectoriesAndPreservesTheirFiles() throws Exception {
        Path workspace = Files.createDirectory(root.resolve(UUID.randomUUID() + ".work"));
        Path nested = Files.createDirectory(workspace.resolve("nested"));
        Path marker = Files.writeString(nested.resolve("keep.txt"), "keep");
        assertThrows(IOException.class, () -> LocalExportStorage.clearWorkspace(workspace));
        assertEquals("keep", Files.readString(marker));
    }
}
