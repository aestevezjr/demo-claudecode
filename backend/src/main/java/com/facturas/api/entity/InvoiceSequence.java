package com.facturas.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "invoice_sequences")
@Getter
@Setter
@NoArgsConstructor
public class InvoiceSequence {

    @Id
    @Column(name = "invoice_year")
    private Integer year;

    @Column(nullable = false)
    private Long lastSeq;

    public InvoiceSequence(Integer year, Long lastSeq) {
        this.year = year;
        this.lastSeq = lastSeq;
    }
}
