package com.example.orderservice.service;

import com.example.orderservice.dto.OrderDetailResponse;
import com.example.orderservice.dto.OrderItemRequest;
import com.example.orderservice.dto.OrderRequest;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.model.Order;
import com.example.orderservice.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class OrderService {

    @Value("${product.service.url}")
    private String productServiceUrl;

    @Value("${product.service.path}")
    private String productPath;

    @Value("${shipping.free.threshold}")
    private double freeShippingThreshold;

    @Value("${shipping.cost}")
    private double shippingCost;

    private final OrderRepository or;
    private final RestTemplate rt;

    public OrderService(OrderRepository or, RestTemplate rt) {
        this.or = or;
        this.rt = rt;
    }

    private static final Map<String, Double> DISCOUNT_MAP = new HashMap<>();
    static {
        DISCOUNT_MAP.put("VIP", 0.15);
        DISCOUNT_MAP.put("PREMIUM", 0.10);
        DISCOUNT_MAP.put("REGULAR", 0.05);
        DISCOUNT_MAP.put("NEW", 0.02);
    }

    private static final Map<String, String> STATUS_MESSAGE_MAP = new HashMap<>();
    static {
        STATUS_MESSAGE_MAP.put("PENDING", "Your order is being processed");
        STATUS_MESSAGE_MAP.put("CONFIRMED", "Your order is confirmed");
        STATUS_MESSAGE_MAP.put("SHIPPED", "Your order is on the way");
        STATUS_MESSAGE_MAP.put("DELIVERED", "Your order has been delivered");
        STATUS_MESSAGE_MAP.put("CANCELLED", "Your order was cancelled");
    }

    public OrderResponse makeOrder(OrderRequest request) {

        String customerType = request.getCustomerType();
        double discount = DISCOUNT_MAP.getOrDefault(customerType, 0.0);

        Set<Long> productIds = new HashSet<>();
        for (OrderItemRequest item : request.getItems()) {
            productIds.add(item.getProductId());
        }

        Map<Long, Map<String, Object>> productCache = new HashMap<>();

        for (Long productId : productIds) {
            String url = productServiceUrl + productPath;
            Map<String, Object> product = rt.getForObject(url, Map.class);

            if (product == null) {
                throw new IllegalArgumentException("Product not found: " + productId);
            }

            productCache.put(productId, product);
        }

        double total = 0;

        for (OrderItemRequest item : request.getItems()) {

            Map<String, Object> product = productCache.get(item.getProductId());

            double price = Double.parseDouble(product.get("price").toString());
            int stock = Integer.parseInt(product.get("stock").toString());

            if (stock <= 0) {
                throw new IllegalArgumentException(
                        "Product out of stock: " + item.getProductId());
            }

            if (item.getQuantity() > stock) {
                throw new IllegalArgumentException(
                        "Not enough stock for product: " + item.getProductId());
            }

            total += price * item.getQuantity();
        }

        double finalTotal = total - (total * discount);

        if (finalTotal > freeShippingThreshold) {
            finalTotal -= shippingCost;
        }

        Order order = new Order();
        order.setCustomerType(customerType);
        order.setTotal(finalTotal);
        order.setStatus("PENDING");

        or.save(order);

        return new OrderResponse(
                order.getId(),
                order.getStatus(),
                order.getTotal()
        );
    }

    public OrderDetailResponse getOrder(Long id) {
        Order order = or.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Order not found"));

        String status = order.getStatus();
        String message = STATUS_MESSAGE_MAP.getOrDefault(
                status, "Unknown status");

        return new OrderDetailResponse(
                order.getId(),
                status,
                message,
                order.getTotal()
        );
    }

    public Map<Long, Integer> getPopularProducts() {
        List<Order> allOrders = or.findAll();
        Map<Long, Integer> productCounts = new HashMap<>();

        for (Order order : allOrders) {
            for (String item : order.getItems()) {
                Long productId = Long.parseLong(item.split(":")[0]);
                productCounts.put(
                        productId,
                        productCounts.getOrDefault(productId, 0) + 1
                );
            }
        }

        return productCounts;
    }

    public void updateOrderStatus(Long id, String newStatus) {
        if (newStatus == null || newStatus.isBlank()) {
            throw new IllegalArgumentException("Missing or empty status");
        }

        Order order = or.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Order not found"));

        order.setStatus(newStatus);
        or.save(order);
    }
}
