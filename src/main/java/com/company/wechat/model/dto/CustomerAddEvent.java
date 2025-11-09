package com.company.wechat.model.dto;

import lombok.Data;

/**
 * 客户添加事件
 * 
 * @author Company
 */
@Data
public class CustomerAddEvent {

    /**
     * 外部联系人ID
     */
    private String externalUserId;

    /**
     * 企业成员UserID
     */
    private String userId;

    /**
     * 添加的客户昵称（仅在客户确认后才能获取）
     */
    private String name;

    /**
     * 添加的客户头像（仅在客户确认后才能获取）
     */
    private String avatar;

    /**
     * 状态码
     */
    private String state;

    /**
     * 欢迎语code
     */
    private String welcomeCode;

    /**
     * 添加时间
     */
    private Long createTime;
}
