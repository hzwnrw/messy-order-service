package com.example.orderservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "shipping")
public class ShippingConfig {

    private double freeThreshold = 100.0;
    private double cost = 10.0;

    public double getFreeThreshold() {
        return freeThreshold;
    }

    public void setFreeThreshold(double freeThreshold) {
        this.freeThreshold = freeThreshold;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }
}
