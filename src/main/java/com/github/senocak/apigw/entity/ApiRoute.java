package com.github.senocak.apigw.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Data
@Document("apiRoutes")
public class ApiRoute {
    @Id
    private String id;
    private String routeIdentifier;
    private String uri;
    private String method;
    private String path;
    private List<String> headers;
    private String host;
    private String body;
    private List<String> requestHeaders;
    private List<String> responseHeader;
    private Integer retry;
    private String rewritePath;
    private boolean requestRateLimiter;
    private Integer setStatus;
    private CircuitBreaker circuitBreaker;
}

