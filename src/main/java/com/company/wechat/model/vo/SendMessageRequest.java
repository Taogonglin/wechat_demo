package com.company.wechat.model.vo;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

/**
 * 发送消息请求对象
 * 
 * @author Company Dev Team
 */
@Data
@Builder
public class SendMessageRequest {

    /**
     * 消息类型
     */
    @SerializedName("msgtype")
    private String msgType;

    /**
     * 指定接收消息的客户UserID列表
     */
    @SerializedName("touser")
    private String toUser;

    /**
     * 文本消息
     */
    private TextContent text;

    /**
     * 文本内容对象
     */
    @Data
    @Builder
    public static class TextContent {
        /**
         * 消息内容
         */
        private String content;
    }
}

