package com.wizlit.path.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wizlit.path.exception.ApiException;
import com.wizlit.path.exception.ErrorCode;
import com.wizlit.path.temp.GoogleService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class PrivateAccessFilter implements WebFilter {

    // List of path prefixes for Swagger and related endpoints/resources.
    private static final List<String> SWAGGER_PATHS = Arrays.asList(
            "/swagger-ui", "/swagger-ui/", "/v3/api-docs", "/swagger-resources", "/webjars/"
    );

    private static final String DEVELOPER_EMAIL = "admin@wizlit.com";
    private static final String ADMIN_EMAIL = "admin@wizlit.com";

    private final RequestMappingHandlerMapping handlerMapping;
    private final Set<String> allowedEmails;
    private final ObjectMapper mapper = new ObjectMapper();
    private final GoogleService googleService;
    private final boolean developerMode;

    public PrivateAccessFilter(
            @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping,
            @Value("${app.privateAccess.allowedEmails}") String emails,
            @Value("${app.developerMode:false}") boolean developerMode,
            GoogleService googleService
    ) {
        this.handlerMapping = handlerMapping;
        this.allowedEmails = Arrays.stream(emails.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
        this.googleService = googleService;
        this.developerMode = developerMode;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // Exclude Swagger UI and related resources from authentication filtering.
        if (SWAGGER_PATHS.stream().anyMatch(path::startsWith)) {
            return chain.filter(exchange);
        }

        // 1️⃣ Allow preflight requests to bypass authentication
        if (HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod())) {
            return chain.filter(exchange);
        }

        return handlerMapping.getHandler(exchange)
                .cast(HandlerMethod.class)
                .flatMap(handlerMethod -> {
                    PrivateAccess privateAccess = handlerMethod.getMethod().getAnnotation(PrivateAccess.class);
                    
                    if (privateAccess != null) {
                        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
                        if ((authHeader == null || authHeader.isEmpty()) && developerMode) {
                            // In developer mode, check if admin role is required
                            if ("admin".equals(privateAccess.role()) && !DEVELOPER_EMAIL.equals(ADMIN_EMAIL)) {
                                return Mono.error(new ApiException(ErrorCode.INACCESSIBLE_USER));
                            }
                            exchange.getAttributes().put("email", DEVELOPER_EMAIL);
                            exchange.getAttributes().put("name", "Developer");
                            exchange.getAttributes().put("avatar", "https://picsum.photos/200");
                            exchange.getAttributes().put("token", "developer-mode-token");
                            return chain.filter(exchange);
                        }
                        return authenticate(exchange, chain);
                    } else {
                        return chain.filter(exchange);
                    }
                });
    }

    private Mono<Void> authenticate(ServerWebExchange exchange, WebFilterChain chain) {
        return Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
                .filter(auth -> auth.startsWith("Bearer "))
                .switchIfEmpty(Mono.error(new ApiException(ErrorCode.INVALID_TOKEN)))
                .map(auth -> auth.substring(7))
                .flatMap(token -> isGoogleToken(token)
                        ? processGoogleToken(token, exchange, chain)
                        : processJwtToken(token, exchange, chain));
    }


    private boolean isGoogleToken(String token) {
        // A simple check that can be replaced with a more robust implementation
        return token.startsWith("y");
    }

    private Mono<Void> processGoogleToken(String token, ServerWebExchange exchange, WebFilterChain chain) {
        return googleService.getUserInfo(token)
                .flatMap(json -> {
                    try {
                        JsonNode userInfo = mapper.readTree(json);
                        String email = userInfo.path("email").asText(null);
                        String name = userInfo.path("name").asText(null);
                        String avatar = userInfo.path("picture").asText(null);
                        if (email == null) {
                            return Mono.error(new ApiException(ErrorCode.INVALID_TOKEN));
                        }
                        return validateAndProceed(email, name, avatar, token, exchange, chain);
                    } catch (IOException e) {
                        return Mono.error(new ApiException(ErrorCode.INVALID_TOKEN, e));
                    }
                });
    }

    private Mono<Void> processJwtToken(String token, ServerWebExchange exchange, WebFilterChain chain) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return Mono.error(new ApiException(ErrorCode.INVALID_TOKEN));
            }
            String payloadJson = new String(
                    Base64.getUrlDecoder().decode(parts[1]),
                    StandardCharsets.UTF_8
            );
            JsonNode payload = mapper.readTree(payloadJson);
            long exp = payload.get("exp").asLong();
            if (exp < Instant.now().getEpochSecond()) {
                return Mono.error(new ApiException(ErrorCode.EXPIRED_TOKEN));
            }
            String email = payload.get("email").asText();
            String name = payload.path("name").asText(null);
            String avatar = payload.path("picture").asText(null);
            return validateAndProceed(email, name, avatar, token, exchange, chain);
        } catch (IOException | IllegalArgumentException e) {
            return Mono.error(new ApiException(ErrorCode.INVALID_TOKEN, e));
        }
    }

    private Mono<Void> validateAndProceed(String email, String name, String avatar, String token, ServerWebExchange exchange, WebFilterChain chain) {
        if (!allowedEmails.contains("*") && !allowedEmails.contains(email)) {
            return Mono.error(new ApiException(ErrorCode.INACCESSIBLE_USER));
        }

        // Get the handler method to check for admin role
        return handlerMapping.getHandler(exchange)
                .cast(HandlerMethod.class)
                .flatMap(handlerMethod -> {
                    PrivateAccess privateAccess = handlerMethod.getMethod().getAnnotation(PrivateAccess.class);
                    if (privateAccess != null && "admin".equals(privateAccess.role()) && !ADMIN_EMAIL.equals(email)) {
                        return Mono.error(new ApiException(ErrorCode.INACCESSIBLE_USER));
                    }
                    exchange.getAttributes().put("email", email);
                    exchange.getAttributes().put("name", name);
                    exchange.getAttributes().put("avatar", avatar);
                    exchange.getAttributes().put("token", token);
                    return chain.filter(exchange);
                });
    }

//    private Mono<Void> authenticate(ServerWebExchange exchange, WebFilterChain chain) {
//        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
//
//        return Mono.justOrEmpty(header)
//                .filter(h -> h.startsWith("Bearer "))
//                .switchIfEmpty(Mono.error(new ApiException(ErrorCode.INVALID_TOKEN)))
//                .map(h -> h.substring(7))
//                .flatMap(token -> {
//                    if (token.startsWith("y")) {
//                        // → Google OAuth2 token
//                        return googleService.getUserInfo(token)
//                                .flatMap(json -> {
//                                    try {
//                                        JsonNode userInfo = mapper.readTree(json);
//                                        String email = userInfo.path("email").asText(null);
//                                        if (email == null) {
//                                            return Mono.error(new ApiException(ErrorCode.INVALID_TOKEN));
//                                        }
//                                        if (!allowed.contains(email)) {
//                                            return Mono.error(new ApiException(ErrorCode.INACCESSIBLE_USER, email));
//                                        }
//                                        exchange.getAttributes().put("email", email);
//                                        exchange.getAttributes().put("token", token);
//                                        return chain.filter(exchange);
//                                    } catch (IOException e) {
//                                        return Mono.error(new ApiException(ErrorCode.INVALID_TOKEN, e));
//                                    }
//                                });
//                    } else {
//                        // → JWT path
//                        try {
//                            JsonNode payload = mapper.readTree(new String(
//                                    Base64.getUrlDecoder().decode(token.split("\\.")[1]),
//                                    StandardCharsets.UTF_8
//                            ));
//                            long exp = payload.get("exp").asLong();
//                            if (exp < Instant.now().getEpochSecond()) {
//                                return Mono.error(new ApiException(ErrorCode.EXPIRED_TOKEN));
//                            }
//                            String email = payload.get("email").asText();
//                            if (!allowed.contains(email)) {
//                                return Mono.error(new ApiException(ErrorCode.INACCESSIBLE_USER, email));
//                            }
//                            exchange.getAttributes().put("email", email);
//                            exchange.getAttributes().put("token", token);
//                            return chain.filter(exchange);
//                        } catch (IOException e) {
//                            return Mono.error(new ApiException(ErrorCode.INVALID_TOKEN, e));
//                        }
//                    }
//                });
//    }

}
