package com.wizlit.path.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Centralized catalog for all error codes and their messages.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCodes {

    // Validation errors
    NULL_PARAMETERS(HttpStatus.BAD_REQUEST, "NULL_PARAMETERS",
            "Origin and destination parameters must not be null - origin: %d, destination: %d"),
    SAME_POINTS(HttpStatus.BAD_REQUEST, "SAME_POINTS",
            "Start point and end point cannot be the same"),
    NON_EXISTENT_POINTS(HttpStatus.BAD_REQUEST, "NON_EXISTENT_POINTS",
            "Either startPoint or endPoint does not exist - origin: %d, destination: %d"),
    BACKWARD_PATH(HttpStatus.CONFLICT, "BACKWARD_PATH",
            "A backward path exists from endPoint to startPoint within %d edges"),
    INVALID_NUMERIC_IDS(HttpStatus.BAD_REQUEST, "INVALID_NUMERIC_IDS",
            "Origin and destination must be valid numeric IDs - origin: %d, destination: %d"),
    EDGE_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "EDGE_ALREADY_EXISTS",
            "An edge already exists between these points - origin: %d, destination: %d"),
    SAVE_FAILED(HttpStatus.BAD_REQUEST, "SAVE_FAILED",
            "Failed to save the edge due to a conflict."),
    
    // Generic errors
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "ERR_INTERNAL",
            "An unexpected error occurred. Please try again later."),
    UNKNOWN_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "ERR_UNKNOWN",
            "An unspecified error occurred.");

    private final HttpStatus status; // A clear and reusable error message
    private final String code;    // A unique error code
    private final String message; // A clear and reusable error message

    /**
     * Returns a formatted error message if the message contains placeholders.
     *
     * @param args Placeholder replacements for the error message
     * @return A formatted error message
     */
    public String getFormattedMessage(Object... args) {
        return String.format(message, args);
    }
}