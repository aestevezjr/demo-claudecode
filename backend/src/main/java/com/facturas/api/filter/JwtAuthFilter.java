package com.facturas.api.filter;

import com.facturas.api.entity.User;
import com.facturas.api.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Reads the JWT from the HttpOnly "jwt" cookie instead of the Authorization header.
 * Moving the token to an HttpOnly cookie prevents JavaScript (and therefore XSS) from
 * reading it. The browser attaches the cookie automatically on every same-site request.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    /** Name must match the cookie set in AuthController.buildJwtCookie(). */
    private static final String JWT_COOKIE_NAME = "jwt";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final SecurityContextRepository securityContextRepository;

    public JwtAuthFilter(JwtService jwtService, UserDetailsService userDetailsService,
                          SecurityContextRepository securityContextRepository) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.securityContextRepository = securityContextRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws IOException, ServletException {

        String token = extractJwtFromCookie(request);

        if (token != null
                && jwtService.isTokenValid(token)
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            String username = jwtService.extractUsername(token);
            try {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                Integer tokenVersion = jwtService.extractTokenVersion(token);
                int currentVersion = (userDetails instanceof User u) ? u.getTokenVersion() : -1;

                // tokenVersion mismatch (or missing "tv" claim) means the token was
                // revoked via logout, or issued before revocation existed — reject it.
                if (tokenVersion != null && tokenVersion == currentVersion) {
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    /*
                     * Explicitly record that this request's SecurityContext is already established.
                     * Without this, SessionManagementFilter (further down the chain) sees
                     * securityContextRepository.containsContext(request) == false on every single
                     * request — since STATELESS apps never persist anything across requests — and
                     * treats each request as a brand-new login, triggering CsrfAuthenticationStrategy
                     * to rotate (delete) the XSRF-TOKEN cookie. Saving here marks the context as
                     * already present for THIS request, so that check passes and the CSRF cookie
                     * is left alone on requests after the first one.
                     */
                    securityContextRepository.saveContext(SecurityContextHolder.getContext(), request, response);
                }
            } catch (UsernameNotFoundException ignored) {
                // User deleted after token was issued — leave SecurityContext empty → 401
            }
        }

        chain.doFilter(request, response);
    }

    /**
     * Extracts the raw JWT string from the HttpOnly "jwt" cookie.
     * Returns null if the cookie is absent.
     */
    private String extractJwtFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (JWT_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
