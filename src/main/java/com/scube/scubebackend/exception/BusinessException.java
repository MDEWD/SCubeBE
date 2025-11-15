package com.scube.scubebackend.exception;

import com.scube.scubebackend.common.ErrorCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final int code;
    private final String message;
    
    public BusinessException(ErrorCode errorCode) {
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
    }
    
    public BusinessException(ErrorCode errorCode, String message) {
        this.code = errorCode.getCode();
        this.message = message;
    }
    
    public BusinessException(int code, String message) {
        this.code = code;
        this.message = message;
    }
}

