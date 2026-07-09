package com.facturas.api.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate-limiting filter: configurable requests/minute per client IP.
 * - /api/**   : app.rate-limit.max-requests (default 60/min)
 * - /auth/login, /auth/register: app.rate-limit.auth-max-requests (default 10/min)
 *
 * Security note on X-Forwarded-For: intentionally ignored — no trusted reverse proxy
 * in this demo. Accepting that header would let clients spoof their IP.
 */
@Component
@Order(2)
public class RateLimitFilter extends OncePerRequestFilter {

    @Value("${app.rate-limit.max-requests:60}")
    private int maxRequestsPerMinute;

    @Value("${app.rate-limit.auth-max-requests:10}")
    private int authMaxRequestsPerMinute;

    private static final int RETRY_AFTER_SECONDS = 60;

    private final ConcurrentHashMap<String, Bucket> apiBuckets  = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Bucket> authBuckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws IOException, ServletException {

        String path = request.getRequestURI();
        String clientIp = request.getRemoteAddr();

        // Brute-force protection on auth endpoints
        if (path.startsWith("/auth/login") || path.startsWith("/auth/register")) {
            Bucket bucket = authBuckets.computeIfAbsent(clientIp, ip -> newBucket(authMaxRequestsPerMinute));
            if (bucket.tryConsume(1)) {
                chain.doFilter(request, response);
            } else {
                rejectWithTooManyRequests(response);
            }
            return;
        }

        // General API rate limiting
        if (path.startsWith("/api/")) {
            Bucket bucket = apiBuckets.computeIfAbsent(clientIp, ip -> newBucket(maxRequestsPerMinute));
            if (bucket.tryConsume(1)) {
                chain.doFilter(request, response);
            } else {
                rejectWithTooManyRequests(response);
            }
            return;
        }

        chain.doFilter(request, response);
    }

    private Bucket newBucket(int maxRequests) {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(maxRequests, Refill.greedy(maxRequests, Duration.ofMinutes(1))))
                .build();
    }

    private void rejectWithTooManyRequests(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Retry-After", String.valueOf(RETRY_AFTER_SECONDS));
        response.getWriter().write(objectMapper.writeValueAsString(Map.of(
                "error", "Too Many Requests",
                "message", "Has superado el límite de peticiones. Intenta de nuevo en " + RETRY_AFTER_SECONDS + " segundos.",
                "retryAfter", RETRY_AFTER_SECONDS
        )));
    }
}
