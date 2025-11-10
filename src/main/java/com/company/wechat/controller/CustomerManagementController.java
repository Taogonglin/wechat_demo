package com.company.wechat.controller;

import com.company.wechat.config.WechatWorkConfig;
import com.company.wechat.service.ExistingCustomerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 客户管理控制器
 * 提供批量发送消息、获取客户列表等功能
 * 
 * @author Company
 */
@RestController
@RequestMapping("/api/customer")
public class CustomerManagementController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerManagementController.class);

    @Autowired
    private ExistingCustomerService existingCustomerService;

    @Autowired
    private WechatWorkConfig config;

    /**
     * 批量发送H5链接给所有客户
     * 
     * @param staffUserId 员工UserID
     * @return 群发任务ID
     */
    @PostMapping("/batch-send")
    public Map<String, Object> batchSendToAllCustomers(@RequestParam String staffUserId) {
        logger.info("收到批量发送请求，员工ID: {}", staffUserId);
        
        // H5链接不带external_id，通过OAuth识别身份
        String h5Url = config.getH5BaseUrl() + "?source=batch";
        
        String msgid = existingCustomerService.sendH5LinkToAllCustomers(staffUserId, h5Url);
        
        Map<String, Object> result = new HashMap<>();
        if (msgid != null) {
            result.put("success", true);
            result.put("msgid", msgid);
            result.put("message", "群发任务创建成功");
        } else {
            result.put("success", false);
            result.put("message", "群发任务创建失败");
        }
        
        return result;
    }

    /**
     * 批量发送给指定标签的客户
     * 
     * @param staffUserId 员工UserID
     * @param tagIds 标签ID列表（逗号分隔）
     * @return 群发任务ID
     */
    @PostMapping("/batch-send-by-tag")
    public Map<String, Object> batchSendByTag(
            @RequestParam String staffUserId,
            @RequestParam String tagIds) {
        
        logger.info("收到按标签批量发送请求，员工ID: {}, 标签: {}", staffUserId, tagIds);
        
        String h5Url = config.getH5BaseUrl() + "?source=batch_tag";
        List<String> tagList = Arrays.asList(tagIds.split(","));
        
        String msgid = existingCustomerService.sendH5LinkToTaggedCustomers(staffUserId, h5Url, tagList);
        
        Map<String, Object> result = new HashMap<>();
        if (msgid != null) {
            result.put("success", true);
            result.put("msgid", msgid);
            result.put("message", "群发任务创建成功");
        } else {
            result.put("success", false);
            result.put("message", "群发任务创建失败");
        }
        
        return result;
    }

    /**
     * 查询群发结果
     * 
     * @param msgid 群发任务ID
     * @return 群发结果统计
     */
    @GetMapping("/batch-send-result")
    public Map<String, Object> getGroupMsgResult(@RequestParam String msgid) {
        logger.info("查询群发结果，msgid: {}", msgid);
        
        Map<String, Object> result = existingCustomerService.getGroupMsgResult(msgid);
        
        if (result != null) {
            return result;
        } else {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "查询失败");
            return errorResult;
        }
    }

    /**
     * 获取员工的客户列表
     * 
     * @param staffUserId 员工UserID
     * @return 客户列表
     */
    @GetMapping("/list")
    public Map<String, Object> getCustomerList(@RequestParam String staffUserId) {
        logger.info("获取客户列表，员工ID: {}", staffUserId);
        
        List<String> customers = existingCustomerService.getCustomerList(staffUserId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("count", customers.size());
        result.put("customers", customers);
        
        return result;
    }

    /**
     * 逐个发送个性化链接（备用方案，适用于少量客户）
     * 注意：此接口有频率限制
     * 
     * @param staffUserId 员工UserID
     * @return 执行结果
     */
    @PostMapping("/send-personalized")
    public Map<String, Object> sendPersonalizedLinks(@RequestParam String staffUserId) {
        logger.info("收到逐个发送请求，员工ID: {}", staffUserId);
        
        // 异步执行，避免阻塞
        new Thread(() -> {
            existingCustomerService.sendPersonalizedLinksOneByOne(staffUserId);
        }).start();
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "发送任务已启动，将在后台执行");
        
        return result;
    }

    /**
     * 发送个性化H5链接（使用企业群发接口）
     * 每个客户收到的链接都带有自己的external_userid参数
     * 
     * @param staffUserId 员工UserID（必填）
     * @param externalUserIds 客户ID列表（可选，逗号分隔，为空则发送给该员工的所有客户）
     * @param linkTitle 链接标题（必填）
     * @param linkDesc 链接描述（可选）
     * @param linkPicUrl 链接封面图片URL（可选）
     * @return 发送结果
     */
    @PostMapping("/send-personalized-h5")
    public Map<String, Object> sendPersonalizedH5Links(
            @RequestParam String staffUserId,
            @RequestParam(required = false) String externalUserIds,
            @RequestParam String linkTitle,
            @RequestParam(required = false) String linkDesc,
            @RequestParam(required = false) String linkPicUrl) {
        
        logger.info("收到个性化H5链接发送请求，员工ID: {}, 标题: {}", staffUserId, linkTitle);
        
        // 解析客户ID列表
        List<String> customerIds = null;
        if (externalUserIds != null && !externalUserIds.trim().isEmpty()) {
            customerIds = Arrays.asList(externalUserIds.split(","));
            logger.info("指定客户数量: {}", customerIds.size());
        } else {
            logger.info("未指定客户，将发送给该员工的所有客户");
        }
        
        // 执行发送
        Map<String, Object> result = existingCustomerService.sendPersonalizedH5Links(
                staffUserId, 
                customerIds, 
                linkTitle, 
                linkDesc != null ? linkDesc : "点击查看详情",
                linkPicUrl);
        
        return result;
    }

    /**
     * 发送个性化H5链接（异步执行，适用于大量客户）
     * 
     * @param staffUserId 员工UserID
     * @param linkTitle 链接标题
     * @param linkDesc 链接描述
     * @param linkPicUrl 链接封面图片URL
     * @return 执行结果
     */
    @PostMapping("/send-personalized-h5-async")
    public Map<String, Object> sendPersonalizedH5LinksAsync(
            @RequestParam String staffUserId,
            @RequestParam String linkTitle,
            @RequestParam(required = false) String linkDesc,
            @RequestParam(required = false) String linkPicUrl) {
        
        logger.info("收到异步个性化H5链接发送请求，员工ID: {}", staffUserId);
        
        // 异步执行，避免阻塞
        new Thread(() -> {
            existingCustomerService.sendPersonalizedH5Links(
                    staffUserId, 
                    null, 
                    linkTitle, 
                    linkDesc != null ? linkDesc : "点击查看详情",
                    linkPicUrl);
        }).start();
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "个性化群发任务已启动，将在后台执行");
        
        return result;
    }

    /**
     * 向企业所有员工下的所有客户发送个性化H5链接（同步）
     * 
     * @param departmentId 部门ID（可选，默认为1表示整个企业）
     * @param linkTitle 链接标题（必填）
     * @param linkDesc 链接描述（可选）
     * @param linkPicUrl 链接封面图片URL（可选）
     * @return 发送结果
     */
    @PostMapping("/send-to-all-customers")
    public Map<String, Object> sendToAllCustomers(
            @RequestParam(required = false, defaultValue = "1") int departmentId,
            @RequestParam String linkTitle,
            @RequestParam(required = false) String linkDesc,
            @RequestParam(required = false) String linkPicUrl) {
        
        logger.info("收到企业全员客户群发请求，部门ID: {}, 标题: {}", departmentId, linkTitle);
        
        // 执行发送
        Map<String, Object> result = existingCustomerService.sendPersonalizedH5LinksToAllCustomers(
                departmentId, 
                linkTitle, 
                linkDesc != null ? linkDesc : "点击查看详情",
                linkPicUrl);
        
        return result;
    }

    /**
     * 向企业所有员工下的所有客户发送个性化H5链接（异步，适用于大量客户）
     * 
     * @param departmentId 部门ID（可选，默认为1表示整个企业）
     * @param linkTitle 链接标题（必填）
     * @param linkDesc 链接描述（可选）
     * @param linkPicUrl 链接封面图片URL（可选）
     * @return 执行结果
     */
    @PostMapping("/send-to-all-customers-async")
    public Map<String, Object> sendToAllCustomersAsync(
            @RequestParam(required = false, defaultValue = "1") int departmentId,
            @RequestParam String linkTitle,
            @RequestParam(required = false) String linkDesc,
            @RequestParam(required = false) String linkPicUrl) {
        
        logger.info("收到异步企业全员客户群发请求，部门ID: {}, 标题: {}", departmentId, linkTitle);
        
        // 异步执行，避免阻塞
        final String finalLinkDesc = linkDesc != null ? linkDesc : "点击查看详情";
        new Thread(() -> {
            existingCustomerService.sendPersonalizedH5LinksToAllCustomers(
                    departmentId, 
                    linkTitle, 
                    finalLinkDesc,
                    linkPicUrl);
        }).start();
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "企业全员客户个性化群发任务已启动，将在后台执行");
        result.put("tip", "任务可能需要较长时间，请查看日志获取进度");
        
        return result;
    }

    /**
     * 获取部门下所有员工列表
     * 
     * @param departmentId 部门ID（可选，默认为1表示整个企业）
     * @return 员工列表
     */
    @GetMapping("/department-users")
    public Map<String, Object> getDepartmentUsers(
            @RequestParam(required = false, defaultValue = "1") int departmentId) {
        
        logger.info("获取部门成员列表，部门ID: {}", departmentId);
        
        List<String> users = existingCustomerService.getDepartmentUserList(departmentId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("departmentId", departmentId);
        result.put("count", users.size());
        result.put("users", users);
        
        return result;
    }

    /**
     * 获取企业所有客户（去重）
     * 
     * @param departmentId 部门ID（可选，默认为1表示整个企业）
     * @return 客户列表及统计
     */
    @GetMapping("/all-customers")
    public Map<String, Object> getAllCustomers(
            @RequestParam(required = false, defaultValue = "1") int departmentId) {
        
        logger.info("获取企业所有客户，部门ID: {}", departmentId);
        
        Map<String, String> customerToStaffMap = existingCustomerService.getAllCustomersInDepartment(departmentId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("departmentId", departmentId);
        result.put("totalCustomers", customerToStaffMap.size());
        result.put("customers", customerToStaffMap.keySet());
        result.put("customerStaffMapping", customerToStaffMap);
        
        return result;
    }
}

