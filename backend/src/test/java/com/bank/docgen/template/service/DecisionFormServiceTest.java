package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.LifecycleDecisionRequest;
import com.bank.docgen.template.domain.LifecycleDecision;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DecisionFormServiceTest {

    @Mock
    private GroupAccessService groupAccessService;

    private DecisionFormService service;
    private ManagementSessionClaims tester;
    private ManagementSessionClaims approver;

    @BeforeEach
    void setUp() {
        service = new DecisionFormService(groupAccessService);
        tester = new ManagementSessionClaims(
                "10000006",
                "Tester",
                "tester@example.com",
                AuthSource.LOCAL,
                List.of("TEMPLATE_TESTER"),
                List.of("RETAIL"),
                "route.template-authoring-home",
                List.of("route.template-authoring-home"),
                Instant.now().plusSeconds(3600)
        );
        approver = new ManagementSessionClaims(
                "10000007",
                "Approver",
                "approver@example.com",
                AuthSource.LOCAL,
                List.of("TEMPLATE_APPROVER"),
                List.of("RETAIL"),
                "route.template-authoring-home",
                List.of("route.template-authoring-home"),
                Instant.now().plusSeconds(3600)
        );
    }

    @Test
    void testPass_requiresFidelityViewedConfirmation() {
        LifecycleDecisionRequest request = new LifecycleDecisionRequest(
                LifecycleDecision.PASSED,
                "Looks good",
                null,
                null,
                false,
                true,
                true,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertThatThrownBy(() -> service.validateTestDecision(request, tester))
                .isInstanceOf(TemplateValidationException.class)
                .hasFieldOrPropertyWithValue(
                        "messageKey",
                        "api.error.template.decisionFidelityConfirmationRequired"
                );
    }

    @Test
    void testFail_requiresReasonCategoryAndImpact() {
        LifecycleDecisionRequest request = new LifecycleDecisionRequest(
                LifecycleDecision.FAILED,
                "Needs fixes",
                null,
                "Binding broken"
        );

        assertThatThrownBy(() -> service.validateTestDecision(request, tester))
                .isInstanceOf(TemplateValidationException.class)
                .hasFieldOrPropertyWithValue(
                        "messageKey",
                        "api.error.template.decisionReasonCategoryRequired"
                );
    }

    @Test
    void approvalReject_requiresCategoryAndRemediation_linksEvidence() {
        LifecycleDecisionRequest request = new LifecycleDecisionRequest(
                LifecycleDecision.REJECTED,
                "Not ready",
                "SCOPE_CHANGE",
                "Scope changed",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertThatThrownBy(() -> service.validateApprovalDecision(request, approver))
                .isInstanceOf(TemplateValidationException.class)
                .hasFieldOrPropertyWithValue(
                        "messageKey",
                        "api.error.template.decisionRemediationLinkRequired"
                );
    }
}
