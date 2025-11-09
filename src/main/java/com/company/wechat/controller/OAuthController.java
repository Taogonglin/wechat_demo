package com.company.wechat.controller;

import com.company.wechat.config.WechatWorkConfig;
import com.company.wechat.model.vo.WechatOAuthResponse;
import com.company.wechat.service.WechatOAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * OAuth认证控制器
 * 处理企业微信OAuth授权流程
 * 
 * @author Company
 */
@RestController
@RequestMapping("/api/oauth")
public class OAuthController {

    private static final Logger logger = LoggerFactory.getLogger(OAuthController.class);

    @Autowired
    private WechatOAuthService oauthService;

    @Autowired
    private WechatWorkConfig config;

    /**
     * OAuth回调接口
     * 客户点击H5链接后，企业微信会重定向到此接口并携带code
     * 
     * @param code OAuth授权码
     * @param state 状态参数
     * @param response HTTP响应
     */
    @GetMapping("/callback")
    public void oauthCallback(
            @RequestParam String code,
            @RequestParam(required = false) String state,
            HttpServletResponse response) throws IOException {
        
        logger.info("收到OAuth回调，code: {}, state: {}", code, state);
        
        // 通过code获取用户身份
        WechatOAuthResponse userInfo = oauthService.getUserInfo(code);
        
        if (userInfo == null) {
            logger.error("获取用户信息失败");
            response.sendRedirect(config.getH5BaseUrl() + "/error.html?msg=auth_failed");
            return;
        }
        
        // 判断是否为外部联系人（客户）
        if (oauthService.isExternalContact(userInfo)) {
            // 是客户，重定向到H5页面并携带external_userid
            String externalUserId = userInfo.getExternalUserId();
            String redirectUrl = config.getH5BaseUrl() + 
                    "?external_userid=" + URLEncoder.encode(externalUserId, "UTF-8") +
                    "&verified=true";
            
            logger.info("识别为外部联系人，重定向到H5页面，external_userid: {}", externalUserId);
            response.sendRedirect(redirectUrl);
        } else if (userInfo.getUserId() != null) {
            // 是企业内部成员
            logger.info("识别为企业成员，userId: {}", userInfo.getUserId());
            response.sendRedirect(config.getH5BaseUrl() + "/staff.html");
        } else {
            // 其他情况（个人微信用户）
            logger.warn("未识别的用户类型，openid: {}", userInfo.getOpenId());
            response.sendRedirect(config.getH5BaseUrl() + "/error.html?msg=unknown_user");
        }
    }

    /**
     * 获取用户身份信息（API接口）
     * 供前端JS调用
     * 
     * @param code OAuth授权码
     * @return 用户信息
     */
    @GetMapping("/userinfo")
    public Map<String, Object> getUserInfo(@RequestParam String code) {
        logger.info("API获取用户信息，code: {}", code);
        
        WechatOAuthResponse userInfo = oauthService.getUserInfo(code);
        
        Map<String, Object> result = new HashMap<>();
        if (userInfo != null && userInfo.getErrcode() == 0) {
            result.put("success", true);
            result.put("userId", userInfo.getUserId());
            result.put("externalUserId", userInfo.getExternalUserId());
            result.put("openId", userInfo.getOpenId());
            result.put("isExternalContact", oauthService.isExternalContact(userInfo));
        } else {
            result.put("success", false);
            result.put("message", userInfo != null ? userInfo.getErrmsg() : "获取用户信息失败");
        }
        
        return result;
    }

    /**
     * 生成OAuth授权URL
     * 供前端获取授权链接
     * 
     * @param redirectUri 授权后重定向的地址
     * @param state 状态参数
     * @return OAuth授权URL
     */
    @GetMapping("/auth-url")
    public Map<String, Object> getAuthUrl(
            @RequestParam String redirectUri,
            @RequestParam(required = false) String state) {
        
        String authUrl = oauthService.buildOAuthUrl(redirectUri, state);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("authUrl", authUrl);
        
        return result;
    }
}

