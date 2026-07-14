package com.bank.docgen.sharedkernel.lifecycle;

import com.bank.docgen.sharedkernel.api.ApiErrorCategories;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.api.LifecycleAuthorizationException;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * CE-G01: shared self-approval enforcement for template / master / content-module
 * decision services. Callers resolve the most recent submitter actor, then invoke
 * {@link #enforce(EnforceRequest)} <em>after</em> authorization / role / state / form
 * validation has passed and <em>before</em> the state-machine transition runs.
 *
 * <p>Comparison is case-sensitive after trimming (CMP-1). A null submitter never
 * triggers the block (CMP-3). Exception intervention is granted only to
 * {@code GROUP_ADMIN} / {@code GLOBAL_ADMIN} (ROLE-1) and requires a non-blank
 * {@code exceptionReason} plus {@code secondaryConfirmed=true} (EX-2).
 */
@Component
public final class SelfApprovalGuard {

    private static final String ROLE_GROUP_ADMIN = "GROUP_ADMIN";
    private static final String ROLE_GLOBAL_ADMIN = "GLOBAL_ADMIN";

    /**
     * Enforce the self-approval block.
     *
     * @return outcome carrying whether a valid self-approval exception was applied
     *         (and the persisted reason text); never {@code null}
     * @throws LifecycleAuthorizationException when the decision must fail closed
     *         ({@code SELF_APPROVAL_FORBIDDEN}, {@code EXCEPTION_INTERVENTION_NOT_ALLOWED},
     *         {@code EXCEPTION_REASON_REQUIRED}, or {@code EXCEPTION_SECONDARY_CONFIRM_REQUIRED})
     */
    public EnforceOutcome enforce(EnforceRequest request) {
        boolean exceptionRequested = Boolean.TRUE.equals(request.exceptionIntervention());
        boolean exceptionValid = false;
        String normalizedReason = null;
        if (exceptionRequested) {
            if (!hasExceptionRole(request.session())) {
                throw new LifecycleAuthorizationException(
                        ApiErrorCodes.EXCEPTION_INTERVENTION_NOT_ALLOWED,
                        ApiErrorCategories.AUTHORIZATION,
                        request.exceptionNotAllowedMessageKey(),
                        HttpStatus.FORBIDDEN
                );
            }
            if (isBlank(request.exceptionReason())) {
                throw new LifecycleAuthorizationException(
                        ApiErrorCodes.EXCEPTION_REASON_REQUIRED,
                        ApiErrorCategories.VALIDATION,
                        request.reasonRequiredMessageKey(),
                        HttpStatus.UNPROCESSABLE_ENTITY
                );
            }
            if (!Boolean.TRUE.equals(request.secondaryConfirmed())) {
                throw new LifecycleAuthorizationException(
                        ApiErrorCodes.EXCEPTION_SECONDARY_CONFIRM_REQUIRED,
                        ApiErrorCategories.VALIDATION,
                        request.secondaryConfirmRequiredMessageKey(),
                        HttpStatus.UNPROCESSABLE_ENTITY
                );
            }
            exceptionValid = true;
            normalizedReason = request.exceptionReason().trim();
        }

        String lastSubmitActor = request.lastSubmitActor();
        if (lastSubmitActor != null && sameActor(request.decisionActor(), lastSubmitActor)) {
            if (exceptionValid) {
                return new EnforceOutcome(true, normalizedReason);
            }
            throw new LifecycleAuthorizationException(
                    ApiErrorCodes.SELF_APPROVAL_FORBIDDEN,
                    ApiErrorCategories.AUTHORIZATION,
                    request.selfApprovalForbiddenMessageKey(),
                    HttpStatus.FORBIDDEN
            );
        }
        return new EnforceOutcome(false, null);
    }

    private static boolean hasExceptionRole(ManagementSessionClaims session) {
        return session.roles().contains(ROLE_GROUP_ADMIN)
                || session.roles().contains(ROLE_GLOBAL_ADMIN);
    }

    /**
     * Case-sensitive comparison after trimming (CMP-1). {@code lastSubmitActor == null}
     * is handled by the caller branch and never reaches here.
     */
    private static boolean sameActor(String decisionActor, String lastSubmitActor) {
        String a = decisionActor == null ? "" : decisionActor.trim();
        String b = lastSubmitActor == null ? "" : lastSubmitActor.trim();
        return a.equals(b);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * Input record carrying the decision actor, the resolved most-recent submitter,
     * the exception-intervention request fields, and the module-specific message keys
     * (template keeps the {@code api.error.template.*} keys; master / content-module
     * use the {@code api.error.lifecycle.*} namespace per Q2).
     */
    public record EnforceRequest(
            String decisionActor,
            String lastSubmitActor,
            boolean exceptionIntervention,
            String exceptionReason,
            Boolean secondaryConfirmed,
            ManagementSessionClaims session,
            String selfApprovalForbiddenMessageKey,
            String exceptionNotAllowedMessageKey,
            String reasonRequiredMessageKey,
            String secondaryConfirmRequiredMessageKey
    ) {
    }

    /**
     * Outcome of a successful enforcement. When {@code selfApprovalException} is
     * {@code true}, callers must persist both flags on the lifecycle audit row.
     */
    public record EnforceOutcome(boolean selfApprovalException, String exceptionReason) {
    }
}
