package com.scube.scubebackend.controller;

import com.scube.scubebackend.common.ErrorCode;
import com.scube.scubebackend.model.dto.BaseResponse;
import com.scube.scubebackend.model.dto.LoginRequest;
import com.scube.scubebackend.model.dto.LoginResponse;
import com.scube.scubebackend.model.dto.UserVO;
import com.scube.scubebackend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController extends BaseController {
    
    @Autowired
    private UserService userService;
    
    @PostMapping("/login")
    public BaseResponse<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        LoginResponse response = userService.login(request);
        return BaseResponse.success("登录成功", response);
    }
    
    @GetMapping("/get/login")
    public BaseResponse<UserVO> getCurrentUser() {
        UserVO user = userService.getCurrentUser();
        return BaseResponse.success(user);
    }
}

