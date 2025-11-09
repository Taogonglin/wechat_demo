package com.company.wechat.service;

import com.company.wechat.config.WechatWorkConfig;
import com.company.wechat.model.vo.WechatOAuthResponse;
import com.company.wechat.util.HttpUtil;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 企业微信OAuth认证服务
 * 用于通过OAuth获取客户身份信息
 * 
 * @author Company
 */
@Service
public class WechatOAuthService {

    private static final Logger logger = LoggerFactory.getLogger(WechatOAuthService.class);

    // 获取访问用户身份API
    private static final String GET_USER_INFO_URL = "https://qyapi.weixin.qq.com/cgi-bin/user/getuserinfo?access_token=%s&code=%s";
    
    // 获取外部联系人详情API
    private static final String GET_EXTERNAL_CONTACT_URL = "https://qyapi.weixin.qq.com/cgi-bin/externalcontact/get?access_token=%s&external_userid=%s";

    @Autowired
    private WechatApiService wechatApiService;

    @Autowired
    private WechatWorkConfig config;

    private final Gson gson = new Gson();

    /**
     * 通过OAuth code获取用户身份
     * 
     * @param code OAuth授权码
     * @return 用户信息
     */
    public WechatOAuthResponse getUserInfo(String code) {
        try {
            String accessToken = wechatApiService.getAccessToken();
            String url = String.format(GET_USER_INFO_URL, accessToken, code);
            
            logger.info("通过code获取用户信息: code={}", code);
            String response = HttpUtil.doGet(url);
            
            WechatOAuthResponse result = gson.fromJson(response, WechatOAuthResponse.class);
            
            if (result.getErrcode() == 0) {
                logger.info("获取用户信息成功: userId={}, externalUserId={}, openId={}", 
                        result.getUserId(), result.getExternalUserId(), result.getOpenId());
                return result;
            } else {
                logger.error("获取用户信息失败: {}", result.getErrmsg());
                return null;
            }
        } catch (Exception e) {
            logger.error("获取用户信息异常", e);
            return null;
        }
    }

    /**
     * 获取外部联系人详细信息
     * 
     * @param externalUserId 外部联系人ID
     * @return 外部联系人详细信息
     */
    public String getExternalContactDetail(String externalUserId) {
        try {
            String accessToken = wechatApiService.getAccessToken();
            String url = String.format(GET_EXTERNAL_CONTACT_URL, accessToken, externalUserId);
            
            logger.info("获取外部联系人详情: externalUserId={}", externalUserId);
            String response = HttpUtil.doGet(url);
            
            logger.debug("外部联系人详情: {}", response);
            return response;
        } catch (Exception e) {
            logger.error("获取外部联系人详情异常", e);
            return null;
        }
    }

    /**
     * 生成OAuth授权URL
     * 
     * @param redirectUri 授权后重定向的地址
     * @param state 状态参数（可选）
     * @return OAuth授权URL
     */
    public String buildOAuthUrl(String redirectUri, String state) {
        // 企业微信网页授权URL
        String baseUrl = "https://open.weixin.qq.com/connect/oauth2/authorize";
        
        StringBuilder url = new StringBuilder(baseUrl);
        url.append("?appid=").append(config.getCorpId());
        url.append("&redirect_uri=").append(redirectUri);
        url.append("&response_type=code");
        url.append("&scope=snsapi_privateinfo"); // snsapi_base 或 snsapi_privateinfo
        
        if (state != null && !state.isEmpty()) {
            url.append("&state=").append(state);
        }
        
        url.append("#wechat_redirect");
        
        logger.debug("生成OAuth URL: {}", url);
        return url.toString();
    }

    /**
     * 检查用户是否为外部联系人
     * 
     * @param oauthResponse OAuth响应
     * @return true表示是外部联系人
     */
    public boolean isExternalContact(WechatOAuthResponse oauthResponse) {
        return oauthResponse != null && 
               oauthResponse.getExternalUserId() != null && 
               !oauthResponse.getExternalUserId().isEmpty();
    }
}

