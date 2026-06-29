package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.RiskPromptConfigView;
import com.bank.docgen.template.api.UpsertRiskPromptConfigRequest;
import com.bank.docgen.template.domain.RiskPromptScope;
import com.bank.docgen.template.persistence.RiskPromptConfigEntity;
import com.bank.docgen.template.persistence.RiskPromptConfigRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RiskPromptConfigServiceTest {

    @Mock
    private RiskPromptConfigRepository riskPromptConfigRepository;
    @Mock
    private GroupAccessService groupAccessService;
    @Mock
    private ManagementAuditRecorder auditRecorder;

    private RiskPromptConfigService service;
    private ManagementSessionClaims globalAdmin;
    private ManagementSessionClaims groupAdmin;

    @BeforeEach
    void setUp() {
        service = new RiskPromptConfigService(
                riskPromptConfigRepository,
                groupAccessService,
                auditRecorder,
                new ObjectMapper()
        );
        globalAdmin = new ManagementSessionClaims(
                "10000001",
                "Global Admin",
                "global@example.com",
                AuthSource.LOCAL,
                List.of("GLOBAL_ADMIN"),
                List.of("*"),
                "route.dashboard-home",
                List.of("route.dashboard-home"),
                Instant.now().plusSeconds(3600)
        );
        groupAdmin = new ManagementSessionClaims(
                "10000002",
                "Group Admin",
                "group.admin@example.com",
                AuthSource.LOCAL,
                List.of("GROUP_ADMIN"),
                List.of("RETAIL"),
                "route.dashboard-home",
                List.of("route.dashboard-home"),
                Instant.now().plusSeconds(3600)
        );
    }

    @Test
    void globalDefault_appliesWhenNoGroupOverride() {
        RiskPromptConfigEntity global = entity(RiskPromptScope.GLOBAL, null, List.of("BINDING_ISSUE"));
        when(groupAccessService.canAccessGroup(groupAdmin, "RETAIL")).thenReturn(true);
        when(riskPromptConfigRepository.findByScopeTypeAndGroupCode(RiskPromptScope.GROUP, "RETAIL"))
                .thenReturn(Optional.empty());
        when(riskPromptConfigRepository.findByScopeTypeAndGroupCode(RiskPromptScope.GLOBAL, null))
                .thenReturn(Optional.of(global));

        RiskPromptConfigView view = service.resolve("RETAIL", groupAdmin);

        assertThat(view.scopeType()).isEqualTo("GLOBAL");
        assertThat(view.reasonCategories()).contains("BINDING_ISSUE");
    }

    @Test
    void groupOverride_takesPrecedence_inScope() {
        RiskPromptConfigEntity group = entity(RiskPromptScope.GROUP, "RETAIL", List.of("COVERAGE_GAP"));
        when(groupAccessService.canAccessGroup(groupAdmin, "RETAIL")).thenReturn(true);
        when(riskPromptConfigRepository.findByScopeTypeAndGroupCode(RiskPromptScope.GROUP, "RETAIL"))
                .thenReturn(Optional.of(group));

        RiskPromptConfigView view = service.resolve("RETAIL", groupAdmin);

        assertThat(view.scopeType()).isEqualTo("GROUP");
        assertThat(view.groupCode()).isEqualTo("RETAIL");
        assertThat(view.reasonCategories()).containsExactly("COVERAGE_GAP");
    }

    @Test
    void configChange_isAudited() {
        when(riskPromptConfigRepository.findByScopeTypeAndGroupCode(RiskPromptScope.GLOBAL, null))
                .thenReturn(Optional.empty());
        when(riskPromptConfigRepository.save(any(RiskPromptConfigEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UpsertRiskPromptConfigRequest request = new UpsertRiskPromptConfigRequest(
                "GLOBAL",
                null,
                List.of("BINDING_ISSUE"),
                Map.of("UNRESOLVED_BLOCKERS", "Review blockers.")
        );

        service.upsert(request, globalAdmin);

        verify(auditRecorder).recordRiskPromptConfigUpdated(
                eq("GLOBAL"),
                eq(null),
                eq("10000001"),
                eq("Global Admin"),
                any(String.class)
        );
    }

    private RiskPromptConfigEntity entity(RiskPromptScope scope, String groupCode, List<String> categories) {
        return new RiskPromptConfigEntity(
                UUID.randomUUID(),
                scope,
                groupCode,
                "[\"" + categories.get(0) + "\"]",
                "{\"UNRESOLVED_BLOCKERS\":\"Review blockers.\"}"
        );
    }
}
