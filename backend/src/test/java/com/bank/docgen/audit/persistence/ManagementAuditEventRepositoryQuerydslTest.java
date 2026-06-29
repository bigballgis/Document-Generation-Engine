package com.bank.docgen.audit.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.infrastructure.config.QuerydslConfig;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@Import({ManagementAuditEventRepositoryImpl.class, QuerydslConfig.class})
@ActiveProfiles("test")
class ManagementAuditEventRepositoryQuerydslTest {

    private static final UUID TEMPLATE_RETAIL = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID TEMPLATE_CORP = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID CREDENTIAL_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @Autowired
    private ManagementAuditEventRepository repository;

    @BeforeEach
    void seedEvents() {
        repository.saveAll(List.of(
                event(
                        UUID.fromString("11111111-1111-1111-1111-111111111111"),
                        Instant.parse("2026-06-23T10:00:00Z"),
                        "API_POLICY_UPDATED",
                        TEMPLATE_RETAIL,
                        "RETAIL"
                ),
                event(
                        UUID.fromString("22222222-2222-2222-2222-222222222222"),
                        Instant.parse("2026-06-23T11:00:00Z"),
                        "API_POLICY_UPDATED",
                        TEMPLATE_CORP,
                        "CORP"
                ),
                event(
                        UUID.fromString("33333333-3333-3333-3333-333333333333"),
                        Instant.parse("2026-06-23T12:00:00Z"),
                        "CREDENTIAL_CREATED",
                        TEMPLATE_RETAIL,
                        "RETAIL"
                ),
                event(
                        UUID.fromString("44444444-4444-4444-4444-444444444444"),
                        Instant.parse("2026-06-23T13:00:00Z"),
                        "API_POLICY_UPDATED",
                        TEMPLATE_RETAIL,
                        "RETAIL"
                )
        ));
    }

    @Test
    void globalAdminPathReturnsAllGroupsWhenGroupFilterNull() {
        AuditSearchPage<ManagementAuditEventEntity> page = repository.searchPaged(
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                20
        );

        assertThat(page.totalElements()).isEqualTo(4);
        assertThat(page.content()).extracting(ManagementAuditEventEntity::getGroupCode)
                .containsExactlyInAnyOrder("RETAIL", "CORP", "RETAIL", "RETAIL");
    }

    @Test
    void groupScopedPathFiltersByGroupCode() {
        AuditSearchPage<ManagementAuditEventEntity> page = repository.searchPaged(
                null,
                null,
                null,
                null,
                null,
                "RETAIL",
                0,
                20
        );

        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(page.content()).extracting(ManagementAuditEventEntity::getGroupCode)
                .containsOnly("RETAIL");
    }

    @Test
    void searchPagedAppliesEventTypeAndTimeWindowFilters() {
        Instant from = Instant.parse("2026-06-23T10:30:00Z");
        Instant to = Instant.parse("2026-06-23T12:30:00Z");

        AuditSearchPage<ManagementAuditEventEntity> page = repository.searchPaged(
                null,
                "API_POLICY_UPDATED",
                null,
                from,
                to,
                null,
                0,
                20
        );

        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.content().getFirst().getGroupCode()).isEqualTo("CORP");
    }

    @Test
    void searchPagedReturnsCorrectPageSliceOrderedByEventAtDesc() {
        AuditSearchPage<ManagementAuditEventEntity> firstPage = repository.searchPaged(
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                2
        );
        AuditSearchPage<ManagementAuditEventEntity> secondPage = repository.searchPaged(
                null,
                null,
                null,
                null,
                null,
                null,
                1,
                2
        );

        assertThat(firstPage.totalElements()).isEqualTo(4);
        assertThat(firstPage.totalPages()).isEqualTo(2);
        assertThat(firstPage.content()).hasSize(2);
        assertThat(firstPage.content().getFirst().getEventAt())
                .isAfter(firstPage.content().get(1).getEventAt());
        assertThat(secondPage.content()).hasSize(2);
        assertThat(firstPage.content().getFirst().getId())
                .isNotIn(secondPage.content().stream().map(ManagementAuditEventEntity::getId).toList());
    }

    @Test
    void searchExportListUsesSameQuerydslPredicates() {
        List<ManagementAuditEventEntity> events = repository.search(
                TEMPLATE_RETAIL,
                "API_POLICY_UPDATED",
                null,
                null,
                null,
                "RETAIL"
        );

        assertThat(events).hasSize(2);
        assertThat(events).extracting(ManagementAuditEventEntity::getTemplateId).containsOnly(TEMPLATE_RETAIL);
        assertThat(events).extracting(ManagementAuditEventEntity::getEventType).containsOnly("API_POLICY_UPDATED");
    }

    private ManagementAuditEventEntity event(
            UUID id,
            Instant eventAt,
            String eventType,
            UUID templateId,
            String groupCode
    ) {
        return new ManagementAuditEventEntity(
                id,
                eventAt,
                eventType,
                templateId,
                groupCode,
                CREDENTIAL_ID,
                1,
                2,
                "[\"OUTPUT_POLICY\"]",
                false,
                null,
                "10000001",
                "Global Admin (10000001)",
                "fp-CRED-ABCD1234",
                "Policy updated",
                "[]"
        );
    }
}
