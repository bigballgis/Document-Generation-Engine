package com.bank.docgen.template.service;

import com.bank.docgen.apimgmt.mapping.ApiPolicyViewMapper;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.TemplateExportBundleView;
import com.bank.docgen.template.api.TemplateExportMetadataView;
import com.bank.docgen.template.api.TemplateExportResult;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TemplateExportService {

    public static final String EXPORT_FORMAT = "template-export-bundle-v1-json";
    private static final String ZIP_ENTRY_NAME = "template-export-bundle.json";
    private static final Set<TemplateLifecycleStatus> EXPORT_ELIGIBLE = EnumSet.of(
            TemplateLifecycleStatus.PENDING_RELEASE,
            TemplateLifecycleStatus.PUBLISHED,
            TemplateLifecycleStatus.STOPPED,
            TemplateLifecycleStatus.DEPRECATED
    );

    private final TemplateRepository templateRepository;
    private final TemplateVersionRepository templateVersionRepository;
    private final ApiPolicyRepository apiPolicyRepository;
    private final ApiPolicyViewMapper apiPolicyViewMapper;
    private final TemplateContentModuleReferenceService contentModuleReferenceService;
    private final ManagementAuditRecorder managementAuditRecorder;
    private final TemplateService templateService;
    private final TemplateExportAccessSupport exportAccessSupport;
    private final ObjectMapper objectMapper;
    private final TemplateCurrentVersionResolver templateVersionSupport;

    public TemplateExportService(
            TemplateRepository templateRepository,
            TemplateVersionRepository templateVersionRepository,
            ApiPolicyRepository apiPolicyRepository,
            ApiPolicyViewMapper apiPolicyViewMapper,
            TemplateContentModuleReferenceService contentModuleReferenceService,
            ManagementAuditRecorder managementAuditRecorder,
            TemplateService templateService,
            TemplateExportAccessSupport exportAccessSupport,
            ObjectMapper objectMapper,
            TemplateCurrentVersionResolver templateVersionSupport
    ) {
        this.templateRepository = templateRepository;
        this.templateVersionRepository = templateVersionRepository;
        this.apiPolicyRepository = apiPolicyRepository;
        this.apiPolicyViewMapper = apiPolicyViewMapper;
        this.contentModuleReferenceService = contentModuleReferenceService;
        this.managementAuditRecorder = managementAuditRecorder;
        this.templateService = templateService;
        this.exportAccessSupport = exportAccessSupport;
        this.objectMapper = objectMapper;
        this.templateVersionSupport = templateVersionSupport;
    }

    @Transactional(readOnly = true)
    public TemplateExportResult exportJson(UUID templateId, ManagementSessionClaims session) {
        TemplateExportBundleView bundle = buildBundle(templateId, session);
        recordAudit(bundle, session);
        return new TemplateExportResult(EXPORT_FORMAT, bundle);
    }

    @Transactional(readOnly = true)
    public TemplateExportZipArtifact exportZip(UUID templateId, ManagementSessionClaims session) {
        TemplateExportBundleView bundle = buildBundle(templateId, session);
        recordAudit(bundle, session);
        return new TemplateExportZipArtifact(buildZipFilename(bundle), zipBundle(bundle));
    }

    private TemplateExportBundleView buildBundle(UUID templateId, ManagementSessionClaims session) {
        TemplateEntity template = templateRepository.findByIdAndDeletedAtIsNull(templateId)
                .orElseThrow(TemplateNotFoundException::new);
        exportAccessSupport.assertCanExport(template, session);
        assertExportEligible(template);
        var detail = templateService.toDetail(template);
        TemplateVersionEntity version = templateVersionSupport.requireExportableVersion(templateId);
        return new TemplateExportBundleView(
                EXPORT_FORMAT,
                new TemplateExportMetadataView(
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
                        Instant.now()
                ),
                detail.variables(),
                detail.bindings(),
                templateService.loadRules(version),
                contentModuleReferenceService.listReferences(templateId, session),
                apiPolicyRepository.findByTemplateId(templateId)
                        .map(apiPolicyViewMapper::toPolicyView)
                        .orElse(null)
        );
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
    }
}
