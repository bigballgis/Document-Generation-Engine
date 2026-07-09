package com.bank.docgen.demo.support;

import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assumptions;

/**
 * Best-effort write for deploy demo DOCX assets. Assertions already validated the bytes;
 * Docker bind-mounts or other processes may lock the target file on Windows.
 */
public final class DemoDeployAssetWriteSupport {

    private DemoDeployAssetWriteSupport() {
    }

    public static void writeBestEffort(Path path, byte[] bytes) {
        try {
            Files.write(path, bytes);
        } catch (FileSystemException ex) {
            Assumptions.assumeTrue(
                    false,
                    "Skipping deploy asset write (file locked): " + path + " — " + ex.getMessage()
            );
        } catch (IOException ex) {
            throw new AssertionError("Failed to write deploy asset: " + path, ex);
        }
    }
}
