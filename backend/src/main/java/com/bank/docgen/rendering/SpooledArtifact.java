package com.bank.docgen.rendering;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** Temp-file backed artifact for stream/spool upload (SOR-P02). */
public record SpooledArtifact(Path path, long sizeBytes) implements AutoCloseable {

    public InputStream openInputStream() throws IOException {
        return Files.newInputStream(path);
    }

    @Override
    public void close() throws IOException {
        Files.deleteIfExists(path);
    }
}
