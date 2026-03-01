package com.scube.scubebackend.modules.user.service;

import com.scube.scubebackend.modules.user.model.dto.WeChatUserInfo;
import java.util.Map;

public interface WeChatService {
    /**
     * 生成登录二维码
     * @return 二维码URL和Ticket
     */
    Map<String, String> generateLoginQrCode();
    
    /**
     * 处理微信回调事件
     * @param xmlData 微信推送的XML数据
     * @return 回复给微信的消息
     */
    String handleWeChatCallback(String xmlData);
    
    /**
     * 检查Ticket是否已被扫描
     * @param ticket 二维码Ticket
     * @return openId，如果未扫描返回null
     */
    String checkTicketScanned(String ticket);
    
    /**
     * 检查Ticket是否已被扫描，并返回用户信息
     * @param ticket 二维码Ticket
     * @return 微信用户信息，如果未扫描返回null
     */
    WeChatUserInfo checkTicketScannedWithUserInfo(String ticket);
    
    /**
     * 获取微信用户信息（昵称和头像）
     * @param openId 用户的openId
     * @return 微信用户信息，如果获取失败返回null
     */
    WeChatUserInfo getUserInfo(String openId);
    
    /**
     * 生成微信网页授权链接
     * @param redirectUri 授权后重定向的URI（需要URL编码）
     * @param state 状态参数，用于保持请求和回调的状态
     * @return 授权链接
     */
    String buildAuthorizationUrl(String redirectUri, String state);
    
    /**
     * 通过授权code获取用户信息（昵称和头像）
     * @param code 授权code
     * @return 微信用户信息，包含openId、nickname、avatar
     */
    WeChatUserInfo getUserInfoByCode(String code);

    /**
     * 处理微信网页授权回调，完成用户信息获取、缓存、落库，并返回重定向地址字符串。
     * @param code 授权code
     * @param state 状态参数（ticket）
     * @return redirect: 开头的重定向字符串
     */
    String handleOAuthCallback(String code, String state);
}
