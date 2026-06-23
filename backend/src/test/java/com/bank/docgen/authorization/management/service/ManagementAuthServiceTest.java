package com.bank.docgen.authorization.management.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.domain.ManagementRole;
import com.bank.docgen.authorization.management.persistence.ManagementUserEntity;
import com.bank.docgen.authorization.management.persistence.ManagementUserRepository;
import com.bank.docgen.sharedkernel.security.JwtTokenService;
import com.bank.docgen.sharedkernel.security.PasswordHashService;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ManagementAuthServiceTest {

    @Mock
    private ManagementUserRepository managementUserRepository;
    @Mock
    private PasswordHashService passwordHashService;
    @Mock
    private JwtTokenService jwtTokenService;
    @Mock
    private SecurityAuditSummaryService securityAuditSummaryService;

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
                securityAuditSummaryService
        );
    }

    @Test
    void authenticateReturnsSessionForValidCredentials() {
        ManagementUserEntity user = user("10000001", Set.of(ManagementRole.GLOBAL_ADMIN), Set.of());
        when(managementUserRepository.findByUsernameAndDeletedAtIsNull("10000001"))
                .thenReturn(Optional.of(user));
        when(passwordHashService.matches("ChangeMe123!", user.getPasswordHash())).thenReturn(true);
        when(jwtTokenService.accessTokenExpiresAt()).thenReturn(Instant.parse("2030-01-01T00:00:00Z"));
        when(jwtTokenService.createManagementToken(any())).thenReturn("jwt-token");

        ManagementAuthService.LoginSession session =
                service.authenticate("10000001", "ChangeMe123!", "AUD-1", "trace-1");

        assertThat(session.accessToken()).isEqualTo("jwt-token");
        assertThat(session.session().username()).isEqualTo("10000001");
        assertThat(session.session().roles()).contains("GLOBAL_ADMIN");
        verify(securityAuditSummaryService).recordLoginSuccess("10000001", "AUD-1", "trace-1");
    }

    @Test
    void authenticateFailsClosedForWrongPassword() {
        ManagementUserEntity user = user("10000001", Set.of(ManagementRole.GLOBAL_ADMIN), Set.of());
        when(managementUserRepository.findByUsernameAndDeletedAtIsNull("10000001"))
                .thenReturn(Optional.of(user));
        when(passwordHashService.matches("wrong", user.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> service.authenticate("10000001", "wrong", "AUD-2", "trace-2"))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(securityAuditSummaryService).recordLoginFailure(eq("10000001"), eq("AUD-2"), eq("trace-2"));
    }

    @Test
    void authenticateFailsClosedForDisabledUser() {
        ManagementUserEntity user = user("10000001", Set.of(ManagementRole.GLOBAL_ADMIN), Set.of());
        user.disable();
        when(managementUserRepository.findByUsernameAndDeletedAtIsNull("10000001"))
                .thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.authenticate("10000001", "ChangeMe123!", "AUD-3", "trace-3"))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(securityAuditSummaryService).recordLoginFailure(eq("10000001"), eq("AUD-3"), eq("trace-3"));
    }

    private ManagementUserEntity user(
            String username,
            Set<ManagementRole> roles,
            Set<String> groupCodes
    ) {
        return new ManagementUserEntity(
                UUID.randomUUID(),
                username,
                "Display",
                username + "@bank.test",
                "hash",
                AuthSource.LOCAL,
                roles,
                groupCodes
        );
    }
}
