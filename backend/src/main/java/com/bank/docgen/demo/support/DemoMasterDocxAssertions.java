package com.bank.docgen.demo.support;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class DemoMasterDocxAssertions {

    private DemoMasterDocxAssertions() {
    }

    public static String readFooterXml(byte[] docxBytes) throws IOException {
        return readZipEntryMatching(docxBytes, name -> name.startsWith("word/footer") && name.endsWith(".xml"));
    }

    public static String readStylesXml(byte[] docxBytes) throws IOException {
        return readZipEntryMatching(docxBytes, "word/styles.xml"::equals);
    }

    public static String readDocumentXml(byte[] docxBytes) throws IOException {
        return readZipEntryMatching(docxBytes, "word/document.xml"::equals);
    }

    public static String readZipEntry(byte[] docxBytes, String entryName) throws IOException {
        return readZipEntryMatching(docxBytes, entryName::equals);
    }

    public static List<String> zipEntryNames(byte[] docxBytes) throws IOException {
        List<String> names = new ArrayList<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(docxBytes))) {
            ZipEntry entry = zip.getNextEntry();
            while (entry != null) {
                names.add(entry.getName());
                entry = zip.getNextEntry();
            }
        }
        return List.copyOf(names);
    }

    private static String readZipEntryMatching(byte[] docxBytes, Predicate<String> matcher) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(docxBytes))) {
            ZipEntry entry = zip.getNextEntry();
            while (entry != null) {
                if (matcher.test(entry.getName())) {
                    builder.append(new String(zip.readAllBytes(), StandardCharsets.UTF_8));
                }
                entry = zip.getNextEntry();
            }
        }
        return builder.toString();
    }
}
