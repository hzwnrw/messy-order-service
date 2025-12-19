# Order Service - Code Refactoring Exercise

## About This Project

This is a Spring Boot application for managing customer orders. The code **works** but has many quality issues that make it hard to maintain and extend.

## How to Run

### Prerequisites
- Java 17 or higher
- Maven 3.6+
- Your favorite IDE (IntelliJ IDEA recommended)

### Steps
```bash
# Clone/download the project
cd messy-order-service

# Run the application
mvn spring-boot:run

# Or from your IDE, run OrderServiceApplication.java
```

The application will start on **http://localhost:8082**

### Test the API

**Create an order:**
```bash
curl -X POST http://localhost:8082/order \
  -H "Content-Type: application/json" \
  -d '{
    "customerType": "VIP",
    "items": [
      {"productId": "1", "quantity": 2},
      {"productId": "2", "quantity": 1}
    ]
  }'
```

**Get an order:**
```bash
curl http://localhost:8082/order/1
```

**Note:** Product Service calls will fail since we don't have a real Product Service running. That's okay for this exercise!

## Your Task

Review this code and identify issues.

### What to Look For

Think about:
- **Clean Code**: Are variable names clear? Are methods too long? Magic numbers?
- **Data Structures**: Are we using the right collections (List/Set/Map)?
- **REST API**: Are we following best practices? DTOs? Status codes?
- **Spring Boot**: Configuration? Exception handling? Proper structure?
