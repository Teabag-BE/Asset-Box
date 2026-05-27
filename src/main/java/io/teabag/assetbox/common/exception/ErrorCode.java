package io.teabag.assetbox.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED),
    FORBIDDEN(HttpStatus.FORBIDDEN),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND),
    CATEGORY_DEPTH_INVALID(HttpStatus.BAD_REQUEST),
    POST_NOT_FOUND(HttpStatus.NOT_FOUND),
    REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND),
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND),
    FEEDBACK_NOT_FOUND(HttpStatus.NOT_FOUND),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
