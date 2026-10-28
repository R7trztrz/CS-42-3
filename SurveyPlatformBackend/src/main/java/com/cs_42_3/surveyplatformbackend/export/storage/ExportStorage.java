package com.cs_42_3.surveyplatformbackend.export.storage;

import java.io.*;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Private export artifact storage, independent of public study image storage.
 *
 * @author Simon Tian
 */
public interface ExportStorage {
    OutputStream createTemporary(UUID id) throws IOException;
    Path createWorkspace(UUID id) throws IOException;
    void publish(UUID id) throws IOException;
    InputStream open(UUID id) throws IOException;
    void delete(UUID id) throws IOException;
}
