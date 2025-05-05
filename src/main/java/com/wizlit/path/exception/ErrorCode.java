package com.wizlit.path.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Centralized catalog for all error codes and their messages.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // path errors
    BACKWARD_PATH(HttpStatus.CONFLICT,
            "It is a backward path from originPoint to destinationPoint within %d edges - origin: %d, destination: %d"),

    // point errors
    NULL_INPUT(HttpStatus.BAD_REQUEST,
            "parameter is null - %s"),
    NULL_POINTS(HttpStatus.BAD_REQUEST,
            "Origin and destination parameters must not be null - origin: %d, destination: %d"),
    SAME_POINTS(HttpStatus.BAD_REQUEST,
            "Start point and end point cannot be the same"),
    NON_EXISTENT_POINTS(HttpStatus.BAD_REQUEST,
            "Either one of the points does not exist - points: %s"),
    INVALID_NUMERIC_IDS(HttpStatus.BAD_REQUEST,
            "Origin and destination must be valid numeric IDs - origin: %d, destination: %d"),
    POINT_NOT_FOUND(HttpStatus.NOT_FOUND,
            "The specified point could not be found - point: %d"),
    POINT_NAME_DUPLICATED(HttpStatus.CONFLICT,
            "The provided name already exists for the point. - name: %s"),
    POINT_NOT_DELETABLE(HttpStatus.CONFLICT,
            "The point cannot be deleted - point: %d (reason: %s)"),
    POINT_MAX_MEMOS_REACHED(HttpStatus.CONFLICT,
            "The point has reached its maximum number of memos - point: %d"),

    // project errors
    PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND,
            "The specified project could not be found - project: %d"),

    // edge errors
    EDGE_ALREADY_EXISTS(HttpStatus.BAD_REQUEST,
            "An edge already exists between these points - origin: %d, destination: %d"),

    // memo errors
    MEMO_ALREADY_EXISTS(HttpStatus.BAD_REQUEST,
            "A memo already exists in the point - point: %d, memo: %d"),
    MEMO_RESERVED(HttpStatus.CONFLICT,
            "The memo is currently reserved by another user - memo: %d / user: %d"),
    MEMO_NOT_FOUND(HttpStatus.NOT_FOUND,
            "The specified memo could not be found - memo: %d"),
    NOT_EXTERNAL_MEMO(HttpStatus.BAD_REQUEST,
            "Memo is not an external document"),
    ABNORMAL_CONTENT_DELETION(HttpStatus.BAD_REQUEST,
            "Content deletion too large: %s"),

    // user errors
    USER_ALREADY_EXISTS(HttpStatus.BAD_REQUEST,
            "A user already exists with the provided email - email: %s"),

    // drive errors
    COPY_FAILED(HttpStatus.BAD_REQUEST,
            "Copy failed: %s"),

    // Generic errors
    EMPTY(HttpStatus.BAD_REQUEST,
            "No data to be returned"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED,
            "Invalid Token"),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED,
            "Token expired"),
    INACCESSIBLE_USER(HttpStatus.FORBIDDEN,
            "You are not allowed to access this resource"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND,
            "User not found with email: %s"),
    INTERNAL_SERVER(HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred. - %s"),
    UNKNOWN(HttpStatus.INTERNAL_SERVER_ERROR,
            "An unspecified error occurred.");


    private final HttpStatus status; // A clear and reusable error message
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