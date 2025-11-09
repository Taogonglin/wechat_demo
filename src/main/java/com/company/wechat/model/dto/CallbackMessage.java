package com.company.wechat.model.dto;

import lombok.Data;

/**
 * 企业微信回调消息
 * 
 * @author Company
 */
@Data
public class CallbackMessage {

    /**
     * 企业ID
     */
    private String toUserName;

    /**
     * 消息的发送者
     */
    private String fromUserName;

    /**
     * 消息创建时间
     */
    private Long createTime;

    /**
     * 消息类型
     */
    private String msgType;

    /**
     * 事件类型
     */
    private String event;

    /**
     * 变更类型
     */
    private String changeType;

    /**
     * 消息ID
     */
    private Long msgId;

    /**
     * 企业微信回调原始XML
     */
    private String originalXml;
}
