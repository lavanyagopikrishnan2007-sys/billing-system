package com.university.billing_system.service;

import com.university.billing_system.dto.AddItemRequest;
import com.university.billing_system.entity.*;
import com.university.billing_system.exception.*;
import com.university.billing_system.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class BillService {

    private final BillRepository billRepository;
    private final ProductRepository productRepository;

    private final EntityManager entityManager;

    public BillService(BillRepository billRepository,
                       ProductRepository productRepository,
                       EntityManager entityManager) {
        this.billRepository = billRepository;
        this.productRepository = productRepository;
        this.entityManager = entityManager;
    }

    @Transactional
    public Bill createBill() {
        return billRepository.save(new Bill());
    }

    @Transactional(readOnly = true)
    public Bill getBill(Long billId) {
        return billRepository.findById(billId)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found with id " + billId));
    }

    // Lists all bills, or only the bills created on one date
    @Transactional(readOnly = true)
    public List<Bill> getBills(LocalDate date) {
        if (date == null) {
            return billRepository.findAll();
        }
        return billRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                date.atStartOfDay(), date.plusDays(1).atStartOfDay());
    }

    // Adds an item to a DRAFT bill. Stock is only CHECKED here, never reduced.
    @Transactional
    public Bill addItem(Long billId, Long productId, int quantity) {
        Bill bill = getBill(billId);
        requireDraft(bill);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id " + productId));

        BillItem existing = bill.getItems().stream()
                .filter(i -> i.getProduct().getProductId().equals(productId))
                .findFirst().orElse(null);

        int totalQty = quantity + (existing != null ? existing.getQuantity() : 0);

        if (!product.hasEnoughStock(totalQty)) {
            throw new InsufficientStockException(
                    "Insufficient stock for '" + product.getName() + "': requested "
                            + totalQty + ", available " + product.getStockQuantity());
        }

        if (existing != null) {
            existing.setQuantity(totalQty);
        } else {
            bill.addItem(new BillItem(product, quantity));
        }

        bill.recalculateTotal();
        return billRepository.save(bill);
    }

    // Creates a bill with all items and finalizes it in ONE transaction.
    // If anything fails, nothing is saved and no stock is reduced.
    @Transactional
    public Bill checkout(List<AddItemRequest> items) {
        Bill bill = createBill();
        for (AddItemRequest request : items) {
            addItem(bill.getId(), request.productId(), request.quantity());
        }
        return finalizeBill(bill.getId());
    }

    // Finalizes the bill: re-checks stock, then reduces it.
    // If any item fails, everything rolls back (no partial deductions).
    @Transactional
    public Bill finalizeBill(Long billId) {
        Bill bill = getBill(billId);
        requireDraft(bill);

        if (bill.getItems().isEmpty()) {
            throw new InvalidBillStateException("Cannot finalize an empty bill");
        }

        List<BillItem> sorted = bill.getItems().stream()
                .sorted(Comparator.comparing(i -> i.getProduct().getProductId()))
                .toList();

        for (BillItem item : sorted) {
            Long productId = item.getProduct().getProductId();
            Product product = productRepository.findByIdForUpdate(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id " + productId));
            // In checkout(), this product was already loaded by addItem() in the same
            // transaction, so Hibernate could hand back an old copy of the stock.
            // Refreshing under the lock re-reads the latest stock from the database.
            entityManager.refresh(product, LockModeType.PESSIMISTIC_WRITE);
            product.reduceStock(item.getQuantity());
        }

        bill.recalculateTotal();
        bill.setStatus(BillStatus.FINALIZED);
        bill.setFinalizedAt(LocalDateTime.now());
        return billRepository.save(bill);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalSalesForDate(LocalDate date) {
        return billRepository.sumSalesBetween(
                BillStatus.FINALIZED,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay());
    }

    private void requireDraft(Bill bill) {
        if (bill.isFinalized()) {
            throw new InvalidBillStateException("Bill " + bill.getId() + " is already finalized");
        }
    }
}