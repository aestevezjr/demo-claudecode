package com.facturas.api.controller;

import com.facturas.api.dto.InvoiceRequestDTO;
import com.facturas.api.dto.InvoiceResponseDTO;
import com.facturas.api.enums.InvoiceStatus;
import com.facturas.api.service.InvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @GetMapping
    public ResponseEntity<Page<InvoiceResponseDTO>> getAll(
            @PageableDefault(size = 50, sort = "issueDate", direction = Sort.Direction.DESC)
            Pageable pageable,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount) {
        return ResponseEntity.ok(invoiceService.getAllInvoices(
                pageable, search, status, dateFrom, dateTo, minAmount, maxAmount));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvoiceResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceService.getInvoiceById(id));
    }

    @PostMapping
    public ResponseEntity<InvoiceResponseDTO> create(@Valid @RequestBody InvoiceRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(invoiceService.createInvoice(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<InvoiceResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody InvoiceRequestDTO dto) {
        return ResponseEntity.ok(invoiceService.updateInvoice(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        invoiceService.deleteInvoice(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<InvoiceResponseDTO> updateStatus(
            @PathVariable Long id,
            @RequestBody InvoiceStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("El estado de la factura no puede ser nulo");
        }
        return ResponseEntity.ok(invoiceService.updateStatus(id, status));
    }
}
