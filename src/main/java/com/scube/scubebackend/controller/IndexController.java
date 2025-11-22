package com.scube.scubebackend.controller;

import com.scube.scubebackend.model.dto.BaseResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class IndexController {
    
    @GetMapping("/")
    public BaseResponse<Map<String, Object>> index() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "算立方（SuanCube）后端系统");
        data.put("version", "1.0.0");
        data.put("status", "running");
        data.put("message", "API服务正常运行");
        return BaseResponse.success(data);
    }
    
    @GetMapping("/health")
    public BaseResponse<Map<String, String>> health() {
        Map<String, String> data = new HashMap<>();
        data.put("status", "UP");
        data.put("timestamp", String.valueOf(System.currentTimeMillis()));
        return BaseResponse.success(data);
    }
}

