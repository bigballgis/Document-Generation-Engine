package com.bank.docgen.template.service;

import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.contentmodule.api.ContentModuleDetailView;
import com.bank.docgen.contentmodule.api.CreateContentModuleRequest;
import com.bank.docgen.contentmodule.api.CreateContentModuleVersionRequest;
import com.bank.docgen.contentmodule.persistence.ContentModuleEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleRepository;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionRepository;
import com.bank.docgen.contentmodule.service.ContentModuleService;
import com.bank.docgen.contentmodule.service.ContentModuleValidationException;
import com.bank.docgen.master.persistence.MasterDocumentEntity;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.master.service.MasterNotFoundException;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.ContentModuleReferenceView;
import com.bank.docgen.template.api.ImportTemplateRequest;
import com.bank.docgen.template.api.TemplateExportBundleView;
import com.bank.docgen.template.api.TemplateExportClauseSnapshotView;
import com.bank.docgen.template.api.TemplateExportMetadataView;
import com.bank.docgen.template.api.TemplateImportDependencyReportView;
import com.bank.docgen.template.api.TemplateImportDryRunResult;
import com.bank.docgen.template.api.TemplateImportResult;
import com.bank.docgen.template.api.TemplateImportSummaryView;
import com.bank.docgen.template.domain.TemplateImportConflictPolicy;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TemplateImportService {

    private final TemplateRepository templateRepository;
    private final TemplateVersionRepository templateVersionRepository;
    private final MasterDocumentRepository masterDocumentRepository;
    private final TemplateService templateService;
    private final ManagementAuditRecorder managementAuditRecorder;
    private final TemplateExportAccessService importAccessSupport;
    private final TemplateImportBundleValidator bundleValidator;
    private final TemplateImportTargetResolutionSupport targetResolution;
    private final TemplateImportApplySupport applySupport;
    private final TemplateImportDependencyPrecheck dependencyPrecheck;
    private final ContentModuleService contentModuleService;
    private final ContentModuleRepository contentModuleRepository;
    private final ContentModuleVersionRepository contentModuleVersionRepository;
    private final ObjectMapper objectMapper;

    public TemplateImportService(
            TemplateRepository templateRepository,
            TemplateVersionRepository templateVersionRepository,
            MasterDocumentRepository masterDocumentRepository,
            ApiPolicyRepository apiPolicyRepository,
            TemplateService templateService,
            TemplateContentModuleReferenceService contentModuleReferenceService,
            CompositionInclusionRuleService compositionInclusionRuleService,
            ManagementAuditRecorder managementAuditRecorder,
            TemplateExportAccessService importAccessSupport,
            TemplateImportBundleValidator bundleValidator,
            ObjectMapper objectMapper,
            TemplateCurrentVersionResolver templateCurrentVersionResolver,
            TemplateImportDependencyPrecheck dependencyPrecheck,
            ContentModuleService contentModuleService,
            ContentModuleRepository contentModuleRepository,
            ContentModuleVersionRepository contentModuleVersionRepository
    ) {
        this.templateRepository = templateRepository;
        this.templateVersionRepository = templateVersionRepository;
        this.masterDocumentRepository = masterDocumentRepository;
        this.templateService = templateService;
        this.managementAuditRecorder = managementAuditRecorder;
        this.importAccessSupport = importAccessSupport;
        this.bundleValidator = bundleValidator;
        this.objectMapper = objectMapper;
        this.dependencyPrecheck = dependencyPrecheck;
        this.contentModuleService = contentModuleService;
        this.contentModuleRepository = contentModuleRepository;
        this.contentModuleVersionRepository = contentModuleVersionRepository;
        this.targetResolution = new TemplateImportTargetResolutionSupport(
                templateRepository,
                templateVersionRepository,
                importAccessSupport,
                templateCurrentVersionResolver
        );
        this.applySupport = new TemplateImportApplySupport(
                templateService,
                contentModuleReferenceService,
                compositionInclusionRuleService,
                apiPolicyRepository,
                objectMapper
        );
    }

    @Transactional(readOnly = true)
    public TemplateImportDryRunResult dryRun(
            ImportTemplateRequest request,
            ManagementSessionClaims session,
            byte[] embeddedMasterDocx,
            boolean zipCarrier
    ) {
        TemplateExportBundleView bundle = request.bundle();
        bundleValidator.validate(bundle);
        TemplateExportMetadataView metadata = bundle.metadata();
        importAccessSupport.assertCanImportForGroup(metadata.groupCode(), session);

        UUID targetMasterId = UUID.fromString(request.masterId());
        dependencyPrecheck.assertMasterGate(targetMasterId, metadata.groupCode());

        boolean requireDocx = TemplateExportV2Support.EXPORT_FORMAT_V2.equals(bundle.format()) && zipCarrier;
        TemplateImportDependencyReportView report = dependencyPrecheck.evaluate(
                new TemplateImportDependencyPrecheck.PrecheckContext(
                        bundle,
                        targetMasterId,
                        embeddedMasterDocx,
                        zipCarrier,
                        requireDocx
                )
        );
        managementAuditRecorder.recordTemplateImportDryRun(
                metadata.groupCode(),
                metadata.externalId(),
                report.readyToCommit(),
                report.blockingCount(),
                bundle.format(),
                session.username(),
                session.displayName()
        );
        return new TemplateImportDryRunResult(false, report);
    }

    @Transactional(readOnly = true)
    public TemplateImportDryRunResult dryRun(ImportTemplateRequest request, ManagementSessionClaims session) {
        return dryRun(request, session, null, false);
    }

    @Transactional
    public TemplateImportResult importBundle(ImportTemplateRequest request, ManagementSessionClaims session) {
        return importBundle(request, session, null, false);
    }

    @Transactional
    public TemplateImportResult importBundle(
            ImportTemplateRequest request,
            ManagementSessionClaims session,
            byte[] embeddedMasterDocx,
            boolean zipCarrier
    ) {
        if (request.isDryRun()) {
            throw new TemplateValidationException("api.error.template.importFailed");
        }
        TemplateExportBundleView bundle = request.bundle();
        bundleValidator.validate(bundle);
        TemplateExportMetadataView metadata = bundle.metadata();
        importAccessSupport.assertCanImportForGroup(metadata.groupCode(), session);

        UUID targetMasterId = UUID.fromString(request.masterId());
        dependencyPrecheck.assertMasterGate(targetMasterId, metadata.groupCode());

        if (TemplateExportV2Support.EXPORT_FORMAT_V2.equals(bundle.format())) {
            boolean requireDocx = zipCarrier || embeddedMasterDocx != null;
            // JSON-only v2 commit without DOCX is not a self-contained carrier — require ZIP bytes
            if (!zipCarrier) {
                requireDocx = true;
            }
            TemplateImportDependencyReportView report = dependencyPrecheck.evaluate(
                    new TemplateImportDependencyPrecheck.PrecheckContext(
                            bundle,
                            targetMasterId,
                            embeddedMasterDocx,
                            zipCarrier,
                            requireDocx
                    )
            );
            if (!report.readyToCommit()) {
                throw new TemplateImportDependenciesException(report);
            }
        }

        TemplateImportConflictPolicy conflictPolicy = request.importConflictPolicy() == null
                ? TemplateImportConflictPolicy.REJECT_IMPORT
                : request.importConflictPolicy();
        UUID sourceTemplateId = UUID.fromString(metadata.templateId());
        String importBatchId = UUID.randomUUID().toString();

        Optional<TemplateEntity> existingById = templateRepository.findByIdAndDeletedAtIsNull(sourceTemplateId);
        Optional<TemplateEntity> existingByExternalId =
                templateRepository.findByExternalIdAndDeletedAtIsNull(metadata.externalId());

        ImportTarget target = targetResolution.resolveImportTarget(
                sourceTemplateId,
                metadata,
                targetMasterId,
                conflictPolicy,
                existingById,
                existingByExternalId,
                session
        );

        MasterDocumentEntity master = masterDocumentRepository.findByIdAndDeletedAtIsNull(targetMasterId)
                .orElseThrow(MasterNotFoundException::new);
        targetResolution.assertMasterCompatible(master, metadata);

        int materializedClauseCount = 0;
        TemplateExportBundleView artifactsBundle = bundle;
        if (TemplateExportV2Support.EXPORT_FORMAT_V2.equals(bundle.format())) {
            MaterializeResult materializeResult = materializeClauses(bundle, metadata, session);
            materializedClauseCount = materializeResult.materializedCount();
            artifactsBundle = remapContentModuleReferences(bundle, materializeResult.remappedBySourceModuleId());
            applyRenderProfile(target.templateId(), bundle);
        }

        applySupport.applyBundleArtifacts(
                target.templateId(),
                artifactsBundle,
                session,
                TemplateExportV2Support.EXPORT_FORMAT_V2.equals(bundle.format())
        );
        applySupport.applyPolicySnapshot(target.templateId(), bundle.policySnapshot(), session);

        TemplateEntity savedTemplate = templateRepository.findByIdAndDeletedAtIsNull(target.templateId())
                .orElseThrow(TemplateNotFoundException::new);
        managementAuditRecorder.recordTemplateImported(
                savedTemplate.getId(),
                savedTemplate.getGroupCode(),
                savedTemplate.getExternalId(),
                importBatchId,
                target.devVersionNumber(),
                session.username(),
                session.displayName(),
                bundle.format(),
                materializedClauseCount
        );

        return new TemplateImportResult(
                new TemplateImportSummaryView(
                        savedTemplate.getId().toString(),
                        target.devVersionNumber(),
                        importBatchId,
                        bundle.format(),
                        TemplateExportV2Support.EXPORT_FORMAT_V2.equals(bundle.format())
                                ? materializedClauseCount
                                : null
                ),
                templateService.toDetail(savedTemplate)
        );
    }

    public ParsedZipImport parseZipBytes(byte[] zipBytes) {
        if (zipBytes == null || zipBytes.length == 0) {
            throw new TemplateValidationException("api.error.template.importBundleInvalid");
        }
        TemplateExportBundleView bundle = null;
        byte[] masterDocx = null;
        Set<String> entries = new HashSet<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry = zip.getNextEntry();
            while (entry != null) {
                String name = entry.getName();
                entries.add(name);
                if ("template-export-bundle.json".equals(name)) {
                    bundle = objectMapper.readValue(zip.readAllBytes(), TemplateExportBundleView.class);
                } else if (TemplateExportV2Support.ZIP_MASTER_ENTRY.equals(name)) {
                    masterDocx = zip.readAllBytes();
                } else if (!entry.isDirectory()) {
                    throw new TemplateValidationException("api.error.template.importBundleInvalid");
                }
                entry = zip.getNextEntry();
            }
        } catch (TemplateValidationException ex) {
            throw ex;
        } catch (IOException | RuntimeException ex) {
            throw new TemplateValidationException("api.error.template.importBundleInvalid");
        }
        if (bundle == null) {
            throw new TemplateValidationException("api.error.template.importBundleInvalid");
        }
        if (TemplateExportV2Support.EXPORT_FORMAT_V2.equals(bundle.format())
                && !entries.contains(TemplateExportV2Support.ZIP_MASTER_ENTRY)) {
            // Allow parse; precheck will mark MASTER_DOCX_ABSENT
            masterDocx = null;
        }
        return new ParsedZipImport(bundle, masterDocx == null ? new byte[0] : masterDocx);
    }

    private MaterializeResult materializeClauses(
            TemplateExportBundleView bundle,
            TemplateExportMetadataView metadata,
            ManagementSessionClaims session
    ) {
        List<TemplateExportClauseSnapshotView> snapshots =
                bundle.clauseSnapshots() == null ? List.of() : bundle.clauseSnapshots();
        Map<String, RemappedClause> remappedBySourceModuleId = new LinkedHashMap<>();
        int count = 0;
        for (TemplateExportClauseSnapshotView snapshot : snapshots) {
            if (snapshot == null || snapshot.moduleCode() == null || snapshot.moduleCode().isBlank()) {
                continue;
            }
            String moduleCode = snapshot.moduleCode().trim().toUpperCase(Locale.ROOT);
            String semanticVersion = resolveSemanticVersion(snapshot);
            String targetModuleId;
            Optional<ContentModuleEntity> existing =
                    contentModuleRepository.findByModuleCodeAndDeletedAtIsNull(moduleCode);
            if (existing.isEmpty()) {
                ContentModuleDetailView created = contentModuleService.create(
                        new CreateContentModuleRequest(
                                moduleCode,
                                metadata.groupCode(),
                                moduleCode,
                                "Imported clause snapshot",
                                List.of(),
                                semanticVersion,
                                snapshot.contentStructureJson(),
                                "Materialized from template export bundle v2",
                                metadata.locale() == null || metadata.locale().isBlank()
                                        ? "zh-CN"
                                        : metadata.locale(),
                                null
                        ),
                        session
                );
                targetModuleId = created.moduleId();
                count++;
            } else {
                ContentModuleEntity module = existing.get();
                targetModuleId = module.getId().toString();
                if (!contentModuleVersionRepository.existsByModuleIdAndSemanticVersion(
                        module.getId(), semanticVersion)) {
                    try {
                        contentModuleService.createVersion(
                                module.getId().toString(),
                                new CreateContentModuleVersionRequest(
                                        semanticVersion,
                                        snapshot.contentStructureJson(),
                                        "Materialized from template export bundle v2",
                                        snapshot.jurisdiction(),
                                        null,
                                        null,
                                        snapshot.legalReviewRef()
                                ),
                                session
                        );
                        count++;
                    } catch (ContentModuleValidationException ex) {
                        if (!"api.error.contentModule.versionExists".equals(ex.messageKey())) {
                            throw ex;
                        }
                    }
                }
            }
            if (snapshot.sourceModuleId() != null && !snapshot.sourceModuleId().isBlank()) {
                remappedBySourceModuleId.put(
                        snapshot.sourceModuleId().trim(),
                        new RemappedClause(targetModuleId, semanticVersion)
                );
            }
        }
        return new MaterializeResult(count, remappedBySourceModuleId);
    }

    private static String resolveSemanticVersion(TemplateExportClauseSnapshotView snapshot) {
        if (snapshot.semanticVersion() != null && !snapshot.semanticVersion().isBlank()) {
            return snapshot.semanticVersion().trim();
        }
        return Math.max(1, snapshot.versionNumber()) + ".0.0";
    }

    private TemplateExportBundleView remapContentModuleReferences(
            TemplateExportBundleView bundle,
            Map<String, RemappedClause> remappedBySourceModuleId
    ) {
        List<ContentModuleReferenceView> sourceRefs =
                bundle.contentModuleReferences() == null ? List.of() : bundle.contentModuleReferences();
        if (sourceRefs.isEmpty() || remappedBySourceModuleId.isEmpty()) {
            return bundle;
        }
        List<ContentModuleReferenceView> remapped = new ArrayList<>(sourceRefs.size());
        for (ContentModuleReferenceView reference : sourceRefs) {
            if (reference == null) {
                continue;
            }
            RemappedClause mapped = null;
            if (reference.moduleId() != null && !reference.moduleId().isBlank()) {
                mapped = remappedBySourceModuleId.get(reference.moduleId().trim());
            }
            if (mapped == null) {
                remapped.add(reference);
                continue;
            }
            remapped.add(new ContentModuleReferenceView(
                    reference.referenceKey(),
                    mapped.targetModuleId(),
                    mapped.semanticVersion(),
                    reference.locked(),
                    false,
                    null
            ));
        }
        return new TemplateExportBundleView(
                bundle.format(),
                bundle.metadata(),
                bundle.variables(),
                bundle.bindings(),
                bundle.rules(),
                remapped,
                bundle.policySnapshot(),
                bundle.masterPin(),
                bundle.clauseSnapshots(),
                bundle.renderProfile(),
                bundle.assetKeyManifest()
        );
    }

    private void applyRenderProfile(UUID templateId, TemplateExportBundleView bundle) {
        if (bundle.renderProfile() == null
                || bundle.renderProfile().json() == null
                || bundle.renderProfile().json().isBlank()) {
            return;
        }
        List<TemplateVersionEntity> versions =
                templateVersionRepository.findByTemplateIdOrderByDevVersionNumberDesc(templateId);
        if (versions.isEmpty()) {
            return;
        }
        TemplateVersionEntity version = versions.get(0);
        version.setRenderProfileJson(bundle.renderProfile().json());
        version.setRenderProfileVersion(
                bundle.renderProfile().version() == null ? "rp-v1" : bundle.renderProfile().version()
        );
        templateVersionRepository.save(version);
    }

    record ImportTarget(UUID templateId, int devVersionNumber) {
    }

    private record RemappedClause(String targetModuleId, String semanticVersion) {
    }

    private record MaterializeResult(int materializedCount, Map<String, RemappedClause> remappedBySourceModuleId) {
        private MaterializeResult {
            remappedBySourceModuleId = remappedBySourceModuleId == null
                    ? Map.of()
                    : Map.copyOf(remappedBySourceModuleId);
        }
    }

    public record ParsedZipImport(TemplateExportBundleView bundle, byte[] masterDocxBytes) {
        public ParsedZipImport {
            masterDocxBytes = masterDocxBytes == null
                    ? new byte[0]
                    : com.bank.docgen.sharedkernel.api.DefensiveCopies.copyBytes(masterDocxBytes);
        }
    }
}
