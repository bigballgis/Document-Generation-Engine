package com.bank.docgen.authorization.management.web;

import com.bank.docgen.authorization.management.session.SessionRevocationStore;
import com.bank.docgen.authorization.management.session.SessionRevocationUnavailableException;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.sharedkernel.security.JwtTokenService;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /**
     * Request attribute carrying the session-validation failure code
     * ({@code SESSION_REVOKED} / {@code SESSION_VALIDATION_UNAVAILABLE}) so the authentication
     * entry point can answer with a distinguishable error envelope (LR-B6 spec 8.3/8.5).
     */
    public static final String SESSION_VALIDATION_FAILURE_ATTRIBUTE = "docgen.session.validationFailure";

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenService jwtTokenService;
    private final SessionRevocationStore sessionRevocationStore;
    private final TraceIdProvider traceIdProvider;

    public JwtAuthenticationFilter(
            JwtTokenService jwtTokenService,
            SessionRevocationStore sessionRevocationStore,
            TraceIdProvider traceIdProvider
    ) {
        this.jwtTokenService = jwtTokenService;
        this.sessionRevocationStore = sessionRevocationStore;
        this.traceIdProvider = traceIdProvider;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring("Bearer ".length()).trim();
            authenticate(request, token);
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request, String token) {
        ManagementSessionClaims session;
        try {
            session = jwtTokenService.parseManagementToken(token);
        } catch (JwtException ignored) {
            SecurityContextHolder.clearContext();
            return;
        }
        try {
            if (sessionRevocationStore.isRevoked(session.jti())) {
                SecurityContextHolder.clearContext();
                request.setAttribute(SESSION_VALIDATION_FAILURE_ATTRIBUTE, ApiErrorCodes.SESSION_REVOKED);
                return;
            }
        } catch (SessionRevocationUnavailableException ex) {
            // Fail-closed (LR-B6 P4): if the revocation list cannot be checked, the token is
            // rejected — the security context stays empty and protected endpoints answer 401.
            String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
            LOGGER.error("session.revocation.check.unavailable failing closed traceId=" + traceId, ex);
            SecurityContextHolder.clearContext();
            request.setAttribute(SESSION_VALIDATION_FAILURE_ATTRIBUTE, ApiErrorCodes.SESSION_VALIDATION_UNAVAILABLE);
            return;
        }
        SecurityContextHolder.getContext().setAuthentication(new ManagementAuthentication(session));
    }
}
