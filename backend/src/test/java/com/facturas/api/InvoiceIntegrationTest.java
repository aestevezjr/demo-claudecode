package com.facturas.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class InvoiceIntegrationTest {

    private static final String INVOICES_URL = "/api/v1/invoices";
    private static final String AUTH_URL = "/auth";

    /*
     * CSRF tokens for mutating /api/** requests.
     *
     * SecurityConfig configures CookieCsrfTokenRepository which reads the expected CSRF
     * token from the "XSRF-TOKEN" request cookie and validates it against the value in
     * the "X-XSRF-TOKEN" request header.  Supplying both with the same value satisfies
     * the filter for all non-safe HTTP methods on /api/** paths.
     *
     * /auth/** endpoints are CSRF-exempt (see SecurityConfig), so register/login/logout
     * requests do not need these cookies/headers.
     */
    private static final String CSRF_TOKEN_VALUE = "test-csrf-token";
    private static final Cookie CSRF_COOKIE = new Cookie("XSRF-TOKEN", CSRF_TOKEN_VALUE);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /** JWT cookie extracted from the login Set-Cookie response header. */
    private Cookie jwtCookie;

    @BeforeEach
    void authenticate() throws Exception {
        Map<String, String> credentials = Map.of("username", "integtest", "password", "pass1234");
        String body = objectMapper.writeValueAsString(credentials);

        // Register (ignore error if user already exists from a previous test run)
        mockMvc.perform(post(AUTH_URL + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // Login — no longer returns a token in the body; sets an HttpOnly "jwt" cookie instead
        MvcResult loginResult = mockMvc.perform(post(AUTH_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        // Extract the JWT value from the Set-Cookie response header.
        // Header format: "jwt=<token>; Path=/; Max-Age=86400; HttpOnly; SameSite=Strict"
        String setCookieHeader = loginResult.getResponse().getHeader("Set-Cookie");
        String jwtValue = setCookieHeader.split(";")[0].split("=", 2)[1];
        jwtCookie = new Cookie("jwt", jwtValue);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Map<String, Object> validInvoiceRequest() {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("description", "Integration Test Item");
        item.put("quantity", 2);
        item.put("unitPrice", 100.00);

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("issueDate", "2026-01-15");
        request.put("clientName", "Integration Client");
        request.put("clientEmail", "integration@test.com");
        request.put("clientAddress", "456 Integration Ave");
        request.put("taxRate", 10);
        request.put("items", List.of(item));
        return request;
    }

    private String json(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    // ── C1: Auth ──────────────────────────────────────────────────────────────

    @Test
    void register_validCredentials_returns201() throws Exception {
        // Since cookie-based auth was adopted, register no longer returns a token in the body.
        // It creates the user and returns 201 with no body.  The client must log in separately.
        Map<String, String> creds = Map.of("username", "newuser_" + System.currentTimeMillis(), "password", "pass1234");

        mockMvc.perform(post(AUTH_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(creds)))
                .andExpect(status().isCreated());
    }

    @Test
    void login_validCredentials_returns200WithJwtCookie() throws Exception {
        // Login now sets an HttpOnly "jwt" cookie rather than returning a token in the body.
        mockMvc.perform(post(AUTH_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "integtest", "password", "pass1234"))))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString("jwt=")));
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        mockMvc.perform(post(AUTH_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "integtest", "password", "wrongpassword"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invoiceEndpoint_withoutToken_returns401() throws Exception {
        // No jwt cookie — JwtAuthFilter skips, SecurityContext remains empty → 401
        mockMvc.perform(get(INVOICES_URL))
                .andExpect(status().isUnauthorized());
    }

    // ── C2: Invoice CRUD ──────────────────────────────────────────────────────

    @Test
    void createInvoice_validRequest_returns201WithServerComputedTotals() throws Exception {
        // qty=2, price=100 → subtotal=200, tax=10% → taxAmount=20, total=220
        MvcResult result = mockMvc.perform(post(INVOICES_URL)
                        .cookie(jwtCookie)
                        .cookie(CSRF_COOKIE)
                        .header("X-XSRF-TOKEN", CSRF_TOKEN_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validInvoiceRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invoiceNumber").value(org.hamcrest.Matchers.matchesPattern("FAC-\\d{4}-\\d{4}")))
                .andExpect(jsonPath("$.subtotal").value(200.00))
                .andExpect(jsonPath("$.taxAmount").value(20.00))
                .andExpect(jsonPath("$.total").value(220.00))
                .andReturn();

        Map<?, ?> created = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        Long id = ((Number) created.get("id")).longValue();

        // Clean up
        mockMvc.perform(delete(INVOICES_URL + "/" + id)
                .cookie(jwtCookie)
                .cookie(CSRF_COOKIE)
                .header("X-XSRF-TOKEN", CSRF_TOKEN_VALUE));
    }

    @Test
    void getInvoiceById_existingInvoice_returns200() throws Exception {
        // Create first
        MvcResult created = mockMvc.perform(post(INVOICES_URL)
                        .cookie(jwtCookie)
                        .cookie(CSRF_COOKIE)
                        .header("X-XSRF-TOKEN", CSRF_TOKEN_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validInvoiceRequest())))
                .andExpect(status().isCreated())
                .andReturn();

        Map<?, ?> createdBody = objectMapper.readValue(created.getResponse().getContentAsString(), Map.class);
        Long id = ((Number) createdBody.get("id")).longValue();

        // Verify get by id
        mockMvc.perform(get(INVOICES_URL + "/" + id)
                        .cookie(jwtCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));

        // Clean up
        mockMvc.perform(delete(INVOICES_URL + "/" + id)
                .cookie(jwtCookie)
                .cookie(CSRF_COOKIE)
                .header("X-XSRF-TOKEN", CSRF_TOKEN_VALUE));
    }

    @Test
    void getAllInvoices_withAuth_returns200WithPageStructure() throws Exception {
        mockMvc.perform(get(INVOICES_URL)
                        .cookie(jwtCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test
    void updateInvoice_validRequest_returns200WithNewTotals() throws Exception {
        // Create
        MvcResult created = mockMvc.perform(post(INVOICES_URL)
                        .cookie(jwtCookie)
                        .cookie(CSRF_COOKIE)
                        .header("X-XSRF-TOKEN", CSRF_TOKEN_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validInvoiceRequest())))
                .andExpect(status().isCreated())
                .andReturn();

        Map<?, ?> createdBody = objectMapper.readValue(created.getResponse().getContentAsString(), Map.class);
        Long id = ((Number) createdBody.get("id")).longValue();

        // Update with different items
        Map<String, Object> updateItem = new LinkedHashMap<>();
        updateItem.put("description", "Updated Item");
        updateItem.put("quantity", 1);
        updateItem.put("unitPrice", 500.00);

        Map<String, Object> updateRequest = new LinkedHashMap<>();
        updateRequest.put("issueDate", "2026-02-01");
        updateRequest.put("clientName", "Updated Client");
        updateRequest.put("clientEmail", "updated@test.com");
        updateRequest.put("clientAddress", "Updated Address 789");
        updateRequest.put("taxRate", 0);
        updateRequest.put("items", List.of(updateItem));

        mockMvc.perform(put(INVOICES_URL + "/" + id)
                        .cookie(jwtCookie)
                        .cookie(CSRF_COOKIE)
                        .header("X-XSRF-TOKEN", CSRF_TOKEN_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotal").value(500.00))
                .andExpect(jsonPath("$.total").value(500.00))
                .andExpect(jsonPath("$.clientName").value("Updated Client"));

        // Clean up
        mockMvc.perform(delete(INVOICES_URL + "/" + id)
                .cookie(jwtCookie)
                .cookie(CSRF_COOKIE)
                .header("X-XSRF-TOKEN", CSRF_TOKEN_VALUE));
    }

    @Test
    void deleteInvoice_existingInvoice_returns204() throws Exception {
        // Create
        MvcResult created = mockMvc.perform(post(INVOICES_URL)
                        .cookie(jwtCookie)
                        .cookie(CSRF_COOKIE)
                        .header("X-XSRF-TOKEN", CSRF_TOKEN_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validInvoiceRequest())))
                .andExpect(status().isCreated())
                .andReturn();

        Map<?, ?> createdBody = objectMapper.readValue(created.getResponse().getContentAsString(), Map.class);
        Long id = ((Number) createdBody.get("id")).longValue();

        // Delete
        mockMvc.perform(delete(INVOICES_URL + "/" + id)
                        .cookie(jwtCookie)
                        .cookie(CSRF_COOKIE)
                        .header("X-XSRF-TOKEN", CSRF_TOKEN_VALUE))
                .andExpect(status().isNoContent());

        // Verify deleted
        mockMvc.perform(get(INVOICES_URL + "/" + id)
                        .cookie(jwtCookie))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateStatus_toPaid_returns200WithUpdatedStatus() throws Exception {
        // Create
        MvcResult created = mockMvc.perform(post(INVOICES_URL)
                        .cookie(jwtCookie)
                        .cookie(CSRF_COOKIE)
                        .header("X-XSRF-TOKEN", CSRF_TOKEN_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validInvoiceRequest())))
                .andExpect(status().isCreated())
                .andReturn();

        Map<?, ?> createdBody = objectMapper.readValue(created.getResponse().getContentAsString(), Map.class);
        Long id = ((Number) createdBody.get("id")).longValue();

        // Update status
        mockMvc.perform(patch(INVOICES_URL + "/" + id + "/status")
                        .cookie(jwtCookie)
                        .cookie(CSRF_COOKIE)
                        .header("X-XSRF-TOKEN", CSRF_TOKEN_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"PAID\""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));

        // Clean up
        mockMvc.perform(delete(INVOICES_URL + "/" + id)
                .cookie(jwtCookie)
                .cookie(CSRF_COOKIE)
                .header("X-XSRF-TOKEN", CSRF_TOKEN_VALUE));
    }

    @Test
    void createInvoice_xssInClientName_htmlStripped() throws Exception {
        Map<String, Object> request = validInvoiceRequest();
        request.put("clientName", "<script>alert('xss')</script>Clean Name");

        MvcResult result = mockMvc.perform(post(INVOICES_URL)
                        .cookie(jwtCookie)
                        .cookie(CSRF_COOKIE)
                        .header("X-XSRF-TOKEN", CSRF_TOKEN_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clientName").value("Clean Name"))
                .andReturn();

        Map<?, ?> body = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        mockMvc.perform(delete(INVOICES_URL + "/" + ((Number) body.get("id")).longValue())
                .cookie(jwtCookie)
                .cookie(CSRF_COOKIE)
                .header("X-XSRF-TOKEN", CSRF_TOKEN_VALUE));
    }

    @Test
    void createInvoice_missingClientName_returns400() throws Exception {
        Map<String, Object> request = validInvoiceRequest();
        request.remove("clientName");

        mockMvc.perform(post(INVOICES_URL)
                        .cookie(jwtCookie)
                        .cookie(CSRF_COOKIE)
                        .header("X-XSRF-TOKEN", CSRF_TOKEN_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.clientName").exists());
    }
}
