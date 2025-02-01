package com.github.senocak.apigw.repository;

import com.github.senocak.apigw.entity.ApiRoute;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RouteRepository extends ReactiveCrudRepository<ApiRoute, String> {
}
