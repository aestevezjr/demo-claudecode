package com.facturas.api.repository;

import com.facturas.api.entity.Invoice;
import com.facturas.api.enums.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Page<Invoice> findAllByOrderByIssueDateDesc(Pageable pageable);

    List<Invoice> findByStatus(InvoiceStatus status);

    @Query(value = """
            SELECT DISTINCT i FROM Invoice i LEFT JOIN i.items it
            WHERE (:search IS NULL OR :search = ''
                   OR LOWER(i.clientName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(it.description) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:status IS NULL OR i.status = :status)
              AND (:dateFrom IS NULL OR i.issueDate >= :dateFrom)
              AND (:dateTo IS NULL OR i.issueDate <= :dateTo)
              AND (:minAmount IS NULL OR i.total >= :minAmount)
              AND (:maxAmount IS NULL OR i.total <= :maxAmount)
            ORDER BY i.issueDate DESC
            """,
           countQuery = """
            SELECT COUNT(DISTINCT i) FROM Invoice i LEFT JOIN i.items it
            WHERE (:search IS NULL OR :search = ''
                   OR LOWER(i.clientName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(it.description) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:status IS NULL OR i.status = :status)
              AND (:dateFrom IS NULL OR i.issueDate >= :dateFrom)
              AND (:dateTo IS NULL OR i.issueDate <= :dateTo)
              AND (:minAmount IS NULL OR i.total >= :minAmount)
              AND (:maxAmount IS NULL OR i.total <= :maxAmount)
            """)
    Page<Invoice> findByFilters(
            @Param("search") String search,
            @Param("status") InvoiceStatus status,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount,
            Pageable pageable);
}
