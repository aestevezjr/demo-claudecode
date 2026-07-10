package com.facturas.api.service.impl;

import com.facturas.api.dto.InvoiceItemDTO;
import com.facturas.api.dto.InvoiceItemRequestDTO;
import com.facturas.api.dto.InvoiceRequestDTO;
import com.facturas.api.dto.InvoiceResponseDTO;
import com.facturas.api.entity.Invoice;
import com.facturas.api.entity.InvoiceItem;
import com.facturas.api.entity.InvoiceSequence;
import com.facturas.api.enums.InvoiceStatus;
import com.facturas.api.exception.ResourceNotFoundException;
import com.facturas.api.repository.InvoiceRepository;
import com.facturas.api.repository.InvoiceSequenceRepository;
import com.facturas.api.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceSequenceRepository invoiceSequenceRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<InvoiceResponseDTO> getAllInvoices(Pageable pageable, String search) {
        Page<Invoice> page = (search == null || search.isBlank())
                ? invoiceRepository.findAllByOrderByIssueDateDesc(pageable)
                : invoiceRepository.findBySearchTerm(search.trim(), pageable);
        return page.map(this::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponseDTO getInvoiceById(Long id) {
        return toResponseDTO(findOrThrow(id));
    }

    @Override
    @Transactional
    public InvoiceResponseDTO createInvoice(InvoiceRequestDTO dto) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(generateInvoiceNumber());
        mapDtoToEntity(dto, invoice);
        computeTotals(invoice);
        return toResponseDTO(invoiceRepository.save(invoice));
    }

    @Override
    @Transactional
    public InvoiceResponseDTO updateInvoice(Long id, InvoiceRequestDTO dto) {
        Invoice invoice = findOrThrow(id);
        invoice.getItems().clear();
        mapDtoToEntity(dto, invoice);
        computeTotals(invoice);
        return toResponseDTO(invoiceRepository.save(invoice));
    }

    @Override
    @Transactional
    public void deleteInvoice(Long id) {
        invoiceRepository.delete(findOrThrow(id));
    }

    @Override
    @Transactional
    public InvoiceResponseDTO updateStatus(Long id, InvoiceStatus status) {
        Invoice invoice = findOrThrow(id);
        invoice.setStatus(status);
        return toResponseDTO(invoiceRepository.save(invoice));
    }

    private String generateInvoiceNumber() {
        int year = LocalDate.now().getYear();
        InvoiceSequence seq = invoiceSequenceRepository.findByYearWithLock(year)
                .orElseGet(() -> new InvoiceSequence(year, 0L));
        seq.setLastSeq(seq.getLastSeq() + 1);
        invoiceSequenceRepository.save(seq);
        return String.format("FAC-%d-%04d", year, seq.getLastSeq());
    }

    private Invoice findOrThrow(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Factura no encontrada con id: " + id));
    }

    private void mapDtoToEntity(InvoiceRequestDTO dto, Invoice invoice) {
        invoice.setIssueDate(dto.getIssueDate());
        invoice.setStatus(dto.getStatus() != null ? dto.getStatus() : InvoiceStatus.PENDING);
        invoice.setClientName(sanitize(dto.getClientName()));
        invoice.setClientEmail(dto.getClientEmail());
        invoice.setClientAddress(sanitize(dto.getClientAddress()));
        invoice.setTaxRate(dto.getTaxRate());
        invoice.setNotes(sanitize(dto.getNotes()));

        IntStream.range(0, dto.getItems().size()).forEach(i -> {
            InvoiceItemRequestDTO itemDto = dto.getItems().get(i);
            InvoiceItem item = new InvoiceItem();
            item.setDescription(sanitize(itemDto.getDescription()));
            item.setQuantity(itemDto.getQuantity());
            item.setUnitPrice(itemDto.getUnitPrice());
            item.setSortOrder(i);
            item.setInvoice(invoice);
            invoice.getItems().add(item);
        });
    }

    private String sanitize(String input) {
        if (input == null) return null;
        return input
                .replaceAll("(?i)<script[^>]*>[\\s\\S]*?</script>", "")
                .replaceAll("<[^>]+>", "")
                .trim();
    }

    private void computeTotals(Invoice invoice) {
        BigDecimal subtotal = invoice.getItems().stream()
                .map(item -> {
                    BigDecimal lineTotal = item.getQuantity().multiply(item.getUnitPrice())
                            .setScale(2, RoundingMode.HALF_UP);
                    item.setLineTotal(lineTotal);
                    return lineTotal;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal taxAmount = subtotal
                .multiply(invoice.getTaxRate())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        invoice.setSubtotal(subtotal);
        invoice.setTaxAmount(taxAmount);
        invoice.setTotal(subtotal.add(taxAmount));
    }

    private InvoiceResponseDTO toResponseDTO(Invoice invoice) {
        InvoiceResponseDTO dto = new InvoiceResponseDTO();
        dto.setId(invoice.getId());
        dto.setInvoiceNumber(invoice.getInvoiceNumber());
        dto.setIssueDate(invoice.getIssueDate());
        dto.setStatus(invoice.getStatus());
        dto.setClientName(invoice.getClientName());
        dto.setClientEmail(invoice.getClientEmail());
        dto.setClientAddress(invoice.getClientAddress());
        dto.setSubtotal(invoice.getSubtotal());
        dto.setTaxRate(invoice.getTaxRate());
        dto.setTaxAmount(invoice.getTaxAmount());
        dto.setTotal(invoice.getTotal());
        dto.setNotes(invoice.getNotes());
        dto.setCreatedAt(invoice.getCreatedAt());
        dto.setUpdatedAt(invoice.getUpdatedAt());

        List<InvoiceItemDTO> itemDTOs = invoice.getItems().stream().map(item -> {
            InvoiceItemDTO itemDto = new InvoiceItemDTO();
            itemDto.setId(item.getId());
            itemDto.setDescription(item.getDescription());
            itemDto.setQuantity(item.getQuantity());
            itemDto.setUnitPrice(item.getUnitPrice());
            itemDto.setLineTotal(item.getLineTotal());
            itemDto.setSortOrder(item.getSortOrder());
            return itemDto;
        }).collect(Collectors.toList());

        dto.setItems(itemDTOs);
        return dto;
    }
}
