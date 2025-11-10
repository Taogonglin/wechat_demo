package com.company.wechat.service;

import com.company.wechat.config.WechatWorkConfig;
import com.company.wechat.model.dto.CustomerAddEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 客户事件处理服务
 * 
 * @author Company
 */
@Service
public class CustomerEventService {

    private static final Logger logger = LoggerFactory.getLogger(CustomerEventService.class);

    @Autowired
    private WechatWorkConfig config;

    @Autowired
    private WechatApiService wechatApiService;

    /**
     * 处理客户添加事件
     */
    public void handleCustomerAddEvent(String xmlContent) {
        try {
            // 解析XML获取客户信息
            CustomerAddEvent event = parseCustomerAddEvent(xmlContent);

            if (event == null || event.getExternalUserId() == null) {
                logger.warn("客户添加事件解析失败或缺少external_userid");
                return;
            }

            logger.info("收到客户添加事件 - External UserID: {}, Staff UserID: {}, Welcome Code: {}",
                    event.getExternalUserId(), event.getUserId(), event.getWelcomeCode());

            // 生成带external_userid参数的H5链接
            String h5Link = buildH5Link(event.getExternalUserId());

            // 构建欢迎消息
            String welcomeMessage = buildWelcomeMessage(h5Link);

            // 发送欢迎语给客户（使用welcome_code，失败时自动降级）
            boolean success = false;
            if (event.getWelcomeCode() != null && !event.getWelcomeCode().isEmpty()) {
                // 优先使用欢迎语API发送（推荐）
                logger.info("尝试使用欢迎语API发送消息...");
                success = wechatApiService.sendWelcomeMessage(event.getWelcomeCode(), welcomeMessage);
                
                // 如果欢迎语API发送失败（如：客户已开始聊天），降级使用普通消息API
                if (!success) {
                    logger.warn("欢迎语API发送失败（可能客户已发送消息），降级使用普通消息API");
                    success = wechatApiService.sendTextMessage(
                            event.getExternalUserId(),
                            welcomeMessage,
                            event.getUserId()
                    );
                }
            } else {
                // 如果没有welcome_code，直接使用普通消息API
                logger.warn("未获取到welcome_code，使用普通消息API发送");
                success = wechatApiService.sendTextMessage(
                        event.getExternalUserId(),
                        welcomeMessage,
                        event.getUserId()
                );
            }

            if (success) {
                logger.info("✓ 成功发送H5链接给客户: {}", event.getExternalUserId());
            } else {
                logger.error("✗ 发送H5链接失败: {}", event.getExternalUserId());
            }

        } catch (Exception e) {
            logger.error("处理客户添加事件异常", e);
        }
    }

    /**
     * 解析客户添加事件XML
     */
    private CustomerAddEvent parseCustomerAddEvent(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

            Element root = document.getDocumentElement();

            CustomerAddEvent event = new CustomerAddEvent();
            event.setExternalUserId(getElementValue(root, "ExternalUserID"));
            event.setUserId(getElementValue(root, "UserID"));
            event.setState(getElementValue(root, "State"));
            event.setWelcomeCode(getElementValue(root, "WelcomeCode"));

            String createTimeStr = getElementValue(root, "CreateTime");
            if (createTimeStr != null && !createTimeStr.isEmpty()) {
                event.setCreateTime(Long.parseLong(createTimeStr));
            }

            return event;
        } catch (Exception e) {
            logger.error("解析客户添加事件XML失败", e);
            return null;
        }
    }

    /**
     * 获取XML元素的值
     */
    private String getElementValue(Element parent, String tagName) {
        try {
            return parent.getElementsByTagName(tagName).item(0).getTextContent();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 构建H5链接（带external_userid参数）
     */
    private String buildH5Link(String externalUserId) {
        try {
            String encodedUserId = URLEncoder.encode(externalUserId, "UTF-8");
            String link = config.getH5BaseUrl() + "?external_userid=" + encodedUserId;
            logger.debug("生成H5链接: {}", link);
            return link;
        } catch (Exception e) {
            logger.error("构建H5链接失败", e);
            return config.getH5BaseUrl();
        }
    }

    /**
     * 构建欢迎消息
     */
    private String buildWelcomeMessage(String h5Link) {
        return "您好！感谢添加我们的企业微信。\n\n" +
                "请点击下方链接完善您的信息：\n" +
                h5Link + "\n\n" +
                "如有任何问题，欢迎随时联系我们！";
    }
}
