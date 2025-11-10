package com.company.wechat.service;

import com.company.wechat.config.WechatWorkConfig;
import com.company.wechat.model.dto.BatchSendRequest;
import com.company.wechat.model.dto.PersonalizedMsgTemplateRequest;
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
    
    // 获取部门成员列表API
    private static final String GET_DEPARTMENT_USER_LIST = "https://qyapi.weixin.qq.com/cgi-bin/user/simplelist?access_token=%s&department_id=%s";
    
    // 获取外部联系人详情API
    private static final String GET_EXTERNAL_CONTACT_DETAIL = "https://qyapi.weixin.qq.com/cgi-bin/externalcontact/get?access_token=%s&external_userid=%s";

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

    /**
     * 针对不同用户单发个性化H5链接（使用企业群发接口）
     * 每个用户收到的链接都带有自己的external_userid
     * 
     * @param staffUserId 发送消息的员工UserID
     * @param externalUserIds 客户的external_userid列表（可选，为空则发送给该员工的所有客户）
     * @param linkTitle 链接标题
     * @param linkDesc 链接描述
     * @param linkPicUrl 链接封面图片URL（可选）
     * @return 发送结果统计
     */
    public Map<String, Object> sendPersonalizedH5Links(
            String staffUserId, 
            List<String> externalUserIds,
            String linkTitle,
            String linkDesc,
            String linkPicUrl) {
        
        // 如果未指定客户列表，则获取该员工的所有客户
        List<String> targetCustomers = (externalUserIds != null && !externalUserIds.isEmpty()) 
                ? externalUserIds 
                : getCustomerList(staffUserId);
        
        if (targetCustomers.isEmpty()) {
            logger.warn("没有找到需要发送的客户");
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "没有找到需要发送的客户");
            return result;
        }
        
        logger.info("开始针对{}个客户发送个性化H5链接", targetCustomers.size());
        
        int successCount = 0;
        int failCount = 0;
        List<String> successList = new ArrayList<>();
        List<String> failList = new ArrayList<>();
        List<String> msgidList = new ArrayList<>();
        
        // 针对每个客户单独调用群发接口
        for (String externalUserId : targetCustomers) {
            try {
                // 生成带external_userid的个性化链接
                String personalizedUrl = buildPersonalizedLink(externalUserId);
                
                // 调用群发接口
                String msgid = sendMsgTemplateToSingleUser(
                        staffUserId, 
                        externalUserId, 
                        linkTitle, 
                        linkDesc, 
                        linkPicUrl, 
                        personalizedUrl);
                
                if (msgid != null) {
                    successCount++;
                    successList.add(externalUserId);
                    msgidList.add(msgid);
                    logger.info("发送成功: external_userid={}, msgid={}", externalUserId, msgid);
                } else {
                    failCount++;
                    failList.add(externalUserId);
                    logger.warn("发送失败: external_userid={}", externalUserId);
                }
                
                // 避免频率限制，添加延迟（企业微信接口有频率限制）
                Thread.sleep(100); // 100ms延迟
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("发送被中断");
                break;
            } catch (Exception e) {
                logger.error("发送消息失败: external_userid={}", externalUserId, e);
                failCount++;
                failList.add(externalUserId);
            }
        }
        
        logger.info("个性化群发完成，总数: {}, 成功: {}, 失败: {}", 
                targetCustomers.size(), successCount, failCount);
        
        // 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("success", failCount == 0);
        result.put("total", targetCustomers.size());
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("successList", successList);
        result.put("failList", failList);
        result.put("msgidList", msgidList);
        result.put("message", String.format("发送完成，成功%d个，失败%d个", successCount, failCount));
        
        return result;
    }

    /**
     * 向单个用户发送群发消息模板
     * 
     * @param staffUserId 员工UserID
     * @param externalUserId 客户的external_userid
     * @param linkTitle 链接标题
     * @param linkDesc 链接描述
     * @param linkPicUrl 链接封面图片URL
     * @param linkUrl 链接URL
     * @return 群发消息ID（msgid），失败返回null
     */
    private String sendMsgTemplateToSingleUser(
            String staffUserId,
            String externalUserId,
            String linkTitle,
            String linkDesc,
            String linkPicUrl,
            String linkUrl) {
        
        try {
            String accessToken = wechatApiService.getAccessToken();
            String url = String.format(ADD_MSG_TEMPLATE_URL, accessToken);
            
            // 构建链接附件
            PersonalizedMsgTemplateRequest.LinkAttachment link = 
                    PersonalizedMsgTemplateRequest.LinkAttachment.builder()
                            .title(linkTitle)
                            .desc(linkDesc)
                            .picUrl(linkPicUrl != null ? linkPicUrl : "")
                            .url(linkUrl)
                            .build();
            
            // 构建附件列表
            PersonalizedMsgTemplateRequest.Attachment attachment = 
                    PersonalizedMsgTemplateRequest.Attachment.builder()
                            .msgtype("link")
                            .link(link)
                            .build();
            
            // 构建请求
            PersonalizedMsgTemplateRequest request = PersonalizedMsgTemplateRequest.builder()
                    .chatType("single")
                    .externalUserid(Collections.singletonList(externalUserId))
                    .sender(staffUserId)
                    .attachments(Collections.singletonList(attachment))
                    .build();
            
            String jsonRequest = gson.toJson(request);
            logger.debug("发送群发消息请求: {}", jsonRequest);
            
            String response = HttpUtil.doPostString(url, jsonRequest);
            @SuppressWarnings("unchecked")
            Map<String, Object> resultMap = gson.fromJson(response, Map.class);
            
            Integer errcode = ((Number) resultMap.get("errcode")).intValue();
            if (errcode == 0) {
                String msgid = (String) resultMap.get("msgid");
                return msgid;
            } else {
                String errmsg = (String) resultMap.get("errmsg");
                logger.error("发送群发消息失败: external_userid={}, errcode={}, errmsg={}", 
                        externalUserId, errcode, errmsg);
                return null;
            }
        } catch (Exception e) {
            logger.error("发送群发消息异常: external_userid={}", externalUserId, e);
            return null;
        }
    }

    /**
     * 批量发送个性化H5链接（指定标签）
     * 
     * @param staffUserId 员工UserID
     * @param tagIds 标签ID列表
     * @param linkTitle 链接标题
     * @param linkDesc 链接描述
     * @param linkPicUrl 链接封面图片URL
     * @return 发送结果
     */
    public Map<String, Object> sendPersonalizedH5LinksByTag(
            String staffUserId,
            List<String> tagIds,
            String linkTitle,
            String linkDesc,
            String linkPicUrl) {
        
        // TODO: 如果需要按标签筛选，需要先调用企业微信的"获取客户列表"接口，并过滤标签
        // 这里暂时使用所有客户的方式
        logger.info("按标签发送个性化链接: staffUserId={}, tags={}", staffUserId, tagIds);
        
        return sendPersonalizedH5Links(staffUserId, null, linkTitle, linkDesc, linkPicUrl);
    }

    /**
     * 获取部门下所有成员的UserID列表
     * 
     * @param departmentId 部门ID，1表示根部门（整个企业）
     * @return 员工UserID列表
     */
    public List<String> getDepartmentUserList(int departmentId) {
        try {
            String accessToken = wechatApiService.getAccessToken();
            String url = String.format(GET_DEPARTMENT_USER_LIST, accessToken, departmentId);
            
            logger.info("获取部门成员列表: departmentId={}", departmentId);
            String response = HttpUtil.doGet(url);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> result = gson.fromJson(response, Map.class);
            
            Integer errcode = ((Number) result.get("errcode")).intValue();
            if (errcode == 0) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> userlist = (List<Map<String, Object>>) result.get("userlist");
                
                if (userlist != null && !userlist.isEmpty()) {
                    List<String> userIds = new ArrayList<>();
                    for (Map<String, Object> user : userlist) {
                        String userId = (String) user.get("userid");
                        if (userId != null && !userId.isEmpty()) {
                            userIds.add(userId);
                        }
                    }
                    logger.info("获取到{}个部门成员", userIds.size());
                    return userIds;
                } else {
                    logger.warn("部门成员列表为空");
                    return Collections.emptyList();
                }
            } else {
                String errmsg = (String) result.get("errmsg");
                logger.error("获取部门成员列表失败: {}", errmsg);
                return Collections.emptyList();
            }
        } catch (Exception e) {
            logger.error("获取部门成员列表异常", e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取企业所有员工下的所有客户（去重）
     * 
     * @param departmentId 部门ID，1表示根部门（整个企业）
     * @return 去重后的客户external_userid列表和对应的第一个添加该客户的员工映射
     */
    public Map<String, String> getAllCustomersInDepartment(int departmentId) {
        // 获取部门所有员工
        List<String> staffUserIds = getDepartmentUserList(departmentId);
        
        if (staffUserIds.isEmpty()) {
            logger.warn("没有找到部门成员");
            return Collections.emptyMap();
        }
        
        logger.info("开始获取{}个员工的客户列表", staffUserIds.size());
        
        // 使用Map存储客户ID和第一个添加该客户的员工ID（用于发送消息）
        Map<String, String> customerToStaffMap = new HashMap<>();
        
        for (String staffUserId : staffUserIds) {
            try {
                List<String> customers = getCustomerList(staffUserId);
                
                for (String externalUserId : customers) {
                    // 如果客户尚未添加到映射中，记录该客户和员工的关系
                    if (!customerToStaffMap.containsKey(externalUserId)) {
                        customerToStaffMap.put(externalUserId, staffUserId);
                    }
                }
                
                logger.info("员工 {} 有 {} 个客户", staffUserId, customers.size());
                
                // 避免频率限制
                Thread.sleep(50);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("获取客户列表被中断");
                break;
            } catch (Exception e) {
                logger.error("获取员工客户列表失败: staffUserId={}", staffUserId, e);
            }
        }
        
        logger.info("去重后共有{}个客户", customerToStaffMap.size());
        return customerToStaffMap;
    }

    /**
     * 向企业所有员工下的所有客户发送个性化H5链接
     * 
     * @param departmentId 部门ID，1表示根部门（整个企业）
     * @param linkTitle 链接标题
     * @param linkDesc 链接描述
     * @param linkPicUrl 链接封面图片URL
     * @return 发送结果统计
     */
    public Map<String, Object> sendPersonalizedH5LinksToAllCustomers(
            int departmentId,
            String linkTitle,
            String linkDesc,
            String linkPicUrl) {
        
        logger.info("开始向企业所有客户发送个性化H5链接，部门ID: {}", departmentId);
        
        // 获取所有客户（去重）
        Map<String, String> customerToStaffMap = getAllCustomersInDepartment(departmentId);
        
        if (customerToStaffMap.isEmpty()) {
            logger.warn("没有找到需要发送的客户");
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "没有找到需要发送的客户");
            return result;
        }
        
        logger.info("开始针对{}个客户发送个性化H5链接", customerToStaffMap.size());
        
        int successCount = 0;
        int failCount = 0;
        List<String> successList = new ArrayList<>();
        List<String> failList = new ArrayList<>();
        List<String> msgidList = new ArrayList<>();
        
        // 针对每个客户单独调用群发接口
        for (Map.Entry<String, String> entry : customerToStaffMap.entrySet()) {
            String externalUserId = entry.getKey();
            String staffUserId = entry.getValue();
            
            try {
                // 生成带external_userid的个性化链接
                String personalizedUrl = buildPersonalizedLink(externalUserId);
                
                // 调用群发接口
                String msgid = sendMsgTemplateToSingleUser(
                        staffUserId, 
                        externalUserId, 
                        linkTitle, 
                        linkDesc, 
                        linkPicUrl, 
                        personalizedUrl);
                
                if (msgid != null) {
                    successCount++;
                    successList.add(externalUserId);
                    msgidList.add(msgid);
                    logger.info("发送成功 [{}/{}]: external_userid={}, staff={}, msgid={}", 
                            successCount, customerToStaffMap.size(), externalUserId, staffUserId, msgid);
                } else {
                    failCount++;
                    failList.add(externalUserId);
                    logger.warn("发送失败: external_userid={}, staff={}", externalUserId, staffUserId);
                }
                
                // 避免频率限制，添加延迟
                Thread.sleep(100);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("发送被中断");
                break;
            } catch (Exception e) {
                logger.error("发送消息失败: external_userid={}, staff={}", externalUserId, staffUserId, e);
                failCount++;
                failList.add(externalUserId);
            }
        }
        
        logger.info("企业全员客户个性化群发完成，总数: {}, 成功: {}, 失败: {}", 
                customerToStaffMap.size(), successCount, failCount);
        
        // 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("success", failCount == 0);
        result.put("total", customerToStaffMap.size());
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("successList", successList);
        result.put("failList", failList);
        result.put("msgidList", msgidList);
        result.put("message", String.format("企业全员客户群发完成，成功%d个，失败%d个", successCount, failCount));
        
        return result;
    }
}

