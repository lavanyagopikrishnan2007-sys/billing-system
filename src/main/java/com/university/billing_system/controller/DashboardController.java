package com.university.billing_system.controller;

import com.university.billing_system.repository.BillRepository;
import com.university.billing_system.repository.ProductRepository;
import com.university.billing_system.service.BillService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final ProductRepository productRepository;
    private final BillRepository billRepository;
    private final BillService billService;

    public DashboardController(ProductRepository productRepository,
                               BillRepository billRepository,
                               BillService billService) {
        this.productRepository = productRepository;
        this.billRepository = billRepository;
        this.billService = billService;
    }

    @GetMapping
    public Map<String, Object> dashboard() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalProducts", productRepository.count());
        result.put("totalBills", billRepository.count());
        result.put("lowStockProducts", productRepository.findBelowReorderLevel().size());
        result.put("todaysSales", billService.getTotalSalesForDate(LocalDate.now()));
        return result;
    }
}