package com.bank.docgen.template.service;

import com.bank.docgen.apimgmt.mapping.ApiPolicyViewMapper;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.ContentModuleReferenceView;
import com.bank.docgen.template.api.TemplateExportBundleView;
import com.bank.docgen.template.api.TemplateExportMetadataView;
import com.bank.docgen.template.api.TemplateExportResult;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TemplateExportService {

    public static final String EXPORT_FORMAT = "template-export-bundle-v1-json";
    public static final String EXPORT_FORMAT_V2 = TemplateExportV2Support.EXPORT_FORMAT_V2;
    private static final String ZIP_ENTRY_NAME = "template-export-bundle.json";
    private static final Set<TemplateLifecycleStatus> EXPORT_ELIGIBLE = EnumSet.of(
            TemplateLifecycleStatus.PENDING_RELEASE,
            TemplateLifecycleStatus.PUBLISHED,
            TemplateLifecycleStatus.STOPPED,
            TemplateLifecycleStatus.DEPRECATED
    );

    private final TemplateRepository templateRepository;
    private final ApiPolicyRepository apiPolicyRepository;
    private final ApiPolicyViewMapper apiPolicyViewMapper;
    private final TemplateContentModuleReferenceService contentModuleReferenceService;
    private final ManagementAuditRecorder managementAuditRecorder;
    private final TemplateService templateService;
    private final TemplateExportAccessService exportAccessSupport;
    private final ObjectMapper objectMapper;
    private final TemplateCurrentVersionResolver templateVersionSupport;
    private final TemplateExportV2Support exportV2Support;

    public TemplateExportService(
            TemplateRepository templateRepository,
            ApiPolicyRepository apiPolicyRepository,
            ApiPolicyViewMapper apiPolicyViewMapper,
            TemplateContentModuleReferenceService contentModuleReferenceService,
            ManagementAuditRecorder managementAuditRecorder,
            TemplateService templateService,
            TemplateExportAccessService exportAccessSupport,
            ObjectMapper objectMapper,
            TemplateCurrentVersionResolver templateVersionSupport,
            TemplateExportV2Support exportV2Support
    ) {
        this.templateRepository = templateRepository;
        this.apiPolicyRepository = apiPolicyRepository;
        this.apiPolicyViewMapper = apiPolicyViewMapper;
        this.contentModuleReferenceService = contentModuleReferenceService;
        this.managementAuditRecorder = managementAuditRecorder;
        this.templateService = templateService;
        this.exportAccessSupport = exportAccessSupport;
        this.objectMapper = objectMapper;
        this.templateVersionSupport = templateVersionSupport;
        this.exportV2Support = exportV2Support;
    }

    @Transactional(readOnly = true)
    public TemplateExportResult exportJson(UUID templateId, ManagementSessionClaims session) {
        return exportJson(templateId, session, 1);
    }

    @Transactional(readOnly = true)
    public TemplateExportResult exportJson(UUID templateId, ManagementSessionClaims session, int bundleVersion) {
        BuiltExport built = buildExport(templateId, session, bundleVersion);
        recordAudit(built.bundle(), session);
        return new TemplateExportResult(built.format(), built.bundle());
    }

    @Transactional(readOnly = true)
    public TemplateExportZipArtifact exportZip(UUID templateId, ManagementSessionClaims session) {
        return exportZip(templateId, session, 1);
    }

    @Transactional(readOnly = true)
    public TemplateExportZipArtifact exportZip(UUID templateId, ManagementSessionClaims session, int bundleVersion) {
        BuiltExport built = buildExport(templateId, session, bundleVersion);
        recordAudit(built.bundle(), session);
        byte[] zipBytes = bundleVersion == 2
                ? zipBundleV2(built.bundle(), built.masterDocxBytes())
                : zipBundle(built.bundle());
        return new TemplateExportZipArtifact(buildZipFilename(built.bundle()), zipBytes);
    }

    /**
     * CE-E03: build an E01 v2 ZIP without recording {@code TEMPLATE_EXPORTED}
     * (library batch audit owns {@code LIBRARY_EXPORT}).
     */
    @Transactional(readOnly = true)
    public BuiltV2Export buildV2ExportWithoutAudit(UUID templateId, ManagementSessionClaims session) {
        BuiltExport built = buildExport(templateId, session, 2);
        byte[] zipBytes = zipBundleV2(built.bundle(), built.masterDocxBytes());
        return new BuiltV2Export(built.bundle(), built.masterDocxBytes(), zipBytes);
    }

    public static boolean isExportEligible(TemplateLifecycleStatus status) {
        return EXPORT_ELIGIBLE.contains(status);
    }

    private BuiltExport buildExport(UUID templateId, ManagementSessionClaims session, int bundleVersion) {
        if (bundleVersion != 1 && bundleVersion != 2) {
            throw new TemplateValidationException("api.error.template.exportFormatUnsupported");
        }
        TemplateEntity template = templateRepository.findByIdAndDeletedAtIsNull(templateId)
                .orElseThrow(TemplateNotFoundException::new);
        exportAccessSupport.assertCanExport(template, session);
        assertExportEligible(template);
        var detail = templateService.toDetail(template);
        TemplateVersionEntity version = templateVersionSupport.requireExportableVersion(templateId);
        List<ContentModuleReferenceView> references =
                contentModuleReferenceService.listReferences(templateId, session);
        TemplateExportMetadataView metadata = new TemplateExportMetadataView(
                template.getId().toString(),
                template.getExternalId(),
                template.getGroupCode(),
                template.getName(),
                template.getDescription(),
                template.getMasterId().toString(),
                template.getLifecycleStatus(),
                template.getReleaseVersion(),
                version.getId().toString(),
                version.getDevVersionNumber(),
                Instant.now(),
                template.getLocale(),
                template.getLocaleVariantFamilyId() == null
                        ? null
                        : template.getLocaleVariantFamilyId().toString()
        );
        var policy = apiPolicyRepository.findByTemplateId(templateId)
                .map(apiPolicyViewMapper::toPolicyView)
                .orElse(null);

        if (bundleVersion == 2) {
            TemplateExportV2Support.V2Artifacts v2 = exportV2Support.assemble(
                    template,
                    version,
                    references,
                    detail.bindings()
            );
            TemplateExportBundleView bundle = new TemplateExportBundleView(
                    EXPORT_FORMAT_V2,
                    metadata,
                    detail.variables(),
                    detail.bindings(),
                    templateService.loadRules(version),
                    references,
                    policy,
                    v2.masterPin(),
                    v2.clauseSnapshots(),
                    v2.renderProfile(),
                    v2.assetKeyManifest()
            );
            return new BuiltExport(EXPORT_FORMAT_V2, bundle, v2.masterDocxBytes());
        }

        TemplateExportBundleView bundle = new TemplateExportBundleView(
                EXPORT_FORMAT,
                metadata,
                detail.variables(),
                detail.bindings(),
                templateService.loadRules(version),
                references,
                policy
        );
        return new BuiltExport(EXPORT_FORMAT, bundle, new byte[0]);
    }

    private void assertExportEligible(TemplateEntity template) {
        if (!EXPORT_ELIGIBLE.contains(template.getLifecycleStatus())) {
            throw new TemplateValidationException("api.error.template.exportNotEligible");
        }
    }

    private void recordAudit(TemplateExportBundleView bundle, ManagementSessionClaims session) {
        managementAuditRecorder.recordTemplateExported(
                UUID.fromString(bundle.metadata().templateId()),
                bundle.metadata().groupCode(),
                bundle.metadata().externalId(),
                session.username(),
                session.displayName()
        );
    }

    private byte[] zipBundle(TemplateExportBundleView bundle) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
                zipOutputStream.putNextEntry(new ZipEntry(ZIP_ENTRY_NAME));
                zipOutputStream.write(objectMapper.writeValueAsBytes(bundle));
                zipOutputStream.closeEntry();
            }
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new TemplateValidationException("api.error.template.exportFailed");
        }
    }

    private byte[] zipBundleV2(TemplateExportBundleView bundle, byte[] masterDocxBytes) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
                zipOutputStream.putNextEntry(new ZipEntry(ZIP_ENTRY_NAME));
                zipOutputStream.write(objectMapper.writeValueAsBytes(bundle));
                zipOutputStream.closeEntry();
                zipOutputStream.putNextEntry(new ZipEntry(TemplateExportV2Support.ZIP_MASTER_ENTRY));
                zipOutputStream.write(masterDocxBytes == null ? new byte[0] : masterDocxBytes);
                zipOutputStream.closeEntry();
            }
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new TemplateValidationException("api.error.template.exportFailed");
        }
    }

    private String buildZipFilename(TemplateExportBundleView bundle) {
        return sanitizeFilename(bundle.metadata().externalId()) + "-export.zip";
    }

    private String sanitizeFilename(String externalId) {
        if (externalId == null || externalId.isBlank()) {
            return "template-export";
        }
        return externalId.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    public record TemplateExportZipArtifact(String filename, byte[] content) {
        public TemplateExportZipArtifact {
            content = com.bank.docgen.sharedkernel.api.DefensiveCopies.copyBytes(content);
        }
    }

    public record BuiltV2Export(
            TemplateExportBundleView bundle,
            byte[] masterDocxBytes,
            byte[] zipBytes
    ) {
        public BuiltV2Export {
            masterDocxBytes = masterDocxBytes == null
                    ? new byte[0]
                    : com.bank.docgen.sharedkernel.api.DefensiveCopies.copyBytes(masterDocxBytes);
            zipBytes = zipBytes == null
                    ? new byte[0]
                    : com.bank.docgen.sharedkernel.api.DefensiveCopies.copyBytes(zipBytes);
        }
    }

    private record BuiltExport(String format, TemplateExportBundleView bundle, byte[] masterDocxBytes) {
        private BuiltExport {
            masterDocxBytes = masterDocxBytes == null
                    ? new byte[0]
                    : com.bank.docgen.sharedkernel.api.DefensiveCopies.copyBytes(masterDocxBytes);
        }
    }
}
