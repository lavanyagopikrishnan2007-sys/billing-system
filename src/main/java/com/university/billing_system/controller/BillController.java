package com.university.billing_system.controller;

import com.university.billing_system.dto.AddItemRequest;
import com.university.billing_system.dto.BillResponse;
import com.university.billing_system.dto.CreateBillRequest;
import com.university.billing_system.service.BillService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bills")
public class BillController {

    private final BillService billService;

    public BillController(BillService billService) {
        this.billService = billService;
    }

    // Create an empty draft bill
    @PostMapping
    public ResponseEntity<BillResponse> create() {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BillResponse.from(billService.createBill()));
    }

    // Create a bill with all items and finalize it in one request
    @PostMapping("/checkout")
    public ResponseEntity<BillResponse> checkout(@Valid @RequestBody CreateBillRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BillResponse.from(billService.checkout(request.items())));
    }

    // Add an item to a draft bill (stock is only checked, not reduced)
    @PostMapping("/{billId}/items")
    public BillResponse addItem(@PathVariable Long billId,
                                @Valid @RequestBody AddItemRequest request) {
        return BillResponse.from(
                billService.addItem(billId, request.productId(), request.quantity()));
    }

    // Finalize the bill (stock is reduced here)
    @PostMapping("/{billId}/finalize")
    public BillResponse finalizeBill(@PathVariable Long billId) {
        return BillResponse.from(billService.finalizeBill(billId));
    }

    // List all bills, or filter by creation date: /api/bills?date=2026-09-28
    @GetMapping
    public List<BillResponse> list(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return billService.getBills(date).stream().map(BillResponse::from).toList();
    }

    @GetMapping("/{billId}")
    public BillResponse getBill(@PathVariable Long billId) {
        return BillResponse.from(billService.getBill(billId));
    }

    // Total sales for one day, e.g. /api/bills/sales?date=2026-09-28
    @GetMapping("/sales")
    public Map<String, Object> salesForDay(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        BigDecimal total = billService.getTotalSalesForDate(date);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("date", date);
        result.put("totalSales", total);
        return result;
    }
}