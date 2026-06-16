package com.parking.common.exception;

/**
 * Vi pham mot quy tac nghiep vu (vd: het cho, het quota, sai trang thai chuyen doi).
 */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
