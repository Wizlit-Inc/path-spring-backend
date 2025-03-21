package com.wizlit.path.exception;

import org.springframework.http.HttpStatus;
import lombok.Getter;

@Getter
public class ApiException extends RuntimeException {

    private final ErrorCodes errorCode;   // Unique error code (from shared catalog)
    private final HttpStatus status;       // HTTP status for the error

    public ApiException(ErrorCodes errorCode, HttpStatus status) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.status = status;
    }

    public ApiException(ErrorCodes errorCode, HttpStatus status, String message) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }
}