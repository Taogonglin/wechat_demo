package com.company.wechat.model.dto;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量发送消息请求
 * 用于企业微信群发功能
 * 
 * @author Company
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchSendRequest {

    /**
     * 群发任务的类型，默认为single，表示发送给客户，group表示发送给客户群
     */
    @SerializedName("chat_type")
    private String chatType;

    /**
     * 发送企业群发消息的成员
     */
    private Sender sender;

    /**
     * 客户筛选条件
     */
    @SerializedName("external_contact")
    private ExternalContact externalContact;

    /**
     * 文本消息
     */
    private TextContent text;

    /**
     * 附件，最多支持9个附件（可选）
     */
    private List<Attachment> attachments;

    /**
     * 发送者
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Sender {
        /**
         * 发送企业群发消息的成员userid
         */
        private String sender;
    }

    /**
     * 外部联系人筛选条件
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExternalContact {
        /**
         * 客户标签的tag_id列表
         * 为空表示对全部客户发送
         */
        @SerializedName("tag_list")
        private List<String> tagList;
    }

    /**
     * 文本内容
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TextContent {
        /**
         * 消息文本内容，最多4000个字节
         */
        private String content;
    }

    /**
     * 附件（图片、链接、小程序等）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Attachment {
        /**
         * 附件类型：image、link、miniprogram、video、file
         */
        @SerializedName("msgtype")
        private String msgType;

        /**
         * 链接消息
         */
        private Link link;

        /**
         * 链接类型附件
         */
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Link {
            /**
             * 链接标题
             */
            private String title;

            /**
             * 链接地址
             */
            private String url;

            /**
             * 链接描述（可选）
             */
            private String desc;

            /**
             * 链接封面图（可选）
             */
            @SerializedName("picurl")
            private String picUrl;
        }
    }
}

