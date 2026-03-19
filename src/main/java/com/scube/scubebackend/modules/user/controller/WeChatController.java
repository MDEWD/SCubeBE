package com.scube.scubebackend.modules.user.controller;

import com.scube.scubebackend.modules.user.service.WeChatService;
import com.scube.scubebackend.util.WeChatSignatureUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;

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

    @Autowired
    private WxMpService wxMpService;

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
    public String callback(
            @RequestBody String xmlData,
            @RequestParam(value = "msg_signature", required = false) String msgSignature,
            @RequestParam(value = "timestamp", required = false) String timestamp,
            @RequestParam(value = "nonce", required = false) String nonce,
            HttpServletRequest request
    ) {
        long startNs = System.nanoTime();
        log.info("WeChat callback POST hit: ip={}, ua={}, contentType={}, bodyLength={}, hasMsgSignature={}, hasTimestamp={}, hasNonce={}",
                request.getRemoteAddr(), request.getHeader("User-Agent"), request.getContentType(),
                xmlData != null ? xmlData.length() : 0,
                msgSignature != null, timestamp != null, nonce != null);
        log.debug("WeChat callback POST raw body: {}", xmlData);

        try {
            String plainXml = normalizeXml(xmlData);

            // 安全模式（加密）：包含 Encrypt 节点，并且 query 里会带 msg_signature/timestamp/nonce
            if (plainXml != null && plainXml.contains("<Encrypt>") && msgSignature != null && timestamp != null && nonce != null) {
                try {
                    WxMpXmlMessage inMessage = WxMpXmlMessage.fromEncryptedXml(plainXml, wxMpService.getWxMpConfigStorage(), timestamp, nonce, msgSignature);

                    // 部分版本的 SDK 不暴露解密后的原始XML，这里用解析后的对象重建最小事件XML，供下游 dom4j 解析。
                    plainXml = normalizeXml(buildPlainXmlFromMessage(inMessage));

                    log.info("WeChat callback POST decrypted. fromUser={}, msgType={}, event={}, eventKey={}",
                            inMessage.getFromUser(), inMessage.getMsgType(), inMessage.getEvent(), inMessage.getEventKey());
                    log.debug("WeChat callback POST decrypted xml: {}", plainXml);
                } catch (Exception decryptEx) {
                    // 解密失败时：返回 success 防止微信重试风暴，同时打印足够日志排查
                    log.error("WeChat callback POST decrypt failed. Please check token/aesKey/appId and that msg_signature/timestamp/nonce are passed correctly.", decryptEx);
                    return "success";
                }
            } else {
                // 明文/兼容模式：不做解密
                if (plainXml != null && plainXml.contains("<Encrypt>")) {
                    log.warn("WeChat callback looks encrypted but missing required query params (msg_signature/timestamp/nonce). urlQuery={}", request.getQueryString());
                }
            }

            return weChatService.handleWeChatCallback(plainXml);
        } catch (Exception e) {
            // Ensure WeChat receives a response even if our handler fails.
            log.error("WeChat callback POST handler failed", e);
            return "success";
        } finally {
            long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;
            log.info("WeChat callback POST done: elapsedMs={}", elapsedMs);
        }
    }

    private String buildPlainXmlFromMessage(WxMpXmlMessage m) {
        // 只构建当前业务用到的字段，避免引入不必要的复杂性。
        // handleWeChatCallback 依赖：MsgType/Event/FromUserName/ToUserName/EventKey/Ticket
        String msgType = nullToEmpty(m.getMsgType());
        String event = nullToEmpty(m.getEvent());
        String from = nullToEmpty(m.getFromUser());
        String to = nullToEmpty(m.getToUser());
        String eventKey = nullToEmpty(m.getEventKey());
        String ticket = nullToEmpty(m.getTicket());

        return "<xml>" +
                "<ToUserName><![CDATA[" + to + "]]></ToUserName>" +
                "<FromUserName><![CDATA[" + from + "]]></FromUserName>" +
                "<CreateTime>" + (System.currentTimeMillis() / 1000) + "</CreateTime>" +
                "<MsgType><![CDATA[" + msgType + "]]></MsgType>" +
                (event.isEmpty() ? "" : "<Event><![CDATA[" + event + "]]></Event>") +
                (eventKey.isEmpty() ? "" : "<EventKey><![CDATA[" + eventKey + "]]></EventKey>") +
                (ticket.isEmpty() ? "" : "<Ticket><![CDATA[" + ticket + "]]></Ticket>") +
                "</xml>";
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private String normalizeXml(String xml) {
        if (xml == null) {
            return null;
        }
        // Remove UTF-8 BOM if present, then trim leading/trailing whitespace.
        String s = xml;
        if (!s.isEmpty() && s.charAt(0) == '\uFEFF') {
            s = s.substring(1);
        }
        return s.trim();
    }

    /**
     * 微信网页授权回调接口
     * 用户授权后，微信会重定向到此接口，并带上code和state参数
     *
     * 这里返回一个中文提示页，避免在微信内打开时出现英文错误/空白。
     */
    @GetMapping(value = "/oauth/callback", produces = "text/html;charset=UTF-8")
    public String oauthCallback(@RequestParam String code, @RequestParam(required = false) String state) {
        try {
            String redirect = weChatService.handleOAuthCallback(code, state);
            String redirectUrl = redirect;
            if (redirectUrl != null && redirectUrl.startsWith("redirect:")) {
                redirectUrl = redirectUrl.substring("redirect:".length());
            }
            return buildOAuthResultHtml(true, "授权成功", "你已完成授权，可以返回继续登录。", redirectUrl);
        } catch (Exception e) {
            log.error("OAuth回调处理失败", e);
            String msg = e.getMessage() == null ? "授权失败，请稍后重试" : e.getMessage();
            return buildOAuthResultHtml(false, "授权失败", msg, null);
        }
    }

    private String buildOAuthResultHtml(boolean ok, String title, String message, String redirectUrl) {
        String statusColor = ok ? "#16a34a" : "#dc2626";
        String safeMessage = escapeHtml(message);
        String safeTitle = escapeHtml(title);

        String buttonHtml;
        if (ok && redirectUrl != null && !redirectUrl.isBlank()) {
            String safeUrl = escapeHtml(redirectUrl);
//            buttonHtml = "<a class='btn' href='" + safeUrl + "'>点击继续</a>";
        } else {
            buttonHtml = "<a class='btn' href='javascript:window.close();'>关闭页面</a>";
        }

        return "<!doctype html>" +
                "<html lang='zh-CN'><head>" +
                "<meta charset='utf-8'/>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1'/>" +
                "<title>" + safeTitle + "</title>" +
                "<style>" +
                "body{font-family:-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial; background:#f6f7fb; margin:0;}" +
                ".card{max-width:560px;margin:10vh auto;background:#fff;border-radius:12px;box-shadow:0 8px 30px rgba(0,0,0,.08);padding:28px;}" +
                ".title{font-size:22px;font-weight:700;margin:0 0 12px;color:" + statusColor + ";}" +
                ".msg{font-size:15px;line-height:1.6;color:#334155;margin:0 0 22px;}" +
                ".btn{display:inline-block;background:#1677ff;color:#fff;text-decoration:none;padding:10px 16px;border-radius:10px;font-size:15px;}" +
                ".hint{margin-top:14px;color:#94a3b8;font-size:12px;}" +
                "</style></head><body>" +
                "<div class='card'>" +
                "<div class='title'>" + safeTitle + "</div>" +
                "<p class='msg'>" + safeMessage + "</p>" +
//                buttonHtml +
//                "<div class='hint'>如果按钮无法跳转，请返回公众号对话继续操作。</div>" +
                "</div></body></html>";
    }

    private String escapeHtml(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
