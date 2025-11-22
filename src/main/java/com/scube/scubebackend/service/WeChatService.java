package com.scube.scubebackend.service;

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
}

