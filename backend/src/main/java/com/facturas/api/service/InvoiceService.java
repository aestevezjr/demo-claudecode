package com.facturas.api.service;

import com.facturas.api.dto.InvoiceRequestDTO;
import com.facturas.api.dto.InvoiceResponseDTO;
import com.facturas.api.enums.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InvoiceService {

    Page<InvoiceResponseDTO> getAllInvoices(Pageable pageable, String search);

    InvoiceResponseDTO getInvoiceById(Long id);

    InvoiceResponseDTO createInvoice(InvoiceRequestDTO dto);

    InvoiceResponseDTO updateInvoice(Long id, InvoiceRequestDTO dto);

    void deleteInvoice(Long id);

    InvoiceResponseDTO updateStatus(Long id, InvoiceStatus status);
}
