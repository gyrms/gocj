package com.yhg.olivemarket.global.exception;

import lombok.Getter;

@Getter
public class ErrorResponse {

    private final int status;
    private final String message;

    private ErrorResponse(ErrorCode errorCode) {
        this.status = errorCode.getStatus().value();
        this.message = errorCode.getMessage();
    }

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode);
    }
}
