package com.bank.docgen.authorization.management.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.domain.ManagementRole;
import com.bank.docgen.authorization.management.persistence.ManagementUserEntity;
import com.bank.docgen.authorization.management.persistence.ManagementUserRepository;
import com.bank.docgen.authorization.management.session.SessionProperties;
import com.bank.docgen.authorization.management.session.SessionRevocationStore;
import com.bank.docgen.authorization.management.session.SessionRevocationUnavailableException;
import com.bank.docgen.sharedkernel.security.JwtTokenService;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.sharedkernel.security.PasswordHashService;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ManagementAuthSessionRenewalTest {

    private static final Duration ABSOLUTE_TTL = Duration.ofHours(8);

    @Mock
    private ManagementUserRepository managementUserRepository;
    @Mock
    private PasswordHashService passwordHashService;
    @Mock
    private JwtTokenService jwtTokenService;
    @Mock
    private SecurityAuditSummaryService securityAuditSummaryService;
    @Mock
    private SessionRevocationStore sessionRevocationStore;

    private ManagementAuthService service;

    @BeforeEach
    void setUp() {
        GroupAccessService groupAccessService = new GroupAccessService();
        service = new ManagementAuthService(
                managementUserRepository,
                passwordHashService,
                jwtTokenService,
                new RouteVisibilityService(),
                new ManagementCapabilitiesService(groupAccessService),
                securityAuditSummaryService,
                sessionRevocationStore,
                new SessionProperties("PT8H", "memory")
        );
    }

    @Test
    void renewIssuesNewJtiInheritsSessionStartAndRevokesOldToken() {
        Instant sessionStartedAt = Instant.now().minus(2, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS);
        Instant oldExpiry = Instant.now().plus(4, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.SECONDS);
        Instant slidingExpiry = Instant.now().plus(30, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.SECONDS);
        ManagementSessionClaims current = claims("jti-OLD", sessionStartedAt, oldExpiry);
        ManagementUserEntity user = user("10000001", Set.of(ManagementRole.GLOBAL_ADMIN));
        when(managementUserRepository.findByUsernameAndDeletedAtIsNull("10000001"))
                .thenReturn(Optional.of(user));
        when(jwtTokenService.accessTokenExpiresAt()).thenReturn(slidingExpiry);
        when(jwtTokenService.createManagementToken(any())).thenReturn("renewed-token");

        ManagementAuthService.LoginSession renewed = service.renew(current, "AUD-R1", "trace-r1");

        ArgumentCaptor<ManagementSessionClaims> issued = ArgumentCaptor.forClass(ManagementSessionClaims.class);
        verify(jwtTokenService).createManagementToken(issued.capture());
        assertThat(issued.getValue().jti()).isNotBlank().isNotEqualTo("jti-OLD");
        assertThat(issued.getValue().sessionStartedAt()).isEqualTo(sessionStartedAt);
        assertThat(issued.getValue().expiresAt()).isEqualTo(slidingExpiry);
        verify(sessionRevocationStore).revoke("jti-OLD", oldExpiry);
        verify(securityAuditSummaryService).recordSessionRenewal("10000001", "AUD-R1", "trace-r1");
        assertThat(renewed.accessToken()).isEqualTo("renewed-token");
        assertThat(renewed.accessTokenExpiresAt()).isEqualTo(slidingExpiry);
        assertThat(renewed.sessionAbsoluteDeadline()).isEqualTo(sessionStartedAt.plus(ABSOLUTE_TTL));
        assertThat(renewed.session().expiresAt()).isEqualTo(slidingExpiry);
        assertThat(renewed.session().absoluteSessionExpiresAt())
                .isEqualTo(sessionStartedAt.plus(ABSOLUTE_TTL));
    }

    @Test
    void renewClampsExpiryToAbsoluteDeadlineNearTheLimit() {
        Instant sessionStartedAt = Instant.now().minus(ABSOLUTE_TTL.minus(Duration.ofMinutes(4)))
                .truncatedTo(ChronoUnit.SECONDS);
        Instant absoluteDeadline = sessionStartedAt.plus(ABSOLUTE_TTL);
        ManagementSessionClaims current =
                claims("jti-OLD", sessionStartedAt, Instant.now().plus(10, ChronoUnit.MINUTES));
        when(managementUserRepository.findByUsernameAndDeletedAtIsNull("10000001"))
                .thenReturn(Optional.of(user("10000001", Set.of(ManagementRole.GLOBAL_ADMIN))));
        when(jwtTokenService.accessTokenExpiresAt())
                .thenReturn(Instant.now().plus(30, ChronoUnit.MINUTES));
        when(jwtTokenService.createManagementToken(any())).thenReturn("renewed-token");

        ManagementAuthService.LoginSession renewed = service.renew(current, "AUD-R2", "trace-r2");

        ArgumentCaptor<ManagementSessionClaims> issued = ArgumentCaptor.forClass(ManagementSessionClaims.class);
        verify(jwtTokenService).createManagementToken(issued.capture());
        assertThat(issued.getValue().expiresAt()).isEqualTo(absoluteDeadline);
        assertThat(renewed.accessTokenExpiresAt()).isEqualTo(absoluteDeadline);
        assertThat(renewed.sessionAbsoluteDeadline()).isEqualTo(absoluteDeadline);
        assertThat(renewed.session().absoluteSessionExpiresAt()).isEqualTo(absoluteDeadline);
    }

    @Test
    void renewBeyondAbsoluteLimitIsRejectedWithoutTouchingStore() {
        Instant sessionStartedAt = Instant.now().minus(ABSOLUTE_TTL).minusSeconds(60);
        ManagementSessionClaims current =
                claims("jti-OLD", sessionStartedAt, Instant.now().plus(10, ChronoUnit.MINUTES));

        assertThatThrownBy(() -> service.renew(current, "AUD-R3", "trace-r3"))
                .isInstanceOf(SessionAbsoluteLimitReachedException.class);

        verifyNoInteractions(sessionRevocationStore);
        verify(jwtTokenService, never()).createManagementToken(any());
        verify(securityAuditSummaryService)
                .recordSessionRenewalDenied(eq("10000001"), anyString(), eq("AUD-R3"), eq("trace-r3"));
    }

    @Test
    void renewForDisabledAccountIsRejected() {
        ManagementUserEntity user = user("10000001", Set.of(ManagementRole.GLOBAL_ADMIN));
        user.disable();
        when(managementUserRepository.findByUsernameAndDeletedAtIsNull("10000001"))
                .thenReturn(Optional.of(user));
        ManagementSessionClaims current = claims(
                "jti-OLD",
                Instant.now().minus(1, ChronoUnit.HOURS),
                Instant.now().plus(10, ChronoUnit.MINUTES));

        assertThatThrownBy(() -> service.renew(current, "AUD-R4", "trace-r4"))
                .isInstanceOf(SessionExpiredException.class);

        verifyNoInteractions(sessionRevocationStore);
        verify(securityAuditSummaryService)
                .recordSessionRenewalDenied(eq("10000001"), anyString(), eq("AUD-R4"), eq("trace-r4"));
    }

    @Test
    void renewForDeletedAccountIsRejected() {
        when(managementUserRepository.findByUsernameAndDeletedAtIsNull("10000001"))
                .thenReturn(Optional.empty());
        ManagementSessionClaims current = claims(
                "jti-OLD",
                Instant.now().minus(1, ChronoUnit.HOURS),
                Instant.now().plus(10, ChronoUnit.MINUTES));

        assertThatThrownBy(() -> service.renew(current, "AUD-R5", "trace-r5"))
                .isInstanceOf(SessionExpiredException.class);

        verifyNoInteractions(sessionRevocationStore);
    }

    @Test
    void logoutRevokesTokenJtiThenRecordsAudit() {
        Instant expiresAt = Instant.now().plus(20, ChronoUnit.MINUTES);
        ManagementSessionClaims current =
                claims("jti-L1", Instant.now().minus(10, ChronoUnit.MINUTES), expiresAt);

        service.logout(current, "AUD-L1", "trace-l1");

        verify(sessionRevocationStore).revoke("jti-L1", expiresAt);
        verify(securityAuditSummaryService).recordLogout("10000001", "AUD-L1", "trace-l1");
    }

    @Test
    void logoutPropagatesStoreUnavailability() {
        ManagementSessionClaims current = claims(
                "jti-L2",
                Instant.now().minus(10, ChronoUnit.MINUTES),
                Instant.now().plus(20, ChronoUnit.MINUTES));
        doThrow(new SessionRevocationUnavailableException(new RuntimeException("redis down")))
                .when(sessionRevocationStore).revoke(eq("jti-L2"), any());

        assertThatThrownBy(() -> service.logout(current, "AUD-L2", "trace-l2"))
                .isInstanceOf(SessionRevocationUnavailableException.class);

        verify(securityAuditSummaryService, never()).recordLogout(anyString(), anyString(), anyString());
    }

    @Test
    void loginStartsSessionNowWithFreshJtiAndDeadlineFields() {
        ManagementUserEntity user = user("10000001", Set.of(ManagementRole.GLOBAL_ADMIN));
        Instant slidingExpiry = Instant.now().plus(30, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.SECONDS);
        when(managementUserRepository.findByUsernameAndDeletedAtIsNull("10000001"))
                .thenReturn(Optional.of(user));
        when(passwordHashService.matches("ChangeMe123!", user.getPasswordHash())).thenReturn(true);
        when(jwtTokenService.accessTokenExpiresAt()).thenReturn(slidingExpiry);
        when(jwtTokenService.createManagementToken(any())).thenReturn("login-token");

        ManagementAuthService.LoginSession session =
                service.authenticate("10000001", "ChangeMe123!", "AUD-A1", "trace-a1");

        ArgumentCaptor<ManagementSessionClaims> issued = ArgumentCaptor.forClass(ManagementSessionClaims.class);
        verify(jwtTokenService).createManagementToken(issued.capture());
        assertThat(issued.getValue().jti()).isNotBlank();
        assertThat(issued.getValue().sessionStartedAt())
                .isBetween(Instant.now().minusSeconds(5), Instant.now().plusSeconds(1));
        assertThat(session.accessTokenExpiresAt()).isEqualTo(slidingExpiry);
        assertThat(session.sessionAbsoluteDeadline())
                .isEqualTo(issued.getValue().sessionStartedAt().plus(ABSOLUTE_TTL));
        assertThat(session.session().absoluteSessionExpiresAt())
                .isEqualTo(issued.getValue().sessionStartedAt().plus(ABSOLUTE_TTL));
    }

    private ManagementSessionClaims claims(String jti, Instant sessionStartedAt, Instant expiresAt) {
        return new ManagementSessionClaims(
                "10000001",
                "Global Admin",
                "global.admin@example.com",
                AuthSource.LOCAL,
                List.of("GLOBAL_ADMIN"),
                List.of("*"),
                "route.dashboard-home",
                List.of("route.dashboard-home"),
                jti,
                sessionStartedAt,
                expiresAt
        );
    }

    private ManagementUserEntity user(String username, Set<ManagementRole> roles) {
        return new ManagementUserEntity(
                UUID.randomUUID(),
                username,
                "Display",
                username + "@bank.test",
                "hash",
                AuthSource.LOCAL,
                roles,
                Set.of()
        );
    }
}
