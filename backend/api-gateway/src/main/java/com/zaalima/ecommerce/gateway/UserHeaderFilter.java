package com.zaalima.ecommerce.gateway;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

/**
 * Strips any client-supplied identity headers, then propagates the authenticated identity
 * to downstream services as X-User-Id / X-User-Roles. Downstream services must only be
 * reachable through the gateway (ClusterIP services in Kubernetes).
 */
@Component
public class UserHeaderFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerWebExchange sanitized = exchange.mutate()
                .request(r -> r.headers(h -> {
                    h.remove("X-User-Id");
                    h.remove("X-User-Roles");
                })).build();

        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication())
                .filter(auth -> auth instanceof JwtAuthenticationToken)
                .cast(JwtAuthenticationToken.class)
                .map(auth -> {
                    String roles = auth.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.joining(","));
                    return sanitized.mutate()
                            .request(r -> r.header("X-User-Id", auth.getName()).header("X-User-Roles", roles))
                            .build();
                })
                .defaultIfEmpty(sanitized)
                .flatMap(chain::filter);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
