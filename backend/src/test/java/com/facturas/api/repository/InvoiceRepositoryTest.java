package com.facturas.api.repository;

import com.facturas.api.entity.Invoice;
import com.facturas.api.entity.InvoiceItem;
import com.facturas.api.enums.InvoiceStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
class InvoiceRepositoryTest {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private InvoiceItemRepository invoiceItemRepository;

    @Autowired
    private TestEntityManager entityManager;

    // ── helpers ───────────────────────────────────────────────────────────────

    private Invoice buildInvoice(String invoiceNumber, LocalDate issueDate, InvoiceStatus status) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setIssueDate(issueDate);
        invoice.setStatus(status);
        invoice.setClientName("Test Client");
        invoice.setClientEmail("test@example.com");
        invoice.setClientAddress("123 Test Street");
        invoice.setTaxRate(new BigDecimal("0.00"));
        invoice.setSubtotal(new BigDecimal("10.00"));
        invoice.setTaxAmount(new BigDecimal("0.00"));
        invoice.setTotal(new BigDecimal("10.00"));
        return invoice;
    }

    private InvoiceItem buildItem(Invoice invoice, String description, int sortOrder) {
        InvoiceItem item = new InvoiceItem();
        item.setInvoice(invoice);
        item.setDescription(description);
        item.setQuantity(new BigDecimal("1.000"));
        item.setUnitPrice(new BigDecimal("10.00"));
        item.setLineTotal(new BigDecimal("10.00"));
        item.setSortOrder(sortOrder);
        return item;
    }

    // ── findAllByOrderByIssueDateDesc ─────────────────────────────────────────

    @Test
    void findAllByOrderByIssueDateDesc_returnsNewestFirst() {
        Invoice oldest = buildInvoice("FAC-2026-0001", LocalDate.of(2026, 1, 1), InvoiceStatus.PENDING);
        Invoice middle = buildInvoice("FAC-2026-0002", LocalDate.of(2026, 3, 1), InvoiceStatus.PAID);
        Invoice newest = buildInvoice("FAC-2026-0003", LocalDate.of(2026, 6, 1), InvoiceStatus.PENDING);

        invoiceRepository.saveAll(List.of(oldest, middle, newest));
        entityManager.flush();
        entityManager.clear();

        Page<Invoice> result = invoiceRepository.findAllByOrderByIssueDateDesc(Pageable.unpaged());

        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getContent().get(0).getInvoiceNumber()).isEqualTo("FAC-2026-0003");
        assertThat(result.getContent().get(1).getInvoiceNumber()).isEqualTo("FAC-2026-0002");
        assertThat(result.getContent().get(2).getInvoiceNumber()).isEqualTo("FAC-2026-0001");
    }

    @Test
    void findAllByOrderByIssueDateDesc_emptyTable_returnsEmptyPage() {
        Page<Invoice> result = invoiceRepository.findAllByOrderByIssueDateDesc(Pageable.unpaged());

        assertThat(result.getContent()).isNotNull().isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    // ── findByStatus ──────────────────────────────────────────────────────────

    @Test
    void findByStatus_returnsOnlyMatchingStatus() {
        invoiceRepository.saveAll(List.of(
                buildInvoice("FAC-2026-0001", LocalDate.now(), InvoiceStatus.PENDING),
                buildInvoice("FAC-2026-0002", LocalDate.now(), InvoiceStatus.PENDING),
                buildInvoice("FAC-2026-0003", LocalDate.now(), InvoiceStatus.PAID)));
        entityManager.flush();
        entityManager.clear();

        List<Invoice> paid = invoiceRepository.findByStatus(InvoiceStatus.PAID);

        assertThat(paid).hasSize(1);
        assertThat(paid.get(0).getStatus()).isEqualTo(InvoiceStatus.PAID);
    }

    @Test
    void findByStatus_noMatches_returnsEmptyList() {
        invoiceRepository.save(buildInvoice("FAC-2026-0001", LocalDate.now(), InvoiceStatus.PENDING));
        entityManager.flush();
        entityManager.clear();

        List<Invoice> result = invoiceRepository.findByStatus(InvoiceStatus.CANCELLED);

        assertThat(result).isEmpty();
    }

    // ── pagination ────────────────────────────────────────────────────────────

    @Test
    void findAllByOrderByIssueDateDesc_pageSize1_returnsCorrectPage() {
        invoiceRepository.saveAll(List.of(
                buildInvoice("FAC-2026-0001", LocalDate.of(2026, 1, 1), InvoiceStatus.PENDING),
                buildInvoice("FAC-2026-0002", LocalDate.of(2026, 3, 1), InvoiceStatus.PAID),
                buildInvoice("FAC-2026-0003", LocalDate.of(2026, 6, 1), InvoiceStatus.PENDING)));
        entityManager.flush();
        entityManager.clear();

        org.springframework.data.domain.PageRequest firstPage =
                org.springframework.data.domain.PageRequest.of(0, 1);
        Page<Invoice> result = invoiceRepository.findAllByOrderByIssueDateDesc(firstPage);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(3);
        assertThat(result.getContent().get(0).getInvoiceNumber()).isEqualTo("FAC-2026-0003");
    }

    // ── cascade save ──────────────────────────────────────────────────────────

    @Test
    void save_invoiceWithItems_cascadesSaveToItems() {
        Invoice invoice = buildInvoice("FAC-2026-0001", LocalDate.now(), InvoiceStatus.PENDING);
        InvoiceItem item1 = buildItem(invoice, "Item A", 0);
        InvoiceItem item2 = buildItem(invoice, "Item B", 1);
        invoice.getItems().addAll(List.of(item1, item2));

        Invoice saved = invoiceRepository.save(invoice);
        entityManager.flush();
        entityManager.clear();

        Invoice reloaded = invoiceRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getItems()).hasSize(2);
        assertThat(reloaded.getItems())
                .extracting(InvoiceItem::getDescription)
                .containsExactly("Item A", "Item B");
    }

    // ── cascade delete / orphan removal ───────────────────────────────────────

    @Test
    void delete_invoice_orphanRemovalDeletesAllItems() {
        Invoice invoice = buildInvoice("FAC-2026-0001", LocalDate.now(), InvoiceStatus.PENDING);
        invoice.getItems().add(buildItem(invoice, "Item A", 0));
        invoice.getItems().add(buildItem(invoice, "Item B", 1));

        Long invoiceId = invoiceRepository.save(invoice).getId();
        entityManager.flush();
        entityManager.clear();

        assertThat(invoiceItemRepository.count()).isEqualTo(2);

        Invoice toDelete = invoiceRepository.findById(invoiceId).orElseThrow();
        invoiceRepository.delete(toDelete);
        entityManager.flush();

        assertThat(invoiceItemRepository.count()).isEqualTo(0);
        assertThat(invoiceRepository.findById(invoiceId)).isEmpty();
    }

    // ── unique constraint ─────────────────────────────────────────────────────

    @Test
    void save_duplicateInvoiceNumber_throwsDataIntegrityViolation() {
        invoiceRepository.saveAndFlush(
                buildInvoice("FAC-2026-0001", LocalDate.now(), InvoiceStatus.PENDING));

        assertThatThrownBy(() -> invoiceRepository.saveAndFlush(
                buildInvoice("FAC-2026-0001", LocalDate.now(), InvoiceStatus.PAID)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Test
    void findById_nonExistentId_returnsEmpty() {
        Optional<Invoice> result = invoiceRepository.findById(999L);

        assertThat(result).isEmpty();
    }
}
