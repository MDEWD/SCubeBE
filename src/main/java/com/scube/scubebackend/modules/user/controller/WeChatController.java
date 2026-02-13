package com.scube.scubebackend.modules.user.controller;

import com.scube.scubebackend.modules.user.model.dto.WeChatUserInfo;
import com.scube.scubebackend.modules.user.service.WeChatService;
import com.scube.scubebackend.util.WeChatSignatureUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * 微信公众号回调接口
 * 注意：此接口需要配置到微信公众号后台，且必须是公网可访问的地址
 */
@RestController
@RequestMapping("/api/wechat")
public class WeChatController {
    
    @Autowired
    private WeChatService weChatService;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Value("${wechat.mp.token}")
    private String token;
    
    /**
     * 微信服务器验证接口（GET请求）
     * 用于验证服务器配置
     * 参考：https://developers.weixin.qq.com/doc/offiaccount/Basic_Information/Access_Overview.html
     */
    @GetMapping("/callback")
    public String verify(@RequestParam(value = "signature", required = false) String signature,
                        @RequestParam(value = "timestamp", required = false) String timestamp,
                        @RequestParam(value = "nonce", required = false) String nonce,
                        @RequestParam(value = "echostr", required = false) String echostr) {
        // 微信服务器验证时会发送这些参数
        // 验证签名
        if (signature != null && timestamp != null && nonce != null && echostr != null) {
            boolean isValid = WeChatSignatureUtil.checkSignature(signature, timestamp, nonce, token);
            if (isValid) {
                return echostr;
            } else {
                return "signature verification failed";
            }
        }
        return "success";
    }
    
    /**
     * 微信消息回调接口（POST请求）
     * 接收微信推送的各种消息和事件
     */
    @PostMapping("/callback")
    public String callback(@RequestBody String xmlData) {
        return weChatService.handleWeChatCallback(xmlData);
    }
    
    /**
     * 微信网页授权回调接口
     * 用户授权后，微信会重定向到此接口，并带上code和state参数
     * 
     * @param code 授权code，用于换取access_token和用户信息
     * @param state 状态参数，可以传递ticket等信息
     * @return 重定向到前端页面，并带上用户信息
     */
    @GetMapping("/oauth/callback")
    public String oauthCallback(@RequestParam String code, @RequestParam(required = false) String state) {
        try {
            // 通过code获取用户信息
            WeChatUserInfo userInfo = weChatService.getUserInfoByCode(code);
            
            // 将用户信息存储到Redis，key为state（通常是ticket）
            if (state != null && !state.isEmpty()) {
                String ticketKey = "wechat:qr:ticket:" + state;
                redisTemplate.opsForValue().set(ticketKey, userInfo.getOpenId(), 300, TimeUnit.SECONDS);
                
                // 存储用户信息（包含昵称和头像）
                String userInfoKey = "wechat:userinfo:" + userInfo.getOpenId();
                redisTemplate.opsForValue().set(userInfoKey, userInfo, 300, TimeUnit.SECONDS);
            }
            
            // 重定向到前端页面，带上用户信息
            // 注意：这里需要根据实际的前端地址修改
            String redirectUrl = "http://localhost:3000/auth/callback?openId=" + userInfo.getOpenId();
            if (userInfo.getNickname() != null) {
                redirectUrl += "&nickname=" + URLEncoder.encode(userInfo.getNickname(), StandardCharsets.UTF_8);
            }
            if (userInfo.getAvatar() != null) {
                redirectUrl += "&avatar=" + URLEncoder.encode(userInfo.getAvatar(), StandardCharsets.UTF_8);
            }
            
            return "redirect:" + redirectUrl;
        } catch (Exception e) {
            // 授权失败，重定向到错误页面
            try {
                return "redirect:http://localhost:3000/auth/error?message=" + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
            } catch (Exception ex) {
                return "redirect:http://localhost:3000/auth/error?message=授权失败";
            }
        }
    }
}
