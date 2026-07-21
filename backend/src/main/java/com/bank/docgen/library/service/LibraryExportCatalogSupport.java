package com.bank.docgen.library.service;

import com.bank.docgen.library.api.LibraryExportClauseCatalogEntryView;
import com.bank.docgen.library.api.LibraryExportMasterCatalogEntryView;
import com.bank.docgen.template.api.TemplateExportAssetKeyManifestItemView;
import com.bank.docgen.template.api.TemplateExportBundleView;
import com.bank.docgen.template.api.TemplateExportClauseSnapshotView;
import com.bank.docgen.template.api.TemplateExportMasterPinView;
import com.bank.docgen.template.domain.TemplateDependencyClosure;
import com.bank.docgen.template.service.TemplateExportAssetPathSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Package-private catalog / ZIP helpers for {@link LibraryExportService} (keeps service under FileLength).
 */
final class LibraryExportCatalogSupport {

    private LibraryExportCatalogSupport() {
    }

    static void writeCatalogBinaries(
            ZipOutputStream zip,
            UUID templateId,
            TemplateExportBundleView bundle,
            byte[] masterDocxBytes,
            Map<String, MasterCatalogAccumulator> masters,
            Map<String, ClauseCatalogAccumulator> clauses,
            Map<String, TemplateExportAssetKeyManifestItemView> assets,
            Set<String> writtenMasterPaths,
            Set<String> writtenClausePaths,
            ObjectMapper objectMapper,
            LibraryExportService.LibraryExportAssemblyProbe assemblyProbe
    ) throws IOException {
        TemplateExportMasterPinView pin = bundle.masterPin();
        if (pin != null && pin.masterFileHash() != null && !pin.masterFileHash().isBlank()) {
            String hash = pin.masterFileHash().toLowerCase(Locale.ROOT);
            String path = "masters/" + hash + ".docx";
            MasterCatalogAccumulator acc = masters.computeIfAbsent(
                    hash,
                    key -> new MasterCatalogAccumulator(
                            hash,
                            pin.masterRevisionId(),
                            pin.revisionSequence(),
                            path,
                            new LinkedHashSet<>()
                    )
            );
            acc.sourceTemplateIds().add(templateId.toString());
            if (writtenMasterPaths.add(path)) {
                writeZipEntry(zip, path, masterDocxBytes == null ? new byte[0] : masterDocxBytes);
                assemblyProbe.afterCatalogBinaryWritten(writtenMasterPaths.size() + writtenClausePaths.size());
            }
        }
        List<TemplateExportClauseSnapshotView> snapshots =
                bundle.clauseSnapshots() == null ? List.of() : bundle.clauseSnapshots();
        for (TemplateExportClauseSnapshotView snapshot : snapshots) {
            if (snapshot == null || snapshot.moduleCode() == null || snapshot.semanticVersion() == null) {
                continue;
            }
            String catalogKey = snapshot.moduleCode() + "\0" + snapshot.semanticVersion();
            String path = "clauses/"
                    + safePathSegment(snapshot.moduleCode())
                    + "__"
                    + safePathSegment(snapshot.semanticVersion())
                    + ".json";
            ClauseCatalogAccumulator acc = clauses.computeIfAbsent(
                    catalogKey,
                    key -> new ClauseCatalogAccumulator(
                            snapshot.moduleCode(),
                            snapshot.semanticVersion(),
                            snapshot.sourceModuleId(),
                            path,
                            new LinkedHashSet<>()
                    )
            );
            acc.sourceTemplateIds().add(templateId.toString());
            if (writtenClausePaths.add(path)) {
                writeZipEntry(zip, path, objectMapper.writeValueAsBytes(snapshot));
                assemblyProbe.afterCatalogBinaryWritten(writtenMasterPaths.size() + writtenClausePaths.size());
            }
        }
        List<TemplateExportAssetKeyManifestItemView> assetItems =
                bundle.assetKeyManifest() == null ? List.of() : bundle.assetKeyManifest();
        for (TemplateExportAssetKeyManifestItemView item : assetItems) {
            if (item == null || item.referenceKey() == null || item.referenceKey().isBlank()) {
                continue;
            }
            String key = item.referenceKey() + "|" + item.usage();
            assets.putIfAbsent(key, item);
        }
    }

    static void writePromotionRootAssets(
            ZipOutputStream zip,
            TemplateDependencyClosure dependencyClosure,
            Map<String, byte[]> promotionAssetBinaries,
            Set<String> writtenMasterPaths,
            Set<String> writtenClausePaths,
            LibraryExportService.LibraryExportAssemblyProbe assemblyProbe
    ) throws IOException {
        if (dependencyClosure != TemplateDependencyClosure.PROMOTION) {
            return;
        }
        Set<String> writtenAssetPaths = new HashSet<>();
        for (Map.Entry<String, byte[]> assetEntry : promotionAssetBinaries.entrySet()) {
            String path = TemplateExportAssetPathSupport.libraryEntryName(assetEntry.getKey());
            if (writtenAssetPaths.add(path)) {
                writeZipEntry(zip, path, assetEntry.getValue());
                assemblyProbe.afterCatalogBinaryWritten(
                        writtenMasterPaths.size() + writtenClausePaths.size() + writtenAssetPaths.size()
                );
            }
        }
    }

    static List<LibraryExportMasterCatalogEntryView> toMasterCatalog(
            Map<String, MasterCatalogAccumulator> masters
    ) {
        List<LibraryExportMasterCatalogEntryView> rows = new ArrayList<>();
        for (MasterCatalogAccumulator acc : masters.values()) {
            rows.add(new LibraryExportMasterCatalogEntryView(
                    acc.masterFileHash(),
                    acc.masterRevisionId(),
                    acc.revisionSequence(),
                    List.copyOf(acc.sourceTemplateIds()),
                    acc.path()
            ));
        }
        return rows;
    }

    static List<LibraryExportClauseCatalogEntryView> toClauseCatalog(
            Map<String, ClauseCatalogAccumulator> clauses
    ) {
        List<LibraryExportClauseCatalogEntryView> rows = new ArrayList<>();
        for (ClauseCatalogAccumulator acc : clauses.values()) {
            rows.add(new LibraryExportClauseCatalogEntryView(
                    acc.moduleCode(),
                    acc.semanticVersion(),
                    acc.sourceModuleId(),
                    List.copyOf(acc.sourceTemplateIds()),
                    acc.path()
            ));
        }
        return rows;
    }

    static void writeZipEntry(ZipOutputStream zip, String name, byte[] bytes) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(bytes == null ? new byte[0] : bytes);
        zip.closeEntry();
    }

    private static String safePathSegment(String value) {
        String encoded = URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace("+", "%20");
        if (encoded.contains("..") || encoded.contains("/") || encoded.contains("\\")) {
            return encoded.replace("..", "_").replace("/", "_").replace("\\", "_");
        }
        return encoded;
    }

    record MasterCatalogAccumulator(
            String masterFileHash,
            String masterRevisionId,
            Integer revisionSequence,
            String path,
            Set<String> sourceTemplateIds
    ) {
    }

    record ClauseCatalogAccumulator(
            String moduleCode,
            String semanticVersion,
            String sourceModuleId,
            String path,
            Set<String> sourceTemplateIds
    ) {
    }
}
