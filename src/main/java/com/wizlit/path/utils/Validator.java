package com.wizlit.path.utils;

import com.wizlit.path.exception.ApiException;
import com.wizlit.path.exception.ErrorCode;

import java.util.stream.Stream;

public class Validator {

    private final String message;
    private final Throwable cause;
    private ApiException exception;

    private Validator(Throwable cause) {
        this.cause = cause;
        this.message = cause.getMessage();
    }

    public static Validator from(Throwable error) {
        return new Validator(error);
    }

    public Validator containsAllElseError(ApiException apiException, String... keywordsContains) {
        if (exception == null && Stream.of(keywordsContains).allMatch(message::contains)) {
            exception = apiException;
        }
        return this;
    }

    public Throwable toException() {
        return (exception != null)
                ? exception
                : new ApiException(ErrorCode.INTERNAL_SERVER, cause);
    }
}