package com.company.wechat.service;

import com.company.wechat.config.WechatWorkConfig;
import com.company.wechat.model.dto.CallbackMessage;
import com.company.wechat.util.AesUtil;
import com.company.wechat.util.WechatSignUtil;
import com.company.wechat.util.XmlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 企业微信回调处理服务
 * 
 * @author Company
 */
@Service
public class WechatCallbackService {

    private static final Logger logger = LoggerFactory.getLogger(WechatCallbackService.class);

    @Autowired
    private WechatWorkConfig config;

    @Autowired
    private CustomerEventService customerEventService;

    /**
     * 验证回调URL
     *
     * @param msgSignature 消息签名
     * @param timestamp 时间戳
     * @param nonce 随机数
     * @param echostr 加密的随机字符串
     * @return 解密后的随机字符串
     */
    public String verifyUrl(String msgSignature, String timestamp, String nonce, String echostr) {
        try {
            // 验证签名
            boolean valid = WechatSignUtil.verifySignature(
                    config.getToken(),
                    timestamp,
                    nonce,
                    echostr,
                    msgSignature
            );

            if (!valid) {
                logger.error("URL验证失败：签名验证不通过");
                return null;
            }

            // 解密echostr
            AesUtil aesUtil = new AesUtil(config.getEncodingAesKey(), config.getCorpId());
            String result = aesUtil.decrypt(echostr);

            logger.info("URL验证成功");
            return result;
        } catch (Exception e) {
            logger.error("URL验证异常", e);
            return null;
        }
    }

    /**
     * 处理回调消息
     *
     * @param msgSignature 消息签名
     * @param timestamp 时间戳
     * @param nonce 随机数
     * @param encryptedData 加密的消息数据
     * @return 响应消息
     */
    public String handleCallback(String msgSignature, String timestamp, String nonce, String encryptedData) {
        try {
            logger.debug("收到回调消息 - Signature: {}, Timestamp: {}, Nonce: {}", msgSignature, timestamp, nonce);

            // 提取加密内容
            String encrypt = extractEncrypt(encryptedData);
            if (encrypt == null) {
                logger.error("提取加密内容失败");
                return buildSuccessResponse();
            }

            // 验证签名
            boolean valid = WechatSignUtil.verifySignature(
                    config.getToken(),
                    timestamp,
                    nonce,
                    encrypt,
                    msgSignature
            );

            if (!valid) {
                logger.error("回调消息签名验证失败");
                return buildSuccessResponse();
            }

            // 解密消息
            AesUtil aesUtil = new AesUtil(config.getEncodingAesKey(), config.getCorpId());
            String xmlContent = aesUtil.decrypt(encrypt);
            
            logger.debug("解密后的消息内容: {}", xmlContent);

            // 解析消息
            CallbackMessage message = XmlUtil.parseCallbackXml(xmlContent);

            // 根据消息类型和事件类型处理
            handleMessage(message, xmlContent);

            // 返回success响应
            return buildSuccessResponse();

        } catch (Exception e) {
            logger.error("处理回调消息异常", e);
            return buildSuccessResponse();
        }
    }

    /**
     * 根据消息类型处理消息
     */
    private void handleMessage(CallbackMessage message, String xmlContent) {
        String msgType = message.getMsgType();
        String event = message.getEvent();

        logger.info("处理消息 - 类型: {}, 事件: {}, 变更类型: {}", msgType, event, message.getChangeType());

        // 处理事件类型消息
        if ("event".equals(msgType)) {
            handleEvent(message, xmlContent);
        }
    }

    /**
     * 处理事件
     */
    private void handleEvent(CallbackMessage message, String xmlContent) {
        String event = message.getEvent();
        String changeType = message.getChangeType();

        // 客户变更事件
        if ("change_external_contact".equals(event)) {
            // 添加客户事件
            if ("add_external_contact".equals(changeType)) {
                logger.info("检测到客户添加事件");
                customerEventService.handleCustomerAddEvent(xmlContent);
            }
            // 删除客户事件
            else if ("del_external_contact".equals(changeType)) {
                logger.info("检测到客户删除事件");
                // 可以在这里处理客户删除逻辑
            }
        }
    }

    /**
     * 从加密数据中提取Encrypt字段
     */
    private String extractEncrypt(String encryptedData) {
        try {
            int start = encryptedData.indexOf("<Encrypt><![CDATA[");
            int end = encryptedData.indexOf("]]></Encrypt>");
            
            if (start != -1 && end != -1) {
                start += "<Encrypt><![CDATA[".length();
                return encryptedData.substring(start, end);
            }
            
            // 尝试其他格式
            start = encryptedData.indexOf("<Encrypt>");
            end = encryptedData.indexOf("</Encrypt>");
            if (start != -1 && end != -1) {
                start += "<Encrypt>".length();
                return encryptedData.substring(start, end);
            }
            
            return null;
        } catch (Exception e) {
            logger.error("提取Encrypt字段失败", e);
            return null;
        }
    }

    /**
     * 构建成功响应
     */
    private String buildSuccessResponse() {
        return "success";
        }
    }
