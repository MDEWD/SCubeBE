package com.scube.scubebackend.service.impl;

import com.scube.scubebackend.model.dto.WeChatUserInfo;
import com.scube.scubebackend.service.WeChatService;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.result.WxMpQrCodeTicket;
import me.chanjar.weixin.mp.bean.result.WxMpUser;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
    
    @Value("${wechat.mp.oauth-domain:http://localhost:8077}")
    private String oauthDomain;
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
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
                            
                            // 生成授权链接（使用ticket作为state参数）
                            String redirectUri = getOAuthRedirectUri();
                            String authorizationUrl = buildAuthorizationUrl(redirectUri, ticketStr);
                            
                            // 构建回复消息，包含验证码和授权链接
                            String message = "欢迎关注！\n\n" +
                                    "您的登录验证码是：" + verifyCode + "，请在3分钟内使用。\n\n" +
                                    "点击下方链接授权获取昵称和头像（可选）：\n" +
                                    authorizationUrl;
                            
                            return buildTextMessage(toUserName, fromUserName, message);
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
                            
                            // 生成授权链接（使用ticket作为state参数）
                            String redirectUri = getOAuthRedirectUri();
                            String authorizationUrl = buildAuthorizationUrl(redirectUri, ticketStr);
                            
                            // 构建回复消息，包含验证码和授权链接
                            String message = "您的登录验证码是：" + verifyCode + "，请在3分钟内使用。\n\n" +
                                    "点击下方链接授权获取昵称和头像（可选）：\n" +
                                    authorizationUrl;
                            
                            return buildTextMessage(toUserName, fromUserName, message);
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
    
    @Override
    public WeChatUserInfo getUserInfo(String openId) {
        try {
            // 方式1: 先尝试使用SDK的方式（如果可用）
            try {
                WxMpUser wxMpUser = wxMpService.getUserService().userInfo(openId, "zh_CN");
                if (wxMpUser != null) {
                    WeChatUserInfo userInfo = new WeChatUserInfo();
                    userInfo.setOpenId(openId);
                    // 使用已废弃但可用的方法
                    String nickname = wxMpUser.getNickname();
                    String avatar = wxMpUser.getHeadImgUrl();
                    userInfo.setNickname(nickname != null && !nickname.trim().isEmpty() ? nickname : null);
                    userInfo.setAvatar(avatar != null && !avatar.trim().isEmpty() ? avatar : null);
                    
                    // 如果SDK方式获取到了信息，直接返回
                    if (userInfo.getNickname() != null || userInfo.getAvatar() != null) {
                        System.out.println("通过SDK获取用户信息成功 - openId: " + openId);
                        return userInfo;
                    }
                }
            } catch (Exception e) {
                System.out.println("SDK方式获取用户信息失败，尝试直接调用API: " + e.getMessage());
            }
            
            // 方式2: 直接调用微信API（参考链接的方式）
            // 获取access_token
            String accessToken = wxMpService.getAccessToken();
            if (accessToken == null || accessToken.isEmpty()) {
                throw new RuntimeException("无法获取access_token");
            }
            
            // 调用微信API获取用户信息
            // API文档: https://developers.weixin.qq.com/doc/offiaccount/User_Management/Get_users_basic_information.html
            String userInfoUrl = "https://api.weixin.qq.com/cgi-bin/user/info?access_token=" + accessToken + "&openid=" + openId + "&lang=zh_CN";
            String response = restTemplate.getForObject(userInfoUrl, String.class);
            
            System.out.println("微信API返回: " + response);
            
            if (response == null || response.isEmpty()) {
                throw new RuntimeException("微信API返回为空");
            }
            
            // 解析JSON响应
            JsonNode jsonNode = objectMapper.readTree(response);
            
            // 检查是否有错误
            if (jsonNode.has("errcode") && jsonNode.get("errcode").asInt() != 0) {
                int errcode = jsonNode.get("errcode").asInt();
                String errmsg = jsonNode.has("errmsg") ? jsonNode.get("errmsg").asText() : "未知错误";
                System.out.println("微信API错误 - errcode: " + errcode + ", errmsg: " + errmsg);
                
                // 常见错误码说明
                switch (errcode) {
                    case 40013:
                        System.out.println("说明: 无效的openId");
                        break;
                    case 40001:
                        System.out.println("说明: access_token无效或已过期");
                        break;
                    case 48001:
                        System.out.println("说明: api功能未授权，请确认公众号已获得该接口权限");
                        break;
                    case 40003:
                        System.out.println("说明: 需要用户关注公众号才能获取信息");
                        break;
                }
                
                // 返回只有openId的信息
                WeChatUserInfo userInfo = new WeChatUserInfo();
                userInfo.setOpenId(openId);
                userInfo.setNickname(null);
                userInfo.setAvatar(null);
                return userInfo;
            }
            
            // 解析用户信息
            WeChatUserInfo userInfo = new WeChatUserInfo();
            userInfo.setOpenId(openId);
            
            // 获取昵称
            if (jsonNode.has("nickname")) {
                String nickname = jsonNode.get("nickname").asText();
                userInfo.setNickname(nickname != null && !nickname.trim().isEmpty() ? nickname : null);
            }
            
            // 获取头像
            if (jsonNode.has("headimgurl")) {
                String headimgurl = jsonNode.get("headimgurl").asText();
                userInfo.setAvatar(headimgurl != null && !headimgurl.trim().isEmpty() ? headimgurl : null);
            }
            
            System.out.println("通过API获取用户信息成功 - openId: " + openId + ", nickname: " + userInfo.getNickname() + ", avatar: " + userInfo.getAvatar());
            
            return userInfo;
            
        } catch (WxErrorException e) {
            System.out.println("获取用户信息失败 - openId: " + openId + ", 错误: " + e.getMessage());
            WeChatUserInfo userInfo = new WeChatUserInfo();
            userInfo.setOpenId(openId);
            userInfo.setNickname(null);
            userInfo.setAvatar(null);
            return userInfo;
        } catch (Exception e) {
            System.out.println("获取用户信息异常 - openId: " + openId + ", 异常: " + e.getMessage());
            e.printStackTrace();
            WeChatUserInfo userInfo = new WeChatUserInfo();
            userInfo.setOpenId(openId);
            userInfo.setNickname(null);
            userInfo.setAvatar(null);
            return userInfo;
        }
    }
    
    @Override
    public String buildAuthorizationUrl(String redirectUri, String state) {
        try {
            // 使用SDK生成授权链接
            // scope: snsapi_userinfo 表示需要用户授权，可以获取用户信息
            // scope: snsapi_base 表示静默授权，只能获取openId
            String authorizationUrl = wxMpService.getOAuth2Service().buildAuthorizationUrl(redirectUri, "snsapi_userinfo", state);
            System.out.println("生成授权链接: " + authorizationUrl);
            return authorizationUrl;
        } catch (Exception e) {
            throw new RuntimeException("生成授权链接失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public WeChatUserInfo getUserInfoByCode(String code) {
        try {
            System.out.println("通过授权code获取用户信息，code: " + code);
            
            // 直接调用微信API（更可靠的方式）
            // 第一步：通过code获取access_token和openId
            String appId = wxMpService.getWxMpConfigStorage().getAppId();
            String secret = wxMpService.getWxMpConfigStorage().getSecret();
            
            String tokenUrl = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=" + appId 
                    + "&secret=" + secret + "&code=" + code + "&grant_type=authorization_code";
            
            String tokenResponse = restTemplate.getForObject(tokenUrl, String.class);
            System.out.println("获取access_token响应: " + tokenResponse);
            
            if (tokenResponse == null || tokenResponse.isEmpty()) {
                throw new RuntimeException("获取access_token失败：响应为空");
            }
            
            JsonNode tokenNode = objectMapper.readTree(tokenResponse);
            
            // 检查是否有错误
            if (tokenNode.has("errcode")) {
                int errcode = tokenNode.get("errcode").asInt();
                String errmsg = tokenNode.has("errmsg") ? tokenNode.get("errmsg").asText() : "未知错误";
                throw new RuntimeException("获取access_token失败 - errcode: " + errcode + ", errmsg: " + errmsg);
            }
            
            String oauthAccessToken = tokenNode.get("access_token").asText();
            String openId = tokenNode.get("openid").asText();
            
            System.out.println("获取到openId: " + openId);
            
            // 第二步：使用access_token和openId获取用户信息
            String userInfoUrl = "https://api.weixin.qq.com/sns/userinfo?access_token=" + oauthAccessToken 
                    + "&openid=" + openId + "&lang=zh_CN";
            
            String userInfoResponse = restTemplate.getForObject(userInfoUrl, String.class);
            System.out.println("获取用户信息响应: " + userInfoResponse);
            
            WeChatUserInfo userInfo = new WeChatUserInfo();
            userInfo.setOpenId(openId);
            
            if (userInfoResponse != null && !userInfoResponse.isEmpty()) {
                JsonNode userInfoNode = objectMapper.readTree(userInfoResponse);
                
                // 检查是否有错误
                if (userInfoNode.has("errcode")) {
                    int errcode = userInfoNode.get("errcode").asInt();
                    String errmsg = userInfoNode.has("errmsg") ? userInfoNode.get("errmsg").asText() : "未知错误";
                    System.out.println("获取用户信息失败 - errcode: " + errcode + ", errmsg: " + errmsg);
                    
                    // 如果获取用户信息失败，至少返回openId
                    userInfo.setNickname(null);
                    userInfo.setAvatar(null);
                    return userInfo;
                }
                
                // 解析用户信息
                if (userInfoNode.has("nickname")) {
                    String nickname = userInfoNode.get("nickname").asText();
                    userInfo.setNickname(nickname != null && !nickname.trim().isEmpty() ? nickname : null);
                }
                
                if (userInfoNode.has("headimgurl")) {
                    String avatar = userInfoNode.get("headimgurl").asText();
                    userInfo.setAvatar(avatar != null && !avatar.trim().isEmpty() ? avatar : null);
                }
                
                System.out.println("通过授权获取用户信息成功 - openId: " + openId + ", nickname: " + userInfo.getNickname() + ", avatar: " + userInfo.getAvatar());
            } else {
                System.out.println("警告: 用户信息响应为空，可能用户未授权");
                userInfo.setNickname(null);
                userInfo.setAvatar(null);
            }
            
            return userInfo;
            
        } catch (Exception e) {
            System.out.println("通过授权code获取用户信息异常 - code: " + code + ", 异常: " + e.getMessage());
            e.printStackTrace();
            WeChatUserInfo userInfo = new WeChatUserInfo();
            userInfo.setNickname(null);
            userInfo.setAvatar(null);
            return userInfo;
        }
    }
    
    @Override
    public WeChatUserInfo checkTicketScannedWithUserInfo(String ticket) {
        String openId = checkTicketScanned(ticket);
        if (openId == null) {
            return null;
        }
        
        // 先尝试从Redis获取已授权的用户信息
        String userInfoKey = "wechat:userinfo:" + openId;
        WeChatUserInfo cachedUserInfo = (WeChatUserInfo) redisTemplate.opsForValue().get(userInfoKey);
        if (cachedUserInfo != null && (cachedUserInfo.getNickname() != null || cachedUserInfo.getAvatar() != null)) {
            System.out.println("从缓存获取用户信息: " + cachedUserInfo);
            return cachedUserInfo;
        }
        
        // 如果缓存中没有，尝试通过openId获取（可能获取不到，因为需要授权）
        WeChatUserInfo userInfo = getUserInfo(openId);
        if (userInfo != null && userInfo.getOpenId() != null) {
            // 如果获取到了信息，缓存起来
            if (userInfo.getNickname() != null || userInfo.getAvatar() != null) {
                redisTemplate.opsForValue().set(userInfoKey, userInfo, 300, TimeUnit.SECONDS);
            }
        }
        
        return userInfo;
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
     * 获取OAuth回调URI
     */
    private String getOAuthRedirectUri() {
        return oauthDomain + "/api/wechat/oauth/callback";
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

