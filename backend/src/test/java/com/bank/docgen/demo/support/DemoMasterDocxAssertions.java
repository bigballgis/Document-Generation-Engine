package com.bank.docgen.demo.support;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class DemoMasterDocxAssertions {

    private DemoMasterDocxAssertions() {
    }

    public static String readFooterXml(byte[] docxBytes) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(docxBytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.getName().startsWith("word/footer") && entry.getName().endsWith(".xml")) {
                    builder.append(new String(zip.readAllBytes(), StandardCharsets.UTF_8));
                }
            }
        }
        return builder.toString();
    }
}
