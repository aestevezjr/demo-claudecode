package com.facturas.api.dto;

import com.facturas.api.enums.InvoiceStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class InvoiceResponseDTO {

    private Long id;
    private String invoiceNumber;
    private LocalDate issueDate;
    private InvoiceStatus status;
    private String clientName;
    private String clientEmail;
    private String clientAddress;
    private BigDecimal subtotal;
    private BigDecimal taxRate;
    private BigDecimal taxAmount;
    private BigDecimal total;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<InvoiceItemDTO> items;
}
