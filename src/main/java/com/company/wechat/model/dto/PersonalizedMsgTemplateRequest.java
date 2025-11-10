package com.company.wechat.model.dto;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 个性化群发消息请求（企业微信群发接口）
 * 
 * @author Company
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonalizedMsgTemplateRequest {
    
    /**
     * 群发任务的类型，默认为single，表示发送给客户
     */
    @SerializedName("chat_type")
    private String chatType;
    
    /**
     * 客户的external_userid列表，仅在chat_type为single时有效
     * 最多可一次指定1万个客户
     */
    @SerializedName("external_userid")
    private List<String> externalUserid;
    
    /**
     * 发送企业群发消息的成员userid
     */
    private String sender;
    
    /**
     * 文本消息内容
     */
    private TextContent text;
    
    /**
     * 附件列表，最多支持添加9个附件
     */
    private List<Attachment> attachments;
    
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
        private String msgtype;
        
        /**
         * 链接附件
         */
        private LinkAttachment link;
    }
    
    /**
     * 链接附件
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LinkAttachment {
        /**
         * 图文消息标题，最长128个字节
         */
        private String title;
        
        /**
         * 图文消息封面的url，最长2048个字节
         */
        @SerializedName("picurl")
        private String picUrl;
        
        /**
         * 图文消息的描述，最多512个字节
         */
        private String desc;
        
        /**
         * 图文消息的链接，最长2048个字节
         */
        private String url;
    }
}

