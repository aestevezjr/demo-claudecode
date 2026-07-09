package com.facturas.api.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Adds security-related HTTP response headers to every response.
 * Runs first (@Order(1)) so headers are set before any downstream filter
 * or controller can short-circuit the chain.
 */
@Component
@Order(1)
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws IOException, ServletException {

        // Prevent MIME-type sniffing
        response.setHeader("X-Content-Type-Options", "nosniff");

        // Disallow framing entirely (clickjacking protection)
        response.setHeader("X-Frame-Options", "DENY");

        // Do not send the Referer header to third parties
        response.setHeader("Referrer-Policy", "no-referrer");

        // XSS-Protection: 0 is the modern correct value.
        // Setting it to 1 (or 1;mode=block) is deprecated and can
        // actually introduce vulnerabilities in older browsers.
        response.setHeader("X-XSS-Protection", "0");

        // Restrict powerful browser features
        response.setHeader("Permissions-Policy", "geolocation=(), microphone=(), camera=()");

        // Prevent caching of potentially sensitive API responses
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.setHeader("Pragma", "no-cache");

        // Enforce HTTPS on all future requests
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");

        // Restrict resource loading to same origin
        response.setHeader("Content-Security-Policy", "default-src 'self'");

        chain.doFilter(request, response);
    }
}
