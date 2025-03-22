package com.wizlit.path.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String BASE_PACKAGE = ApiException.class.getPackageName().replaceFirst("\\.[^.]+$", "");

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

}