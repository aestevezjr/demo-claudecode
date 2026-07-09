package com.facturas.api.dto;

import com.facturas.api.enums.InvoiceStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class InvoiceRequestDTO {

    @NotNull(message = "La fecha de emisión es requerida")
    private LocalDate issueDate;

    private InvoiceStatus status = InvoiceStatus.PENDING;

    @NotBlank(message = "El nombre del cliente es requerido")
    @Size(max = 255, message = "El nombre del cliente no puede superar los 255 caracteres")
    private String clientName;

    @NotBlank(message = "El email del cliente es requerido")
    @Email(message = "El email del cliente no es válido")
    @Size(max = 255, message = "El email del cliente no puede superar los 255 caracteres")
    private String clientEmail;

    @NotBlank(message = "La dirección del cliente es requerida")
    @Size(max = 500, message = "La dirección del cliente no puede superar los 500 caracteres")
    private String clientAddress;

    @NotNull(message = "La tasa de impuesto es requerida")
    @DecimalMin(value = "0.0", message = "La tasa de impuesto no puede ser negativa")
    @DecimalMax(value = "100.0", message = "La tasa de impuesto no puede superar 100%")
    private BigDecimal taxRate;

    @Size(max = 2000, message = "Las notas no pueden superar los 2000 caracteres")
    private String notes;

    @NotEmpty(message = "La factura debe tener al menos un item")
    @Size(max = 100, message = "Una factura no puede tener más de 100 items")
    @Valid
    private List<InvoiceItemRequestDTO> items;
}
