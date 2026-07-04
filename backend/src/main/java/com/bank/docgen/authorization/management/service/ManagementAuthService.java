package com.bank.docgen.authorization.management.service;

import com.bank.docgen.authorization.management.api.ManagementSessionView;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.domain.ManagementRole;
import com.bank.docgen.authorization.management.persistence.ManagementUserEntity;
import com.bank.docgen.authorization.management.persistence.ManagementUserRepository;
import com.bank.docgen.authorization.management.session.SessionProperties;
import com.bank.docgen.authorization.management.session.SessionRevocationStore;
import com.bank.docgen.sharedkernel.security.JwtTokenService;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.sharedkernel.security.PasswordHashService;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ManagementAuthService {

    private static final String ALL_GROUPS_SCOPE = "*";
    private static final String RENEWAL_DENIED_ABSOLUTE_LIMIT = "ABSOLUTE_LIMIT_REACHED";
    private static final String RENEWAL_DENIED_ACCOUNT_NOT_ELIGIBLE = "ACCOUNT_NOT_ELIGIBLE";

    private final ManagementUserRepository managementUserRepository;
    private final PasswordHashService passwordHashService;
    private final JwtTokenService jwtTokenService;
    private final RouteVisibilityService routeVisibilityService;
    private final ManagementCapabilitiesService managementCapabilitiesService;
    private final SecurityAuditSummaryService securityAuditSummaryService;
    private final SessionRevocationStore sessionRevocationStore;
    private final SessionProperties sessionProperties;

    public ManagementAuthService(
            ManagementUserRepository managementUserRepository,
            PasswordHashService passwordHashService,
            JwtTokenService jwtTokenService,
            RouteVisibilityService routeVisibilityService,
            ManagementCapabilitiesService managementCapabilitiesService,
            SecurityAuditSummaryService securityAuditSummaryService,
            SessionRevocationStore sessionRevocationStore,
            SessionProperties sessionProperties
    ) {
        this.managementUserRepository = managementUserRepository;
        this.passwordHashService = passwordHashService;
        this.jwtTokenService = jwtTokenService;
        this.routeVisibilityService = routeVisibilityService;
        this.managementCapabilitiesService = managementCapabilitiesService;
        this.securityAuditSummaryService = securityAuditSummaryService;
        this.sessionRevocationStore = sessionRevocationStore;
        this.sessionProperties = sessionProperties;
    }

    @Transactional(readOnly = true)
    public LoginSession authenticate(String username, String password, String auditId, String traceId) {
        ManagementUserEntity user = managementUserRepository.findByUsernameAndDeletedAtIsNull(username)
                .filter(ManagementUserEntity::isEnabled)
                .filter(found -> passwordHashService.matches(password, found.getPasswordHash()))
                .orElseThrow(() -> {
                    securityAuditSummaryService.recordLoginFailure(username, auditId, traceId);
                    return new InvalidCredentialsException();
                });

        // Second precision: the claim travels as epoch seconds, and the absolute deadline
        // (sessionStartedAt + absolute TTL) must stay byte-identical across the renewal chain.
        Instant sessionStartedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        LoginSession loginSession = issueSession(user, UUID.randomUUID().toString(), sessionStartedAt);
        securityAuditSummaryService.recordLoginSuccess(user.getUsername(), auditId, traceId);
        return loginSession;
    }

    /**
     * Sliding renewal (LR-B6): re-derives the account's authorization state from the database,
     * issues a fresh token (new {@code jti}, inherited {@code sessionStartedAt}, expiry clamped
     * to the absolute deadline) and revokes the presented token's {@code jti}.
     */
    @Transactional(readOnly = true)
    public LoginSession renew(ManagementSessionClaims current, String auditId, String traceId) {
        Instant sessionStartedAt = current.sessionStartedAt();
        Instant absoluteDeadline = sessionStartedAt.plus(absoluteTtl());
        if (!Instant.now().isBefore(absoluteDeadline)) {
            securityAuditSummaryService.recordSessionRenewalDenied(
                    current.username(), RENEWAL_DENIED_ABSOLUTE_LIMIT, auditId, traceId);
            throw new SessionAbsoluteLimitReachedException();
        }
        ManagementUserEntity user = managementUserRepository
                .findByUsernameAndDeletedAtIsNull(current.username())
                .filter(ManagementUserEntity::isEnabled)
                .orElseThrow(() -> {
                    securityAuditSummaryService.recordSessionRenewalDenied(
                            current.username(), RENEWAL_DENIED_ACCOUNT_NOT_ELIGIBLE, auditId, traceId);
                    return new SessionExpiredException();
                });

        LoginSession renewed = issueSession(user, UUID.randomUUID().toString(), sessionStartedAt);
        sessionRevocationStore.revoke(current.jti(), current.expiresAt());
        securityAuditSummaryService.recordSessionRenewal(user.getUsername(), auditId, traceId);
        return renewed;
    }

    @Transactional(readOnly = true)
    public ManagementSessionView currentSession(ManagementSessionClaims claims) {
        ManagementUserEntity user = managementUserRepository.findByUsernameAndDeletedAtIsNull(claims.username())
                .filter(ManagementUserEntity::isEnabled)
                .orElseThrow(SessionExpiredException::new);
        return buildSessionView(user, claims.expiresAt(), claims.sessionStartedAt().plus(absoluteTtl()));
    }

    public void logout(ManagementSessionClaims claims, String auditId, String traceId) {
        sessionRevocationStore.revoke(claims.jti(), claims.expiresAt());
        securityAuditSummaryService.recordLogout(claims.username(), auditId, traceId);
    }

    private LoginSession issueSession(ManagementUserEntity user, String jti, Instant sessionStartedAt) {
        Instant absoluteDeadline = sessionStartedAt.plus(absoluteTtl());
        Instant expiresAt = earliest(jwtTokenService.accessTokenExpiresAt(), absoluteDeadline);
        ManagementSessionView session = buildSessionView(user, expiresAt, absoluteDeadline);
        String token = jwtTokenService.createManagementToken(toClaims(session, jti, sessionStartedAt));
        return new LoginSession(token, expiresAt, absoluteDeadline, session);
    }

    private Duration absoluteTtl() {
        return Duration.parse(sessionProperties.absoluteTtl());
    }

    private static Instant earliest(Instant left, Instant right) {
        return left.isBefore(right) ? left : right;
    }

    private ManagementSessionView buildSessionView(
            ManagementUserEntity user, Instant expiresAt, Instant absoluteSessionExpiresAt) {
        var roles = routeVisibilityService.normalizeRoles(user.getRoles());
        List<String> roleCodes = roles.stream().map(Enum::name).toList();
        List<String> groupCodes = resolveGroupCodes(user, roles);
        return new ManagementSessionView(
                user.getUsername(),
                user.getDisplayName(),
                user.getEmail(),
                user.getAuthSource().name(),
                roleCodes,
                groupCodes,
                routeVisibilityService.resolveDefaultRoute(roles),
                routeVisibilityService.resolveVisibleRoutes(roles),
                managementCapabilitiesService.resolve(roles),
                expiresAt,
                absoluteSessionExpiresAt
        );
    }

    private List<String> resolveGroupCodes(ManagementUserEntity user, java.util.Set<ManagementRole> roles) {
        if (roles.contains(ManagementRole.GLOBAL_ADMIN)) {
            return List.of(ALL_GROUPS_SCOPE);
        }
        return new ArrayList<>(user.getAuthorizedGroupCodes());
    }

    private ManagementSessionClaims toClaims(ManagementSessionView session, String jti, Instant sessionStartedAt) {
        return new ManagementSessionClaims(
                session.username(),
                session.displayName(),
                session.email(),
                AuthSource.valueOf(session.authSource()),
                session.roles(),
                session.authorizedGroupCodes(),
                session.defaultRoute(),
                session.visibleRoutes(),
                jti,
                sessionStartedAt,
                session.expiresAt()
        );
    }

    public record LoginSession(
            String accessToken,
            Instant accessTokenExpiresAt,
            Instant sessionAbsoluteDeadline,
            ManagementSessionView session
    ) {
    }
}
