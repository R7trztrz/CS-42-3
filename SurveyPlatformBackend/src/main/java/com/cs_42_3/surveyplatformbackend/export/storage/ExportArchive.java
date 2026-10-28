package com.cs_42_3.surveyplatformbackend.export.storage;

import org.apache.commons.csv.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.*;

/**
 * Owns one UTF-8 ZIP stream; category writers finish entries without closing the archive.
 *
 * @author Simon Tian
 */
public class ExportArchive implements Closeable {
    private final ZipOutputStream zip;

    public ExportArchive(OutputStream output) {
        zip = new ZipOutputStream(output, StandardCharsets.UTF_8);
    }

    public CSVPrinter csv(String name, String... headers) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        return new CSVPrinter(new OutputStreamWriter(zip, StandardCharsets.UTF_8),
                CSVFormat.RFC4180.builder().setHeader(headers).get());
    }

    public void finish(CSVPrinter printer) throws IOException {
        printer.flush();
        zip.closeEntry();
    }

    public void text(String name, String value) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(value.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    @Override
    public void close() throws IOException {
        zip.close();
    }
}
