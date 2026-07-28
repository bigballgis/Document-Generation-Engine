package com.bank.docgen.template.service;

import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.contentmodule.persistence.ContentModuleRepository;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionRepository;
import com.bank.docgen.contentmodule.service.ContentModuleService;
import com.bank.docgen.library.service.AssetLibraryService;
import com.bank.docgen.master.api.MasterDocumentDetailView;
import com.bank.docgen.master.persistence.MasterDocumentEntity;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.master.service.MasterDocumentService;
import com.bank.docgen.master.service.MasterNotFoundException;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.ImportTemplateRequest;
import com.bank.docgen.template.api.TemplateExportBundleView;
import com.bank.docgen.template.api.TemplateExportMetadataView;
import com.bank.docgen.template.api.TemplateImportDependencyReportView;
import com.bank.docgen.template.api.TemplateImportDryRunResult;
import com.bank.docgen.template.api.TemplateImportResult;
import com.bank.docgen.template.api.TemplateImportSummaryView;
import com.bank.docgen.template.domain.TemplateImportConflictPolicy;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
    private final MasterDocumentRepository masterDocumentRepository;
    private final TemplateService templateService;
    private final ManagementAuditRecorder managementAuditRecorder;
    private final TemplateExportAccessService importAccessSupport;
    private final TemplateImportBundleValidator bundleValidator;
    private final TemplateImportTargetResolutionSupport targetResolution;
    private final TemplateImportApplySupport applySupport;
    private final TemplateImportMaterializeSupport materializeSupport;
    private final TemplateImportAssetMaterializeSupport assetMaterializeSupport;
    private final TemplateImportDependencyPrecheck dependencyPrecheck;
    private final MasterDocumentService masterDocumentService;
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
            ContentModuleVersionRepository contentModuleVersionRepository,
            AssetLibraryService assetLibraryService,
            MasterDocumentService masterDocumentService
    ) {
        this.templateRepository = templateRepository;
        this.masterDocumentRepository = masterDocumentRepository;
        this.templateService = templateService;
        this.managementAuditRecorder = managementAuditRecorder;
        this.importAccessSupport = importAccessSupport;
        this.bundleValidator = bundleValidator;
        this.objectMapper = objectMapper;
        this.dependencyPrecheck = dependencyPrecheck;
        this.masterDocumentService = masterDocumentService;
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
        this.materializeSupport = new TemplateImportMaterializeSupport(
                contentModuleService,
                contentModuleRepository,
                contentModuleVersionRepository
        );
        this.assetMaterializeSupport = new TemplateImportAssetMaterializeSupport(
                assetLibraryService,
                templateVersionRepository
        );
    }

    @Transactional(readOnly = true)
    public TemplateImportDryRunResult dryRun(
            ImportTemplateRequest request,
            ManagementSessionClaims session,
            byte[] embeddedMasterDocx,
            boolean zipCarrier
    ) {
        return dryRun(request, session, embeddedMasterDocx, zipCarrier, Map.of());
    }

    @Transactional(readOnly = true)
    public TemplateImportDryRunResult dryRun(
            ImportTemplateRequest request,
            ManagementSessionClaims session,
            byte[] embeddedMasterDocx,
            boolean zipCarrier,
            Map<String, byte[]> embeddedAssetBinaries
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
                        requireDocx,
                        embeddedAssetBinaries
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
        return dryRun(request, session, null, false, Map.of());
    }

    @Transactional
    public TemplateImportResult importBundle(ImportTemplateRequest request, ManagementSessionClaims session) {
        return importBundle(request, session, null, false, Map.of());
    }

    @Transactional
    public TemplateImportResult importBundle(
            ImportTemplateRequest request,
            ManagementSessionClaims session,
            byte[] embeddedMasterDocx,
            boolean zipCarrier
    ) {
        return importBundle(request, session, embeddedMasterDocx, zipCarrier, Map.of());
    }

    @Transactional
    public TemplateImportResult importBundle(
            ImportTemplateRequest request,
            ManagementSessionClaims session,
            byte[] embeddedMasterDocx,
            boolean zipCarrier,
            Map<String, byte[]> embeddedAssetBinaries
    ) {
        if (request.isDryRun()) {
            throw new TemplateValidationException("api.error.template.importFailed");
        }
        TemplateExportBundleView bundle = request.bundle();
        bundleValidator.validate(bundle);
        TemplateExportMetadataView metadata = bundle.metadata();
        importAccessSupport.assertCanImportForGroup(metadata.groupCode(), session);

        UUID gateMasterId = UUID.fromString(request.masterId());
        dependencyPrecheck.assertMasterGate(gateMasterId, metadata.groupCode());

        TemplateImportDependencyReportView report = null;
        if (TemplateExportV2Support.EXPORT_FORMAT_V2.equals(bundle.format())) {
            boolean requireDocx = zipCarrier || embeddedMasterDocx != null;
            // JSON-only v2 commit without DOCX is not a self-contained carrier — require ZIP bytes
            if (!zipCarrier) {
                requireDocx = true;
            }
            report = dependencyPrecheck.evaluate(
                    new TemplateImportDependencyPrecheck.PrecheckContext(
                            bundle,
                            gateMasterId,
                            embeddedMasterDocx,
                            zipCarrier,
                            requireDocx,
                            embeddedAssetBinaries
                    )
            );
            if (!report.readyToCommit()) {
                throw new TemplateImportDependenciesException(report);
            }
        }

        boolean materializeMaster = report != null && report.items().stream()
                .anyMatch(item -> "MASTER_WILL_MATERIALIZE".equals(item.code()));
        UUID targetMasterId = gateMasterId;
        if (materializeMaster) {
            MasterDocumentDetailView drafted = masterDocumentService.materializeDraftFromImport(
                    metadata.groupCode(),
                    metadata.name() == null ? "Imported letterhead" : metadata.name() + " letterhead",
                    "Materialized from promotion pack (DRAFT; must re-approve on PROD)",
                    embeddedMasterDocx,
                    session
            );
            if (!"DRAFT".equals(drafted.status())) {
                throw new TemplateValidationException("api.error.template.masterNotApproved");
            }
            targetMasterId = UUID.fromString(drafted.id());
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
        targetResolution.assertMasterCompatible(master, metadata, materializeMaster);

        int materializedClauseCount = 0;
        TemplateExportBundleView artifactsBundle = bundle;
        if (TemplateExportV2Support.EXPORT_FORMAT_V2.equals(bundle.format())) {
            assetMaterializeSupport.materializeAssets(bundle, embeddedAssetBinaries, session);
            TemplateImportMaterializeSupport.MaterializeResult materializeResult =
                    materializeSupport.materializeClauses(bundle, metadata, session);
            materializedClauseCount = materializeResult.materializedCount();
            artifactsBundle = materializeSupport.remapContentModuleReferences(
                    bundle,
                    materializeResult.remappedBySourceModuleId()
            );
            assetMaterializeSupport.applyRenderProfile(target.templateId(), bundle);
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
        Map<String, byte[]> assetBinaries = new LinkedHashMap<>();
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
                } else if (name != null && name.startsWith(TemplateExportAssetPathSupport.ZIP_ASSET_DIR)
                        && !entry.isDirectory()) {
                    String segment = name.substring(TemplateExportAssetPathSupport.ZIP_ASSET_DIR.length());
                    String assetKey = TemplateExportAssetPathSupport.decodePathSegment(segment);
                    assetBinaries.put(assetKey, zip.readAllBytes());
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
        return new ParsedZipImport(
                bundle,
                masterDocx == null ? new byte[0] : masterDocx,
                assetBinaries
        );
    }

    record ImportTarget(UUID templateId, int devVersionNumber) {
    }

    public record ParsedZipImport(
            TemplateExportBundleView bundle,
            byte[] masterDocxBytes,
            Map<String, byte[]> assetBinaries
    ) {
        public ParsedZipImport {
            masterDocxBytes = masterDocxBytes == null
                    ? new byte[0]
                    : com.bank.docgen.sharedkernel.api.DefensiveCopies.copyBytes(masterDocxBytes);
            assetBinaries = assetBinaries == null ? Map.of() : Map.copyOf(assetBinaries);
        }

        /** Backward-compatible CE-E01 constructor. */
        public ParsedZipImport(TemplateExportBundleView bundle, byte[] masterDocxBytes) {
            this(bundle, masterDocxBytes, Map.of());
        }
    }
}
