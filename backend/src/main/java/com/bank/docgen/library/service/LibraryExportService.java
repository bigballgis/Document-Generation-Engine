package com.bank.docgen.library.service;

import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.authorization.management.persistence.BusinessGroupEntity;
import com.bank.docgen.authorization.management.persistence.BusinessGroupRepository;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.library.api.LibraryExportActorView;
import com.bank.docgen.library.api.LibraryExportClauseCatalogEntryView;
import com.bank.docgen.library.api.LibraryExportCountsView;
import com.bank.docgen.library.api.LibraryExportManifestView;
import com.bank.docgen.library.api.LibraryExportMasterCatalogEntryView;
import com.bank.docgen.library.api.LibraryExportRequest;
import com.bank.docgen.library.api.LibraryExportScopeView;
import com.bank.docgen.library.api.LibraryExportTemplateEntryView;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.api.DefensiveCopies;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.TemplateExportAssetKeyManifestItemView;
import com.bank.docgen.template.api.TemplateExportBundleView;
import com.bank.docgen.template.api.TemplateExportClauseSnapshotView;
import com.bank.docgen.template.api.TemplateExportMasterPinView;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.service.TemplateAccessDeniedException;
import com.bank.docgen.template.service.TemplateExportAccessService;
import com.bank.docgen.template.service.TemplateExportService;
import com.bank.docgen.template.service.TemplateGovernanceException;
import com.bank.docgen.template.service.TemplateValidationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CE-E03: assembles {@code template-library-export-v1-zip} from nested E01 v2 packs.
 */
@Service
public class LibraryExportService {

    public static final String LIBRARY_EXPORT_FORMAT = "template-library-export-v1-zip";
    public static final String MANIFEST_ENTRY = "library-export-manifest.json";
    public static final int MAX_TEMPLATE_IDS = 500;
    public static final int MAX_ELIGIBLE_CANDIDATES = 500;

    private static final String STATUS_INCLUDED = "INCLUDED";
    private static final String STATUS_SKIPPED = "SKIPPED";
    private static final String STATUS_FAILED = "FAILED";
    private static final String REASON_NOT_ELIGIBLE = "EXPORT_NOT_ELIGIBLE";
    private static final String SCOPE_ALL = "ALL_AUTHORIZED";
    private static final String SCOPE_GROUP = "GROUP";
    private static final String SCOPE_TEMPLATE_IDS = "TEMPLATE_IDS";

    private final TemplateRepository templateRepository;
    private final BusinessGroupRepository businessGroupRepository;
    private final TemplateExportService templateExportService;
    private final TemplateExportAccessService exportAccessService;
    private final GroupAccessService groupAccessService;
    private final ManagementAuditRecorder managementAuditRecorder;
    private final ObjectMapper objectMapper;

    public LibraryExportService(
            TemplateRepository templateRepository,
            BusinessGroupRepository businessGroupRepository,
            TemplateExportService templateExportService,
            TemplateExportAccessService exportAccessService,
            GroupAccessService groupAccessService,
            ManagementAuditRecorder managementAuditRecorder,
            ObjectMapper objectMapper
    ) {
        this.templateRepository = templateRepository;
        this.businessGroupRepository = businessGroupRepository;
        this.templateExportService = templateExportService;
        this.exportAccessService = exportAccessService;
        this.groupAccessService = groupAccessService;
        this.managementAuditRecorder = managementAuditRecorder;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public LibraryExportZipArtifact exportLibrary(
            LibraryExportRequest request,
            ManagementSessionClaims session
    ) {
        if (!groupAccessService.canExportTemplates(session)) {
            throw new TemplateAccessDeniedException();
        }
        LibraryExportRequest effective = request == null
                ? new LibraryExportRequest(null, null, true)
                : request;
        if (effective.templateIds() != null && effective.templateIds().size() > MAX_TEMPLATE_IDS) {
            throw new LibraryExportValidationException(
                    ApiErrorCodes.LIBRARY_EXPORT_LIMIT_EXCEEDED,
                    "api.error.library.exportLimitExceeded"
            );
        }

        ResolvedScope scope = resolveCandidates(effective, session);
        int eligibleCount = 0;
        for (TemplateEntity template : scope.authorizedTemplates()) {
            if (TemplateExportService.isExportEligible(template.getLifecycleStatus())) {
                eligibleCount++;
            }
        }
        if (eligibleCount > MAX_ELIGIBLE_CANDIDATES) {
            throw new LibraryExportValidationException(
                    ApiErrorCodes.LIBRARY_EXPORT_LIMIT_EXCEEDED,
                    "api.error.library.exportLimitExceeded"
            );
        }

        UUID exportBatchId = UUID.randomUUID();
        Instant exportedAt = Instant.now();
        List<LibraryExportTemplateEntryView> templateEntries = new ArrayList<>();
        Map<String, MasterCatalogAccumulator> masters = new LinkedHashMap<>();
        Map<String, ClauseCatalogAccumulator> clauses = new LinkedHashMap<>();
        Map<String, TemplateExportAssetKeyManifestItemView> assets = new LinkedHashMap<>();
        Map<String, byte[]> nestedZips = new LinkedHashMap<>();
        Map<String, byte[]> masterBytesByHash = new LinkedHashMap<>();
        Map<String, byte[]> clauseJsonByPath = new LinkedHashMap<>();

        int skippedCount = 0;
        int failedCount = 0;
        int includedCount = 0;

        for (TemplateEntity template : scope.authorizedTemplates()) {
            if (!TemplateExportService.isExportEligible(template.getLifecycleStatus())) {
                skippedCount++;
                if (effective.includeSkippedOrDefault()) {
                    templateEntries.add(new LibraryExportTemplateEntryView(
                            template.getId().toString(),
                            STATUS_SKIPPED,
                            REASON_NOT_ELIGIBLE,
                            null
                    ));
                }
                continue;
            }
            try {
                TemplateExportService.BuiltV2Export built =
                        templateExportService.buildV2ExportWithoutAudit(template.getId(), session);
                String relativePath = "templates/" + template.getId() + ".zip";
                nestedZips.put(relativePath, built.zipBytes());
                templateEntries.add(new LibraryExportTemplateEntryView(
                        template.getId().toString(),
                        STATUS_INCLUDED,
                        null,
                        relativePath
                ));
                includedCount++;
                accumulateCatalogs(
                        template.getId(),
                        built.bundle(),
                        built.masterDocxBytes(),
                        masters,
                        clauses,
                        assets,
                        masterBytesByHash,
                        clauseJsonByPath
                );
            } catch (TemplateGovernanceException ex) {
                failedCount++;
                templateEntries.add(new LibraryExportTemplateEntryView(
                        template.getId().toString(),
                        STATUS_FAILED,
                        ex.errorCode(),
                        null
                ));
            } catch (TemplateValidationException ex) {
                failedCount++;
                templateEntries.add(new LibraryExportTemplateEntryView(
                        template.getId().toString(),
                        STATUS_FAILED,
                        "EXPORT_FAILED",
                        null
                ));
            }
        }

        if (includedCount < 1) {
            throw new LibraryExportValidationException(
                    ApiErrorCodes.LIBRARY_EXPORT_EMPTY,
                    "api.error.library.exportEmpty"
            );
        }

        LibraryExportCountsView counts = new LibraryExportCountsView(
                includedCount,
                skippedCount,
                failedCount,
                scope.omittedCount(),
                masters.size(),
                clauses.size(),
                assets.size()
        );
        LibraryExportManifestView manifest = new LibraryExportManifestView(
                LIBRARY_EXPORT_FORMAT,
                exportBatchId.toString(),
                exportedAt,
                2,
                new LibraryExportActorView(session.username(), primaryRole(session)),
                scope.scopeView(),
                counts,
                templateEntries,
                toMasterCatalog(masters),
                toClauseCatalog(clauses),
                List.copyOf(assets.values())
        );

        byte[] zipBytes = assembleZip(manifest, nestedZips, masterBytesByHash, clauseJsonByPath);
        managementAuditRecorder.recordLibraryExport(
                exportBatchId.toString(),
                scope.scopeView().selection(),
                includedCount,
                skippedCount,
                failedCount,
                scope.omittedCount(),
                session.username(),
                session.displayName()
        );
        return new LibraryExportZipArtifact(
                "library-export-" + exportBatchId + ".zip",
                zipBytes
        );
    }

    private ResolvedScope resolveCandidates(LibraryExportRequest request, ManagementSessionClaims session) {
        List<UUID> templateIds = request.templateIds();
        if (templateIds != null && !templateIds.isEmpty()) {
            return resolveByTemplateIds(templateIds, session);
        }
        if (request.groupId() != null) {
            return resolveByGroupId(request.groupId(), session);
        }
        return new ResolvedScope(
                loadAuthorizedTemplates(session),
                0,
                new LibraryExportScopeView(SCOPE_ALL, null, null)
        );
    }

    private ResolvedScope resolveByTemplateIds(List<UUID> templateIds, ManagementSessionClaims session) {
        List<TemplateEntity> found = templateRepository.findByIdInAndDeletedAtIsNull(templateIds);
        Map<UUID, TemplateEntity> byId = new LinkedHashMap<>();
        for (TemplateEntity entity : found) {
            byId.put(entity.getId(), entity);
        }
        List<TemplateEntity> authorized = new ArrayList<>();
        int omitted = 0;
        for (UUID id : templateIds) {
            TemplateEntity entity = byId.get(id);
            if (entity == null || !exportAccessService.canExport(entity, session)) {
                omitted++;
                continue;
            }
            authorized.add(entity);
        }
        List<String> echoed = templateIds.stream().map(UUID::toString).toList();
        return new ResolvedScope(
                authorized,
                omitted,
                new LibraryExportScopeView(SCOPE_TEMPLATE_IDS, null, echoed)
        );
    }

    private ResolvedScope resolveByGroupId(UUID groupId, ManagementSessionClaims session) {
        Optional<BusinessGroupEntity> group = businessGroupRepository.findByIdAndDeletedAtIsNull(groupId);
        if (group.isEmpty() || !groupAccessService.canAccessGroup(session, group.get().getGroupCode())) {
            return new ResolvedScope(
                    List.of(),
                    0,
                    new LibraryExportScopeView(SCOPE_GROUP, groupId.toString(), null)
            );
        }
        String groupCode = group.get().getGroupCode();
        List<TemplateEntity> inGroup =
                templateRepository.findByDeletedAtIsNullAndGroupCodeInOrderByUpdatedAtDesc(List.of(groupCode));
        List<TemplateEntity> authorized = filterAuthorized(inGroup, session);
        return new ResolvedScope(
                authorized,
                0,
                new LibraryExportScopeView(SCOPE_GROUP, groupId.toString(), null)
        );
    }

    private List<TemplateEntity> loadAuthorizedTemplates(ManagementSessionClaims session) {
        List<TemplateEntity> templates;
        if (session.roles().contains("GLOBAL_ADMIN")) {
            templates = templateRepository.findByDeletedAtIsNullOrderByUpdatedAtDesc();
        } else {
            List<String> groups = session.authorizedGroupCodes();
            if (groups == null || groups.isEmpty()) {
                return List.of();
            }
            templates = templateRepository.findByDeletedAtIsNullAndGroupCodeInOrderByUpdatedAtDesc(groups);
        }
        return filterAuthorized(templates, session);
    }

    private List<TemplateEntity> filterAuthorized(
            List<TemplateEntity> templates,
            ManagementSessionClaims session
    ) {
        List<TemplateEntity> authorized = new ArrayList<>();
        for (TemplateEntity template : templates) {
            if (exportAccessService.canExport(template, session)) {
                authorized.add(template);
            }
        }
        return authorized;
    }

    private void accumulateCatalogs(
            UUID templateId,
            TemplateExportBundleView bundle,
            byte[] masterDocxBytes,
            Map<String, MasterCatalogAccumulator> masters,
            Map<String, ClauseCatalogAccumulator> clauses,
            Map<String, TemplateExportAssetKeyManifestItemView> assets,
            Map<String, byte[]> masterBytesByHash,
            Map<String, byte[]> clauseJsonByPath
    ) {
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
            masterBytesByHash.putIfAbsent(path, masterDocxBytes == null ? new byte[0] : masterDocxBytes);
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
            if (!clauseJsonByPath.containsKey(path)) {
                try {
                    clauseJsonByPath.put(path, objectMapper.writeValueAsBytes(snapshot));
                } catch (IOException ex) {
                    throw new LibraryExportValidationException(
                            ApiErrorCodes.LIBRARY_EXPORT_FAILED,
                            "api.error.library.exportFailed"
                    );
                }
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

    private static List<LibraryExportMasterCatalogEntryView> toMasterCatalog(
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

    private static List<LibraryExportClauseCatalogEntryView> toClauseCatalog(
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

    private byte[] assembleZip(
            LibraryExportManifestView manifest,
            Map<String, byte[]> nestedZips,
            Map<String, byte[]> masterBytesByHash,
            Map<String, byte[]> clauseJsonByPath
    ) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(outputStream)) {
                zip.putNextEntry(new ZipEntry(MANIFEST_ENTRY));
                zip.write(objectMapper.writeValueAsBytes(manifest));
                zip.closeEntry();
                for (Map.Entry<String, byte[]> entry : nestedZips.entrySet()) {
                    zip.putNextEntry(new ZipEntry(entry.getKey()));
                    zip.write(entry.getValue());
                    zip.closeEntry();
                }
                for (Map.Entry<String, byte[]> entry : masterBytesByHash.entrySet()) {
                    zip.putNextEntry(new ZipEntry(entry.getKey()));
                    zip.write(entry.getValue());
                    zip.closeEntry();
                }
                for (Map.Entry<String, byte[]> entry : clauseJsonByPath.entrySet()) {
                    zip.putNextEntry(new ZipEntry(entry.getKey()));
                    zip.write(entry.getValue());
                    zip.closeEntry();
                }
            }
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new LibraryExportValidationException(
                    ApiErrorCodes.LIBRARY_EXPORT_FAILED,
                    "api.error.library.exportFailed"
            );
        }
    }

    private static String safePathSegment(String value) {
        String encoded = URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace("+", "%20");
        if (encoded.contains("..") || encoded.contains("/") || encoded.contains("\\")) {
            return encoded.replace("..", "_").replace("/", "_").replace("\\", "_");
        }
        return encoded;
    }

    private static String primaryRole(ManagementSessionClaims session) {
        if (session.roles().contains("GLOBAL_ADMIN")) {
            return "GLOBAL_ADMIN";
        }
        if (session.roles().contains("GROUP_ADMIN")) {
            return "GROUP_ADMIN";
        }
        if (session.roles().contains("TEMPLATE_AUTHOR")) {
            return "TEMPLATE_AUTHOR";
        }
        return session.roles().isEmpty() ? "UNKNOWN" : session.roles().getFirst();
    }

    public record LibraryExportZipArtifact(String filename, byte[] content) {
        public LibraryExportZipArtifact {
            content = DefensiveCopies.copyBytes(content);
        }
    }

    private record ResolvedScope(
            List<TemplateEntity> authorizedTemplates,
            int omittedCount,
            LibraryExportScopeView scopeView
    ) {
    }

    private record MasterCatalogAccumulator(
            String masterFileHash,
            String masterRevisionId,
            Integer revisionSequence,
            String path,
            Set<String> sourceTemplateIds
    ) {
    }

    private record ClauseCatalogAccumulator(
            String moduleCode,
            String semanticVersion,
            String sourceModuleId,
            String path,
            Set<String> sourceTemplateIds
    ) {
    }
}
