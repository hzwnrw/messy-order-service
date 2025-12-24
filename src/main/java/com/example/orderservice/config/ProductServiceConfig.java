package com.example.orderservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "product.service")
public class ProductServiceConfig {

    private String url = "http://localhost:8081";
    private String path = "/api/products";

}
