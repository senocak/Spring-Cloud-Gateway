package com.github.senocak.apigw.service;

import com.github.senocak.apigw.entity.ApiRoute;
import com.github.senocak.apigw.repository.RouteRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class RouteService {
    private final RouteRepository routeRepository;

    public RouteService(RouteRepository routeRepository) {
        this.routeRepository = routeRepository;
    }

    public Flux<ApiRoute> getAll() {
        return this.routeRepository.findAll();
    }

    public Mono<ApiRoute> create(ApiRoute apiRoute) {
        return this.routeRepository.save(apiRoute);
    }

    public Mono<ApiRoute> getById(String id) {
        return this.routeRepository.findById(id);
    }
}
