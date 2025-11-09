package com.company.wechat.service;

import com.company.wechat.config.WechatWorkConfig;
import com.company.wechat.model.dto.BatchSendRequest;
import com.company.wechat.model.vo.ExternalContactListResponse;
import com.company.wechat.util.HttpUtil;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

/**
 * 存量客户服务
 * 用于处理已存在的客户，批量发送消息等
 * 
 * @author Company
 */
@Service
public class ExistingCustomerService {

    private static final Logger logger = LoggerFactory.getLogger(ExistingCustomerService.class);

    // 获取客户列表API
    private static final String GET_EXTERNAL_CONTACT_LIST = "https://qyapi.weixin.qq.com/cgi-bin/externalcontact/list?access_token=%s&userid=%s";
    
    // 批量发送消息API（创建群发）
    private static final String ADD_MSG_TEMPLATE_URL = "https://qyapi.weixin.qq.com/cgi-bin/externalcontact/add_msg_template?access_token=%s";
    
    // 获取群发结果API
    private static final String GET_GROUP_MSG_RESULT_URL = "https://qyapi.weixin.qq.com/cgi-bin/externalcontact/get_groupmsg_result?access_token=%s";

    @Autowired
    private WechatApiService wechatApiService;

    @Autowired
    private WechatWorkConfig config;

    private final Gson gson = new Gson();

    /**
     * 获取员工的客户列表
     * 
     * @param staffUserId 员工UserID
     * @return 客户external_userid列表
     */
    public List<String> getCustomerList(String staffUserId) {
        try {
            String accessToken = wechatApiService.getAccessToken();
            String url = String.format(GET_EXTERNAL_CONTACT_LIST, accessToken, staffUserId);
            
            logger.info("获取员工客户列表: staffUserId={}", staffUserId);
            String response = HttpUtil.doGet(url);
            
            ExternalContactListResponse result = gson.fromJson(response, ExternalContactListResponse.class);
            
            if (result.getErrcode() == 0 && result.getExternalUserid() != null) {
                logger.info("获取到{}个客户", result.getExternalUserid().size());
                return result.getExternalUserid();
            } else {
                logger.error("获取客户列表失败: {}", result.getErrmsg());
                return Collections.emptyList();
            }
        } catch (Exception e) {
            logger.error("获取客户列表异常", e);
            return Collections.emptyList();
        }
    }

    /**
     * 创建群发任务（批量发送消息给客户）
     * 
     * @param staffUserId 发送消息的员工UserID
     * @param content 消息内容
     * @param tagList 客户标签列表（可选，为空表示发给所有客户）
     * @return 群发任务ID（msgid）
     */
    public String createBatchSendTask(String staffUserId, String content, List<String> tagList) {
        try {
            String accessToken = wechatApiService.getAccessToken();
            String url = String.format(ADD_MSG_TEMPLATE_URL, accessToken);

            // 构建请求
            BatchSendRequest request = BatchSendRequest.builder()
                    .chatType("single")
                    .sender(BatchSendRequest.Sender.builder()
                            .sender(staffUserId)
                            .build())
                    .externalContact(BatchSendRequest.ExternalContact.builder()
                            .tagList(tagList != null ? tagList : new ArrayList<>())
                            .build())
                    .text(BatchSendRequest.TextContent.builder()
                            .content(content)
                            .build())
                    .build();

            String jsonRequest = gson.toJson(request);
            logger.info("创建群发任务: staffUserId={}, content={}", staffUserId, content);

            String response = HttpUtil.doPostString(url, jsonRequest);
            @SuppressWarnings("unchecked")
            Map<String, Object> resultMap = gson.fromJson(response, Map.class);
            
            Integer errcode = ((Number) resultMap.get("errcode")).intValue();
            if (errcode == 0) {
                // 提取msgid
                String msgid = (String) resultMap.get("msgid");
                logger.info("创建群发任务成功，msgid={}", msgid);
                return msgid;
            } else {
                String errmsg = (String) resultMap.get("errmsg");
                logger.error("创建群发任务失败: {}", errmsg);
                return null;
            }
        } catch (Exception e) {
            logger.error("创建群发任务异常", e);
            return null;
        }
    }

    /**
     * 批量发送H5链接给所有客户
     * 
     * @param staffUserId 员工UserID
     * @param h5Url H5页面URL（不带external_id）
     * @return 群发任务ID
     */
    public String sendH5LinkToAllCustomers(String staffUserId, String h5Url) {
        String message = buildBatchMessage(h5Url);
        return createBatchSendTask(staffUserId, message, null);
    }

    /**
     * 批量发送H5链接给指定标签的客户
     * 
     * @param staffUserId 员工UserID
     * @param h5Url H5页面URL
     * @param tagList 客户标签列表
     * @return 群发任务ID
     */
    public String sendH5LinkToTaggedCustomers(String staffUserId, String h5Url, List<String> tagList) {
        String message = buildBatchMessage(h5Url);
        return createBatchSendTask(staffUserId, message, tagList);
    }

    /**
     * 查询群发结果
     * 
     * @param msgid 群发任务ID
     * @return 群发结果统计
     */
    public Map<String, Object> getGroupMsgResult(String msgid) {
        try {
            String accessToken = wechatApiService.getAccessToken();
            String url = String.format(GET_GROUP_MSG_RESULT_URL, accessToken);

            Map<String, Object> request = new HashMap<>();
            request.put("msgid", msgid);

            String response = HttpUtil.doPostJson(url, request);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = gson.fromJson(response, Map.class);

            if (result.get("errcode") != null && ((Number) result.get("errcode")).intValue() == 0) {
                logger.info("群发结果: {}", result);
                return result;
            } else {
                logger.error("查询群发结果失败: {}", result.get("errmsg"));
                return null;
            }
        } catch (Exception e) {
            logger.error("查询群发结果异常", e);
            return null;
        }
    }

    /**
     * 构建批量发送的消息内容
     */
    private String buildBatchMessage(String h5Url) {
        return "您好！感谢您对我们的支持。\n\n" +
               "为了更好地为您服务，请点击下方链接完善您的客户信息：\n" +
               h5Url + "\n\n" +
               "点击链接后将自动识别您的身份，无需手动输入。\n" +
               "如有任何问题，欢迎随时联系我们！";
    }

    /**
     * 逐个发送个性化链接（适用于少量客户）
     * 注意：此方法有频率限制
     * 
     * @param staffUserId 员工UserID
     */
    public void sendPersonalizedLinksOneByOne(String staffUserId) {
        List<String> customerIds = getCustomerList(staffUserId);
        
        if (customerIds.isEmpty()) {
            logger.warn("没有找到客户");
            return;
        }
        
        logger.info("开始逐个发送，共{}个客户", customerIds.size());
        
        int successCount = 0;
        int failCount = 0;
        
        for (String externalUserId : customerIds) {
            try {
                // 生成带external_id的个性化链接
                String h5Link = buildPersonalizedLink(externalUserId);
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
                logger.error("发送被中断");
                break;
            } catch (Exception e) {
                logger.error("发送消息失败: external_userid={}", externalUserId, e);
                failCount++;
            }
        }
        
        logger.info("发送完成，成功: {}, 失败: {}", successCount, failCount);
    }

    /**
     * 构建个性化链接（带external_id）
     */
    private String buildPersonalizedLink(String externalUserId) {
        try {
            String encodedUserId = URLEncoder.encode(externalUserId, "UTF-8");
            return config.getH5BaseUrl() + "?external_userid=" + encodedUserId;
        } catch (UnsupportedEncodingException e) {
            logger.error("URL编码失败", e);
            return config.getH5BaseUrl();
        }
    }

    /**
     * 构建个性化消息内容
     */
    private String buildPersonalizedMessage(String h5Link) {
        return "您好！为了更好地为您服务，请点击下方链接完善您的信息：\n" +
               h5Link + "\n\n" +
               "感谢您的配合！";
    }
}

