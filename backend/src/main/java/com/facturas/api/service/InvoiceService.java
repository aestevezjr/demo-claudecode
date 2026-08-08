package com.facturas.api.service;

import com.facturas.api.dto.InvoiceRequestDTO;
import com.facturas.api.dto.InvoiceResponseDTO;
import com.facturas.api.enums.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface InvoiceService {

    Page<InvoiceResponseDTO> getAllInvoices(Pageable pageable, String search,
            InvoiceStatus status, LocalDate dateFrom, LocalDate dateTo,
            BigDecimal minAmount, BigDecimal maxAmount);

    InvoiceResponseDTO getInvoiceById(Long id);

    InvoiceResponseDTO createInvoice(InvoiceRequestDTO dto);

    InvoiceResponseDTO updateInvoice(Long id, InvoiceRequestDTO dto);

    void deleteInvoice(Long id);

    InvoiceResponseDTO updateStatus(Long id, InvoiceStatus status);
}
