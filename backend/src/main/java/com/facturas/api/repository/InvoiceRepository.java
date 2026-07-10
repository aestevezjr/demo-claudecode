package com.facturas.api.repository;

import com.facturas.api.entity.Invoice;
import com.facturas.api.enums.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Page<Invoice> findAllByOrderByIssueDateDesc(Pageable pageable);

    List<Invoice> findByStatus(InvoiceStatus status);

    @Query(value = """
            SELECT DISTINCT i FROM Invoice i LEFT JOIN i.items it
            WHERE LOWER(i.clientName) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(it.description) LIKE LOWER(CONCAT('%', :search, '%'))
            ORDER BY i.issueDate DESC
            """,
           countQuery = """
            SELECT COUNT(DISTINCT i) FROM Invoice i LEFT JOIN i.items it
            WHERE LOWER(i.clientName) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(it.description) LIKE LOWER(CONCAT('%', :search, '%'))
            """)
    Page<Invoice> findBySearchTerm(@Param("search") String search, Pageable pageable);
}
