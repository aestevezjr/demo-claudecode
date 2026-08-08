package com.facturas.api.config;

import com.facturas.api.filter.JwtAuthFilter;
import com.facturas.api.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Backs the per-request "is this SecurityContext already established?" check that
     * SessionManagementFilter relies on (see JwtAuthFilter for why this matters here).
     */
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new RequestAttributeSecurityContextRepository();
    }

    @Bean
    public JwtAuthFilter jwtAuthFilter(JwtService jwtService, UserDetailsService userDetailsService,
                                        SecurityContextRepository securityContextRepository) {
        return new JwtAuthFilter(jwtService, userDetailsService, securityContextRepository);
    }

    @Bean
    public AuthenticationEntryPoint unauthorizedEntryPoint() {
        return new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                    JwtAuthFilter jwtAuthFilter,
                                                    SecurityContextRepository securityContextRepository) throws Exception {
        http
            /*
             * CSRF: enabled with CookieCsrfTokenRepository so Angular can read the
             * XSRF-TOKEN cookie (HttpOnly=false) and send it back as X-XSRF-TOKEN header.
             *
             * CsrfTokenRequestAttributeHandler (not the default XorCsrfTokenRequestAttributeHandler)
             * is used deliberately: XOR masking is designed for server-rendered HTML forms where
             * the server injects a masked token into each page.  For SPAs, the XSRF-TOKEN cookie
             * is read directly by JavaScript and sent as-is in the X-XSRF-TOKEN header, so raw
             * (unmasked) comparison is required.  See Spring Security 6 migration guide § CSRF.
             *
             * /auth/** is exempt: login/register have no CSRF cookie yet, and logout is safe
             * to exempt (logging-out CSRF is a low-risk attack).
             */
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                .ignoringRequestMatchers("/auth/**")
            )
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            /*
             * Explicit SecurityContextRepository (request-attribute-backed, not session-backed):
             * required so SessionManagementFilter's containsContext(request) check (see
             * JwtAuthFilter) has something to find. Without this bean, STATELESS mode falls
             * back to NullSecurityContextRepository, whose containsContext() always returns
             * false — which made SessionManagementFilter treat every authenticated request as
             * a brand-new login and invoke CsrfAuthenticationStrategy to rotate (delete) the
             * XSRF-TOKEN cookie on every other request, causing intermittent 401s on
             * POST/PUT/PATCH/DELETE calls from the Angular app.
             */
            .securityContext(context -> context.securityContextRepository(securityContextRepository))
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .cors(Customizer.withDefaults())
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(unauthorizedEntryPoint()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Public auth endpoints — /auth/me is intentionally omitted so it requires auth
                .requestMatchers("/auth/login", "/auth/register", "/auth/logout").permitAll()
                .anyRequest().authenticated())
            /*
             * CsrfCookieFilter forces the deferred CSRF token to resolve on every
             * request so the XSRF-TOKEN cookie is written in the response.
             * Without this, Spring Security 6's lazy token never sets the cookie on
             * safe-method (GET) responses, and Angular's authInterceptor has no cookie
             * to read for subsequent PUT/POST/DELETE calls.
             */
            .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private static final class CsrfCookieFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                FilterChain filterChain) throws IOException, ServletException {
            CsrfToken csrf = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (csrf != null) {
                csrf.getToken(); // resolves the deferred token → triggers saveToken() → sets the cookie
            }
            filterChain.doFilter(request, response);
        }
    }
}
