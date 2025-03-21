package com.wizlit.path.exception;

import org.springframework.http.HttpStatus;

public class BusinessLogicException extends ApiException {

    public BusinessLogicException(ErrorCodes errorCode) {
        super(errorCode, HttpStatus.CONFLICT);
    }

    public BusinessLogicException(ErrorCodes errorCode, String message) {
        super(errorCode, HttpStatus.CONFLICT, message);
    }

    public BusinessLogicException(ErrorCodes errorCode, int array, Object... inputs) {
        super(errorCode, HttpStatus.CONFLICT, errorCode.getFormattedMessage(inputs));
    }
}