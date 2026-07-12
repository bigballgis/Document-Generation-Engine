package com.bank.docgen.master.service;

import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.master.persistence.MasterDocumentEntity;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import java.util.UUID;

/**
 * Package-private read/write access guards for master documents.
 */
final class MasterDocumentAccessSupport {

    private final MasterDocumentRepository masterDocumentRepository;
    private final GroupAccessService groupAccessService;

    MasterDocumentAccessSupport(
            MasterDocumentRepository masterDocumentRepository,
            GroupAccessService groupAccessService
    ) {
        this.masterDocumentRepository = masterDocumentRepository;
        this.groupAccessService = groupAccessService;
    }

    MasterDocumentEntity requireReadableMaster(UUID masterId, ManagementSessionClaims session) {
        MasterDocumentEntity master = masterDocumentRepository.findByIdAndDeletedAtIsNull(masterId)
                .orElseThrow(MasterNotFoundException::new);
        if (!groupAccessService.canAccessGroup(session, master.getGroupCode())) {
            throw new MasterAccessDeniedException();
        }
        return master;
    }

    MasterDocumentEntity requireReadableMasterWithAnchors(UUID masterId, ManagementSessionClaims session) {
        MasterDocumentEntity master = masterDocumentRepository.findWithAnchorsByIdAndDeletedAtIsNull(masterId)
                .orElseThrow(MasterNotFoundException::new);
        if (!groupAccessService.canAccessGroup(session, master.getGroupCode())) {
            throw new MasterAccessDeniedException();
        }
        return master;
    }

    MasterDocumentEntity requireWritableMaster(UUID masterId, ManagementSessionClaims session) {
        MasterDocumentEntity master = requireReadableMaster(masterId, session);
        if (!groupAccessService.canManageMasters(session)) {
            throw new MasterAccessDeniedException();
        }
        return master;
    }

    MasterDocumentEntity requireWritableMasterWithAnchors(UUID masterId, ManagementSessionClaims session) {
        MasterDocumentEntity master = requireReadableMasterWithAnchors(masterId, session);
        if (!groupAccessService.canManageMasters(session)) {
            throw new MasterAccessDeniedException();
        }
        return master;
    }

    void assertGroupWritable(ManagementSessionClaims session, String groupCode) {
        if (!groupAccessService.canManageMasters(session)
                || !groupAccessService.canAccessGroup(session, groupCode)) {
            throw new MasterAccessDeniedException();
        }
    }

    boolean canReviewMasters(ManagementSessionClaims session) {
        return groupAccessService.canReviewMasters(session);
    }

    boolean canAccessGroup(ManagementSessionClaims session, String groupCode) {
        return groupAccessService.canAccessGroup(session, groupCode);
    }
}
