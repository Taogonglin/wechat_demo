package com.company.wechat.service;

import com.company.wechat.config.WechatWorkConfig;
import com.company.wechat.model.dto.BatchMessageRequest;
import com.company.wechat.model.dto.BatchMessageResponse;
import com.company.wechat.model.dto.ExternalContactListResponse;
import com.company.wechat.util.HttpUtil;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 外部客户管理服务
 * 用于管理存量客户
 * 
 * @author Company
 */
@Service
public class ExternalCustomerService {

    private static final Logger logger = LoggerFactory.getLogger(ExternalCustomerService.class);
    
    private static final String GET_EXTERNAL_CONTACT_LIST_URL = "https://qyapi.weixin.qq.com/cgi-bin/externalcontact/list?access_token=%s&userid=%s";
    private static final String ADD_MSG_TEMPLATE_URL = "https://qyapi.weixin.qq.com/cgi-bin/externalcontact/add_msg_template?access_token=%s";

    @Autowired
    private WechatApiService wechatApiService;
    
    @Autowired
    private WechatWorkConfig config;
    
    private final Gson gson = new Gson();

    /**
     * 获取指定员工的客户列表
     * 
     * @param staffUserId 企业成员UserID
     * @return 客户external_userid列表
     */
    public List<String> getCustomerList(String staffUserId) {
        try {
            String accessToken = wechatApiService.getAccessToken();
            String url = String.format(GET_EXTERNAL_CONTACT_LIST_URL, accessToken, staffUserId);
            
            logger.info("获取员工{}的客户列表", staffUserId);
            String response = HttpUtil.doGet(url);
            
            ExternalContactListResponse result = gson.fromJson(response, ExternalContactListResponse.class);
            
            if (result.isSuccess()) {
                List<String> customerIds = result.getExternalUserid();
                logger.info("获取到{}个客户", customerIds != null ? customerIds.size() : 0);
                return customerIds != null ? customerIds : Collections.emptyList();
            } else {
                logger.error("获取客户列表失败: {}", result.getErrMsg());
                return Collections.emptyList();
            }
        } catch (Exception e) {
            logger.error("获取客户列表异常", e);
            return Collections.emptyList();
        }
    }

    /**
     * 批量发送消息给客户（群发）
     * 
     * @param staffUserIds 发送消息的员工UserID列表
     * @param content 消息内容
     * @param linkTitle 链接标题（可选）
     * @param linkUrl 链接URL（可选）
     * @return 发送结果
     */
    public BatchMessageResponse sendBatchMessage(List<String> staffUserIds, String content, 
                                                   String linkTitle, String linkUrl) {
        try {
            String accessToken = wechatApiService.getAccessToken();
            String url = String.format(ADD_MSG_TEMPLATE_URL, accessToken);
            
            // 构建请求
            BatchMessageRequest.BatchMessageRequestBuilder requestBuilder = BatchMessageRequest.builder()
                    .chatType("single") // 单聊
                    .sender(BatchMessageRequest.Sender.builder()
                            .senderList(staffUserIds)
                            .build())
                    .externalContact(BatchMessageRequest.ExternalContact.builder()
                            .tagList(new ArrayList<>()) // 空表示发给所有客户
                            .build())
                    .text(BatchMessageRequest.TextContent.builder()
                            .content(content)
                            .build());
            
            // 如果有链接附件，添加链接
            if (linkTitle != null && linkUrl != null) {
                List<BatchMessageRequest.Attachment> attachments = new ArrayList<>();
                BatchMessageRequest.Attachment linkAttachment = BatchMessageRequest.Attachment.builder()
                        .msgType("link")
                        .link(BatchMessageRequest.Link.builder()
                                .title(linkTitle)
                                .url(linkUrl)
                                .desc("点击查看详情")
                                .build())
                        .build();
                attachments.add(linkAttachment);
                requestBuilder.attachments(attachments);
            }
            
            BatchMessageRequest request = requestBuilder.build();
            
            String jsonRequest = gson.toJson(request);
            logger.info("创建群发任务，员工列表: {}", staffUserIds);
            
            String response = HttpUtil.doPostString(url, jsonRequest);
            BatchMessageResponse result = gson.fromJson(response, BatchMessageResponse.class);
            
            if (result.isSuccess()) {
                logger.info("创建群发任务成功，msgId: {}", result.getMsgId());
            } else {
                logger.error("创建群发任务失败: {}", result.getErrMsg());
            }
            
            return result;
        } catch (Exception e) {
            logger.error("批量发送消息异常", e);
            BatchMessageResponse errorResponse = new BatchMessageResponse();
            errorResponse.setErrCode(-1);
            errorResponse.setErrMsg("系统异常: " + e.getMessage());
            return errorResponse;
        }
    }

    /**
     * 给存量客户发送H5链接（通过群发）
     * 
     * @param staffUserIds 员工UserID列表
     * @return 发送结果
     */
    public BatchMessageResponse sendH5LinkToExistingCustomers(List<String> staffUserIds) {
        String h5Url = config.getH5BaseUrl();
        
        String message = "您好！为了更好地为您服务，请点击下方链接完善您的信息。\n\n" +
                        "感谢您的配合！";
        
        String linkTitle = "完善客户信息";
        
        return sendBatchMessage(staffUserIds, message, linkTitle, h5Url);
    }

    /**
     * 逐个给客户发送个性化链接（包含external_id）
     * 注意：此方法会受到企业微信频率限制
     * 
     * @param staffUserId 员工UserID
     */
    public void sendPersonalizedLinksToCustomers(String staffUserId) {
        List<String> customerIds = getCustomerList(staffUserId);
        
        if (customerIds.isEmpty()) {
            logger.warn("员工{}没有客户", staffUserId);
            return;
        }
        
        logger.info("开始给{}个客户发送个性化链接", customerIds.size());
        
        int successCount = 0;
        int failCount = 0;
        
        for (String externalUserId : customerIds) {
            try {
                // 生成带external_userid的个性化链接
                String h5Link = buildH5Link(externalUserId);
                String message = buildPersonalizedMessage(h5Link);
                
                // 发送消息
                boolean success = wechatApiService.sendTextMessage(externalUserId, message, staffUserId);
                
                if (success) {
                    successCount++;
                } else {
                    failCount++;
                }
                
                // 避免频率限制，添加延迟
                Thread.sleep(200); // 200ms延迟
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("发送被中断", e);
                break;
            } catch (Exception e) {
                failCount++;
                logger.error("给客户{}发送消息失败", externalUserId, e);
            }
        }
        
        logger.info("发送完成，成功: {}, 失败: {}", successCount, failCount);
    }

    /**
     * 构建带external_userid的H5链接
     */
    private String buildH5Link(String externalUserId) {
        try {
            String encodedUserId = java.net.URLEncoder.encode(externalUserId, "UTF-8");
            return config.getH5BaseUrl() + "?external_userid=" + encodedUserId;
        } catch (Exception e) {
            logger.error("构建H5链接失败", e);
            return config.getH5BaseUrl();
        }
    }

    /**
     * 构建个性化消息
     */
    private String buildPersonalizedMessage(String h5Link) {
        return "您好！为了更好地为您服务，请点击下方链接完善您的信息：\n\n" +
               h5Link + "\n\n" +
               "感谢您的配合！";
    }
}

