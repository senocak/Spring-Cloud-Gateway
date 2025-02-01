package com.github.senocak.apigw.service;

import com.github.senocak.apigw.entity.ApiRoute;
import com.github.senocak.apigw.entity.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.cloud.gateway.filter.factory.RetryGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.SpringCloudCircuitBreakerFilterFactory;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.BooleanSpec;
import org.springframework.cloud.gateway.route.builder.Buildable;
import org.springframework.cloud.gateway.route.builder.GatewayFilterSpec;
import org.springframework.cloud.gateway.route.builder.PredicateSpec;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class ApiRouteLocatorImpl implements RouteLocator {
    private final RouteLocatorBuilder routeLocatorBuilder;
    private final RouteService routeService;
    private final RedisRateLimiter redisRateLimiter;

    public ApiRouteLocatorImpl(final RouteLocatorBuilder routeLocatorBuilder, final RouteService routeService,
                               final RedisRateLimiter redisRateLimiter) {
        this.routeLocatorBuilder = routeLocatorBuilder;
        this.routeService = routeService;
        this.redisRateLimiter = redisRateLimiter;
    }

    @Bean
    public ReactiveRedisTemplate<String, String> reactiveRedisTemplate(ReactiveRedisConnectionFactory factory) {
        return new ReactiveRedisTemplate<>(factory, RedisSerializationContext.string());
    }

    @Bean
    CircuitBreakerRegistry customCircuitBreakerRegistry() {
        return CircuitBreakerRegistry.ofDefaults();
    }

    @Override
    public Flux<Route> getRoutes() {
        RouteLocatorBuilder.Builder routesBuilder = routeLocatorBuilder.routes();
        return routeService.getAll()
                .map(apiRoute -> routesBuilder.route(String.valueOf(apiRoute.getRouteIdentifier()),
                        predicateSpec -> setPredicateSpec(apiRoute, predicateSpec)))
                .collectList()
                .flatMapMany(builders -> routesBuilder.build()
                        .getRoutes());
    }

    private Buildable<Route> setPredicateSpec(ApiRoute apiRoute, PredicateSpec predicateSpec) {
        BooleanSpec booleanSpec = predicateSpec.path(apiRoute.getPath());
        final String method = apiRoute.getMethod();
        if (method != null)
            booleanSpec.and().method(method);
        final List<String> headers = apiRoute.getHeaders();
        if (headers != null && !headers.isEmpty())
            for (String header: headers) {
                String[] headerParts = header.split(":", 2);
                if (headerParts.length == 2) {
                    String key = headerParts[0];
                    String value = headerParts[1];
                    if (value == null) {
                        booleanSpec.and().header(key);
                    } else if (value.startsWith("regex=")) {
                        booleanSpec.and().header(key, value);
                    }
                    booleanSpec.and().header(key, value);
                }
            }
        final String host = apiRoute.getHost();
        if (host != null)
            booleanSpec.and().host(host);
        final String body = apiRoute.getBody();
        if (body != null) {
            String[] strings = body.split(":", 2);
            booleanSpec.and().readBody(String.class, s -> {
                if (strings.length == 2) {
                    String key = strings[0];
                    String value = strings[1];
                    return switch (key) {
                        case "contains" -> s.contains(value);
                        case "equals" -> s.equals(value);
                        case "startsWith" -> s.startsWith(value);
                        case "endsWith" -> s.endsWith(value);
                        case "matches" -> s.matches(value);
                        default -> throw new IllegalStateException("Unexpected value: " + value);
                    };
                }
                return s.contains(body);
            });
        }
        return booleanSpec.filters(gatewayFilterSpec -> {
            GatewayFilterSpec gwfs = gatewayFilterSpec;
            final List<String> requestHeaders = apiRoute.getRequestHeaders();
            if (requestHeaders != null && !requestHeaders.isEmpty())
                for (String requestHeader: requestHeaders) {
                    String[] strings = requestHeader.split(":", 2);
                    gwfs = gwfs.addRequestHeader(strings[0], strings[1]);
                }
            final List<String> responseHeaders = apiRoute.getResponseHeader();
            if (responseHeaders != null && !responseHeaders.isEmpty())
                for (String responseHeader: responseHeaders) {
                    String[] strings = responseHeader.split(":", 2);
                    gwfs = gwfs.addResponseHeader(strings[0], strings[1]);
                }
            final Integer retry = apiRoute.getRetry();
            if (retry != null)
                gwfs = gwfs.retry(retryConfig -> {
                    final RetryGatewayFilterFactory.BackoffConfig backoffConfig = new RetryGatewayFilterFactory.BackoffConfig();
                    backoffConfig.setFirstBackoff(Duration.ofMillis(10));
                    backoffConfig.setMaxBackoff(Duration.ofMillis(50));
                    backoffConfig.setFactor(2);
                    backoffConfig.setBasedOnPreviousValue(false);
                    retryConfig
                            .setRetries(retry)
                            .allMethods()
                            .setSeries(HttpStatus.Series.CLIENT_ERROR, HttpStatus.Series.SERVER_ERROR,
                                    HttpStatus.Series.INFORMATIONAL, HttpStatus.Series.REDIRECTION,
                                    HttpStatus.Series.SUCCESSFUL)
                            .setStatuses(HttpStatus.NOT_FOUND)
                            .setExceptions(Exception.class)
                            .setBackoff(backoffConfig);
                });
            final String rewritePath = apiRoute.getRewritePath();
            if (rewritePath != null) {
                String[] strings = rewritePath.split(":", 2);
                gwfs = gwfs.rewritePath(strings[0], strings[1]);
            }
            final boolean requestRateLimiter = apiRoute.isRequestRateLimiter();
            if (requestRateLimiter)
                gwfs = gwfs.requestRateLimiter(config -> config
                        .setRateLimiter(redisRateLimiter)
                        .setKeyResolver(exchange -> {
                            //exchange.request.queryParams.getFirst("user")
                            String hostAddress = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
                            if (hostAddress == null)
                                hostAddress = "unknown";
                            return Mono.just(hostAddress);
                        }));
            final Integer setStatus = apiRoute.getSetStatus();
            if (setStatus != null)
                gwfs = gwfs.setStatus(setStatus);
            final CircuitBreaker circuitBreaker =  apiRoute.getCircuitBreaker();
            if (circuitBreaker != null)
                gwfs = gwfs.circuitBreaker(c -> {
                        SpringCloudCircuitBreakerFilterFactory.Config config = c.setName(circuitBreaker.getName());
                        config = config.setFallbackUri(circuitBreaker.getFallbackUri());
                        config = config.setStatusCodes(circuitBreaker.getStatusCodes());
                        config.setResumeWithoutError(circuitBreaker.isResumeWithoutError());
                        config.setRouteId(circuitBreaker.getRouteId());
                    });
            return gwfs;
        }).uri(apiRoute.getUri());
    }

    @Override
    public Flux<Route> getRoutesByMetadata(Map<String, Object> metadata) {
        return RouteLocator.super.getRoutesByMetadata(metadata);
    }
}
