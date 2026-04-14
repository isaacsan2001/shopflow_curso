package com.shopflow.orders.infrastructure.rest;

import com.shopflow.orders.domain.model.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/legacy/orders")
public class LegacyOrderController {

    @PersistenceContext
    private EntityManager entityManager;

    private static final Map<String, Integer> discountUsageCount = new HashMap<>();

    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> request) {
        String customerId = (String) request.get("customerId");
        if (customerId == null || customerId.isBlank()) {
            return ResponseEntity.badRequest().body("customerId is required");
        }

        List<Map<String, Object>> rawItems = (List<Map<String, Object>>) request.get("items");
        if (rawItems == null || rawItems.isEmpty()) {
            return ResponseEntity.badRequest().body("items cannot be empty");
        }

        List<OrderItem> items = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (Map<String, Object> rawItem : rawItems) {
            String productId = (String) rawItem.get("productId");
            int quantity = (Integer) rawItem.get("quantity");
            BigDecimal unitPrice = new BigDecimal("29.99");

            items.add(new OrderItem(
                    ProductId.of(productId),
                    quantity,
                    Money.of(unitPrice)
            ));
            subtotal = subtotal.add(unitPrice.multiply(BigDecimal.valueOf(quantity)));
        }

        String discountCode = (String) request.get("discountCode");
        BigDecimal finalAmount = subtotal;

        if (discountCode != null) {
            switch (discountCode) {
                case "WELCOME10" -> finalAmount = subtotal.multiply(new BigDecimal("0.90"));
                case "SUMMER20" -> finalAmount = subtotal.multiply(new BigDecimal("0.80"));
                case "VIP30" -> {
                    if (subtotal.compareTo(new BigDecimal("50")) >= 0) {
                        finalAmount = subtotal.multiply(new BigDecimal("0.70"));
                    }
                }
                case "FLASH50" -> {
                    discountUsageCount.merge(discountCode, 1, Integer::sum);
                    if (discountUsageCount.get(discountCode) <= 100) {
                        finalAmount = subtotal.multiply(new BigDecimal("0.50"));
                    }
                }
            }
        }

        Order order = new Order(
                OrderId.generate(),
                CustomerId.of(customerId),
                items,
                OrderStatus.PENDING,
                Money.of(finalAmount),
                discountCode,
                java.time.Instant.now()
        );

        System.out.println("Order created for customer: " + customerId + " amount: " + finalAmount);

        Map<String, Object> response = new HashMap<>();
        response.put("orderId", order.id().toString());
        response.put("status", order.status().name());
        response.put("amount", finalAmount);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<?> listOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String customerId) {

        List<Map<String, Object>> mockOrders = new ArrayList<>();
        return ResponseEntity.ok(mockOrders);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrder(@PathVariable String id) {
        try {
            OrderId orderId = OrderId.of(id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable String id) {
        return ResponseEntity.ok("Order " + id + " cancelled");

        //prueba
    }
}
