package com.scube.scubebackend.service.impl;

import com.scube.scubebackend.service.WeChatService;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.result.WxMpQrCodeTicket;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class WeChatServiceImpl implements WeChatService {
    
    private static final String QR_CODE_URL_PREFIX = "https://mp.weixin.qq.com/cgi-bin/showqrcode?ticket=";
    private static final int QR_CODE_EXPIRE_SECONDS = 300; // 5分钟
    private static final int VERIFY_CODE_EXPIRE_MINUTES = 3; // 验证码3分钟过期
    
    @Autowired
    private WxMpService wxMpService;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Override
    public Map<String, String> generateLoginQrCode() {
        try {
            // 生成临时二维码，scene_id使用时间戳确保唯一性
            int sceneId = (int) (System.currentTimeMillis() % 100000);
            WxMpQrCodeTicket ticket = wxMpService.getQrcodeService().qrCodeCreateTmpTicket(sceneId, QR_CODE_EXPIRE_SECONDS);
            
            String ticketStr = ticket.getTicket();
            String qrCodeUrl = QR_CODE_URL_PREFIX + ticketStr;
            
            // 将Ticket存储到Redis，用于后续验证
            String ticketKey = "wechat:qr:ticket:" + ticketStr;
            redisTemplate.opsForValue().set(ticketKey, "pending", QR_CODE_EXPIRE_SECONDS, TimeUnit.SECONDS);
            
            // 存储sceneId到Ticket的映射，用于扫码时查找
            String sceneKey = "wechat:qr:scene:" + sceneId;
            redisTemplate.opsForValue().set(sceneKey, ticketStr, QR_CODE_EXPIRE_SECONDS, TimeUnit.SECONDS);
            
            Map<String, String> result = new HashMap<>();
            result.put("ticket", ticketStr);
            result.put("qrCodeUrl", qrCodeUrl);
            result.put("expireSeconds", String.valueOf(QR_CODE_EXPIRE_SECONDS));
            
            return result;
        } catch (WxErrorException e) {
            throw new RuntimeException("生成二维码失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public String handleWeChatCallback(String xmlData) {
        try {
            // 添加日志用于调试
            System.out.println("========== 收到微信回调 ==========");
            System.out.println("XML数据: " + xmlData);
            
            Document document = DocumentHelper.parseText(xmlData);
            Element root = document.getRootElement();
            
            String msgType = root.elementText("MsgType");
            String event = root.elementText("Event");
            String fromUserName = root.elementText("FromUserName"); // 用户的openId
            String toUserName = root.elementText("ToUserName");
            String eventKey = root.elementText("EventKey");
            String ticket = root.elementText("Ticket");
            
            System.out.println("解析结果 - msgType: " + msgType + ", event: " + event + ", eventKey: " + eventKey + ", ticket: " + ticket);
            System.out.println("fromUserName (openId): " + fromUserName);
            
            // 处理关注事件（用户扫码关注公众号）
            if ("event".equals(msgType) && "subscribe".equals(event)) {
                System.out.println("处理subscribe事件");
                // 如果是扫码关注，EventKey格式为: qrscene_sceneId
                if (eventKey != null && eventKey.startsWith("qrscene_")) {
                    // 从EventKey中提取sceneId
                    String sceneIdStr = eventKey.substring("qrscene_".length());
                    System.out.println("提取的sceneId: " + sceneIdStr);
                    try {
                        int sceneId = Integer.parseInt(sceneIdStr);
                        
                        // 根据sceneId查找对应的Ticket
                        String sceneKey = "wechat:qr:scene:" + sceneId;
                        String ticketStr = (String) redisTemplate.opsForValue().get(sceneKey);
                        System.out.println("根据sceneId查找Ticket: " + sceneKey + " -> " + ticketStr);
                        
                        if (ticketStr != null) {
                            // 生成6位验证码
                            String verifyCode = generateVerifyCode();
                            System.out.println("生成验证码: " + verifyCode);
                            
                            // 将Ticket和openId绑定，存储到Redis
                            String ticketKey = "wechat:qr:ticket:" + ticketStr;
                            redisTemplate.opsForValue().set(ticketKey, fromUserName, QR_CODE_EXPIRE_SECONDS, TimeUnit.SECONDS);
                            System.out.println("绑定Ticket和openId: " + ticketKey + " -> " + fromUserName);
                            
                            // 将验证码存储到Redis，key为openId，3分钟过期
                            String codeKey = "login:code:" + fromUserName;
                            redisTemplate.opsForValue().set(codeKey, verifyCode, VERIFY_CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);
                            System.out.println("存储验证码: " + codeKey + " -> " + verifyCode);
                            
                            // 构建回复消息
                            return buildTextMessage(toUserName, fromUserName, 
                                "欢迎关注！您的登录验证码是：" + verifyCode + "，请在3分钟内使用。");
                        } else {
                            System.out.println("警告: 未找到对应的Ticket，sceneId: " + sceneId);
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("sceneId解析失败: " + sceneIdStr + ", 错误: " + e.getMessage());
                    }
                } else {
                    System.out.println("EventKey格式不正确或为空: " + eventKey);
                }
            }
            
            // 处理已关注用户扫码事件（SCAN事件）
            if ("event".equals(msgType) && "SCAN".equals(event)) {
                System.out.println("处理SCAN事件（已关注用户扫码）");
                // 已关注用户扫码，EventKey直接是sceneId（数字字符串）
                if (eventKey != null && !eventKey.isEmpty()) {
                    try {
                        int sceneId = Integer.parseInt(eventKey);
                        System.out.println("提取的sceneId: " + sceneId);
                        
                        // 根据sceneId查找对应的Ticket
                        String sceneKey = "wechat:qr:scene:" + sceneId;
                        String ticketStr = (String) redisTemplate.opsForValue().get(sceneKey);
                        System.out.println("根据sceneId查找Ticket: " + sceneKey + " -> " + ticketStr);
                        
                        if (ticketStr != null) {
                            // 生成6位验证码
                            String verifyCode = generateVerifyCode();
                            System.out.println("生成验证码: " + verifyCode);
                            
                            // 将Ticket和openId绑定，存储到Redis
                            String ticketKey = "wechat:qr:ticket:" + ticketStr;
                            redisTemplate.opsForValue().set(ticketKey, fromUserName, QR_CODE_EXPIRE_SECONDS, TimeUnit.SECONDS);
                            System.out.println("绑定Ticket和openId: " + ticketKey + " -> " + fromUserName);
                            
                            // 将验证码存储到Redis，key为openId，3分钟过期
                            String codeKey = "login:code:" + fromUserName;
                            redisTemplate.opsForValue().set(codeKey, verifyCode, VERIFY_CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);
                            System.out.println("存储验证码: " + codeKey + " -> " + verifyCode);
                            
                            // 构建回复消息
                            return buildTextMessage(toUserName, fromUserName, 
                                "您的登录验证码是：" + verifyCode + "，请在3分钟内使用。");
                        } else {
                            System.out.println("警告: 未找到对应的Ticket，sceneId: " + sceneId);
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("sceneId解析失败: " + eventKey + ", 错误: " + e.getMessage());
                    }
                } else {
                    System.out.println("EventKey为空");
                }
            }
            
            // 处理文本消息（用户发送"登录"关键词）
            if ("text".equals(msgType)) {
                String content = root.elementText("Content");
                if ("登录".equals(content) || "login".equalsIgnoreCase(content)) {
                    // 生成6位验证码
                    String verifyCode = generateVerifyCode();
                    
                    // 将验证码存储到Redis，key为openId，3分钟过期
                    String codeKey = "login:code:" + fromUserName;
                    redisTemplate.opsForValue().set(codeKey, verifyCode, VERIFY_CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);
                    
                    // 构建回复消息
                    return buildTextMessage(toUserName, fromUserName, 
                        "您的登录验证码是：" + verifyCode + "，请在3分钟内使用。");
                }
            }
            
            // 默认回复
            return buildTextMessage(toUserName, fromUserName, "欢迎关注！发送\"登录\"获取验证码。");
            
        } catch (Exception e) {
            throw new RuntimeException("处理微信回调失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public String checkTicketScanned(String ticket) {
        String ticketKey = "wechat:qr:ticket:" + ticket;
        Object openId = redisTemplate.opsForValue().get(ticketKey);
        System.out.println("检查Ticket: " + ticketKey + " -> " + openId);
        if (openId != null && !"pending".equals(openId.toString())) {
            return openId.toString();
        }
        return null;
    }
    
    /**
     * 生成6位数字验证码
     */
    private String generateVerifyCode() {
        Random random = new Random();
        int code = random.nextInt(900000) + 100000; // 生成100000-999999之间的随机数
        return String.valueOf(code);
    }
    
    /**
     * 构建文本消息回复
     */
    private String buildTextMessage(String toUserName, String fromUserName, String content) {
        return String.format(
            "<xml>" +
            "<ToUserName><![CDATA[%s]]></ToUserName>" +
            "<FromUserName><![CDATA[%s]]></FromUserName>" +
            "<CreateTime>%d</CreateTime>" +
            "<MsgType><![CDATA[text]]></MsgType>" +
            "<Content><![CDATA[%s]]></Content>" +
            "</xml>",
            fromUserName, toUserName, System.currentTimeMillis() / 1000, content
        );
    }
}

