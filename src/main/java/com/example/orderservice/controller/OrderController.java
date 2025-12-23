package com.example.orderservice.controller;

import com.example.orderservice.dto.OrderDetailResponse;
import com.example.orderservice.dto.OrderRequest;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.model.Order;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@RestController
public class OrderController {
    
    @Autowired
    private OrderRepository or;
    
    @Autowired
    private RestTemplate rt;

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }
    // BAD: Everything in controller, no service layer
    // BAD: Magic numbers, hardcoded URLs, no error handling
    // BAD: Using Map instead of DTOs
    @PostMapping("/order")
    public ResponseEntity<OrderResponse> makeOrder(
            @RequestBody OrderRequest request) {

        OrderResponse response = orderService.makeOrder(request);
        return ResponseEntity.ok(response);
    }
    
    // BAD: Another giant method with duplicated logic
    @GetMapping("/order/{id}")
    public ResponseEntity<OrderDetailResponse> getOrder(
            @PathVariable Long id) {

        return ResponseEntity.ok(orderService.getOrder(id));
    }
    
    // BAD: Another method with performance issues
    @GetMapping("/analytics/popular")
    public ResponseEntity<Map<Long, Integer>> getPopularProducts() {
        return ResponseEntity.ok(orderService.getPopularProducts());
    }
    
    // BAD: No validation, no proper response
    @PutMapping("/order/{id}/status")
    public ResponseEntity<String> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> req) {

        try {
            String newStatus = req != null ? req.get("status") : null;
            orderService.updateOrderStatus(id, newStatus);
            return ResponseEntity.ok("Status updated");

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
