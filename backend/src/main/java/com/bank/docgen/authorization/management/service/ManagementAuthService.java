package com.bank.docgen.authorization.management.service;

import com.bank.docgen.authorization.management.api.ManagementSessionView;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.domain.ManagementRole;
import com.bank.docgen.authorization.management.persistence.ManagementUserEntity;
import com.bank.docgen.authorization.management.persistence.ManagementUserRepository;
import com.bank.docgen.sharedkernel.security.JwtTokenService;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.sharedkernel.security.PasswordHashService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ManagementAuthService {

    private static final String ALL_GROUPS_SCOPE = "*";

    private final ManagementUserRepository managementUserRepository;
    private final PasswordHashService passwordHashService;
    private final JwtTokenService jwtTokenService;
    private final RouteVisibilityService routeVisibilityService;
    private final ManagementCapabilitiesService managementCapabilitiesService;
    private final SecurityAuditSummaryService securityAuditSummaryService;

    public ManagementAuthService(
            ManagementUserRepository managementUserRepository,
            PasswordHashService passwordHashService,
            JwtTokenService jwtTokenService,
            RouteVisibilityService routeVisibilityService,
            ManagementCapabilitiesService managementCapabilitiesService,
            SecurityAuditSummaryService securityAuditSummaryService
    ) {
        this.managementUserRepository = managementUserRepository;
        this.passwordHashService = passwordHashService;
        this.jwtTokenService = jwtTokenService;
        this.routeVisibilityService = routeVisibilityService;
        this.managementCapabilitiesService = managementCapabilitiesService;
        this.securityAuditSummaryService = securityAuditSummaryService;
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

        ManagementSessionView session = buildSessionView(user);
        String token = jwtTokenService.createManagementToken(toClaims(session));
        securityAuditSummaryService.recordLoginSuccess(user.getUsername(), auditId, traceId);
        return new LoginSession(token, session);
    }

    @Transactional(readOnly = true)
    public ManagementSessionView currentSession(ManagementSessionClaims claims) {
        ManagementUserEntity user = managementUserRepository.findByUsernameAndDeletedAtIsNull(claims.username())
                .filter(ManagementUserEntity::isEnabled)
                .orElseThrow(SessionExpiredException::new);
        return buildSessionView(user, claims.expiresAt());
    }

    public void logout(ManagementSessionClaims claims, String auditId, String traceId) {
        securityAuditSummaryService.recordLogout(claims.username(), auditId, traceId);
    }

    private ManagementSessionView buildSessionView(ManagementUserEntity user) {
        Instant expiresAt = jwtTokenService.accessTokenExpiresAt();
        return buildSessionView(user, expiresAt);
    }

    private ManagementSessionView buildSessionView(ManagementUserEntity user, Instant expiresAt) {
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
                expiresAt
        );
    }

    private List<String> resolveGroupCodes(ManagementUserEntity user, java.util.Set<ManagementRole> roles) {
        if (roles.contains(ManagementRole.GLOBAL_ADMIN)) {
            return List.of(ALL_GROUPS_SCOPE);
        }
        return new ArrayList<>(user.getAuthorizedGroupCodes());
    }

    private ManagementSessionClaims toClaims(ManagementSessionView session) {
        return new ManagementSessionClaims(
                session.username(),
                session.displayName(),
                session.email(),
                AuthSource.valueOf(session.authSource()),
                session.roles(),
                session.authorizedGroupCodes(),
                session.defaultRoute(),
                session.visibleRoutes(),
                session.expiresAt()
        );
    }

    public record LoginSession(String accessToken, ManagementSessionView session) {
    }
}
