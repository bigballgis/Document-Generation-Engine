package com.bank.docgen.infrastructure.web;

import com.bank.docgen.sharedkernel.api.TraceIdConstants;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdMdcFilter extends OncePerRequestFilter {

    private final TraceIdProvider traceIdProvider;

    public TraceIdMdcFilter(TraceIdProvider traceIdProvider) {
        this.traceIdProvider = traceIdProvider;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String traceId = traceIdProvider.currentOrNew(request.getHeader(TraceIdConstants.HEADER_NAME));
        MDC.put(TraceIdConstants.MDC_KEY, traceId);
        response.setHeader(TraceIdConstants.HEADER_NAME, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TraceIdConstants.MDC_KEY);
        }
    }
}
