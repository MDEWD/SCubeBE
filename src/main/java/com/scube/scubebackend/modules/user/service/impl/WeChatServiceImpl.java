package com.scube.scubebackend.modules.user.service.impl;

import com.scube.scubebackend.modules.user.mapper.UserMapper;
import com.scube.scubebackend.modules.user.model.entity.User;
import com.scube.scubebackend.modules.user.model.dto.WeChatUserInfo;
import com.scube.scubebackend.modules.user.service.WeChatService;
import com.scube.scubebackend.util.DisplayIDGenerator;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.kefu.WxMpKefuMessage;
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
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class WeChatServiceImpl implements WeChatService {

    private static final String QR_CODE_URL_PREFIX = "https://mp.weixin.qq.com/cgi-bin/showqrcode?ticket=";
    private static final int QR_CODE_EXPIRE_SECONDS = 300; // 5分钟
    private static final int VERIFY_CODE_EXPIRE_MINUTES = 3; // 验证码3分钟过期

    @Autowired
    private WxMpService wxMpService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DisplayIDGenerator displayIDGenerator;

    @Value("${wechat.mp.oauth-domain:http://localhost:8077}")
    private String oauthDomain;

    @Value("${frontend.oauth-callback-url:http://localhost:3000/auth/callback}")
    private String frontendOauthCallbackUrl;

    @Value("${frontend.oauth-error-url:http://localhost:3000/auth/error}")
    private String frontendOauthErrorUrl;

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
            log.info("========== 收到微信回调 ==========");
            log.debug("XML数据: {}", xmlData);

            Document document = DocumentHelper.parseText(xmlData);
            Element root = document.getRootElement();

            String msgType = root.elementText("MsgType");
            String event = root.elementText("Event");
            String fromUserName = root.elementText("FromUserName"); // 用户的openId
            String toUserName = root.elementText("ToUserName");
            String eventKey = root.elementText("EventKey");
            String ticket = root.elementText("Ticket");

            log.debug("解析结果 - msgType: {}, event: {}, eventKey: {}, ticket: {}", msgType, event, eventKey, ticket);
            log.debug("fromUserName (openId): {}", fromUserName);

            // 处理关注事件（用户扫码关注公众号）
            if ("event".equals(msgType) && "subscribe".equals(event)) {
                log.info("处理subscribe事件");
                // 如果是扫码关注，EventKey格式为: qrscene_sceneId
                if (eventKey != null && eventKey.startsWith("qrscene_")) {
                    // 从EventKey中提取sceneId
                    String sceneIdStr = eventKey.substring("qrscene_".length());
                    log.debug("提取的sceneId: {}", sceneIdStr);
                    try {
                        int sceneId = Integer.parseInt(sceneIdStr);

                        // 根据sceneId查找对应的Ticket
                        String sceneKey = "wechat:qr:scene:" + sceneId;
                        String ticketStr = (String) redisTemplate.opsForValue().get(sceneKey);
                        log.debug("根据sceneId查找Ticket: {} -> {}", sceneKey, ticketStr);

                        if (ticketStr != null) {
                            // 生成授权链接（使用ticket作为state参数）
                            String redirectUri = getOAuthRedirectUri();
                            String authorizationUrl = buildAuthorizationUrl(redirectUri, ticketStr);

                            // 1) 通过客服消息主动推送授权链接（更符合你希望的“服务号发链接”）
                            sendKefuAuthLink(fromUserName, authorizationUrl);

                            // 2) 同时返回被动回复（防止客服消息受限/失败时用户看不到）
                            String message = "欢迎关注 SCube！\n\n" +
                                    "我已给你发送了一条授权登录消息，请在公众号对话里点击链接完成授权。\n\n" +
                                    "如果未收到，可点击这里：\n" +
                                    "<a href=\"" + authorizationUrl + "\">点击这里立即登录</a>";

                            return buildTextMessage(toUserName, fromUserName, message);
                        }
                    } catch (NumberFormatException e) {
                        log.warn("sceneId解析失败: {}, 错误: {}", sceneIdStr, e.getMessage());
                    }
                }
            }

            // 处理已关注用户扫码事件（SCAN事件）
            if ("event".equals(msgType) && "SCAN".equals(event)) {
                log.info("处理SCAN事件（已关注用户扫码）");
                // 已关注用户扫码，EventKey直接是sceneId（数字字符串）
                if (eventKey != null && !eventKey.isEmpty()) {
                    try {
                        int sceneId = Integer.parseInt(eventKey);
                        log.debug("提取的sceneId: {}", sceneId);

                        // 根据sceneId查找对应的Ticket
                        String sceneKey = "wechat:qr:scene:" + sceneId;
                        String ticketStr = (String) redisTemplate.opsForValue().get(sceneKey);
                        log.debug("根据sceneId查找Ticket: {} -> {}", sceneKey, ticketStr);

                        if (ticketStr != null) {
                          String ticketKey = "wechat:qr:ticket:" + ticketStr;
                          String openIdKey = ticketKey + ":openid";
                          String statusKey = ticketKey + ":status";

                          redisTemplate.opsForValue().set(openIdKey, fromUserName, QR_CODE_EXPIRE_SECONDS, TimeUnit.SECONDS);
                          redisTemplate.opsForValue().set(statusKey, "scanned", QR_CODE_EXPIRE_SECONDS, TimeUnit.SECONDS);
                          redisTemplate.opsForValue().set(ticketKey, fromUserName, QR_CODE_EXPIRE_SECONDS, TimeUnit.SECONDS);

                          // 核心：通过服务号（公众号）客服消息，给用户推送授权链接
                          String redirectUri = getOAuthRedirectUri();
                          String authorizationUrl = buildAuthorizationUrl(redirectUri, ticketStr);
                          sendKefuAuthLink(fromUserName, authorizationUrl);

                          String message = "扫码成功！我已给你发送授权登录链接，请在公众号聊天窗口点击完成授权。";
                          return buildTextMessage(toUserName, fromUserName, message);
                        }
                    } catch (NumberFormatException e) {
                        log.warn("sceneId解析失败: {}, 错误: {}", eventKey, e.getMessage());
                    }
                } else {
                    log.warn("EventKey为空");
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
            log.error("处理微信回调失败", e);
            throw new RuntimeException("处理微信回调失败: " + e.getMessage(), e);
        }
    }

    /**
     * 通过公众号客服消息推送授权链接（用户在手机微信里点开即可完成 OAuth 授权）
     * 注意：需要公众号后台开通“客服消息”能力；并且在用户最近与公众号互动的时间窗口内才能推送。
     */
    private void sendKefuAuthLink(String openId, String authorizationUrl) {
        try {
            String text = "请点击链接完成授权登录（用于获取头像/昵称）：\n" + authorizationUrl;
            WxMpKefuMessage kefuMessage = WxMpKefuMessage.TEXT().toUser(openId).content(text).build();
            wxMpService.getKefuService().sendKefuMessage(kefuMessage);
            log.info("已通过客服消息发送授权链接给用户: {}", openId);
        } catch (Exception ex) {
            log.warn("发送客服消息失败(openId={}): {}", openId, ex.getMessage());
        }
    }

    @Override
    public String checkTicketScanned(String ticket) {
        String ticketKey = "wechat:qr:ticket:" + ticket;

        // 新结构：优先读取 openidKey（SCAN事件会写入）
        String openIdKey = ticketKey + ":openid";
        Object openIdV2 = redisTemplate.opsForValue().get(openIdKey);
        if (openIdV2 != null) {
            log.debug("检查Ticket(V2): {} -> {}", openIdKey, openIdV2);
            return openIdV2.toString();
        }

        // 兼容旧结构
        Object openId = redisTemplate.opsForValue().get(ticketKey);
        log.debug("检查Ticket: {} -> {}", ticketKey, openId);
        if (openId != null && !"pending".equals(openId.toString())) {
            return openId.toString();
        }
        return null;
    }

    @Override
    public WeChatUserInfo checkTicketScannedWithUserInfo(String ticket) {
        try {
            String openId = checkTicketScanned(ticket);
            if (openId == null) {
                return null;
            }

            // 标记授权状态（oauth 回调会写入 authed）
            String statusKey = "wechat:qr:ticket:" + ticket + ":status";
            Object status = redisTemplate.opsForValue().get(statusKey);
            boolean authorized = status != null && "authed".equalsIgnoreCase(status.toString());

            // 先尝试从Redis获取已授权的用户信息
            String userInfoKey = "wechat:userinfo:" + openId;
            WeChatUserInfo cachedUserInfo = (WeChatUserInfo) redisTemplate.opsForValue().get(userInfoKey);
            if (cachedUserInfo != null && (cachedUserInfo.getNickname() != null || cachedUserInfo.getAvatar() != null)) {
                cachedUserInfo.setNeedAuthorize(false);
                cachedUserInfo.setAuthorizationUrl(null);
                // 只有当 oauth 回调真正完成时才算 authorized
                cachedUserInfo.setNeedAuthorize(!authorized);
                log.info("从缓存获取用户信息: {}", cachedUserInfo);
                return cachedUserInfo;
            }

            // 缓存没有的话，尝试通过 openId 获取（可能拿不到昵称/头像）
            WeChatUserInfo userInfo = getUserInfo(openId);

            boolean missingProfile = (userInfo == null)
                    || (userInfo.getNickname() == null && userInfo.getAvatar() == null);

            if (missingProfile) {
                WeChatUserInfo minimal = userInfo != null ? userInfo : new WeChatUserInfo();
                minimal.setOpenId(openId);
                minimal.setNickname(null);
                minimal.setAvatar(null);

                try {
                    String redirectUri = getOAuthRedirectUri();
                    String authorizationUrl = buildAuthorizationUrl(redirectUri, ticket);
                    minimal.setNeedAuthorize(!authorized);
                    minimal.setAuthorizationUrl(authorizationUrl);
                } catch (Exception e) {
                    minimal.setNeedAuthorize(!authorized);
                    minimal.setAuthorizationUrl(null);
                }
                return minimal;
            }

            // 拿到了资料则缓存
            if (userInfo.getNickname() != null || userInfo.getAvatar() != null) {
                redisTemplate.opsForValue().set(userInfoKey, userInfo, 300, TimeUnit.SECONDS);
            }

            userInfo.setNeedAuthorize(!authorized);
            userInfo.setAuthorizationUrl(null);
            return userInfo;
        } catch (Exception e) {
            log.error("checkTicketScannedWithUserInfo失败(ticket={})", ticket, e);
            throw new RuntimeException("checkTicketScannedWithUserInfo失败: " + e.getMessage(), e);
        }
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
                        log.info("通过SDK获取用户信息成功 - openId: {}", openId);
                        return userInfo;
                    }
                }
            } catch (Exception e) {
                log.debug("SDK方式获取用户信息失败，尝试直接调用API: {}", e.getMessage());
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

            log.debug("微信API返回(openId={}): {}", openId, response);

            if (response == null || response.isEmpty()) {
                throw new RuntimeException("微信API返回为空");
            }

            // 解析JSON响应
            JsonNode jsonNode = objectMapper.readTree(response);

            // 检查是否有错误
            if (jsonNode.has("errcode") && jsonNode.get("errcode").asInt() != 0) {
                int errcode = jsonNode.get("errcode").asInt();
                String errmsg = jsonNode.has("errmsg") ? jsonNode.get("errmsg").asText() : "未知错误";
                log.warn("微信API错误(openId={}): errcode={}, errmsg={}", openId, errcode, errmsg);

                // 常见错误码说明
                switch (errcode) {
                    case 40013:
                        log.warn("说明: 无效的openId");
                        break;
                    case 40001:
                        log.warn("说明: access_token无效或已过期");
                        break;
                    case 48001:
                        log.warn("说明: api功能未授权，请确认公众号已获得该接口权限");
                        break;
                    case 40003:
                        log.warn("说明: 需要用户关注公众号才能获取信息");
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

            log.info("通过API获取用户信息成功 - openId: {}, nicknamePresent={}, avatarPresent={}", openId,
                    userInfo.getNickname() != null, userInfo.getAvatar() != null);

            return userInfo;

        } catch (WxErrorException e) {
            log.warn("获取用户信息失败(openId={}): {}", openId, e.getMessage());
            WeChatUserInfo userInfo = new WeChatUserInfo();
            userInfo.setOpenId(openId);
            userInfo.setNickname(null);
            userInfo.setAvatar(null);
            return userInfo;
        } catch (Exception e) {
            log.error("获取用户信息异常(openId={})", openId, e);
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
            String authorizationUrl = wxMpService.getOAuth2Service().buildAuthorizationUrl(redirectUri, "snsapi_userinfo", state);
            log.debug("生成授权链接(state={}): {}", state, authorizationUrl);
            return authorizationUrl;
        } catch (Exception e) {
            throw new RuntimeException("生成授权链接失败: " + e.getMessage(), e);
        }
    }

    @Override
    public WeChatUserInfo getUserInfoByCode(String code) {
        try {
            log.info("通过授权code获取用户信息");
            log.debug("code: {}", code);

            String appId = wxMpService.getWxMpConfigStorage().getAppId();
            String secret = wxMpService.getWxMpConfigStorage().getSecret();

            String tokenUrl = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=" + appId
                    + "&secret=" + secret + "&code=" + code + "&grant_type=authorization_code";

            String tokenResponse = restTemplate.getForObject(tokenUrl, String.class);
            log.debug("获取access_token响应: {}", tokenResponse);

            if (tokenResponse == null || tokenResponse.isEmpty()) {
                throw new RuntimeException("获取access_token失败：响应为空");
            }

            JsonNode tokenNode = objectMapper.readTree(tokenResponse);

            if (tokenNode.has("errcode")) {
                int errcode = tokenNode.get("errcode").asInt();
                String errmsg = tokenNode.has("errmsg") ? tokenNode.get("errmsg").asText() : "未知错误";
                throw new RuntimeException("获取access_token失败 - errcode: " + errcode + ", errmsg: " + errmsg);
            }

            String oauthAccessToken = tokenNode.get("access_token").asText();
            String openId = tokenNode.get("openid").asText();

            log.info("OAuth获取到openId: {}", openId);

            String userInfoUrl = "https://api.weixin.qq.com/sns/userinfo?access_token=" + oauthAccessToken
                    + "&openid=" + openId + "&lang=zh_CN";

            String userInfoResponse = restTemplate.getForObject(userInfoUrl, String.class);
            log.debug("获取用户信息响应(openId={}): {}", openId, userInfoResponse);

            WeChatUserInfo userInfo = new WeChatUserInfo();
            userInfo.setOpenId(openId);

            if (userInfoResponse != null && !userInfoResponse.isEmpty()) {
                JsonNode userInfoNode = objectMapper.readTree(userInfoResponse);

                if (userInfoNode.has("errcode")) {
                    int errcode = userInfoNode.get("errcode").asInt();
                    String errmsg = userInfoNode.has("errmsg") ? userInfoNode.get("errmsg").asText() : "未知错误";
                    log.warn("获取用户信息失败(openId={}): errcode={}, errmsg={}", openId, errcode, errmsg);
                    userInfo.setNickname(null);
                    userInfo.setAvatar(null);
                    return userInfo;
                }

                if (userInfoNode.has("nickname")) {
                    String nickname = userInfoNode.get("nickname").asText();
                    userInfo.setNickname(nickname != null && !nickname.trim().isEmpty() ? nickname : null);
                }

                if (userInfoNode.has("headimgurl")) {
                    String avatar = userInfoNode.get("headimgurl").asText();
                    userInfo.setAvatar(avatar != null && !avatar.trim().isEmpty() ? avatar : null);
                }

                log.info("通过授权获取用户信息成功 - openId: {}, nicknamePresent={}, avatarPresent={} ", openId,
                        userInfo, userInfo.getAvatar() != null);
            } else {
                log.warn("用户信息响应为空(openId={})，可能用户未授权", openId);
                userInfo.setNickname(null);
                userInfo.setAvatar(null);
            }

            return userInfo;

        } catch (Exception e) {
            log.error("通过授权code获取用户信息异常", e);
            WeChatUserInfo userInfo = new WeChatUserInfo();
            userInfo.setNickname(null);
            userInfo.setAvatar(null);
            return userInfo;
        }
    }

    /**
     * 获取OAuth回调URI
     */
    private String getOAuthRedirectUri() {
        return oauthDomain + "/api/wechat/oauth/callback";
    }

    /**
     * 生成6位数字验证码
     */
    private String generateVerifyCode() {
        Random random = new Random();
        int code = random.nextInt(900000) + 100000;
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

    @Override
    public String handleOAuthCallback(String code, String state) {
        try {
            WeChatUserInfo userInfo = getUserInfoByCode(code);

            if (userInfo == null || userInfo.getOpenId() == null || userInfo.getOpenId().isEmpty()) {
                throw new RuntimeException("未获取到openId");
            }

            // 将用户信息存储到Redis，key为state（通常是ticket）
            if (state != null && !state.isEmpty()) {
                String ticketKey = "wechat:qr:ticket:" + state;
                redisTemplate.opsForValue().set(ticketKey, userInfo.getOpenId(), 300, TimeUnit.SECONDS);

                // 存储用户信息（包含昵称和头像）
                String userInfoKey = "wechat:userinfo:" + userInfo.getOpenId();
                redisTemplate.opsForValue().set(userInfoKey, userInfo, 300, TimeUnit.SECONDS);

                // 同时写入新结构（便于轮询端读取）
                redisTemplate.opsForValue().set(ticketKey + ":openid", userInfo.getOpenId(), 300, TimeUnit.SECONDS);
                redisTemplate.opsForValue().set(ticketKey + ":status", "authed", 300, TimeUnit.SECONDS);
            }

            // 落库：openId 唯一，存在则更新 nickname/avatar，不存在则创建
            User dbUser = userMapper.selectByOpenId(userInfo.getOpenId());
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            if (dbUser == null) {
                dbUser = new User();
                dbUser.setOpenId(userInfo.getOpenId());
                dbUser.setNickname(userInfo.getNickname());
                dbUser.setAvatar(userInfo.getAvatar());
                dbUser.setUserRole("USER");
                dbUser.setCreateTime(now);
                dbUser.setUpdateTime(now);
                dbUser.setIsDelete(0);

                dbUser.setDisplayId(generateUniqueDisplayId());

                userMapper.insert(dbUser);
            } else {
                boolean needUpdate = false;
                if (userInfo.getNickname() != null && !userInfo.getNickname().isEmpty() && !userInfo.getNickname().equals(dbUser.getNickname())) {
                    dbUser.setNickname(userInfo.getNickname());
                    needUpdate = true;
                }
                if (userInfo.getAvatar() != null && !userInfo.getAvatar().isEmpty() && !userInfo.getAvatar().equals(dbUser.getAvatar())) {
                    dbUser.setAvatar(userInfo.getAvatar());
                    needUpdate = true;
                }
                if (needUpdate) {
                    dbUser.setUpdateTime(now);
                    userMapper.updateById(dbUser);
                }
            }

            String redirectUrl = frontendOauthCallbackUrl + "?openId=" + userInfo.getOpenId();
            if (userInfo.getNickname() != null) {
                redirectUrl += "&nickname=" + java.net.URLEncoder.encode(userInfo.getNickname(), java.nio.charset.StandardCharsets.UTF_8);
            }
            if (userInfo.getAvatar() != null) {
                redirectUrl += "&avatar=" + java.net.URLEncoder.encode(userInfo.getAvatar(), java.nio.charset.StandardCharsets.UTF_8);
            }

            return "redirect:" + redirectUrl;
        } catch (Exception e) {
            log.error("处理OAuth回调失败", e);
            try {
                return "redirect:" + frontendOauthErrorUrl + "?message=" + java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
            } catch (Exception ex) {
                return "redirect:" + frontendOauthErrorUrl + "?message=oauth_failed";
            }
        }
    }

    private String generateUniqueDisplayId() {
        String displayId;
        boolean isUnique;
        do {
            displayId = displayIDGenerator.generateDisplayID();
            isUnique = !userMapper.existsByDisplayId(displayId);
        } while (!isUnique);
        return displayId;
    }
}
