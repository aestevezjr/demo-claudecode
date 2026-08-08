package com.facturas.api.controller;

import com.facturas.api.config.CorsConfig;
import com.facturas.api.config.SecurityConfig;
import com.facturas.api.dto.InvoiceResponseDTO;
import com.facturas.api.enums.InvoiceStatus;
import com.facturas.api.exception.ResourceNotFoundException;
import com.facturas.api.service.InvoiceService;
import com.facturas.api.service.JwtService;
import org.springframework.security.core.userdetails.UserDetailsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        value = InvoiceController.class,
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class
)
@Import({SecurityConfig.class, CorsConfig.class})
@WithMockUser
class InvoiceControllerTest {

    private static final String BASE_URL = "/api/v1/invoices";

    /*
     * CSRF tokens for mutating requests.
     *
     * SecurityConfig now uses CookieCsrfTokenRepository, which reads the CSRF token
     * from the "XSRF-TOKEN" request cookie and validates it against the "X-XSRF-TOKEN"
     * request header.  We supply both in every non-safe request so the CSRF filter passes.
     *
     * Using the manual cookie+header approach rather than SecurityMockMvcRequestPostProcessors
     * .csrf() because the latter's default implementation doesn't reliably populate the
     * request cookie when CookieCsrfTokenRepository is configured.
     */
    private static final String CSRF_TOKEN_VALUE = "test-csrf-token";
    private static final Cookie CSRF_COOKIE = new Cookie("XSRF-TOKEN", CSRF_TOKEN_VALUE);

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InvoiceService invoiceService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    // ── helpers ───────────────────────────────────────────────────────────────

    private InvoiceResponseDTO sampleResponse() {
        InvoiceResponseDTO dto = new InvoiceResponseDTO();
        dto.setId(1L);
        dto.setInvoiceNumber("FAC-2026-0001");
        dto.setIssueDate(LocalDate.of(2026, 1, 15));
        dto.setStatus(InvoiceStatus.PENDING);
        dto.setClientName("Acme Corp");
        dto.setClientEmail("billing@acme.com");
        dto.setClientAddress("123 Main Street, Springfield");
        dto.setSubtotal(new BigDecimal("999.99"));
        dto.setTaxRate(new BigDecimal("10.00"));
        dto.setTaxAmount(new BigDecimal("100.00"));
        dto.setTotal(new BigDecimal("1099.99"));
        dto.setItems(List.of());
        return dto;
    }

    private Page<InvoiceResponseDTO> samplePage() {
        return new PageImpl<>(List.of(sampleResponse()));
    }

    private Map<String, Object> validRequest() {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("description", "Laptop");
        item.put("quantity", 1);
        item.put("unitPrice", 999.99);

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("issueDate", "2026-01-15");
        request.put("clientName", "Acme Corp");
        request.put("clientEmail", "billing@acme.com");
        request.put("clientAddress", "123 Main Street, Springfield");
        request.put("taxRate", 10);
        request.put("items", List.of(item));
        return request;
    }

    private String json(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    // ── B1: GET all ───────────────────────────────────────────────────────────

    @Test
    void getAll_returns200WithInvoicePage() throws Exception {
        when(invoiceService.getAllInvoices(any(Pageable.class), isNull(), isNull(), isNull(), isNull(), isNull(), isNull())).thenReturn(samplePage());

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].invoiceNumber").value("FAC-2026-0001"));
    }

    @Test
    void getAll_emptyList_returns200WithEmptyContent() throws Exception {
        when(invoiceService.getAllInvoices(any(Pageable.class), isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    // ── B2: GET by ID ─────────────────────────────────────────────────────────

    @Test
    void getById_existingId_returns200WithInvoiceNumber() throws Exception {
        when(invoiceService.getInvoiceById(1L)).thenReturn(sampleResponse());

        mockMvc.perform(get(BASE_URL + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceNumber").value("FAC-2026-0001"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void getById_nonExistentId_returns404() throws Exception {
        when(invoiceService.getInvoiceById(99L))
                .thenThrow(new ResourceNotFoundException("Factura no encontrada con id: 99"));

        mockMvc.perform(get(BASE_URL + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void getById_nonNumericId_returns400() throws Exception {
        mockMvc.perform(get(BASE_URL + "/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    // ── B3: POST create ───────────────────────────────────────────────────────

    @Test
    void create_validRequest_returns201() throws Exception {
        when(invoiceService.createInvoice(any())).thenReturn(sampleResponse());

        mockMvc.perform(post(BASE_URL)
                        .cookie(CSRF_COOKIE)
                        .header("X-XSRF-TOKEN", CSRF_TOKEN_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invoiceNumber").value("FAC-2026-0001"));
    }

    @Test
    void create_missingIssueDate_returns400() throws Exception {
        Map<String, Object> request = validRequest();
        request.remove("issueDate");

        mockMvc.perform(post(BASE_URL)
                        .cookie(CSRF_COOKIE)
                        .header("X-XSRF-TOKEN", CSRF_TOKEN_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.issueDate").exists());
    }

    @Test
    void create_blankClientName_returns400() throws Exception {
        Map<String, Object> request = validRequest();
        request.put("clientName", "");

        mockMvc.perform(post(BASE_URL)
                        .cookie(CSRF_COOKIE)
                        .header("X-XSRF-TOKEN", CSRF_TOKEN_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.clientName").exists());
    }

    @Test
    void create_invalidEmail_returns400() throws Exception {
        Map<String, Object> request = validRequest();
        request.put("clientEmail", "not-an-email");

        mockMvc.perform(post(BASE_URL)
                        .cookie(CSRF_COOKIE)
                        .header("X-XSRF-TOKEN", CSRF_TOKEN_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.clientEmail").exists());
    }

    @Test
    void create_blankClientAddress_returns400() throws Exception {
        Map<String, Object> request = validRequest();
        request.put("clientAddress", "");

        mockMvc.perform(post(BASE_URL)
                        .cookie(CSRF_COOKIE)
                        .header("X-XSRF-TOKEN", CSRF_TOKEN_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.clientAddress").exists());
    }

    @Test
    void create_nullTaxRate_returns400() throws Exception {
        Map<String, Object> request = validRequest();
        request.put("taxRate", null);

        mockMvc.perform(post(BASE_URL)
                        .cookie(CSRF_COOKIE)
                        .header("X-XSRF-TOKEN", CSRF_TOKEN_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.taxRate").exists());
    }

    @Test
    void create_negativeTaxRate_returns400() throws Exception {
        Map<String, Object> request = validRequest();
        request.put("taxRate", -1);

        mockMvc.perform(post(BASE_URL)
                        .cookie(CSRF_COOKIE)
                        .header("X-XSRF-TOKEN", CSRF_TOKEN_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.taxRate").exists());
    }

    @Test
    void create_taxRateOver100_returns400() throws Exception {
        Map<String, Object> request = validRequest();
        request.put("taxRate", 101);

        mockMvc.perform(post(BASE_URL)
                        .cookie(CSRF_COOKIE)
                        .header("X-XSRF-TOKEN", CSRF_TOKEN_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.taxRate").exists());
    }

    @Test
    void create_emptyItemsList_returns400() throws Exception {
        Map<String, Object> request = validRequest();
        request.put("items", List.of());

        mockMvc.perform(post(BASE_URL)
                        .cookie(CSRF_COOKIE)
                        .header("X-XSRF-TOKEN", CSRF_TOKEN_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.items").exists());
    }

    @Test
    void create_itemWithZeroQuantity_returns400() throws Exception {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("description", "Laptop");
        item.put("quantity", 0);
        item.put("unitPrice", 100);

        Map<String, Object> request = validRequest();
        request.put("items", List.of(item));

        mockMvc.perform(post(BASE_URL)
                        .cookie(CSRF_COOKIE)
                        .header("X-XSRF-TOKEN", CSRF_TOKEN_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields['items[0].quantity']").exists());
    }

    @Test
    void create_itemWithNegativeUnitPrice_returns400() throws Exception {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("description", "Laptop");
        item.put("quantity", 1);
        item.put("unitPrice", -1);

        Map<String, Object> request = validRequest();
        request.put("items", List.of(item));

        mockMvc.perform(post(BASE_URL)
                        .cookie(CSRF_COOKIE)
                        .header("X-XSRF-TOKEN", CSRF_TOKEN_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields['items[0].unitPrice']").exists());
    }

    @Test
    void create_itemWithBlankDescription_returns400() throws Exception {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("description", "");
        item.put("quantity", 1);
        item.put("unitPrice", 100);

        Map<String, Object> request = validRequest();
        request.put("items", List.of(item));

        mockMvc.perform(post(BASE_URL)
                        .cookie(CSRF_COOKIE)
                        .header("X-XSRF-TOKEN", CSRF_TOKEN_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields['items[0].description']").exists());
    }

    @Test
    void create_validationError_responseBodyHasFieldsKey() throws Exception {
        Map<String, Object> request = validRequest();
        request.remove("clientName");

        mockMvc.perform(post(BASE_URL)
                        .cookie(CSRF_COOKIE)
                        .header("X-XSRF-TOKEN", CSRF_TOKEN_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.fields").isMap());
    }

    // ── B4: PUT update ────────────────────────────────────────────────────────

    @Test
    void update_validRequest_returns200() throws Exception {
        when(invoiceService.updateInvoice(eq(1L), any())).thenReturn(sampleResponse());

        mockMvc.perform(put(BASE_URL + "/1")
                        .cookie(CSRF_COOKIE)
                        .header("X-XSRF-TOKEN", CSRF_TOKEN_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceNumber").value("FAC-2026-0001"));
    }

    @Test
    void update_nonExistentId_returns404() throws Exception {
        when(invoiceService.updateInvoice(eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("Factura no encontrada con id: 99"));

        mockMvc.perform(put(BASE_URL + "/99")
                        .cookie(CSRF_COOKIE)
                        .header("X-XSRF-TOKEN", CSRF_TOKEN_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validRequest())))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_invalidBody_returns400() throws Exception {
        Map<String, Object> request = validRequest();
        request.remove("clientName");

        mockMvc.perform(put(BASE_URL + "/1")
                        .cookie(CSRF_COOKIE)
                        .header("X-XSRF-TOKEN", CSRF_TOKEN_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest());
    }

    // ── B5: DELETE ────────────────────────────────────────────────────────────

    @Test
    void delete_existingId_returns204NoContent() throws Exception {
        doNothing().when(invoiceService).deleteInvoice(1L);

        mockMvc.perform(delete(BASE_URL + "/1")
                        .cookie(CSRF_COOKIE)
                        .header("X-XSRF-TOKEN", CSRF_TOKEN_VALUE))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    void delete_nonExistentId_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Factura no encontrada con id: 99"))
                .when(invoiceService).deleteInvoice(99L);

        mockMvc.perform(delete(BASE_URL + "/99")
                        .cookie(CSRF_COOKIE)
                        .header("X-XSRF-TOKEN", CSRF_TOKEN_VALUE))
                .andExpect(status().isNotFound());
    }

    // ── B6: PATCH status ──────────────────────────────────────────────────────

    @Test
    void updateStatus_validEnumValue_returns200() throws Exception {
        InvoiceResponseDTO paid = sampleResponse();
        paid.setStatus(InvoiceStatus.PAID);
        when(invoiceService.updateStatus(1L, InvoiceStatus.PAID)).thenReturn(paid);

        mockMvc.perform(patch(BASE_URL + "/1/status")
                        .cookie(CSRF_COOKIE)
                        .header("X-XSRF-TOKEN", CSRF_TOKEN_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"PAID\""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    void updateStatus_invalidEnumValue_returns400() throws Exception {
        mockMvc.perform(patch(BASE_URL + "/1/status")
                        .cookie(CSRF_COOKIE)
                        .header("X-XSRF-TOKEN", CSRF_TOKEN_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"INVALID_STATUS\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void updateStatus_nonExistentId_returns404() throws Exception {
        when(invoiceService.updateStatus(eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("Factura no encontrada con id: 99"));

        mockMvc.perform(patch(BASE_URL + "/99/status")
                        .cookie(CSRF_COOKIE)
                        .header("X-XSRF-TOKEN", CSRF_TOKEN_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"PAID\""))
                .andExpect(status().isNotFound());
    }
}
