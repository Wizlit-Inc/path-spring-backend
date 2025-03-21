package com.wizlit.path.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Handle all custom API exceptions
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException exception) {
        return ResponseEntity
                .status(exception.getStatus())
                .body(new ErrorResponse(
                        exception.getErrorCode().getCode(),
                        exception.getMessage(),
                        exception.getStatus().value()
                ));
    }

    // Fallback for other exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
        return ResponseEntity
                .status(ErrorCodes.INTERNAL_SERVER_ERROR.getStatus())
                .body(new ErrorResponse(
                        ErrorCodes.INTERNAL_SERVER_ERROR.getCode(),
                        ErrorCodes.INTERNAL_SERVER_ERROR.getMessage(),
                        ErrorCodes.INTERNAL_SERVER_ERROR.getStatus().value()
                ));
    }
}