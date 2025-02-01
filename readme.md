# API Gateway Using Spring Cloud Gateway with Database Configuration

## Description
This project is an API Gateway built using Spring Cloud Gateway, Spring Boot, and reactive programming with WebFlux. It includes features such as route configuration, request and response logging, rate limiting with Redis, and circuit breaker patterns with Resilience4j.

## Prerequisites
- JDK 21
- Gradle 8.10
- MongoDB
- Redis

## Getting Started

### Clone the repository
```bash
git clone https://github.com/senocak/Spring-Cloud-Gateway.git
cd Spring-Cloud-Gateway
```

### Build the project
To build the project, run the following command:
```bash
./gradlew build
```

### Run the project
To run the project, use the following command:
```bash
./gradlew bootRun
```

### Configuration
The application configuration is managed through application.yml located in resources. Update the MongoDB and Redis connection details as needed.

```yaml
spring:
  data:
    mongodb:
      uri: mongodb://your-mongodb-uri
      database: your-database-name
    redis:
      host: your-redis-host
      port: your-redis-port
      password: your-redis-password
```

##### Endpoints The following endpoints are available:

- POST /routes - Create a new route
- GET /routes - Get all routes
- GET /routes/{routeId} - Get a route by ID
- GET /routes/refresh-routes - Refresh routes
- GET /fallback - Fallback endpoint

### Dependencies
This project uses the following dependencies:
- Spring Boot 3.4.2
- Spring Cloud Gateway
- Spring Boot Starter WebFlux
- Spring Boot Starter Data MongoDB Reactive
- Spring Boot Starter Data Redis Reactive
- Spring Cloud Circuit Breaker with Resilience4j
- Lombok

### Contributing
- Fork the repository
- Create a new branch (git checkout -b feature-branch)
- Make your changes
- Commit your changes (git commit -m 'Add some feature')
- Push to the branch (git push origin feature-branch)
- Open a pull request

### Reference Documentation
For further reference, please consider the following sections:

### Official Gradle documentation
Spring Boot Gradle Plugin Reference Guide
Create an OCI image
Reactive Gateway

### Example rest api
```java
@SpringBootApplication
@RestController
public class OrderApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }

    @RequestMapping("orders")
    public Object getOrders(HttpServletRequest request) throws InterruptedException {
        System.out.println("Ping from order service");
        if (new Random().nextBoolean())
            return ResponseEntity.badRequest().body("Order service is down");
        Map<String, Object> map = new HashMap<>();
        request.getHeaderNames()
                .asIterator()
                .forEachRemaining(header -> map.put(header, request.getHeader(header)));
        map.put("method", request.getMethod());
        map.put("URI", request.getRequestURI());
        map.put("message", "Order service is up and running");
        return map;
    }
}
```

### Test 

```bash
curl --location 'http://localhost:8080/routes' \
--header 'Content-Type: application/json' \
--data '{
    "routeIdentifier": "routeId",
    "uri": "http://localhost:8081",
    "method": "GET",
    "path": "/custom/orders/**",
    "headers": [
        "anil:senocak"
    ],
    "host": "**.turkcell.com.tr",
    "body": "contains:hello",
    "requestHeaders": [
        "key:value"
    ],
    "responseHeader": [
        "response1:response2"
    ],
    "retry": 4,
    "rewritePath": "/custom(?<segment>.*):/${segment}",
    "requestRateLimiter": true,
    "setStatus": 405,
    "circuitBreaker": {
        "name": "myCircuitBreaker",
        "fallbackUri": "forward:/fallback",
        "statusCodes":[
            "404",
            "INTERNAL_SERVER_ERROR"
        ],
        "routeId": "routeId",
        "resumeWithoutError": true
    }
}'
```

```bash
curl --location --request GET 'http://localhost:8080/routes/refresh-routes'
```

```bash
curl --location --request GET 'http://localhost:8080/custom/orders' \
--header 'anil: senocak' \
--header 'Host: turkcell.com.tr' \
--header 'Content-Type: application/json' \
--data '{
    "hello": "anil"
}'
```