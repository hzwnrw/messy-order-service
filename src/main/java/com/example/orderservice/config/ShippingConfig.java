package com.example.orderservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "shipping")
public class ShippingConfig {

    private double freeThreshold = 1000.0;
    private double cost = 50.0;

}
