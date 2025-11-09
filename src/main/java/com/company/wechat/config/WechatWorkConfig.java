package com.company.wechat.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 企业微信配置类
 * 
 * @author Company
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "wechat.work")
public class WechatWorkConfig {

    /**
     * 企业ID
     */
    private String corpId;

    /**
     * 应用Secret（用于获取access_token）
     * 注意：需要使用"微信客服"应用的secret，不是"客户联系"的secret
     */
    private String appSecret;

    /**
     * 回调Token
     */
    private String token;

    /**
     * 回调EncodingAESKey
     */
    private String encodingAesKey;

    /**
     * H5链接基础URL
     */
    private String h5BaseUrl;

    /**
     * Access Token缓存时间（秒）
     */
    private Integer tokenExpireTime = 7000;
}
