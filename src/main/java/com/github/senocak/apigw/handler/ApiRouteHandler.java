package com.github.senocak.apigw.handler;

import com.github.senocak.apigw.configuration.GatewayRoutesRefresher;
import com.github.senocak.apigw.entity.ApiRoute;
import com.github.senocak.apigw.service.RouteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static org.springframework.web.reactive.function.BodyInserters.fromValue;

@Component
public class ApiRouteHandler {
    private final RouteService routeService;
    private final GatewayRoutesRefresher gatewayRoutesRefresher;

    public ApiRouteHandler(final RouteService routeService, final GatewayRoutesRefresher gatewayRoutesRefresher) {
        this.routeService = routeService;
        this.gatewayRoutesRefresher = gatewayRoutesRefresher;
    }

    public Mono<ServerResponse> create(ServerRequest serverRequest) {
        Mono<ApiRoute> apiRoute = serverRequest.bodyToMono(ApiRoute.class);
        return apiRoute.flatMap(route ->
                ServerResponse.status(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(routeService.create(route), ApiRoute.class));
    }

    public Mono<ServerResponse> getAll(ServerRequest serverRequest) {
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(routeService.getAll(), ApiRoute.class);
    }

    public Mono<ServerResponse> getById(ServerRequest serverRequest) {
        final String apiId = serverRequest.pathVariable("routeId");
        Mono<ApiRoute> apiRoute = routeService.getById(apiId);
        return apiRoute.flatMap(route -> ServerResponse.ok()
                        .body(fromValue(route)))
                .switchIfEmpty(ServerResponse.notFound()
                        .build());
    }

    public Mono<ServerResponse> refreshRoutes(ServerRequest serverRequest) {
        gatewayRoutesRefresher.refreshRoutes();
        return ServerResponse.ok().body(BodyInserters.fromObject("Routes reloaded successfully"));
    }
}
