package com.bank.docgen.apimgmt.service;

import com.bank.docgen.apimgmt.api.InvocationRegeneratedAuditDetail;
import com.bank.docgen.apimgmt.api.ManagementInvocationRegenerateRequest;
import com.bank.docgen.apimgmt.api.ManagementInvocationRegenerateView;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.apimgmt.persistence.InvocationRegenerationEntity;
import com.bank.docgen.apimgmt.persistence.InvocationRegenerationRepository;
import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.rendering.DocxAssemblyException;
import com.bank.docgen.rendering.RenderingOperationException;
import com.bank.docgen.runtime.domain.InvocationKind;
import com.bank.docgen.runtime.persistence.ApiInvocationRecordEntity;
import com.bank.docgen.runtime.persistence.ApiInvocationRecordRepository;
import com.bank.docgen.sharedkernel.api.ApiErrorCategories;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.service.TemplateService;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvocationRegenerationService {

    private static final Set<InvocationKind> REGENERABLE_KINDS = EnumSet.of(
            InvocationKind.SINGLE,
            InvocationKind.BATCH_ITEM,
            InvocationKind.ASYNC_TASK
    );
    private static final Set<String> ALLOWED_OUTPUT_FORMATS = Set.of("DOCX", "PDF");
    private static final int PRODUCTION_REISSUE_REASON_MAX_LENGTH = 500;
    private static final String PINNED_MASTER_UNAVAILABLE_KEY = "api.error.rendering.pinnedMasterUnavailable";
    private static final String SPECIMEN_WATERMARK_FAILED_KEY = "api.error.audit.specimenWatermarkFailed";

    private final TemplateService templateService;
    private final ApiPolicyRepository apiPolicyRepository;
    private final ApiInvocationRecordRepository invocationRecordRepository;
    private final InvocationRegenerationRepository regenerationRepository;
    private final GroupAccessService groupAccessService;
    private final ManagementAuditRecorder managementAuditRecorder;
    private final InvocationRegenerationAssemblySupport assemblySupport;

    public InvocationRegenerationService(
            TemplateService templateService,
            ApiPolicyRepository apiPolicyRepository,
            ApiInvocationRecordRepository invocationRecordRepository,
            InvocationRegenerationRepository regenerationRepository,
            GroupAccessService groupAccessService,
            ManagementAuditRecorder managementAuditRecorder,
            InvocationRegenerationAssemblySupport assemblySupport
    ) {
        this.templateService = templateService;
        this.apiPolicyRepository = apiPolicyRepository;
        this.invocationRecordRepository = invocationRecordRepository;
        this.regenerationRepository = regenerationRepository;
        this.groupAccessService = groupAccessService;
        this.managementAuditRecorder = managementAuditRecorder;
        this.assemblySupport = assemblySupport;
    }

    @Transactional
    public ManagementInvocationRegenerateView regenerate(
            UUID templateId,
            String invocationId,
            ManagementInvocationRegenerateRequest request,
            ManagementSessionClaims session
    ) {
        if (!groupAccessService.canRegenerateInvocation(session)) {
            throw new ApiManagementAccessDeniedException();
        }
        TemplateEntity template = templateService.requireReadableTemplate(templateId, session);
        apiPolicyRepository.findByTemplateId(templateId).orElseThrow(ApiManagementNotFoundException::new);

        ApiInvocationRecordEntity invocation = invocationRecordRepository.findByInvocationExternalId(invocationId)
                .orElseThrow(ApiManagementNotFoundException::new);
        if (!invocation.getTemplateId().equals(templateId)) {
            throw new ApiManagementNotFoundException();
        }

        boolean productionReissue = request != null && request.productionReissueRequested();
        if (productionReissue && !groupAccessService.canProductionReissueInvocation(session)) {
            throw new ApiManagementAccessDeniedException();
        }

        UUID regenerationId = UUID.randomUUID();
        String outputFormat = null;
        boolean specimen = !productionReissue;
        String productionReason = null;
        try {
            if (productionReissue) {
                productionReason = resolveProductionReissueReason(request);
            }
            outputFormat = resolveOutputFormat(request, invocation);
            assertNotExpired(invocation);
            assertRegenerableKind(invocation);
            assertFingerprintPresent(invocation);

            InvocationRegenerationAssemblySupport.AssembledRegeneration assembled = productionReissue
                    ? assemblySupport.assembleProductionReissue(
                            template, invocation, outputFormat, regenerationId)
                    : assemblySupport.assembleSpecimen(
                            template, invocation, outputFormat, regenerationId);

            regenerationRepository.save(new InvocationRegenerationEntity(
                    regenerationId,
                    regenerationId.toString(),
                    invocation.getInvocationExternalId(),
                    templateId,
                    invocation.getReleaseBundleSnapshotId(),
                    invocation.getReleaseBundleHash(),
                    outputFormat,
                    "SUCCESS",
                    null,
                    assembled.artifactStorageKey(),
                    specimen,
                    false,
                    session.username() == null ? "unknown" : session.username(),
                    Instant.now(),
                    productionReason
            ));
            recordAudit(
                    template,
                    invocation,
                    regenerationId,
                    outputFormat,
                    "SUCCESS",
                    null,
                    session.username(),
                    productionReissue,
                    specimen,
                    productionReason
            );
            return new ManagementInvocationRegenerateView(
                    regenerationId,
                    invocation.getInvocationExternalId(),
                    invocation.getReleaseBundleSnapshotId(),
                    invocation.getReleaseBundleHash(),
                    outputFormat,
                    specimen,
                    false,
                    null,
                    assembled.artifactStorageKey()
            );
        } catch (InvocationRegenerationException ex) {
            recordAudit(
                    template,
                    invocation,
                    regenerationId,
                    outputFormat,
                    "FAILURE",
                    ex.errorCode(),
                    session.username(),
                    productionReissue,
                    specimen,
                    productionReason
            );
            throw ex;
        } catch (ApiManagementNotFoundException | ApiManagementAccessDeniedException ex) {
            throw ex;
        } catch (DocxAssemblyException ex) {
            String code = ex.errorCode() == null ? ApiErrorCodes.RENDERING_FAILED : ex.errorCode();
            recordAudit(
                    template,
                    invocation,
                    regenerationId,
                    outputFormat,
                    "FAILURE",
                    code,
                    session.username(),
                    productionReissue,
                    specimen,
                    productionReason
            );
            throw new InvocationRegenerationException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    code,
                    ex.category() == null ? ApiErrorCategories.RENDERING : ex.category(),
                    ex.messageKey() == null ? "api.error.rendering.generationFailed" : ex.messageKey()
            );
        } catch (RenderingOperationException ex) {
            boolean pinned = PINNED_MASTER_UNAVAILABLE_KEY.equals(ex.messageKey());
            boolean watermarkFailed = SPECIMEN_WATERMARK_FAILED_KEY.equals(ex.messageKey());
            String code;
            String messageKey;
            HttpStatus status;
            String category;
            if (pinned) {
                code = ApiErrorCodes.PINNED_MASTER_UNAVAILABLE;
                messageKey = ex.messageKey();
                status = HttpStatus.UNPROCESSABLE_ENTITY;
                category = ApiErrorCategories.RENDERING;
            } else if (watermarkFailed) {
                code = ApiErrorCodes.SPECIMEN_WATERMARK_FAILED;
                messageKey = SPECIMEN_WATERMARK_FAILED_KEY;
                status = HttpStatus.INTERNAL_SERVER_ERROR;
                category = ApiErrorCategories.GENERATION;
            } else {
                code = ApiErrorCodes.INTERNAL_ERROR;
                messageKey = "api.error.generation.internalError";
                status = HttpStatus.INTERNAL_SERVER_ERROR;
                category = ApiErrorCategories.GENERATION;
            }
            recordAudit(
                    template,
                    invocation,
                    regenerationId,
                    outputFormat,
                    "FAILURE",
                    code,
                    session.username(),
                    productionReissue,
                    specimen,
                    productionReason
            );
            throw new InvocationRegenerationException(status, code, category, messageKey);
        } catch (RuntimeException ex) {
            // Do not map arbitrary failures to SPECIMEN_WATERMARK_FAILED (arch remediation).
            String code = ApiErrorCodes.INTERNAL_ERROR;
            recordAudit(
                    template,
                    invocation,
                    regenerationId,
                    outputFormat,
                    "FAILURE",
                    code,
                    session.username(),
                    productionReissue,
                    specimen,
                    productionReason
            );
            throw new InvocationRegenerationException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    code,
                    ApiErrorCategories.GENERATION,
                    "api.error.generation.internalError"
            );
        }
    }

    private String resolveProductionReissueReason(ManagementInvocationRegenerateRequest request) {
        String raw = request == null ? null : request.reason();
        if (raw == null || raw.isBlank()) {
            throw new InvocationRegenerationException(
                    HttpStatus.BAD_REQUEST,
                    ApiErrorCodes.PRODUCTION_REISSUE_REASON_REQUIRED,
                    ApiErrorCategories.VALIDATION,
                    "api.error.audit.productionReissueReasonRequired"
            );
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            throw new InvocationRegenerationException(
                    HttpStatus.BAD_REQUEST,
                    ApiErrorCodes.PRODUCTION_REISSUE_REASON_REQUIRED,
                    ApiErrorCategories.VALIDATION,
                    "api.error.audit.productionReissueReasonRequired"
            );
        }
        if (trimmed.length() > PRODUCTION_REISSUE_REASON_MAX_LENGTH) {
            throw new InvocationRegenerationException(
                    HttpStatus.BAD_REQUEST,
                    ApiErrorCodes.PRODUCTION_REISSUE_REASON_REQUIRED,
                    ApiErrorCategories.VALIDATION,
                    "api.error.validation.fieldSizeInvalid"
            );
        }
        return trimmed;
    }

    private void assertNotExpired(ApiInvocationRecordEntity invocation) {
        if (!invocation.getRecordExpiresAt().isAfter(Instant.now())) {
            throw new InvocationRegenerationException(
                    HttpStatus.GONE,
                    ApiErrorCodes.INVOCATION_RECORD_EXPIRED,
                    "API_POLICY",
                    "api.error.audit.invocationRecordExpired"
            );
        }
    }

    private void assertRegenerableKind(ApiInvocationRecordEntity invocation) {
        if (!REGENERABLE_KINDS.contains(invocation.getInvocationKind())) {
            throw new InvocationRegenerationException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    ApiErrorCodes.INVOCATION_KIND_NOT_REGENERABLE,
                    ApiErrorCategories.VALIDATION,
                    "api.error.audit.invocationKindNotRegenerable"
            );
        }
    }

    private void assertFingerprintPresent(ApiInvocationRecordEntity invocation) {
        if (invocation.getReleaseBundleSnapshotId() == null
                || invocation.getReleaseBundleHash() == null
                || invocation.getReleaseBundleHash().isBlank()) {
            throw new InvocationRegenerationException(
                    HttpStatus.CONFLICT,
                    ApiErrorCodes.RELEASE_BUNDLE_SNAPSHOT_UNAVAILABLE,
                    ApiErrorCategories.GENERATION,
                    "api.error.audit.releaseBundleSnapshotUnavailable"
            );
        }
    }

    private static String resolveOutputFormat(
            ManagementInvocationRegenerateRequest request,
            ApiInvocationRecordEntity invocation
    ) {
        String format;
        if (request != null && request.outputFormat() != null && !request.outputFormat().isBlank()) {
            format = request.outputFormat().trim().toUpperCase(Locale.ROOT);
        } else if (invocation.getOutputFormat() != null && !invocation.getOutputFormat().isBlank()) {
            format = invocation.getOutputFormat().trim().toUpperCase(Locale.ROOT);
        } else {
            format = "PDF";
        }
        if (!ALLOWED_OUTPUT_FORMATS.contains(format)) {
            throw new InvocationRegenerationException(
                    HttpStatus.BAD_REQUEST,
                    ApiErrorCodes.OUTPUT_FORMAT_NOT_ALLOWED,
                    ApiErrorCategories.VALIDATION,
                    "api.error.runtime.outputFormatUnsupported"
            );
        }
        return format;
    }

    private void recordAudit(
            TemplateEntity template,
            ApiInvocationRecordEntity invocation,
            UUID regenerationId,
            String outputFormat,
            String outcome,
            String errorCode,
            String actorUsername,
            boolean productionReissue,
            boolean specimen,
            String reason
    ) {
        managementAuditRecorder.recordInvocationRegenerated(new InvocationRegeneratedAuditDetail(
                invocation.getInvocationExternalId(),
                regenerationId.toString(),
                invocation.getReleaseBundleSnapshotId() == null
                        ? null
                        : invocation.getReleaseBundleSnapshotId().toString(),
                invocation.getReleaseBundleHash(),
                outputFormat,
                outcome,
                errorCode,
                actorUsername,
                false,
                template.getId(),
                template.getGroupCode(),
                productionReissue,
                specimen,
                reason
        ));
    }
}
