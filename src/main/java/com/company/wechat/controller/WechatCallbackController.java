package com.company.wechat.controller;

import com.company.wechat.service.WechatCallbackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 企业微信回调接口控制器
 * 
 * @author Company
 */
@RestController
@RequestMapping("/api/wechat")
public class WechatCallbackController {

    private static final Logger logger = LoggerFactory.getLogger(WechatCallbackController.class);

    @Autowired
    private WechatCallbackService callbackService;

    /**
     * 验证回调URL（GET请求）
     * 企业微信会发送GET请求来验证回调URL的有效性
     * 
     * @param msg_signature 消息签名
     * @param timestamp 时间戳
     * @param nonce 随机数
     * @param echostr 加密的随机字符串
     * @return 解密后的随机字符串
     */
    @GetMapping("/callback")
    public String verifyCallback(
            @RequestParam("msg_signature") String msg_signature,
            @RequestParam("timestamp") String timestamp,
            @RequestParam("nonce") String nonce,
            @RequestParam("echostr") String echostr) {
        
        logger.info("收到URL验证请求 - Timestamp: {}, Nonce: {}", timestamp, nonce);
        
        String result = callbackService.verifyUrl(msg_signature, timestamp, nonce, echostr);
        
        if (result != null) {
            logger.info("URL验证成功");
            return result;
        } else {
            logger.error("URL验证失败");
            return "fail";
        }
    }

    /**
     * 接收回调消息（POST请求）
     * 企业微信会通过POST请求推送各种事件消息
     * 
     * @param msg_signature 消息签名
     * @param timestamp 时间戳
     * @param nonce 随机数
     * @param requestBody 加密的消息体（XML格式）
     * @return 响应消息
     */
    @PostMapping(value = "/callback", produces = "text/plain;charset=UTF-8")
    public String receiveCallback(
            @RequestParam("msg_signature") String msg_signature,
            @RequestParam("timestamp") String timestamp,
            @RequestParam("nonce") String nonce,
            @RequestBody String requestBody) {
        
        logger.info("收到回调消息 - Timestamp: {}, Nonce: {}", timestamp, nonce);
        logger.debug("消息体: {}", requestBody);
        
        return callbackService.handleCallback(msg_signature, timestamp, nonce, requestBody);
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
