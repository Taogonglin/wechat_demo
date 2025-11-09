package com.company.wechat.model.dto;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量发送消息请求
 * 
 * @author Company
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchMessageRequest {
    
    /**
     * 群聊类型：single-单聊，group-群聊
     */
    @SerializedName("chat_type")
    private String chatType;
    
    /**
     * 外部联系人筛选条件
     */
    @SerializedName("external_contact")
    private ExternalContact externalContact;
    
    /**
     * 发送者
     */
    private Sender sender;
    
    /**
     * 文本消息
     */
    private TextContent text;
    
    /**
     * 附件（可选）
     */
    private List<Attachment> attachments;
    
    /**
     * 外部联系人筛选条件
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExternalContact {
        /**
         * 客户标签列表，为空表示发送给该成员的所有客户
         */
        @SerializedName("tag_list")
        private List<String> tagList;
    }
    
    /**
     * 发送者
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Sender {
        /**
         * 发送者的userid列表
         */
        @SerializedName("sender_list")
        private List<String> senderList;
    }
    
    /**
     * 文本内容
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TextContent {
        private String content;
    }
    
    /**
     * 附件
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
         * 链接附件
         */
        private Link link;
    }
    
    /**
     * 链接附件
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Link {
        private String title;
        private String url;
        @SerializedName("picurl")
        private String picUrl;
        private String desc;
    }
}

