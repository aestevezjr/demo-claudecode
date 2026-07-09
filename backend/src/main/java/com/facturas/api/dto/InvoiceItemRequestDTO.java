package com.facturas.api.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class InvoiceItemRequestDTO {

    @NotBlank(message = "La descripción del item es requerida")
    @Size(max = 500, message = "La descripción del item no puede superar los 500 caracteres")
    private String description;

    @NotNull(message = "La cantidad es requerida")
    @Positive(message = "La cantidad debe ser mayor a 0")
    @Digits(integer = 7, fraction = 3, message = "La cantidad no puede tener más de 7 dígitos enteros y 3 decimales")
    private BigDecimal quantity;

    @NotNull(message = "El precio unitario es requerido")
    @PositiveOrZero(message = "El precio unitario no puede ser negativo")
    @Digits(integer = 10, fraction = 2, message = "El precio unitario no puede tener más de 10 dígitos enteros y 2 decimales")
    private BigDecimal unitPrice;
}
