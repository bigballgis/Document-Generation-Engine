package com.bank.docgen.legalhold.service;

import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.authorization.management.api.PageView;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.legalhold.api.CreateLegalHoldRequest;
import com.bank.docgen.legalhold.api.LegalHoldView;
import com.bank.docgen.legalhold.domain.LegalHoldScopeType;
import com.bank.docgen.legalhold.domain.LegalHoldStatus;
import com.bank.docgen.legalhold.persistence.LegalHoldEntity;
import com.bank.docgen.legalhold.persistence.LegalHoldRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.service.TemplateNotFoundException;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LegalHoldService {

    static final int MAX_INVOCATION_IDS = 500;

    private final LegalHoldRepository repository;
    private final TemplateRepository templateRepository;
    private final GroupAccessService groupAccessService;
    private final ManagementAuditRecorder auditRecorder;

    public LegalHoldService(
            LegalHoldRepository repository,
            TemplateRepository templateRepository,
            GroupAccessService groupAccessService,
            ManagementAuditRecorder auditRecorder
    ) {
        this.repository = repository;
        this.templateRepository = templateRepository;
        this.groupAccessService = groupAccessService;
        this.auditRecorder = auditRecorder;
    }

    @Transactional(readOnly = true)
    public PageView<LegalHoldView> list(LegalHoldStatus status, int page, int size, ManagementSessionClaims session) {
        requireManage(session);
        List<LegalHoldEntity> entities = status == null
                ? repository.findAllByOrderByCreatedAtDesc()
                : repository.findByStatusOrderByCreatedAtDesc(status);
        List<LegalHoldView> views = entities.stream().map(this::toView).toList();
        return PageView.of(views, page, size);
    }

    @Transactional(readOnly = true)
    public LegalHoldView get(UUID id, ManagementSessionClaims session) {
        requireManage(session);
        return toView(requireHold(id));
    }

    @Transactional
    public LegalHoldView create(CreateLegalHoldRequest request, ManagementSessionClaims session) {
        requireManage(session);
        if (request == null || request.scopeType() == null) {
            throw new LegalHoldValidationException("api.error.validation.requestBodyInvalid");
        }
        validateReason(request.reason());

        LegalHoldEntity entity = switch (request.scopeType()) {
            case TEMPLATE_WINDOW -> buildTemplateWindow(request, session);
            case INVOCATION_SET -> buildInvocationSet(request, session);
        };
        LegalHoldEntity saved = repository.save(entity);
        auditRecorder.recordLegalHoldCreated(saved, session.username(), actorSummary(session));
        return toView(saved);
    }

    @Transactional
    public LegalHoldView release(UUID id, ManagementSessionClaims session) {
        requireManage(session);
        LegalHoldEntity hold = requireHold(id);
        if (hold.getStatus() == LegalHoldStatus.RELEASED) {
            throw new LegalHoldAlreadyReleasedException();
        }
        hold.release(Instant.now(), session.username());
        LegalHoldEntity saved = repository.save(hold);
        auditRecorder.recordLegalHoldReleased(saved, session.username(), actorSummary(session));
        return toView(saved);
    }

    private LegalHoldEntity buildTemplateWindow(CreateLegalHoldRequest request, ManagementSessionClaims session) {
        rejectInvocationIds(request);
        if (request.effectiveFrom() == null) {
            throw new LegalHoldValidationException("api.error.validation.fieldRequired");
        }
        if (request.effectiveTo() != null && request.effectiveTo().isBefore(request.effectiveFrom())) {
            throw new LegalHoldValidationException("api.error.validation.fieldInvalid");
        }
        TemplateEntity template = resolveTemplate(request.templateId(), request.templateExternalId());
        return new LegalHoldEntity(
                UUID.randomUUID(),
                newExternalId(),
                LegalHoldScopeType.TEMPLATE_WINDOW,
                LegalHoldStatus.ACTIVE,
                blankToNull(request.reason()),
                template.getId(),
                template.getExternalId(),
                request.effectiveFrom(),
                request.effectiveTo(),
                Instant.now(),
                session.username(),
                Set.of()
        );
    }

    private LegalHoldEntity buildInvocationSet(CreateLegalHoldRequest request, ManagementSessionClaims session) {
        rejectTemplateWindowFields(request);
        List<String> rawIds = request.invocationExternalIds();
        if (rawIds == null || rawIds.isEmpty()) {
            throw new LegalHoldValidationException("api.error.validation.fieldRequired");
        }
        Set<String> ids = new HashSet<>();
        for (String id : rawIds) {
            if (id == null || id.isBlank()) {
                throw new LegalHoldValidationException("api.error.validation.fieldInvalid");
            }
            ids.add(id.trim());
        }
        if (ids.isEmpty()) {
            throw new LegalHoldValidationException("api.error.validation.fieldRequired");
        }
        if (ids.size() > MAX_INVOCATION_IDS) {
            throw new LegalHoldValidationException("api.error.validation.fieldSizeInvalid");
        }
        return new LegalHoldEntity(
                UUID.randomUUID(),
                newExternalId(),
                LegalHoldScopeType.INVOCATION_SET,
                LegalHoldStatus.ACTIVE,
                blankToNull(request.reason()),
                null,
                null,
                null,
                null,
                Instant.now(),
                session.username(),
                ids
        );
    }

    private TemplateEntity resolveTemplate(UUID templateId, String templateExternalId) {
        if (templateId == null && (templateExternalId == null || templateExternalId.isBlank())) {
            throw new LegalHoldValidationException("api.error.validation.fieldRequired");
        }
        if (templateId != null && templateExternalId != null && !templateExternalId.isBlank()) {
            TemplateEntity byId = templateRepository.findByIdAndDeletedAtIsNull(templateId)
                    .orElseThrow(TemplateNotFoundException::new);
            if (!Objects.equals(byId.getExternalId(), templateExternalId.trim())) {
                throw new LegalHoldValidationException("api.error.validation.fieldInvalid");
            }
            return byId;
        }
        if (templateId != null) {
            return templateRepository.findByIdAndDeletedAtIsNull(templateId)
                    .orElseThrow(TemplateNotFoundException::new);
        }
        return templateRepository.findByExternalIdAndDeletedAtIsNull(templateExternalId.trim())
                .orElseThrow(TemplateNotFoundException::new);
    }

    private static void rejectInvocationIds(CreateLegalHoldRequest request) {
        if (request.invocationExternalIds() != null && !request.invocationExternalIds().isEmpty()) {
            throw new LegalHoldValidationException("api.error.validation.requestBodyInvalid");
        }
    }

    private static void rejectTemplateWindowFields(CreateLegalHoldRequest request) {
        if (request.templateId() != null
                || (request.templateExternalId() != null && !request.templateExternalId().isBlank())
                || request.effectiveFrom() != null
                || request.effectiveTo() != null) {
            throw new LegalHoldValidationException("api.error.validation.requestBodyInvalid");
        }
    }

    private static void validateReason(String reason) {
        if (reason != null && reason.length() > 512) {
            throw new LegalHoldValidationException("api.error.validation.fieldSizeInvalid");
        }
    }

    private LegalHoldEntity requireHold(UUID id) {
        return repository.findById(id).orElseThrow(LegalHoldNotFoundException::new);
    }

    private void requireManage(ManagementSessionClaims session) {
        if (!groupAccessService.canManageLegalHold(session)) {
            throw new LegalHoldAccessDeniedException();
        }
    }

    private LegalHoldView toView(LegalHoldEntity entity) {
        List<String> ids = entity.getInvocationExternalIds().stream().sorted().toList();
        return new LegalHoldView(
                entity.getId(),
                entity.getHoldExternalId(),
                entity.getScopeType(),
                entity.getStatus(),
                entity.getReason(),
                entity.getTemplateId(),
                entity.getTemplateExternalId(),
                entity.getEffectiveFrom(),
                entity.getEffectiveTo(),
                ids,
                ids.size(),
                entity.getCreatedAt(),
                entity.getCreatedByUsername(),
                entity.getReleasedAt(),
                entity.getReleasedByUsername()
        );
    }

    private static String actorSummary(ManagementSessionClaims session) {
        return session.displayName() + " (" + session.username() + ")";
    }

    private static String newExternalId() {
        return "HOLD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
