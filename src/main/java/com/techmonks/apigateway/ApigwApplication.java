package com.techmonks.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.reactive.config.EnableWebFlux;

@EnableWebFlux
@SpringBootApplication
public class ApigwApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApigwApplication.class, args);
    }

}
