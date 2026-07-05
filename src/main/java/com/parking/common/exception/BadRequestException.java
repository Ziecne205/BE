package com.parking.common.exception;

import lombok.Getter;

/**
 * Yeu cau khong hop le tu client -> HTTP 400. Khac BusinessRuleException (409, xung dot trang thai
 * nghiep vu): dung cho input sai/thieu, token khong hop le/het han, vi pham policy...
 * Co the kem errorCode de FE map (vd: INVALID_TOKEN, TOKEN_EXPIRED, VALIDATION_ERROR).
 */
@Getter
public class BadRequestException extends RuntimeException {

    private final String errorCode;

    public BadRequestException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}
