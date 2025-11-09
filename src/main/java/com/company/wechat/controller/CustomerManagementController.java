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
}

