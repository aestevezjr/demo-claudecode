package com.facturas.api.controller;

import com.facturas.api.dto.AuthRequestDTO;
import com.facturas.api.dto.UserInfoDTO;
import com.facturas.api.service.impl.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    /** Name of the HttpOnly cookie that carries the JWT. */
    private static final String COOKIE_NAME = "jwt";

    /** Cookie lifetime: 1 day (must match jwt.expiration millis / 1000). */
    private static final long MAX_AGE_SECONDS = 86400L;

    /**
     * Set to true in production (HTTPS). Controlled via application.properties
     * so it can differ between dev and prod without code changes.
     */
    @Value("${jwt.cookie.secure:false}")
    private boolean cookieSecure;

    private final AuthService authService;

    /**
     * POST /auth/register
     * Creates a new user account. Returns 201 with no body.
     * The client must call /auth/login separately to obtain a session cookie.
     */
    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody AuthRequestDTO request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * POST /auth/login
     * Validates credentials and sets an HttpOnly JWT cookie on success.
     * Returns 200 OK with no body — the JWT is never exposed to JavaScript.
     */
    @PostMapping("/login")
    public ResponseEntity<Void> login(@Valid @RequestBody AuthRequestDTO request,
                                      HttpServletResponse response) {
        String token = authService.login(request);
        response.addHeader(HttpHeaders.SET_COOKIE, buildJwtCookie(token).toString());
        return ResponseEntity.ok().build();
    }

    /**
     * GET /auth/me
     * Returns the username of the currently authenticated user.
     * Requires a valid JWT cookie. Returns 401 if the cookie is missing or expired
     * (enforced by SecurityConfig — this endpoint is NOT in the permit-all list).
     */
    @GetMapping("/me")
    public ResponseEntity<UserInfoDTO> me(Authentication authentication) {
        return ResponseEntity.ok(new UserInfoDTO(authentication.getName()));
    }

    /**
     * POST /auth/logout
     * Clears the JWT cookie by setting Max-Age=0 and an empty value.
     * Permitted without authentication so users with an expired token can still log out.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, clearJwtCookie().toString());
        return ResponseEntity.ok().build();
    }

    // ── cookie builders ──────────────────────────────────────────────────────

    private ResponseCookie buildJwtCookie(String token) {
        return ResponseCookie.from(COOKIE_NAME, token)
                .httpOnly(true)            // Not accessible via JavaScript — prevents XSS theft
                .secure(cookieSecure)      // true in production (HTTPS); false in dev (HTTP)
                .path("/")                 // Sent on all requests to this server
                .maxAge(MAX_AGE_SECONDS)   // 1 day
                .sameSite("Strict")        // CSRF protection: cookie not sent on cross-site requests
                .build();
    }

    private ResponseCookie clearJwtCookie() {
        return ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(0)     // Expire immediately — browser deletes the cookie
                .sameSite("Strict")
                .build();
    }
}
