package com.bank.docgen.sharedkernel.quality;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.domain.ManagementRole;
import com.bank.docgen.authorization.management.persistence.ManagementUserEntity;
import com.bank.docgen.master.domain.MasterDocumentStatus;
import com.bank.docgen.master.persistence.MasterAnchorEntity;
import com.bank.docgen.master.persistence.MasterDocumentEntity;
import com.bank.docgen.master.persistence.MasterRevisionLineAnchorEntity;
import com.bank.docgen.master.persistence.MasterRevisionLineEntity;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PersistenceEntityCollectionImmutabilityTest {

    @Test
    void masterDocumentEntityDefensivelyCopiesAnchors() {
        UUID masterId = UUID.randomUUID();
        MasterDocumentEntity master = new MasterDocumentEntity(
                masterId,
                "RETAIL",
                "Policy Master",
                "Description",
                "storage/key.docx",
                "policy.docx",
                "10000001"
        );
        MasterAnchorEntity anchor = new MasterAnchorEntity(masterId, "ANCHOR-1", "Section 1", 1);
        master.replaceAnchors(new ArrayList<>(List.of(anchor)));

        List<MasterAnchorEntity> returned = master.getAnchors();
        assertThat(returned).containsExactly(anchor);
        assertThatThrownBy(() -> returned.add(new MasterAnchorEntity(masterId, "ANCHOR-2", "Section 2", 2)))
                .isInstanceOf(UnsupportedOperationException.class);

        master.replaceAnchors(List.of());
        assertThat(master.getAnchors()).isEmpty();
    }

    @Test
    void masterRevisionLineEntityDefensivelyCopiesAnchors() {
        UUID lineId = UUID.randomUUID();
        UUID masterId = UUID.randomUUID();
        MasterRevisionLineEntity line = new MasterRevisionLineEntity(
                lineId,
                masterId,
                "storage/revision.docx",
                "revision.docx",
                1,
                MasterDocumentStatus.DRAFT,
                1,
                true,
                "Initial upload",
                "10000001"
        );
        MasterRevisionLineAnchorEntity anchor = new MasterRevisionLineAnchorEntity(
                lineId,
                "ANCHOR-1",
                "Section 1",
                1
        );
        line.replaceAnchors(new ArrayList<>(List.of(anchor)));

        List<MasterRevisionLineAnchorEntity> returned = line.getAnchors();
        assertThat(returned).containsExactly(anchor);
        assertThatThrownBy(() -> returned.add(new MasterRevisionLineAnchorEntity(
                lineId,
                "ANCHOR-2",
                "Section 2",
                2
        ))).isInstanceOf(UnsupportedOperationException.class);

        line.replaceAnchors(List.of());
        assertThat(line.getAnchors()).isEmpty();
    }

    @Test
    void managementUserEntityDefensivelyCopiesRolesAndGroupScope() {
        Set<ManagementRole> roles = new LinkedHashSet<>(Set.of(ManagementRole.GROUP_ADMIN));
        Set<String> groups = new LinkedHashSet<>(Set.of("RETAIL"));

        ManagementUserEntity user = new ManagementUserEntity(
                UUID.randomUUID(),
                "10000002",
                "Group Admin",
                "admin@example.com",
                "hash",
                AuthSource.LOCAL,
                roles,
                groups
        );

        roles.add(ManagementRole.GLOBAL_ADMIN);
        groups.add("CORP");

        assertThat(user.getRoles()).containsExactly(ManagementRole.GROUP_ADMIN);
        assertThat(user.getAuthorizedGroupCodes()).containsExactly("RETAIL");
        assertThatThrownBy(() -> user.getRoles().add(ManagementRole.TEMPLATE_AUTHOR))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> user.getAuthorizedGroupCodes().add("WHOLESALE"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
