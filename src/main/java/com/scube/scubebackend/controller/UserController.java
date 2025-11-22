package com.scube.scubebackend.controller;

import com.scube.scubebackend.common.ErrorCode;
import com.scube.scubebackend.model.dto.BaseResponse;
import com.scube.scubebackend.model.dto.LoginRequest;
import com.scube.scubebackend.model.dto.LoginResponse;
import com.scube.scubebackend.model.dto.UserVO;
import com.scube.scubebackend.service.UserService;
import com.scube.scubebackend.service.WeChatService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController extends BaseController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private WeChatService weChatService;
    
    /**
     * 生成登录二维码
     */
    @GetMapping("/qr-code")
    public BaseResponse<Map<String, String>> generateQrCode() {
        Map<String, String> result = weChatService.generateLoginQrCode();
        return BaseResponse.success(result);
    }
    
    /**
     * 检查Ticket是否已被扫描（轮询接口）
     */
    @GetMapping("/check-ticket")
    public BaseResponse<Map<String, String>> checkTicket(@RequestParam String ticket) {
        String openId = weChatService.checkTicketScanned(ticket);
        Map<String, String> result = new java.util.HashMap<>();
        if (openId != null) {
            result.put("scanned", "true");
            result.put("openId", openId);
        } else {
            result.put("scanned", "false");
        }
        return BaseResponse.success(result);
    }

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

