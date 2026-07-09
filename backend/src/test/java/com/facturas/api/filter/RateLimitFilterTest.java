package com.facturas.api.filter;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.MethodMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(
    locations = "classpath:application-test.properties",
    properties = "app.rate-limit.max-requests=3"
)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@WithMockUser
class RateLimitFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @Order(1)
    void firstRequest_withinLimit_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/invoices"))
                .andExpect(status().isOk());
    }

    @Test
    @Order(2)
    @DirtiesContext(methodMode = MethodMode.BEFORE_METHOD)
    void nonApiPath_withExhaustedApiBucket_notRateLimited() throws Exception {
        // Exhaust the API bucket (limit=3)
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/api/v1/invoices"));
        }
        // /auth paths are not subject to the rate limiter — expect 401 (bad creds), not 429
        mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"nouser\",\"password\":\"password123\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(3)
    @DirtiesContext(methodMode = MethodMode.BEFORE_METHOD)
    void afterExhaustingBucket_returns429WithRetryAfterHeader() throws Exception {
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/api/v1/invoices"))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/v1/invoices"))
                .andExpect(status().is(429))
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.error").value("Too Many Requests"));
    }

    @Test
    @Order(4)
    @DirtiesContext(methodMode = MethodMode.BEFORE_METHOD)
    void rateLimitResponse_hasRetryAfterBody() throws Exception {
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/api/v1/invoices"));
        }

        mockMvc.perform(get("/api/v1/invoices"))
                .andExpect(status().is(429))
                .andExpect(jsonPath("$.retryAfter").isNumber())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }
}
