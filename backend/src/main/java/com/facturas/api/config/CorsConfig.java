package com.facturas.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    /*
     * allowCredentials(true) is required for the browser to send the HttpOnly JWT
     * cookie and the XSRF-TOKEN cookie on cross-origin requests (frontend on :4200,
     * backend on :8082).  When allowCredentials is true, allowedOrigins must be an
     * explicit list — wildcard "*" is rejected by the browser.
     *
     * Configured via app.cors.allowed-origins in application.properties.
     */
    @Value("${app.cors.allowed-origins}")
    private String[] allowedOrigins;

    private static final List<String> ALLOWED_HEADERS = List.of(
            "Content-Type", "Authorization", "X-Requested-With", "Accept", "Origin",
            // Angular's XSRF interceptor sends this header with every non-safe request
            "X-XSRF-TOKEN"
    );

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders(ALLOWED_HEADERS.toArray(String[]::new))
                .allowCredentials(true)   // Enables cookie transmission for /api/** requests
                .maxAge(3600);

        registry.addMapping("/auth/**")
                .allowedOrigins(allowedOrigins)
                // GET added for /auth/me; POST for login/register/logout
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders(ALLOWED_HEADERS.toArray(String[]::new))
                .allowCredentials(true)   // Enables cookie transmission for /auth/** requests
                .maxAge(3600);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigins));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(ALLOWED_HEADERS);
        config.setAllowCredentials(true);  // Must be true when allowedOrigins is explicit
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        source.registerCorsConfiguration("/auth/**", config);
        return source;
    }
}
