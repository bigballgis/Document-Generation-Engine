package com.bank.docgen.authorization.management.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.session.SessionRevocationStore;
import com.bank.docgen.authorization.management.session.SessionRevocationUnavailableException;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.sharedkernel.security.JwtTokenService;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import jakarta.servlet.FilterChain;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterRevocationTest {

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private SessionRevocationStore sessionRevocationStore;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtTokenService, sessionRevocationStore, new TraceIdProvider());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void revokedTokenIsRejectedWithDistinguishableFailure() throws Exception {
        when(jwtTokenService.parseManagementToken("revoked-token")).thenReturn(claims("jti-R1"));
        when(sessionRevocationStore.isRevoked("jti-R1")).thenReturn(true);

        MockHttpServletRequest request = bearerRequest("revoked-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(request.getAttribute(JwtAuthenticationFilter.SESSION_VALIDATION_FAILURE_ATTRIBUTE))
                .isEqualTo(ApiErrorCodes.SESSION_REVOKED);
        verify(chain).doFilter(request, response);
    }

    @Test
    void revocationStoreOutageFailsClosed() throws Exception {
        when(jwtTokenService.parseManagementToken("valid-token")).thenReturn(claims("jti-R2"));
        when(sessionRevocationStore.isRevoked("jti-R2"))
                .thenThrow(new SessionRevocationUnavailableException(new RuntimeException("redis down")));

        MockHttpServletRequest request = bearerRequest("valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(request.getAttribute(JwtAuthenticationFilter.SESSION_VALIDATION_FAILURE_ATTRIBUTE))
                .isEqualTo(ApiErrorCodes.SESSION_VALIDATION_UNAVAILABLE);
        verify(chain).doFilter(request, response);
    }

    @Test
    void nonRevokedTokenInstallsAuthentication() throws Exception {
        when(jwtTokenService.parseManagementToken("valid-token")).thenReturn(claims("jti-R3"));
        when(sessionRevocationStore.isRevoked("jti-R3")).thenReturn(false);

        MockHttpServletRequest request = bearerRequest("valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isInstanceOf(ManagementAuthentication.class);
        assertThat(request.getAttribute(JwtAuthenticationFilter.SESSION_VALIDATION_FAILURE_ATTRIBUTE)).isNull();
        verify(chain).doFilter(request, response);
    }

    private MockHttpServletRequest bearerRequest(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/management/v1/templates");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        return request;
    }

    private ManagementSessionClaims claims(String jti) {
        return new ManagementSessionClaims(
                "10000001",
                "Admin",
                "admin@example.com",
                AuthSource.LOCAL,
                List.of("GLOBAL_ADMIN"),
                List.of("*"),
                "route.dashboard-home",
                List.of("route.dashboard-home"),
                jti,
                Instant.now().minusSeconds(600),
                Instant.now().plusSeconds(1200)
        );
    }
}
