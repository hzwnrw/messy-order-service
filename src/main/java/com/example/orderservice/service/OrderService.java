package com.example.orderservice.service;

import com.example.orderservice.config.ProductServiceConfig;
import com.example.orderservice.config.ShippingConfig;
import com.example.orderservice.dto.*;
import com.example.orderservice.model.Order;
import com.example.orderservice.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final ProductServiceConfig productConfig;
    private final ShippingConfig shippingConfig;
    private final OrderRepository or;
    private final RestTemplate rt;

    public OrderService(
            ProductServiceConfig productConfig,
            ShippingConfig shippingConfig,
            OrderRepository or,
            RestTemplate rt
    ) {
        this.productConfig = productConfig;
        this.shippingConfig = shippingConfig;
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

        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }

        String customerType = request.getCustomerType();
        double discount = DISCOUNT_MAP.getOrDefault(customerType, 0.0);

        Set<Long> productIds = request.getItems().stream()
                .map(OrderItemRequest::getProductId)
                .collect(Collectors.toSet());

        Map<Long, ProductResponse> productCache = new HashMap<>();

        for (Long productId : productIds) {
            String url = productConfig.getUrl() + productConfig.getPath() + "/{id}";

            try {
                ProductResponse product = rt.getForObject(url, ProductResponse.class, productId);

                if (product == null) {
                    throw new IllegalArgumentException("Product not found: " + productId);
                }

                productCache.put(productId, product);

            } catch (HttpClientErrorException.NotFound e) {
                throw new IllegalArgumentException("Product not found: " + productId);
            } catch (RestClientException e) {
                throw new IllegalStateException(
                        "Product service unavailable for product: " + productId, e);
            }
        }

        double total = 0.0;

        for (OrderItemRequest item : request.getItems()) {

            if (item.getQuantity() <= 0) {
                throw new IllegalArgumentException(
                        "Invalid quantity for product: " + item.getProductId());
            }

            ProductResponse product = productCache.get(item.getProductId());

            if (product.getStock() <= 0) {
                throw new IllegalArgumentException(
                        "Product out of stock: " + item.getProductId());
            }

            if (item.getQuantity() > product.getStock()) {
                throw new IllegalArgumentException(
                        "Not enough stock for product: " + item.getProductId());
            }

            total += product.getPrice() * item.getQuantity();
        }

        double finalTotal = total - (total * discount);

        if (finalTotal < shippingConfig.getFreeThreshold()) {
            finalTotal += shippingConfig.getCost();
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

        String message = STATUS_MESSAGE_MAP.getOrDefault(
                order.getStatus(), "Unknown status");

        return new OrderDetailResponse(
                order.getId(),
                order.getStatus(),
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
