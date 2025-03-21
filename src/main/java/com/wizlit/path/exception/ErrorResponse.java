package com.wizlit.path.exception;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ErrorResponse {

    private String errorCode;  // Use the ErrorCode enum
    private String message;       // Detailed error message
    private int status;           // HTTP status
}