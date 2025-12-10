package com.scube.scubebackend.controller;

import com.scube.scubebackend.common.ErrorCode;
import com.scube.scubebackend.model.dto.BaseResponse;
import com.scube.scubebackend.model.dto.CheckTicketResponse;
import com.scube.scubebackend.model.dto.LoginRequest;
import com.scube.scubebackend.model.dto.LoginResponse;
import com.scube.scubebackend.model.dto.UserVO;
import com.scube.scubebackend.model.dto.WeChatUserInfo;
import com.scube.scubebackend.service.UserService;
import com.scube.scubebackend.service.WeChatService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
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
    public BaseResponse<CheckTicketResponse> checkTicket(@RequestParam String ticket) {
        WeChatUserInfo userInfo = weChatService.checkTicketScannedWithUserInfo(ticket);
        CheckTicketResponse response = new CheckTicketResponse();
        if (userInfo != null) {
            response.setScanned(true);
            response.setUserInfo(userInfo);
        } else {
            response.setScanned(false);
            response.setUserInfo(null);
        }
        return BaseResponse.success(response);
    }
    
    /**
     * 生成微信网页授权链接
     * @param redirectUri 授权后重定向的URI（需要URL编码）
     * @param state 状态参数，用于保持请求和回调的状态（如ticket）
     * @return 授权链接
     */
    @GetMapping("/authorize-url")
    public BaseResponse<Map<String, String>> getAuthorizationUrl(
            @RequestParam String redirectUri,
            @RequestParam(required = false) String state) {
        try {
            String authUrl = weChatService.buildAuthorizationUrl(redirectUri, state != null ? state : "");
            Map<String, String> result = new java.util.HashMap<>();
            result.put("authorizationUrl", authUrl);
            return BaseResponse.success(result);
        } catch (Exception e) {
            return BaseResponse.error(50000, "生成授权链接失败: " + e.getMessage());
        }
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
    
    /**
     * 退出登录
     * 注意：由于使用JWT，服务端是无状态的，退出登录主要是清除前端的token
     * 如果需要服务端主动使token失效，可以将token加入黑名单（需要Redis支持）
     */
    @PostMapping("/logout")
    public BaseResponse<String> logout() {
        // JWT是无状态的，服务端不需要做特殊处理
        // 前端会清除localStorage中的token
        // 如果需要服务端主动使token失效，可以在这里将token加入黑名单
        return BaseResponse.success("退出登录成功");
    }
}

