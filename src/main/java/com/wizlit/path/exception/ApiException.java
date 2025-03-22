package com.wizlit.path.exception;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public class ApiException extends RuntimeException {
    private final ErrorCode errorCode;
    private final Object[] args;
    private final List<String> stacks;

    private static final String BASE_PACKAGE = ApiException.class.getPackageName().replaceFirst("\\.[^.]+$", "");
    
    public ApiException(ErrorCode errorCode, Object... args) {
        super(String.format(errorCode.getMessage(), args));
        this.errorCode = errorCode;
        this.args = args;
        
        // Build a list of "FileName:LineNumber" for every stack frame beyond this constructor
        this.stacks = Arrays.stream(Thread.currentThread().getStackTrace())
                .skip(2)                                                   // skip getStackTrace & constructor
                .filter(frame -> frame.getClassName().startsWith(BASE_PACKAGE))
                .map(frame -> frame.getFileName() + ":" + frame.getLineNumber())
                .toList();
    }
}