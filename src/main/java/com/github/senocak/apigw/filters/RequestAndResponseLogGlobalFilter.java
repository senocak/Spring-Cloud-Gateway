package com.github.senocak.apigw.filters;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

@Component
public class RequestAndResponseLogGlobalFilter implements GlobalFilter, Ordered {
    private static final Logger logger = LogManager.getLogger(RequestAndResponseLogGlobalFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        final String originalUri = exchange.getRequest().getPath().toString();
        final Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
        logger.info("Incoming request: {} is routed to id: {}, uri: {}", originalUri, route.getId(), route.getUri());
        return chain.filter(exchange)
                .doOnError(error -> logger.info("Post Error Filter Logic: Request path: {}, due to {}", originalUri,error.getMessage()))
                .then(Mono.fromRunnable(() -> logger.info("Post Filter Logic: HTTP Status Code: {}", exchange.getResponse().getStatusCode().toString())));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
