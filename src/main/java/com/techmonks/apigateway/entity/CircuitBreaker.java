package com.techmonks.apigateway.entity;

import lombok.Data;

import java.util.Set;

@Data
public class CircuitBreaker {
    private String name;
    private String fallbackUri;
    private Set<String> statusCodes;
    private String routeId;
    private boolean resumeWithoutError;
}
