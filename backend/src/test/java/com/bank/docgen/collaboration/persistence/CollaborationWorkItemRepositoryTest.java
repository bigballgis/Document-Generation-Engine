package com.bank.docgen.collaboration.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.collaboration.domain.CollaborationWorkItemQueue;
import com.bank.docgen.collaboration.domain.CollaborationWorkItemStatus;
import com.bank.docgen.collaboration.domain.CollaborationWorkItemTriggerType;
import com.bank.docgen.infrastructure.config.QuerydslConfig;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@Import(QuerydslConfig.class)
@ActiveProfiles("test")
class CollaborationWorkItemRepositoryTest {

    private static final UUID TEMPLATE_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Autowired
    private CollaborationWorkItemRepository repository;

    @BeforeEach
    void seedItems() {
        repository.save(openItem(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "RETAIL",
                CollaborationWorkItemQueue.TEST
        ));
        repository.save(openItem(
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                "RETAIL",
                CollaborationWorkItemQueue.APPROVAL
        ));
        repository.save(openItem(
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                "CORP",
                CollaborationWorkItemQueue.TEST
        ));
        CollaborationWorkItemEntity resolved = openItem(
                UUID.fromString("44444444-4444-4444-4444-444444444444"),
                "RETAIL",
                CollaborationWorkItemQueue.TEST
        );
        resolved.setStatus(CollaborationWorkItemStatus.RESOLVED);
        resolved.setResolvedAt(Instant.parse("2026-06-26T00:00:00Z"));
        repository.save(resolved);
    }

    @Test
    void findOpenByQueuesAndGroups_returnsMatchingOpenItems() {
        List<CollaborationWorkItemEntity> items = repository.findOpenByQueuesAndGroups(
                List.of(CollaborationWorkItemQueue.TEST),
                List.of("RETAIL")
        );

        assertThat(items).hasSize(1);
        assertThat(items.get(0).getGroupCode()).isEqualTo("RETAIL");
        assertThat(items.get(0).getQueue()).isEqualTo(CollaborationWorkItemQueue.TEST);
        assertThat(items.get(0).getStatus()).isEqualTo(CollaborationWorkItemStatus.OPEN);
    }

    @Test
    void findOpenByQueuesAndGroups_supportsWildcardGroupScope() {
        List<CollaborationWorkItemEntity> items = repository.findOpenByQueuesAndGroups(
                List.of(CollaborationWorkItemQueue.TEST, CollaborationWorkItemQueue.APPROVAL),
                List.of("*")
        );

        assertThat(items).hasSize(3);
    }

    @Test
    void findOpenEscalationCandidates_returnsOnlyOperationalQueues() {
        List<CollaborationWorkItemEntity> items = repository.findOpenEscalationCandidates();

        assertThat(items).hasSize(3);
        assertThat(items).extracting(CollaborationWorkItemEntity::getQueue)
                .doesNotContain(CollaborationWorkItemQueue.ESCALATION);
    }

    @Test
    void existsOpenEscalationForSource_detectsOpenEscalation() {
        UUID sourceId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        CollaborationWorkItemEntity escalation = openItem(
                UUID.fromString("55555555-5555-5555-5555-555555555555"),
                "RETAIL",
                CollaborationWorkItemQueue.ESCALATION
        );
        escalation.setSourceWorkItemId(sourceId);
        repository.save(escalation);

        assertThat(repository.existsOpenEscalationForSource(sourceId)).isTrue();
        assertThat(repository.existsOpenEscalationForSource(UUID.randomUUID())).isFalse();
    }

    @Test
    void findOpenByTemplateIdAndQueue_returnsOpenItemOnly() {
        UUID otherTemplateId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
        UUID otherItemId = UUID.fromString("66666666-6666-6666-6666-666666666666");
        repository.save(openItem(otherItemId, otherTemplateId, "RETAIL", CollaborationWorkItemQueue.TEST));

        Optional<CollaborationWorkItemEntity> templateTestItem =
                repository.findOpenByTemplateIdAndQueue(TEMPLATE_ID, CollaborationWorkItemQueue.TEST);
        assertThat(templateTestItem).isPresent();
        assertThat(templateTestItem.orElseThrow().getId())
                .isEqualTo(UUID.fromString("11111111-1111-1111-1111-111111111111"));

        Optional<CollaborationWorkItemEntity> otherTemplateTestItem =
                repository.findOpenByTemplateIdAndQueue(otherTemplateId, CollaborationWorkItemQueue.TEST);
        assertThat(otherTemplateTestItem).isPresent();
        assertThat(otherTemplateTestItem.orElseThrow().getId()).isEqualTo(otherItemId);

        Optional<CollaborationWorkItemEntity> templateApprovalItem =
                repository.findOpenByTemplateIdAndQueue(TEMPLATE_ID, CollaborationWorkItemQueue.APPROVAL);
        assertThat(templateApprovalItem).isPresent();
        assertThat(templateApprovalItem.orElseThrow().getId())
                .isEqualTo(UUID.fromString("22222222-2222-2222-2222-222222222222"));
    }

    private CollaborationWorkItemEntity openItem(UUID id, String groupCode, CollaborationWorkItemQueue queue) {
        return openItem(id, TEMPLATE_ID, groupCode, queue);
    }

    private CollaborationWorkItemEntity openItem(
            UUID id,
            UUID templateId,
            String groupCode,
            CollaborationWorkItemQueue queue
    ) {
        return new CollaborationWorkItemEntity(
                id,
                templateId,
                "TPL-001",
                "Sample Template",
                groupCode,
                queue,
                CollaborationWorkItemTriggerType.SUBMIT_FOR_TEST,
                CollaborationWorkItemStatus.OPEN,
                "10000003",
                "Non-sensitive summary"
        );
    }
}
