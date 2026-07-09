package com.facturas.api.service.impl;

import com.facturas.api.dto.InvoiceItemRequestDTO;
import com.facturas.api.dto.InvoiceItemDTO;
import com.facturas.api.dto.InvoiceRequestDTO;
import com.facturas.api.dto.InvoiceResponseDTO;
import com.facturas.api.entity.Invoice;
import com.facturas.api.entity.InvoiceItem;
import com.facturas.api.entity.InvoiceSequence;
import com.facturas.api.enums.InvoiceStatus;
import com.facturas.api.exception.ResourceNotFoundException;
import com.facturas.api.repository.InvoiceRepository;
import com.facturas.api.repository.InvoiceSequenceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceImplTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private InvoiceSequenceRepository invoiceSequenceRepository;

    @InjectMocks
    private InvoiceServiceImpl invoiceService;

    // ── helpers ───────────────────────────────────────────────────────────────

    private InvoiceRequestDTO buildRequest(BigDecimal taxRate, InvoiceItemRequestDTO... items) {
        InvoiceRequestDTO dto = new InvoiceRequestDTO();
        dto.setIssueDate(LocalDate.of(2026, 1, 15));
        dto.setClientName("Acme Corp");
        dto.setClientEmail("billing@acme.com");
        dto.setClientAddress("123 Main Street, Springfield");
        dto.setTaxRate(taxRate);
        dto.setItems(List.of(items));
        return dto;
    }

    private InvoiceItemRequestDTO buildItem(String description, BigDecimal qty, BigDecimal price) {
        InvoiceItemRequestDTO item = new InvoiceItemRequestDTO();
        item.setDescription(description);
        item.setQuantity(qty);
        item.setUnitPrice(price);
        return item;
    }

    private Invoice buildSavedInvoice(Long id, String invoiceNumber, InvoiceStatus status) {
        Invoice invoice = new Invoice();
        invoice.setId(id);
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setIssueDate(LocalDate.of(2026, 1, 15));
        invoice.setStatus(status);
        invoice.setClientName("Acme Corp");
        invoice.setClientEmail("billing@acme.com");
        invoice.setClientAddress("123 Main Street, Springfield");
        invoice.setTaxRate(new BigDecimal("0.00"));
        invoice.setSubtotal(new BigDecimal("100.00"));
        invoice.setTaxAmount(new BigDecimal("0.00"));
        invoice.setTotal(new BigDecimal("100.00"));
        return invoice;
    }

    private void stubSaveReturnsSameInvoice() {
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private void stubSequence(long lastSeq) {
        if (lastSeq == 0L) {
            when(invoiceSequenceRepository.findByYearWithLock(anyInt()))
                    .thenReturn(Optional.empty());
        } else {
            when(invoiceSequenceRepository.findByYearWithLock(anyInt()))
                    .thenReturn(Optional.of(new InvoiceSequence(LocalDate.now().getYear(), lastSeq)));
        }
        when(invoiceSequenceRepository.save(any(InvoiceSequence.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // ── A1: computeTotals ─────────────────────────────────────────────────────

    @Test
    void computeTotals_singleItem_correctLineAndSubtotal() {
        stubSaveReturnsSameInvoice();
        stubSequence(0L);

        InvoiceResponseDTO response = invoiceService.createInvoice(
                buildRequest(new BigDecimal("0"),
                        buildItem("Widget", new BigDecimal("2"), new BigDecimal("10.00"))));

        assertThat(response.getSubtotal()).isEqualByComparingTo("20.00");
        assertThat(response.getTaxAmount()).isEqualByComparingTo("0.00");
        assertThat(response.getTotal()).isEqualByComparingTo("20.00");
        assertThat(response.getItems().get(0).getLineTotal()).isEqualByComparingTo("20.00");
    }

    @Test
    void computeTotals_withTaxRate_halfUpRounding() {
        stubSaveReturnsSameInvoice();
        stubSequence(0L);

        // 33.33 * 10 / 100 = 3.333 → rounds HALF_UP to 3.33
        InvoiceResponseDTO response = invoiceService.createInvoice(
                buildRequest(new BigDecimal("10"),
                        buildItem("Service", new BigDecimal("1"), new BigDecimal("33.33"))));

        assertThat(response.getSubtotal()).isEqualByComparingTo("33.33");
        assertThat(response.getTaxAmount()).isEqualByComparingTo("3.33");
        assertThat(response.getTotal()).isEqualByComparingTo("36.66");
    }

    @Test
    void computeTotals_multipleItems_sumsCorrectly() {
        stubSaveReturnsSameInvoice();
        stubSequence(0L);

        // 10.00 + 10.00 + 9.99 = 29.99
        InvoiceResponseDTO response = invoiceService.createInvoice(
                buildRequest(new BigDecimal("0"),
                        buildItem("A", new BigDecimal("1"), new BigDecimal("10.00")),
                        buildItem("B", new BigDecimal("2"), new BigDecimal("5.00")),
                        buildItem("C", new BigDecimal("3"), new BigDecimal("3.33"))));

        assertThat(response.getSubtotal()).isEqualByComparingTo("29.99");
        assertThat(response.getItems()).hasSize(3);
    }

    @Test
    void computeTotals_zeroTaxRate_taxAmountIsZero() {
        stubSaveReturnsSameInvoice();
        stubSequence(0L);

        InvoiceResponseDTO response = invoiceService.createInvoice(
                buildRequest(new BigDecimal("0"),
                        buildItem("Item", new BigDecimal("5"), new BigDecimal("10.00"))));

        assertThat(response.getTaxAmount()).isEqualByComparingTo("0.00");
        assertThat(response.getTotal()).isEqualByComparingTo(response.getSubtotal());
    }

    @Test
    void computeTotals_maxTaxRate100_totalIsDoubleSubtotal() {
        stubSaveReturnsSameInvoice();
        stubSequence(0L);

        InvoiceResponseDTO response = invoiceService.createInvoice(
                buildRequest(new BigDecimal("100"),
                        buildItem("Item", new BigDecimal("1"), new BigDecimal("100.00"))));

        assertThat(response.getSubtotal()).isEqualByComparingTo("100.00");
        assertThat(response.getTaxAmount()).isEqualByComparingTo("100.00");
        assertThat(response.getTotal()).isEqualByComparingTo("200.00");
    }

    @Test
    void computeTotals_fractionalQuantity_lineTotalRoundedHalfUp() {
        stubSaveReturnsSameInvoice();
        stubSequence(0L);

        // 1.005 * 10.00 = 10.050 → setScale(2, HALF_UP) = 10.05
        InvoiceResponseDTO response = invoiceService.createInvoice(
                buildRequest(new BigDecimal("0"),
                        buildItem("Item", new BigDecimal("1.005"), new BigDecimal("10.00"))));

        assertThat(response.getItems().get(0).getLineTotal()).isEqualByComparingTo("10.05");
    }

    @Test
    void computeTotals_setsLineTotalOnEachItem() {
        stubSaveReturnsSameInvoice();
        stubSequence(0L);

        InvoiceResponseDTO response = invoiceService.createInvoice(
                buildRequest(new BigDecimal("0"),
                        buildItem("A", new BigDecimal("2"), new BigDecimal("5.00")),
                        buildItem("B", new BigDecimal("3"), new BigDecimal("4.00"))));

        assertThat(response.getItems().get(0).getLineTotal()).isEqualByComparingTo("10.00");
        assertThat(response.getItems().get(1).getLineTotal()).isEqualByComparingTo("12.00");
    }

    @Test
    void computeTotals_total_hasScale2() {
        ArgumentCaptor<Invoice> captor = ArgumentCaptor.forClass(Invoice.class);
        when(invoiceRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));
        stubSequence(0L);

        invoiceService.createInvoice(
                buildRequest(new BigDecimal("10"),
                        buildItem("Item", new BigDecimal("1"), new BigDecimal("10.00"))));

        assertThat(captor.getValue().getTotal().scale()).isEqualTo(2);
    }

    // ── A2: generateInvoiceNumber ─────────────────────────────────────────────

    @Test
    void createInvoice_invoiceNumberMatchesFormat() {
        stubSaveReturnsSameInvoice();
        stubSequence(0L);

        InvoiceResponseDTO response = invoiceService.createInvoice(
                buildRequest(new BigDecimal("0"),
                        buildItem("Item", BigDecimal.ONE, new BigDecimal("10.00"))));

        assertThat(response.getInvoiceNumber()).matches(Pattern.compile("FAC-\\d{4}-\\d{4}"));
    }

    @Test
    void createInvoice_invoiceNumberContainsCurrentYear() {
        stubSaveReturnsSameInvoice();
        stubSequence(0L);

        InvoiceResponseDTO response = invoiceService.createInvoice(
                buildRequest(new BigDecimal("0"),
                        buildItem("Item", BigDecimal.ONE, new BigDecimal("10.00"))));

        assertThat(response.getInvoiceNumber()).contains(String.valueOf(LocalDate.now().getYear()));
    }

    @Test
    void createInvoice_invoiceNumberSequence_basedOnLastSeq() {
        stubSaveReturnsSameInvoice();
        stubSequence(5L);

        InvoiceResponseDTO response = invoiceService.createInvoice(
                buildRequest(new BigDecimal("0"),
                        buildItem("Item", BigDecimal.ONE, new BigDecimal("10.00"))));

        assertThat(response.getInvoiceNumber()).endsWith("-0006");
    }

    @Test
    void createInvoice_emptyRepository_sequenceStartsAt0001() {
        stubSaveReturnsSameInvoice();
        stubSequence(0L);

        InvoiceResponseDTO response = invoiceService.createInvoice(
                buildRequest(new BigDecimal("0"),
                        buildItem("Item", BigDecimal.ONE, new BigDecimal("10.00"))));

        assertThat(response.getInvoiceNumber()).endsWith("-0001");
    }

    // ── A3: createInvoice ─────────────────────────────────────────────────────

    @Test
    void createInvoice_validRequest_returnsNonNullDtoWithAllFields() {
        stubSaveReturnsSameInvoice();
        stubSequence(0L);

        InvoiceResponseDTO response = invoiceService.createInvoice(
                buildRequest(new BigDecimal("0"),
                        buildItem("Item", BigDecimal.ONE, new BigDecimal("50.00"))));

        assertThat(response).isNotNull();
        assertThat(response.getInvoiceNumber()).isNotBlank();
        assertThat(response.getClientName()).isEqualTo("Acme Corp");
        assertThat(response.getClientEmail()).isEqualTo("billing@acme.com");
        assertThat(response.getIssueDate()).isEqualTo(LocalDate.of(2026, 1, 15));
        assertThat(response.getStatus()).isEqualTo(InvoiceStatus.PENDING);
    }

    @Test
    void createInvoice_nullStatus_defaultsToPending() {
        stubSaveReturnsSameInvoice();
        stubSequence(0L);

        InvoiceRequestDTO dto = buildRequest(new BigDecimal("0"),
                buildItem("Item", BigDecimal.ONE, new BigDecimal("10.00")));
        dto.setStatus(null);

        InvoiceResponseDTO response = invoiceService.createInvoice(dto);

        assertThat(response.getStatus()).isEqualTo(InvoiceStatus.PENDING);
    }

    @Test
    void createInvoice_totalsAreComputedByServer() {
        stubSaveReturnsSameInvoice();
        stubSequence(0L);

        // qty=2, price=25.00 → subtotal=50.00, tax=10% → taxAmount=5.00, total=55.00
        InvoiceResponseDTO response = invoiceService.createInvoice(
                buildRequest(new BigDecimal("10"),
                        buildItem("Item", new BigDecimal("2"), new BigDecimal("25.00"))));

        assertThat(response.getSubtotal()).isEqualByComparingTo("50.00");
        assertThat(response.getTaxAmount()).isEqualByComparingTo("5.00");
        assertThat(response.getTotal()).isEqualByComparingTo("55.00");
    }

    @Test
    void createInvoice_itemsAreMappedCorrectly() {
        stubSaveReturnsSameInvoice();
        stubSequence(0L);

        InvoiceResponseDTO response = invoiceService.createInvoice(
                buildRequest(new BigDecimal("0"),
                        buildItem("Alpha", BigDecimal.ONE, new BigDecimal("10.00")),
                        buildItem("Beta", BigDecimal.ONE, new BigDecimal("20.00"))));

        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getItems().get(0).getDescription()).isEqualTo("Alpha");
        assertThat(response.getItems().get(1).getDescription()).isEqualTo("Beta");
    }

    @Test
    void createInvoice_repositorySaveIsCalledOnce() {
        stubSaveReturnsSameInvoice();
        stubSequence(0L);

        invoiceService.createInvoice(
                buildRequest(new BigDecimal("0"),
                        buildItem("Item", BigDecimal.ONE, new BigDecimal("10.00"))));

        verify(invoiceRepository, times(1)).save(any(Invoice.class));
    }

    @Test
    void createInvoice_htmlTagsInClientName_stripped() {
        stubSaveReturnsSameInvoice();
        stubSequence(0L);

        InvoiceRequestDTO dto = buildRequest(new BigDecimal("0"),
                buildItem("Item", BigDecimal.ONE, new BigDecimal("10.00")));
        dto.setClientName("<script>alert('xss')</script>Acme");

        InvoiceResponseDTO response = invoiceService.createInvoice(dto);

        assertThat(response.getClientName()).isEqualTo("Acme");
        assertThat(response.getClientName()).doesNotContain("<script>");
    }

    // ── A4: updateInvoice ─────────────────────────────────────────────────────

    @Test
    void updateInvoice_preservesOriginalInvoiceNumber() {
        Invoice existing = buildSavedInvoice(1L, "FAC-2026-0001", InvoiceStatus.PENDING);
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(existing));
        stubSaveReturnsSameInvoice();

        InvoiceResponseDTO response = invoiceService.updateInvoice(1L,
                buildRequest(new BigDecimal("0"),
                        buildItem("Item", BigDecimal.ONE, new BigDecimal("10.00"))));

        assertThat(response.getInvoiceNumber()).isEqualTo("FAC-2026-0001");
    }

    @Test
    void updateInvoice_clearsOldItemsBeforeMapping() {
        Invoice existing = buildSavedInvoice(1L, "FAC-2026-0001", InvoiceStatus.PENDING);
        for (int i = 1; i <= 3; i++) {
            InvoiceItem old = new InvoiceItem();
            old.setDescription("Old " + i);
            old.setQuantity(BigDecimal.ONE);
            old.setUnitPrice(BigDecimal.ONE);
            existing.getItems().add(old);
        }
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(existing));
        stubSaveReturnsSameInvoice();

        InvoiceResponseDTO response = invoiceService.updateInvoice(1L,
                buildRequest(new BigDecimal("0"),
                        buildItem("New Item", BigDecimal.ONE, new BigDecimal("10.00"))));

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getDescription()).isEqualTo("New Item");
    }

    @Test
    void updateInvoice_recomputesTotalsFromNewItems() {
        Invoice existing = buildSavedInvoice(1L, "FAC-2026-0001", InvoiceStatus.PENDING);
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(existing));
        stubSaveReturnsSameInvoice();

        InvoiceResponseDTO response = invoiceService.updateInvoice(1L,
                buildRequest(new BigDecimal("10"),
                        buildItem("Item", new BigDecimal("3"), new BigDecimal("100.00"))));

        assertThat(response.getSubtotal()).isEqualByComparingTo("300.00");
        assertThat(response.getTaxAmount()).isEqualByComparingTo("30.00");
        assertThat(response.getTotal()).isEqualByComparingTo("330.00");
    }

    @Test
    void updateInvoice_nonExistentId_throwsResourceNotFoundException() {
        when(invoiceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.updateInvoice(99L,
                buildRequest(new BigDecimal("0"),
                        buildItem("Item", BigDecimal.ONE, new BigDecimal("10.00")))))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void updateInvoice_updatesClientFields() {
        Invoice existing = buildSavedInvoice(1L, "FAC-2026-0001", InvoiceStatus.PENDING);
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(existing));
        stubSaveReturnsSameInvoice();

        InvoiceRequestDTO dto = buildRequest(new BigDecimal("0"),
                buildItem("Item", BigDecimal.ONE, new BigDecimal("10.00")));
        dto.setClientName("New Client");
        dto.setClientEmail("new@client.com");

        InvoiceResponseDTO response = invoiceService.updateInvoice(1L, dto);

        assertThat(response.getClientName()).isEqualTo("New Client");
        assertThat(response.getClientEmail()).isEqualTo("new@client.com");
    }

    // ── A5: deleteInvoice ─────────────────────────────────────────────────────

    @Test
    void deleteInvoice_existingId_delegatesToRepository() {
        Invoice existing = buildSavedInvoice(1L, "FAC-2026-0001", InvoiceStatus.PENDING);
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(existing));

        invoiceService.deleteInvoice(1L);

        verify(invoiceRepository, times(1)).delete(existing);
    }

    @Test
    void deleteInvoice_nonExistentId_throwsResourceNotFoundException() {
        when(invoiceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.deleteInvoice(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ── A6: getInvoiceById ────────────────────────────────────────────────────

    @Test
    void getInvoiceById_existingId_returnsMappedDto() {
        Invoice existing = buildSavedInvoice(1L, "FAC-2026-0001", InvoiceStatus.PAID);
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(existing));

        InvoiceResponseDTO response = invoiceService.getInvoiceById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getInvoiceNumber()).isEqualTo("FAC-2026-0001");
        assertThat(response.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(response.getClientName()).isEqualTo("Acme Corp");
    }

    @Test
    void getInvoiceById_nonExistentId_throwsResourceNotFoundException() {
        when(invoiceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.getInvoiceById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ── A7: getAllInvoices ────────────────────────────────────────────────────

    @Test
    void getAllInvoices_returnsAllInvoicesAsDtos() {
        Invoice inv1 = buildSavedInvoice(1L, "FAC-2026-0001", InvoiceStatus.PENDING);
        Invoice inv2 = buildSavedInvoice(2L, "FAC-2026-0002", InvoiceStatus.PAID);
        Pageable pageable = PageRequest.of(0, 50);
        when(invoiceRepository.findAllByOrderByIssueDateDesc(pageable))
                .thenReturn(new PageImpl<>(List.of(inv1, inv2), pageable, 2));

        Page<InvoiceResponseDTO> result = invoiceService.getAllInvoices(pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getInvoiceNumber()).isEqualTo("FAC-2026-0001");
        assertThat(result.getContent().get(1).getInvoiceNumber()).isEqualTo("FAC-2026-0002");
    }

    @Test
    void getAllInvoices_emptyRepository_returnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 50);
        when(invoiceRepository.findAllByOrderByIssueDateDesc(pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        Page<InvoiceResponseDTO> result = invoiceService.getAllInvoices(pageable);

        assertThat(result.getContent()).isNotNull().isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    // ── A8: updateStatus ──────────────────────────────────────────────────────

    @Test
    void updateStatus_toPaid_updatesStatusAndPersists() {
        Invoice existing = buildSavedInvoice(1L, "FAC-2026-0001", InvoiceStatus.PENDING);
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(existing));
        stubSaveReturnsSameInvoice();

        InvoiceResponseDTO response = invoiceService.updateStatus(1L, InvoiceStatus.PAID);

        assertThat(response.getStatus()).isEqualTo(InvoiceStatus.PAID);
        verify(invoiceRepository, times(1)).save(existing);
    }

    @Test
    void updateStatus_toCancelled_updatesStatus() {
        Invoice existing = buildSavedInvoice(1L, "FAC-2026-0001", InvoiceStatus.PENDING);
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(existing));
        stubSaveReturnsSameInvoice();

        InvoiceResponseDTO response = invoiceService.updateStatus(1L, InvoiceStatus.CANCELLED);

        assertThat(response.getStatus()).isEqualTo(InvoiceStatus.CANCELLED);
    }

    @Test
    void updateStatus_nonExistentId_throwsResourceNotFoundException() {
        when(invoiceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.updateStatus(99L, InvoiceStatus.PAID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }
}
