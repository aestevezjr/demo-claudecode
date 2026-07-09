package com.facturas.api.filter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@WithMockUser
class SecurityHeadersFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void response_hasXContentTypeOptionsHeader() throws Exception {
        mockMvc.perform(get("/api/v1/invoices"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    void response_hasXFrameOptionsHeader() throws Exception {
        mockMvc.perform(get("/api/v1/invoices"))
                .andExpect(header().string("X-Frame-Options", "DENY"));
    }

    @Test
    void response_hasStrictTransportSecurityHeader() throws Exception {
        mockMvc.perform(get("/api/v1/invoices"))
                .andExpect(header().string("Strict-Transport-Security",
                        "max-age=31536000; includeSubDomains"));
    }

    @Test
    void response_hasContentSecurityPolicyHeader() throws Exception {
        mockMvc.perform(get("/api/v1/invoices"))
                .andExpect(header().string("Content-Security-Policy", "default-src 'self'"));
    }

    @Test
    void response_hasReferrerPolicyHeader() throws Exception {
        mockMvc.perform(get("/api/v1/invoices"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"));
    }
}
