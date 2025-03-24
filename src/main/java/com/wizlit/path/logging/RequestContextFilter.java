package com.wizlit.path.logging;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

@Component
@Order(-1)
public class RequestContextFilter implements WebFilter {

    public static final String REQUEST_ID = "requestId";
    public static final String USER_ID    = "userId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String userId = "user1";
//        String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
        String requestId = userId + "-" + UUID.randomUUID();

        // Add to response header too (optional)
        exchange.getResponse().getHeaders().add("X-Request-Id", requestId);

        return chain.filter(exchange)
                .contextWrite(ctx -> ctx
                        .put(REQUEST_ID, requestId)
                        .put(USER_ID, Objects.requireNonNull(userId))
                );
    }
}