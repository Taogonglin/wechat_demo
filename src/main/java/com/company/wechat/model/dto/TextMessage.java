package com.company.wechat.model.dto;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文本消息
 * 
 * @author Company
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TextMessage {

    /**
     * 消息内容
     */
    private String content;

    /**
     * 发送消息的请求体
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SendRequest {
        /**
         * 消息类型
         */
        @SerializedName("msgtype")
        private String msgType;

        /**
         * 指定接收消息的客户UserID
         */
        @SerializedName("touser")
        private String toUser;

        /**
         * 指定接收消息的成员
         */
        @SerializedName("agentid")
        private Integer agentId;

        /**
         * 文本消息
         */
        private TextMessage text;

        /**
         * 表示是否开启重复消息检查
         */
        @SerializedName("safe")
        private Integer safe;

        /**
         * 表示是否开启id转译
         */
        @SerializedName("enable_id_trans")
        private Integer enableIdTrans;

        /**
         * 表示是否开启重复消息检查的时间间隔
         */
        @SerializedName("enable_duplicate_check")
        private Integer enableDuplicateCheck;

        /**
         * 重复消息检查的时间间隔
         */
        @SerializedName("duplicate_check_interval")
        private Integer duplicateCheckInterval;
    }
}

