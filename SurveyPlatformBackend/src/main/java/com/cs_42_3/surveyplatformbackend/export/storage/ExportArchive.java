package com.cs_42_3.surveyplatformbackend.export.storage;

import org.apache.commons.csv.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/**
 * Owns one UTF-8 ZIP stream; category writers finish entries without closing the archive.
 *
 * @author Simon Tian
 */
public class ExportArchive implements Closeable {
    private final ZipOutputStream zip;
    private final Path workspace;
    private final Map<String, StagedCsv> staged = new LinkedHashMap<>();

    public ExportArchive(OutputStream output) {
        this(output, null);
    }

    /** Disk staging lets session writers append to a bounded number of shared CSV files. */
    public ExportArchive(OutputStream output, Path workspace) {
        zip = new ZipOutputStream(output, StandardCharsets.UTF_8);
        this.workspace = workspace;
    }

    public CSVPrinter csv(String name, String... headers) throws IOException {
        if (workspace != null) {
            if (!name.matches("[a-z_]+\\.csv")) throw new IOException("Invalid internal CSV filename.");
            var existing = staged.get(name);
            if (existing != null) {
                if (!existing.headers().equals(List.of(headers))) throw new IOException("CSV header mismatch.");
                return existing.printer();
            }
            var printer = new CSVPrinter(Files.newBufferedWriter(workspace.resolve(name), StandardCharsets.UTF_8),
                    CSVFormat.RFC4180.builder().setHeader(headers).get());
            staged.put(name, new StagedCsv(printer, List.of(headers), printer.getRecordCount()));
            return printer;
        }
        zip.putNextEntry(new ZipEntry(name));
        return new CSVPrinter(new OutputStreamWriter(zip, StandardCharsets.UTF_8),
                CSVFormat.RFC4180.builder().setHeader(headers).get());
    }

    public void finish(CSVPrinter printer) throws IOException {
        printer.flush();
        if (workspace == null) zip.closeEntry();
    }

    public long records(String name) {
        var entry = staged.get(name);
        return entry == null ? 0 : entry.printer().getRecordCount() - entry.headerRecords();
    }

    /** Called only after every session has validated; failed exports never pack partial CSV data. */
    public void complete() throws IOException {
        if (workspace == null) return;
        for (var entry : staged.values()) entry.printer().close();
        for (String name : staged.keySet()) {
            zip.putNextEntry(new ZipEntry(name));
            Files.copy(workspace.resolve(name), zip);
            zip.closeEntry();
        }
        LocalExportStorage.clearWorkspace(workspace);
    }

    public void text(String name, String value) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(value.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    @Override
    public void close() throws IOException {
        IOException failure = null;
        for (var entry : staged.values()) {
            try { entry.printer().close(); }
            catch (IOException error) { if (failure == null) failure = error; else failure.addSuppressed(error); }
        }
        try { zip.close(); }
        catch (IOException error) { if (failure == null) failure = error; else failure.addSuppressed(error); }
        if (failure != null) throw failure;
    }

    /**
     * One small, reusable CSV writer; row contents stay on disk instead of in application memory.
     *
     * @author Simon Tian
     */
    private record StagedCsv(CSVPrinter printer, List<String> headers, long headerRecords) { }
}
