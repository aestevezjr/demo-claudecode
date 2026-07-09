package com.facturas.api.repository;

import com.facturas.api.entity.Invoice;
import com.facturas.api.enums.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Page<Invoice> findAllByOrderByIssueDateDesc(Pageable pageable);

    List<Invoice> findByStatus(InvoiceStatus status);
}
