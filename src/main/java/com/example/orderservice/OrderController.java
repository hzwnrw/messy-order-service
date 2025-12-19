package com.example.orderservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@RestController
public class OrderController {
    
    @Autowired
    private OrderRepository or;
    
    @Autowired
    private RestTemplate rt;
    
    // BAD: Everything in controller, no service layer
    // BAD: Magic numbers, hardcoded URLs, no error handling
    // BAD: Using Map instead of DTOs
    @PostMapping("/order")
    public String makeOrder(@RequestBody Map<String, Object> req) {
        
        // BAD: Long if-else that should be strategy/enum or HashMap
        String customerType = (String) req.get("customerType");
        double discount = 0;
        if (customerType.equals("VIP")) {
            discount = 0.15;
        } else if (customerType.equals("PREMIUM")) {
            discount = 0.10;
        } else if (customerType.equals("REGULAR")) {
            discount = 0.05;
        } else if (customerType.equals("NEW")) {
            discount = 0.02;
        } else {
            discount = 0;
        }

        // BAD: Using raw ArrayList, should use proper collection
        ArrayList items = (ArrayList) req.get("items");
        double total = 0;
        
        // BAD: No duplicate detection - should use Set!
        // BAD: Nested loops, bad variable names, no Stream API
        // BAD: N+1 query problem - API call inside loop!
        for (int i = 0; i < items.size(); i++) {
            Map item = (Map) items.get(i);
            Long pid = Long.parseLong(item.get("productId").toString());
            int q = Integer.parseInt(item.get("quantity").toString());
            
            // BAD: Hardcoded URL, no error handling
            String url = "http://localhost:8081/api/products/" + pid;
            Map prod = rt.getForObject(url, Map.class);
            
            if (prod != null) {
                double p = Double.parseDouble(prod.get("price").toString());
                int stock = Integer.parseInt(prod.get("stock").toString());
                
                // BAD: Deep nesting, should extract method
                if (stock > 0) {
                    if (q <= stock) {
                        total = total + (p * q);
                    } else {
                        return "Error: Not enough stock for product " + pid;
                    }
                } else {
                    return "Error: Product " + pid + " out of stock";
                }
            }
        }
        
        // BAD: Magic numbers, unclear calculation
        double finalTotal = total - (total * discount);
        if (finalTotal > 1000) {
            finalTotal = finalTotal - 50; // free shipping
        }
        
        // BAD: No proper entity, saving raw data
        Order o = new Order();
        o.setCustomerType(customerType);
        o.setTotal(finalTotal);
        o.setStatus("PENDING");
        or.save(o);
        
        // BAD: Returning String instead of proper response DTO
        return "Order created: " + o.getId();
    }
    
    // BAD: Another giant method with duplicated logic
    @GetMapping("/order/{id}")
    public String getOrder(@PathVariable Long id) {
        Order o = or.findById(id).get(); // BAD: No error handling
        
        String status = o.getStatus();
        String msg = "";
        
        // BAD: Should use switch or enum
        if (status.equals("PENDING")) {
            msg = "Your order is being processed";
        } else if (status.equals("CONFIRMED")) {
            msg = "Your order is confirmed";
        } else if (status.equals("SHIPPED")) {
            msg = "Your order is on the way";
        } else if (status.equals("DELIVERED")) {
            msg = "Your order has been delivered";
        } else if (status.equals("CANCELLED")) {
            msg = "Your order was cancelled";
        } else {
            msg = "Unknown status";
        }
        
        return "Order " + id + ": " + msg + " | Total: $" + o.getTotal();
    }
    
    // BAD: Another method with performance issues
    @GetMapping("/analytics/popular")
    public String getPopularProducts() {
        List<Order> allOrders = or.findAll();
        
        // BAD: Nested loops to count products - O(n²)
        // BAD: Should use HashMap to count!
        ArrayList productCounts = new ArrayList();
        
        for (Order order : allOrders) {
            for (String item : order.getItems()) {
                // Simulating product ID extraction
                Long productId = Long.parseLong(item.split(":")[0]);
                
                // BAD: Linear search every time
                boolean found = false;
                for (int i = 0; i < productCounts.size(); i++) {
                    Map count = (Map) productCounts.get(i);
                    if (count.get("productId").equals(productId)) {
                        count.put("count", (Integer) count.get("count") + 1);
                        found = true;
                        break;
                    }
                }
                
                if (!found) {
                    Map newCount = new HashMap();
                    newCount.put("productId", productId);
                    newCount.put("count", 1);
                    productCounts.add(newCount);
                }
            }
        }
        
        return productCounts.toString();
    }
    
    // BAD: No validation, no proper response
    @PutMapping("/order/{id}/status")
    public String updateStatus(@PathVariable Long id, @RequestBody Map<String, String> req) {
        Order o = or.findById(id).get(); // BAD: No error handling
        String newStatus = req.get("status");
        o.setStatus(newStatus);
        or.save(o);
        return "Status updated";
    }
}
