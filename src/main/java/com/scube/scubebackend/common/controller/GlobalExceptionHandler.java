package com.scube.scubebackend.common.controller;

import com.scube.scubebackend.common.ErrorCode;
import com.scube.scubebackend.exception.BusinessException;
import com.scube.scubebackend.common.model.dto.BaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<BaseResponse<?>> handleBusinessException(BusinessException e) {
        log.error("业务异常：{}", e.getMessage());
        int code = e.getCode();
        HttpStatus status = mapErrorCodeToStatus(code);
        return ResponseEntity.status(status).body(BaseResponse.error(code, e.getMessage()));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<BaseResponse<?>> handleBindException(BindException e) {
        log.error("参数校验异常：{}", e.getMessage());
        String message = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(BaseResponse.error(ErrorCode.PARAMS_ERROR.getCode(), message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<?>> handleException(Exception e) {
        log.error("系统异常：", e);
        // 开发环境返回详细错误信息，生产环境只返回通用错误
        String message = "系统内部异常";
        if (e.getMessage() != null && !e.getMessage().isEmpty()) {
            message = e.getMessage();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(BaseResponse.error(ErrorCode.SYSTEM_ERROR.getCode(), message));
    }

    private HttpStatus mapErrorCodeToStatus(int code) {
        if (code == ErrorCode.PARAMS_ERROR.getCode()) {
            return HttpStatus.BAD_REQUEST;
        }
        if (code == ErrorCode.NOT_LOGIN_ERROR.getCode()) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (code == ErrorCode.NO_AUTH_ERROR.getCode()) {
            return HttpStatus.FORBIDDEN;
        }
        if (code == ErrorCode.NOT_FOUND_ERROR.getCode()) {
            return HttpStatus.NOT_FOUND;
        }
        // Others -> 500
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
