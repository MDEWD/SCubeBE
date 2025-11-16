package com.scube.scubebackend.model.dto;

import com.scube.scubebackend.common.ErrorCode;
import lombok.Data;

@Data
public class BaseResponse<T> {
    private Integer code;
    private String message;
    private T data;
    
    public static <T> BaseResponse<T> success(T data) {
        BaseResponse<T> response = new BaseResponse<>();
        response.setCode(0);
        response.setMessage("操作成功");
        response.setData(data);
        return response;
    }
    
    public static <T> BaseResponse<T> success(String message, T data) {
        BaseResponse<T> response = new BaseResponse<>();
        response.setCode(0);
        response.setMessage(message);
        response.setData(data);
        return response;
    }
    
    public static <T> BaseResponse<T> error(Integer code, String message) {
        BaseResponse<T> response = new BaseResponse<>();
        response.setCode(code);
        response.setMessage(message);
        return response;
    }
    
    public static <T> BaseResponse<T> error(String message) {
        return error(50000, message);
    }
    
    public static <T> BaseResponse<T> error(ErrorCode errorCode) {
        return error(errorCode.getCode(), errorCode.getMessage());
    }
}

