package com.wizlit.path.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

@Component
@Order(-2)
@AllArgsConstructor
@RestControllerAdvice
public class GlobalExceptionHandler implements WebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String BASE_PACKAGE = ApiException.class.getPackageName().replaceFirst("\\.[^.]+$", "");

    private final ObjectMapper mapper;

    // Handle all custom API exceptions
    @ExceptionHandler(ApiException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleApiException(ApiException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        String message = errorCode.getFormattedMessage(exception.getArgs());

        log.error("API error [{}]: {}", errorCode.name(), message, exception);

        ErrorResponse errorResponse = new ErrorResponse(
                errorCode.name(),
                message,
                errorCode.getStatus().value(),
                exception.getStacks()
        );

        return Mono.just(ResponseEntity
                .status(errorCode.getStatus())
                .body(errorResponse));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception exception) {
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER;
        log.error("Server error [{}]: {}", errorCode.name(), exception.getMessage(), exception);

        List<String> stacks = Arrays.stream(Thread.currentThread().getStackTrace())
                .skip(2)                                                   // skip getStackTrace & constructor
                .filter(frame -> frame.getClassName().startsWith(BASE_PACKAGE))
                .map(frame -> frame.getFileName() + ":" + frame.getLineNumber())
                .toList();

        ErrorResponse errorResponse = new ErrorResponse(
                errorCode.name(),
                exception.getMessage(),
                errorCode.getStatus().value(),
                stacks
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // 2️⃣ WebExceptionHandler.handle() catches **all** exceptions — including those from Filters
    @SneakyThrows
    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        // Check if the response is already committed
        if (exchange.getResponse().isCommitted()) {
            // Log a warning and simply return an error (or empty)
            log.warn("Response already committed, cannot write error response", ex);
            return Mono.empty();
        }

        Mono<ResponseEntity<ErrorResponse>> responseMono = (ex instanceof ApiException apiEx)
                ? handleApiException(apiEx)
                : Mono.just(handleGenericException((Exception) ex));

        return responseMono.flatMap(response -> {
            try {
                ServerHttpResponse resp = exchange.getResponse();
                resp.setStatusCode(response.getStatusCode());
                resp.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                DataBuffer buffer = resp.bufferFactory().wrap(mapper.writeValueAsBytes(response.getBody()));
                return resp.writeWith(Mono.just(buffer));
            } catch (Exception e) {
                return Mono.error(e);
            }
        });
    }

}