package com.parking.common.exception;

import lombok.Getter;

/**
 * Vuot nguong rate-limit (vd: goi forgot-password / reset-password / login qua nhieu lan).
 * Duoc GlobalExceptionHandler map sang HTTP 429 Too Many Requests.
 */
@Getter
public class TooManyRequestsException extends RuntimeException {

    private final String errorCode;

    public TooManyRequestsException(String message) {
        this(message, "RATE_LIMITED");
    }

    public TooManyRequestsException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}
