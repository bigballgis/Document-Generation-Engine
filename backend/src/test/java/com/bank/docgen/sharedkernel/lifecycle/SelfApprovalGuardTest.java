package com.bank.docgen.sharedkernel.lifecycle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.sharedkernel.api.ApiErrorCategories;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.api.LifecycleAuthorizationException;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class SelfApprovalGuardTest {

    private static final String SELF_APPROVAL_KEY = "api.error.lifecycle.selfApprovalForbidden";
    private static final String NOT_ALLOWED_KEY = "api.error.lifecycle.exceptionInterventionNotAllowed";
    private static final String REASON_KEY = "api.error.lifecycle.exceptionReasonRequired";
    private static final String SECONDARY_KEY = "api.error.lifecycle.exceptionSecondaryConfirmRequired";

    @Test
    void sameActorWithoutException_isBlocked() {
        ManagementSessionClaims approver = session("alice", List.of("GROUP_ADMIN"));
        SelfApprovalGuard guard = new SelfApprovalGuard();

        assertThatThrownBy(() -> guard.enforce(request("alice", "alice", false, null, null, approver)))
                .isInstanceOf(LifecycleAuthorizationException.class)
                .satisfies(ex -> {
                    LifecycleAuthorizationException e = (LifecycleAuthorizationException) ex;
                    assertThat(e.errorCode()).isEqualTo(ApiErrorCodes.SELF_APPROVAL_FORBIDDEN);
                    assertThat(e.category()).isEqualTo(ApiErrorCategories.AUTHORIZATION);
                    assertThat(e.messageKey()).isEqualTo(SELF_APPROVAL_KEY);
                    assertThat(e.httpStatus().value()).isEqualTo(403);
                });
    }

    @Test
    void sameActorGroupAdminException_isAllowedAndMarksOutcome() {
        ManagementSessionClaims admin = session("alice", List.of("GROUP_ADMIN"));
        SelfApprovalGuard guard = new SelfApprovalGuard();

        SelfApprovalGuard.EnforceOutcome outcome = guard.enforce(
                request("alice", "alice", true, "Solo approval due to approver pool outage", true, admin));

        assertThat(outcome.selfApprovalException()).isTrue();
        assertThat(outcome.exceptionReason()).isEqualTo("Solo approval due to approver pool outage");
    }

    @Test
    void sameActorGlobalAdminException_isAllowedPerRole1() {
        ManagementSessionClaims admin = session("root", List.of("GLOBAL_ADMIN"));
        SelfApprovalGuard guard = new SelfApprovalGuard();

        SelfApprovalGuard.EnforceOutcome outcome = guard.enforce(
                request("root", "root", true, "Cross-group override", true, admin));

        assertThat(outcome.selfApprovalException()).isTrue();
    }

    @Test
    void nonGroupAdminExceptionRequest_isRejected403() {
        // ADR-0070: former TEMPLATE_APPROVER absorbed into GROUP_ADMIN; pure authors cannot intervene.
        ManagementSessionClaims author = session("alice", List.of("DOCUMENT_AUTHOR"));
        SelfApprovalGuard guard = new SelfApprovalGuard();

        assertThatThrownBy(() -> guard.enforce(request("alice", "alice", true, "reason", true, author)))
                .isInstanceOf(LifecycleAuthorizationException.class)
                .satisfies(ex -> {
                    LifecycleAuthorizationException e = (LifecycleAuthorizationException) ex;
                    assertThat(e.errorCode()).isEqualTo(ApiErrorCodes.EXCEPTION_INTERVENTION_NOT_ALLOWED);
                    assertThat(e.messageKey()).isEqualTo(NOT_ALLOWED_KEY);
                    assertThat(e.httpStatus().value()).isEqualTo(403);
                });
    }

    @Test
    void exceptionReasonBlank_isRejected422() {
        ManagementSessionClaims admin = session("alice", List.of("GROUP_ADMIN"));
        SelfApprovalGuard guard = new SelfApprovalGuard();

        assertThatThrownBy(() -> guard.enforce(request("alice", "alice", true, "   ", true, admin)))
                .isInstanceOf(LifecycleAuthorizationException.class)
                .satisfies(ex -> {
                    LifecycleAuthorizationException e = (LifecycleAuthorizationException) ex;
                    assertThat(e.errorCode()).isEqualTo(ApiErrorCodes.EXCEPTION_REASON_REQUIRED);
                    assertThat(e.messageKey()).isEqualTo(REASON_KEY);
                    assertThat(e.httpStatus().value()).isEqualTo(422);
                });
    }

    @Test
    void exceptionSecondaryNotConfirmed_isRejected422() {
        ManagementSessionClaims admin = session("alice", List.of("GROUP_ADMIN"));
        SelfApprovalGuard guard = new SelfApprovalGuard();

        assertThatThrownBy(() -> guard.enforce(request("alice", "alice", true, "reason", false, admin)))
                .isInstanceOf(LifecycleAuthorizationException.class)
                .satisfies(ex -> {
                    LifecycleAuthorizationException e = (LifecycleAuthorizationException) ex;
                    assertThat(e.errorCode()).isEqualTo(ApiErrorCodes.EXCEPTION_SECONDARY_CONFIRM_REQUIRED);
                    assertThat(e.messageKey()).isEqualTo(SECONDARY_KEY);
                    assertThat(e.httpStatus().value()).isEqualTo(422);
                });
    }

    @Test
    void nullLastSubmitActor_doesNotBlock() {
        ManagementSessionClaims approver = session("alice", List.of("GROUP_ADMIN"));
        SelfApprovalGuard guard = new SelfApprovalGuard();

        SelfApprovalGuard.EnforceOutcome outcome = guard.enforce(
                request("alice", null, false, null, null, approver));

        assertThat(outcome.selfApprovalException()).isFalse();
        assertThat(outcome.exceptionReason()).isNull();
    }

    @Test
    void differentActor_passesWithoutException() {
        ManagementSessionClaims approver = session("bob", List.of("GROUP_ADMIN"));
        SelfApprovalGuard guard = new SelfApprovalGuard();

        SelfApprovalGuard.EnforceOutcome outcome = guard.enforce(
                request("bob", "alice", false, null, null, approver));

        assertThat(outcome.selfApprovalException()).isFalse();
        assertThat(outcome.exceptionReason()).isNull();
    }

    @Test
    void comparisonIsCaseSensitiveAfterTrim() {
        ManagementSessionClaims approver = session("alice", List.of("GROUP_ADMIN"));
        SelfApprovalGuard guard = new SelfApprovalGuard();

        // "Alice" vs "alice" => different people (CMP-1)
        SelfApprovalGuard.EnforceOutcome outcome = guard.enforce(
                request("alice", "Alice", false, null, null, approver));
        assertThat(outcome.selfApprovalException()).isFalse();

        // trimmed equality still blocks
        assertThatThrownBy(() -> guard.enforce(request("alice", "  alice  ", false, null, null, approver)))
                .isInstanceOf(LifecycleAuthorizationException.class);
    }

    private static SelfApprovalGuard.EnforceRequest request(
            String decisionActor,
            String lastSubmitActor,
            boolean exceptionIntervention,
            String exceptionReason,
            Boolean secondaryConfirmed,
            ManagementSessionClaims session
    ) {
        return new SelfApprovalGuard.EnforceRequest(
                decisionActor,
                lastSubmitActor,
                exceptionIntervention,
                exceptionReason,
                secondaryConfirmed,
                session,
                SELF_APPROVAL_KEY,
                NOT_ALLOWED_KEY,
                REASON_KEY,
                SECONDARY_KEY
        );
    }

    private static ManagementSessionClaims session(String username, List<String> roles) {
        return new ManagementSessionClaims(
                username,
                username,
                username + "@example.com",
                AuthSource.LOCAL,
                roles,
                List.of("RETAIL"),
                "route.dashboard-home",
                List.of("route.dashboard-home"),
                Instant.now().plusSeconds(3600)
        );
    }
}
