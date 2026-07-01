package com.parking.common.exception;

import lombok.Getter;

/**
 * Vi pham mot quy tac nghiep vu (vd: het cho, het quota, sai trang thai chuyen doi).
 * Co the kem theo errorCode de FE xu ly (vd: QUOTA_FULL).
 */
@Getter
public class BusinessRuleException extends RuntimeException {

    private final String errorCode;

    public BusinessRuleException(String message) {
        this(message, null);
    }

    public BusinessRuleException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}
