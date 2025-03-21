package com.wizlit.path.exception;

import org.springframework.http.HttpStatus;

public class ValidationException extends ApiException {

    public ValidationException(ErrorCodes errorCode) {
        super(errorCode, HttpStatus.BAD_REQUEST);
    }

    public ValidationException(ErrorCodes errorCode, String message) {
        super(errorCode, HttpStatus.BAD_REQUEST, message);
    }

    public ValidationException(ErrorCodes errorCode, int array, Object... inputs) {
        super(errorCode, HttpStatus.BAD_REQUEST, errorCode.getFormattedMessage(inputs));
    }
}