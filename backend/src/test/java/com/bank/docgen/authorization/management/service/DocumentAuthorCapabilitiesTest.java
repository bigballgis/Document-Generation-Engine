package com.bank.docgen.authorization.management.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.authorization.management.domain.ManagementRole;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * BDD-SYS-NORM-ROLE-002 / ROLE-010 — DOCUMENT_AUTHOR = MASTER_DESIGNER ∪ TEMPLATE_AUTHOR;
 * no decideTests / decideApprovals / reviewMasters / publish.
 */
class DocumentAuthorCapabilitiesTest {

    private final ManagementCapabilitiesService service =
            new ManagementCapabilitiesService(new GroupAccessService());

    @Test
    void documentAuthorHasAuthoringUnionWithoutGovernanceDecideBits() {
        var capabilities = service.resolve(Set.of(ManagementRole.DOCUMENT_AUTHOR));

        assertThat(capabilities.manageMasters()).isTrue();
        assertThat(capabilities.authorTemplates()).isTrue();
        assertThat(capabilities.authorContentModules()).isTrue();
        assertThat(capabilities.exportTemplates()).isTrue();
        assertThat(capabilities.stopTemplates()).isTrue();
        assertThat(capabilities.viewCollaborationWorkItems()).isTrue();
        assertThat(capabilities.manageAssetLibrary()).isTrue();

        assertThat(capabilities.decideTests()).isFalse();
        assertThat(capabilities.decideApprovals()).isFalse();
        assertThat(capabilities.decideLegalApprovals()).isFalse();
        assertThat(capabilities.reviewMasters()).isFalse();
        assertThat(capabilities.publishTemplates()).isFalse();
        assertThat(capabilities.decideContentModuleReviews()).isFalse();
        assertThat(capabilities.manageContentModuleLifecycle()).isFalse();
        assertThat(capabilities.manageLegalHold()).isFalse();
        assertThat(capabilities.deleteTemplates()).isFalse();
    }

    @Test
    void groupAdminAbsorbsFormerApproverDecideApprovals() {
        var capabilities = service.resolve(Set.of(ManagementRole.GROUP_ADMIN));

        assertThat(capabilities.decideApprovals()).isTrue();
        assertThat(capabilities.decideContentModuleReviews()).isTrue();
        assertThat(capabilities.decideTests()).isTrue();
        assertThat(capabilities.publishTemplates()).isTrue();
    }
}
