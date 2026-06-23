package com.bank.docgen.authorization.management.service;

import com.bank.docgen.authorization.management.api.ManagementCapabilitiesView;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.domain.ManagementRole;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ManagementCapabilitiesService {

    private final GroupAccessService groupAccessService;

    public ManagementCapabilitiesService(GroupAccessService groupAccessService) {
        this.groupAccessService = groupAccessService;
    }

    public ManagementCapabilitiesView resolve(Set<ManagementRole> roles) {
        ManagementSessionClaims session = toSyntheticClaims(roles);
        return new ManagementCapabilitiesView(
                groupAccessService.canManageMasters(session),
                groupAccessService.canReviewMasters(session),
                groupAccessService.canAuthorTemplates(session),
                groupAccessService.canDecideTemplateTests(session),
                groupAccessService.canDecideTemplateApprovals(session),
                groupAccessService.canPublishTemplates(session),
                groupAccessService.canManageApiPolicy(session),
                groupAccessService.canReadAudit(session)
        );
    }

    private ManagementSessionClaims toSyntheticClaims(Set<ManagementRole> roles) {
        List<String> roleCodes = roles.stream().map(Enum::name).toList();
        return new ManagementSessionClaims(
                "synthetic",
                "Synthetic",
                "synthetic@example.com",
                AuthSource.LOCAL,
                roleCodes,
                List.of(),
                "",
                List.of(),
                Instant.EPOCH
        );
    }
}
