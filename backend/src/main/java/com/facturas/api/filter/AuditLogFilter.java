package com.facturas.api.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Logs write operations (POST, PUT, PATCH, DELETE) for audit purposes.
 * Request bodies are intentionally NOT logged to avoid persisting PII
 * (client emails, addresses, personal data).
 */
@Component
@Order(3)
@Slf4j
public class AuditLogFilter extends OncePerRequestFilter {

    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws IOException, ServletException {

        if (!WRITE_METHODS.contains(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        long start = System.currentTimeMillis();
        try {
            chain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - start;
            log.info("AUDIT method={} uri={} status={} ip={} duration={}ms",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    request.getRemoteAddr(),
                    duration);
        }
    }
}
