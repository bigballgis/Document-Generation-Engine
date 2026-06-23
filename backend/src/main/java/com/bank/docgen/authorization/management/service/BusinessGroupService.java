package com.bank.docgen.authorization.management.service;

import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.authorization.management.api.BusinessGroupView;
import com.bank.docgen.authorization.management.api.CreateGroupRequest;
import com.bank.docgen.authorization.management.api.PageView;
import com.bank.docgen.authorization.management.api.UpdateGroupRequest;
import com.bank.docgen.authorization.management.persistence.BusinessGroupEntity;
import com.bank.docgen.authorization.management.persistence.BusinessGroupRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BusinessGroupService {

    private static final String GLOBAL_ADMIN = "GLOBAL_ADMIN";
    private static final String GROUP_ADMIN = "GROUP_ADMIN";

    private final BusinessGroupRepository businessGroupRepository;
    private final ManagementAuditRecorder auditRecorder;

    public BusinessGroupService(
            BusinessGroupRepository businessGroupRepository,
            ManagementAuditRecorder auditRecorder
    ) {
        this.businessGroupRepository = businessGroupRepository;
        this.auditRecorder = auditRecorder;
    }

    @Transactional(readOnly = true)
    public PageView<BusinessGroupView> list(ManagementSessionClaims session, int page, int size) {
        List<BusinessGroupView> visible = businessGroupRepository.findByDeletedAtIsNullOrderByGroupCodeAsc().stream()
                .filter(group -> canView(session, group.getGroupCode()))
                .map(BusinessGroupService::toView)
                .toList();
        return PageView.of(visible, page, size);
    }

    @Transactional(readOnly = true)
    public BusinessGroupView get(UUID id, ManagementSessionClaims session) {
        BusinessGroupEntity group = businessGroupRepository.findByIdAndDeletedAtIsNull(id)
                .filter(found -> canView(session, found.getGroupCode()))
                .orElseThrow(GroupNotFoundException::new);
        return toView(group);
    }

    @Transactional
    public BusinessGroupView create(CreateGroupRequest request, ManagementSessionClaims session) {
        requireGlobalAdmin(session, "create");
        if (businessGroupRepository.existsByGroupCodeAndDeletedAtIsNull(request.groupCode())) {
            throw new GroupCodeAlreadyExistsException();
        }
        BusinessGroupEntity group = new BusinessGroupEntity(
                UUID.randomUUID(),
                request.groupCode(),
                request.displayName(),
                request.dimension()
        );
        businessGroupRepository.save(group);
        auditRecorder.recordGroupEvent(
                ManagementAuditRecorder.GROUP_CREATED,
                group.getGroupCode(),
                session.username(),
                actorSummary(session),
                "Created group dimension=" + group.getDimension()
        );
        return toView(group);
    }

    @Transactional
    public BusinessGroupView updateDisplayName(UUID id, UpdateGroupRequest request, ManagementSessionClaims session) {
        requireGlobalAdmin(session, "update");
        BusinessGroupEntity group = businessGroupRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(GroupNotFoundException::new);
        group.rename(request.displayName());
        businessGroupRepository.save(group);
        auditRecorder.recordGroupEvent(
                ManagementAuditRecorder.GROUP_UPDATED,
                group.getGroupCode(),
                session.username(),
                actorSummary(session),
                "Updated display name"
        );
        return toView(group);
    }

    @Transactional
    public BusinessGroupView disable(UUID id, ManagementSessionClaims session) {
        requireGlobalAdmin(session, "disable");
        BusinessGroupEntity group = businessGroupRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(GroupNotFoundException::new);
        group.disable();
        businessGroupRepository.save(group);
        auditRecorder.recordGroupEvent(
                ManagementAuditRecorder.GROUP_DISABLED,
                group.getGroupCode(),
                session.username(),
                actorSummary(session),
                "Disabled group"
        );
        return toView(group);
    }

    @Transactional
    public BusinessGroupView enable(UUID id, ManagementSessionClaims session) {
        requireGlobalAdmin(session, "enable");
        BusinessGroupEntity group = businessGroupRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(GroupNotFoundException::new);
        group.enable();
        businessGroupRepository.save(group);
        auditRecorder.recordGroupEvent(
                ManagementAuditRecorder.GROUP_ENABLED,
                group.getGroupCode(),
                session.username(),
                actorSummary(session),
                "Enabled group"
        );
        return toView(group);
    }

    private void requireGlobalAdmin(ManagementSessionClaims session, String action) {
        if (!session.roles().contains(GLOBAL_ADMIN)) {
            auditRecorder.recordEscalationDenied(
                    "GROUP_MANAGEMENT_NOT_ALLOWED",
                    session.username(),
                    actorSummary(session),
                    "Attempted group " + action
            );
            throw new GroupManagementNotAllowedException();
        }
    }

    private boolean canView(ManagementSessionClaims session, String groupCode) {
        if (session.roles().contains(GLOBAL_ADMIN)) {
            return true;
        }
        if (session.roles().contains(GROUP_ADMIN)) {
            return session.authorizedGroupCodes().contains(groupCode);
        }
        return false;
    }

    private String actorSummary(ManagementSessionClaims session) {
        return session.displayName() + " (" + session.username() + ")";
    }

    private static BusinessGroupView toView(BusinessGroupEntity group) {
        return new BusinessGroupView(
                group.getId().toString(),
                group.getGroupCode(),
                group.getDisplayName(),
                group.getDimension().name(),
                group.isEnabled(),
                group.getCreatedAt(),
                group.getUpdatedAt()
        );
    }
}
