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
 * Client IP resolution: by default uses request.getRemoteAddr(). When this app is
 * deployed behind a reverse proxy / load balancer, the socket peer is the proxy for
 * EVERY client, which would collapse all users into a single shared bucket and
 * effectively disable the limit. Set app.rate-limit.trust-proxy-headers=true ONLY if
 * the app sits behind a proxy you control that always overwrites (never merely
 * appends to) X-Forwarded-For — otherwise a client can spoof that header and bypass
 * the limit entirely.
 */
@Component
@Order(2)
public class RateLimitFilter extends OncePerRequestFilter {

    @Value("${app.rate-limit.max-requests:60}")
    private int maxRequestsPerMinute;

    @Value("${app.rate-limit.auth-max-requests:10}")
    private int authMaxRequestsPerMinute;

    @Value("${app.rate-limit.trust-proxy-headers:false}")
    private boolean trustProxyHeaders;

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
        String clientIp = resolveClientIp(request);

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

    /**
     * X-Forwarded-For may contain a client-supplied chain of IPs (client, proxy1, proxy2...);
     * the first entry is the original client as seen by the nearest trusted proxy.
     */
    private String resolveClientIp(HttpServletRequest request) {
        if (trustProxyHeaders) {
            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isBlank()) {
                return forwardedFor.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
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
