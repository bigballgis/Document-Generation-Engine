package com.bank.docgen.rendering;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.stereotype.Component;

/** Writes finalized artifacts to temp files with size enforcement (SOR-P02). */
@Component
public class ArtifactSpoolService {

    private final GeneratedArtifactSizeGuard artifactSizeGuard;

    public ArtifactSpoolService(GeneratedArtifactSizeGuard artifactSizeGuard) {
        this.artifactSizeGuard = artifactSizeGuard;
    }

    public SpooledArtifact spool(byte[] artifactBytes) throws IOException {
        artifactSizeGuard.assertWithinLimit(artifactBytes);
        Path tempFile = Files.createTempFile("docgen-artifact-", ".bin");
        try {
            Files.write(tempFile, artifactBytes);
            artifactSizeGuard.assertWithinLimit(tempFile);
            return new SpooledArtifact(tempFile, artifactBytes.length);
        } catch (RuntimeException | IOException ex) {
            Files.deleteIfExists(tempFile);
            throw ex;
        }
    }
}
