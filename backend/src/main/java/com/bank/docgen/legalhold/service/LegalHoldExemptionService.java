package com.bank.docgen.legalhold.service;

import com.bank.docgen.legalhold.domain.LegalHoldScopeType;
import com.bank.docgen.legalhold.domain.LegalHoldStatus;
import com.bank.docgen.legalhold.persistence.LegalHoldEntity;
import com.bank.docgen.legalhold.persistence.LegalHoldRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CE-G04 sole exemption entry for invocation + audit retention cleanup (G04-C9…C15).
 */
@Service
public class LegalHoldExemptionService {

    private static final Logger LOG = LoggerFactory.getLogger(LegalHoldExemptionService.class);

    private final LegalHoldRepository repository;

    public LegalHoldExemptionService(LegalHoldRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public boolean isInvocationExempt(UUID templateId, String invocationExternalId, Instant createdAt) {
        List<LegalHoldEntity> active = repository.findByStatus(LegalHoldStatus.ACTIVE);
        for (LegalHoldEntity hold : active) {
            if (matchesInvocation(hold, templateId, invocationExternalId, createdAt)) {
                LOG.info(
                        "Legal hold exemption: invocationExternalId={} holdId={} scope={}",
                        invocationExternalId,
                        hold.getId(),
                        hold.getScopeType()
                );
                return true;
            }
        }
        return false;
    }

    @Transactional(readOnly = true)
    public boolean isManagementAuditExempt(UUID templateId, Instant eventAt) {
        if (templateId == null || eventAt == null) {
            return false;
        }
        List<LegalHoldEntity> active = repository.findByStatus(LegalHoldStatus.ACTIVE);
        for (LegalHoldEntity hold : active) {
            if (hold.getScopeType() == LegalHoldScopeType.TEMPLATE_WINDOW
                    && matchesTemplateWindow(hold, templateId, eventAt)) {
                LOG.info(
                        "Legal hold exemption: managementAudit templateId={} holdId={}",
                        templateId,
                        hold.getId()
                );
                return true;
            }
            // G04-C13: INVOCATION_SET does not protect management audit rows.
        }
        return false;
    }

    @Transactional(readOnly = true)
    public boolean isRuntimeAuditExempt(
            UUID templateId,
            Instant eventAt,
            String taskExternalId,
            String documentId
    ) {
        List<LegalHoldEntity> active = repository.findByStatus(LegalHoldStatus.ACTIVE);
        for (LegalHoldEntity hold : active) {
            if (hold.getScopeType() == LegalHoldScopeType.TEMPLATE_WINDOW
                    && templateId != null
                    && eventAt != null
                    && matchesTemplateWindow(hold, templateId, eventAt)) {
                LOG.info(
                        "Legal hold exemption: runtimeAudit templateId={} holdId={}",
                        templateId,
                        hold.getId()
                );
                return true;
            }
            if (hold.getScopeType() == LegalHoldScopeType.INVOCATION_SET
                    && matchesInvocationSetIds(hold, taskExternalId, documentId)) {
                LOG.info(
                        "Legal hold exemption: runtimeAudit holdId={} (task/document match)",
                        hold.getId()
                );
                return true;
            }
        }
        return false;
    }

    private static boolean matchesInvocation(
            LegalHoldEntity hold,
            UUID templateId,
            String invocationExternalId,
            Instant createdAt
    ) {
        if (hold.getScopeType() == LegalHoldScopeType.TEMPLATE_WINDOW) {
            return templateId != null
                    && createdAt != null
                    && matchesTemplateWindow(hold, templateId, createdAt);
        }
        if (hold.getScopeType() == LegalHoldScopeType.INVOCATION_SET) {
            return invocationExternalId != null && hold.containsInvocationExternalId(invocationExternalId);
        }
        return false;
    }

    private static boolean matchesTemplateWindow(LegalHoldEntity hold, UUID templateId, Instant instant) {
        if (hold.getTemplateId() == null || hold.getEffectiveFrom() == null) {
            return false;
        }
        if (!hold.getTemplateId().equals(templateId)) {
            return false;
        }
        // G04-C5: t >= from AND (to == null OR t <= to)
        if (instant.isBefore(hold.getEffectiveFrom())) {
            return false;
        }
        Instant to = hold.getEffectiveTo();
        return to == null || !instant.isAfter(to);
    }

    private static boolean matchesInvocationSetIds(
            LegalHoldEntity hold,
            String taskExternalId,
            String documentId
    ) {
        if (taskExternalId != null && hold.containsInvocationExternalId(taskExternalId)) {
            return true;
        }
        return documentId != null && hold.containsInvocationExternalId(documentId);
    }
}
