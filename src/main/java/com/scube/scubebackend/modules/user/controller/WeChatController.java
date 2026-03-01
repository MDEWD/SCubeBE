package com.scube.scubebackend.modules.user.controller;

import com.scube.scubebackend.modules.user.service.WeChatService;
import com.scube.scubebackend.util.WeChatSignatureUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

/**
 * 微信公众号回调接口
 * 注意：此接口需要配置到微信公众号后台，且必须是公网可访问的地址
 */
@RestController
@RequestMapping("/api/wechat")
@Slf4j
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
                        @RequestParam(value = "echostr", required = false) String echostr,
                        HttpServletRequest request) {
        long startNs = System.nanoTime();
        log.info("WeChat callback GET hit: ip={}, ua={}, query={}, hasSignature={}, hasTimestamp={}, hasNonce={}, hasEchostr={}",
                request.getRemoteAddr(), request.getHeader("User-Agent"), request.getQueryString(),
                signature != null, timestamp != null, nonce != null, echostr != null);
        // 微信服务器验证时会发送这些参数
        // 验证签名
        try {
            if (signature != null && timestamp != null && nonce != null && echostr != null) {
                boolean isValid = WeChatSignatureUtil.checkSignature(signature, timestamp, nonce, token);
                if (isValid) {
                    return echostr;
                } else {
                    return "signature verification failed";
                }
            }
            return "success";
        } finally {
            long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;
            log.info("WeChat callback GET done: elapsedMs={}", elapsedMs);
        }
    }

    /**
     * 微信消息回调接口（POST请求）
     * 接收微信推送的各种消息和事件
     */
    @PostMapping("/callback")
    public String callback(@RequestBody String xmlData, HttpServletRequest request) {
        long startNs = System.nanoTime();
        log.info("WeChat callback POST hit: ip={}, ua={}, contentType={}, bodyLength={}",
                request.getRemoteAddr(), request.getHeader("User-Agent"), request.getContentType(),
                xmlData != null ? xmlData.length() : 0);
        log.debug("WeChat callback POST body: {}", xmlData);
        try {
            return weChatService.handleWeChatCallback(xmlData);
        } catch (Exception e) {
            // Ensure WeChat receives a response even if our handler fails.
            log.error("WeChat callback POST handler failed", e);
            return "success";
        } finally {
            long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;
            log.info("WeChat callback POST done: elapsedMs={}", elapsedMs);
        }
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
        return weChatService.handleOAuthCallback(code, state);
    }
}
