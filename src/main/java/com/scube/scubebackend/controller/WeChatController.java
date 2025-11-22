package com.scube.scubebackend.controller;

import com.scube.scubebackend.service.WeChatService;
import com.scube.scubebackend.util.WeChatSignatureUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

/**
 * 微信公众号回调接口
 * 注意：此接口需要配置到微信公众号后台，且必须是公网可访问的地址
 */
@RestController
@RequestMapping("/api/wechat")
public class WeChatController {
    
    @Autowired
    private WeChatService weChatService;
    
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
}

