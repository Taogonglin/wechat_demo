package com.company.wechat.service;

import com.company.wechat.config.WechatWorkConfig;
import com.company.wechat.model.dto.TextMessage;
import com.company.wechat.model.dto.WelcomeMessageRequest;
import com.company.wechat.model.vo.WechatResponse;
import com.company.wechat.util.HttpUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 企业微信API调用服务
 * 
 * @author Company
 */
@Service
public class WechatApiService {

    private static final Logger logger = LoggerFactory.getLogger(WechatApiService.class);
    private static final String ACCESS_TOKEN_KEY = "wechat:work:access_token";
    private static final String GET_TOKEN_URL = "https://qyapi.weixin.qq.com/cgi-bin/gettoken?corpid=%s&corpsecret=%s";
    // 发送欢迎语API（用于新添加的客户）
    private static final String SEND_WELCOME_MSG_URL = "https://qyapi.weixin.qq.com/cgi-bin/externalcontact/send_welcome_msg?access_token=%s";
    // 发送应用消息API（用于已添加的客户）
    private static final String SEND_MESSAGE_URL = "https://qyapi.weixin.qq.com/cgi-bin/message/send?access_token=%s";

    @Autowired
    private WechatWorkConfig config;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private final Gson gson = new Gson();

    /**
     * 获取Access Token（带缓存）
     */
    public String getAccessToken() {
        try {
            // 先从缓存获取
            String token = redisTemplate.opsForValue().get(ACCESS_TOKEN_KEY);
            if (token != null && !token.isEmpty()) {
                logger.debug("从缓存获取Access Token: {}", token);
                return token;
            }

            // 缓存中没有，调用API获取
            String url = String.format(GET_TOKEN_URL, config.getCorpId(), config.getContactSecret());
            String response = HttpUtil.doGet(url);

            WechatResponse<String> result = gson.fromJson(response, new TypeToken<WechatResponse<String>>() {}.getType());

            if (result.isSuccess() && result.getAccessToken() != null) {
                String accessToken = result.getAccessToken();
                // 缓存到Redis，提前5分钟过期
                int expireTime = config.getTokenExpireTime() != null ? config.getTokenExpireTime() : 7000;
                redisTemplate.opsForValue().set(ACCESS_TOKEN_KEY, accessToken, expireTime, TimeUnit.SECONDS);
                logger.info("获取Access Token成功，已缓存，过期时间: {}秒", expireTime);
                return accessToken;
            } else {
                logger.error("获取Access Token失败: {}", response);
                throw new RuntimeException("获取Access Token失败: " + result.getErrMsg());
            }
        } catch (Exception e) {
            logger.error("获取Access Token异常", e);
            throw new RuntimeException("获取Access Token异常", e);
        }
    }

    /**
     * 发送欢迎语给新添加的客户
     * 
     * @param welcomeCode 欢迎语code（从客户添加事件中获取）
     * @param content 消息内容
     */
    public boolean sendWelcomeMessage(String welcomeCode, String content) {
        try {
            String accessToken = getAccessToken();
            String url = String.format(SEND_WELCOME_MSG_URL, accessToken);

            // 构建欢迎语请求
            WelcomeMessageRequest request = WelcomeMessageRequest.builder()
                    .welcomeCode(welcomeCode)
                    .text(WelcomeMessageRequest.TextContent.builder()
                            .content(content)
                            .build())
                    .build();

            String jsonRequest = gson.toJson(request);
            logger.info("发送欢迎语给客户: welcomeCode={}, 内容: {}", welcomeCode, content);

            String response = HttpUtil.doPostString(url, jsonRequest);
            WechatResponse<?> result = gson.fromJson(response, WechatResponse.class);

            if (result.isSuccess()) {
                logger.info("发送欢迎语成功: welcomeCode={}", welcomeCode);
                return true;
            } else {
                logger.error("发送欢迎语失败: welcomeCode={}, 错误: {}", welcomeCode, result.getErrMsg());
                return false;
            }
        } catch (Exception e) {
            logger.error("发送欢迎语异常: welcomeCode=" + welcomeCode, e);
            return false;
        }
    }

    /**
     * 发送文本消息给客户（已添加的客户）
     * 
     * @param externalUserId 客户的external_userid
     * @param content 消息内容
     * @param staffUserId 企业成员UserID
     */
    public boolean sendTextMessage(String externalUserId, String content, String staffUserId) {
        try {
            String accessToken = getAccessToken();
            String url = String.format(SEND_MESSAGE_URL, accessToken);

            // 构建消息内容
            TextMessage textMessage = TextMessage.builder()
                    .content(content)
                    .build();

            // 构建发送请求
            TextMessage.SendRequest sendRequest = TextMessage.SendRequest.builder()
                    .msgType("text")
                    .toUser(externalUserId)
                    .text(textMessage)
                    .enableDuplicateCheck(0)
                    .build();

            String jsonRequest = gson.toJson(sendRequest);
            logger.info("发送消息给客户: {}, 内容: {}", externalUserId, content);

            String response = HttpUtil.doPostString(url, jsonRequest);
            WechatResponse<?> result = gson.fromJson(response, WechatResponse.class);

            if (result.isSuccess()) {
                logger.info("发送消息成功: {}", externalUserId);
                return true;
            } else {
                logger.error("发送消息失败: {}, 错误: {}", externalUserId, result.getErrMsg());
                return false;
            }
        } catch (Exception e) {
            logger.error("发送消息异常: " + externalUserId, e);
            return false;
        }
    }

    /**
     * 清除Access Token缓存
     */
    public void clearAccessToken() {
        redisTemplate.delete(ACCESS_TOKEN_KEY);
        logger.info("已清除Access Token缓存");
    }
}
