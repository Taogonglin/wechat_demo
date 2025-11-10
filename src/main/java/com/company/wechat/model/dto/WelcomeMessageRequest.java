package com.company.wechat.model.dto;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 发送欢迎语请求（外部联系人欢迎语接口）
 * 根据官方文档：https://developer.work.weixin.qq.com/document/path/92137
 * 
 * @author Company
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WelcomeMessageRequest {

    /**
     * 通过添加外部联系人事件推送给企业的发送欢迎语的凭证
     * 有效期为20秒
     */
    @SerializedName("welcome_code")
    private String welcomeCode;

    /**
     * 文本消息内容
     */
    private TextContent text;

    /**
     * 附件，最多可添加9个附件
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
         * 消息文本内容，最长为4000字节
         */
        private String content;
    }

    /**
     * 附件信息
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
         * 图片附件
         */
        private ImageAttachment image;

        /**
         * 链接附件
         */
        private LinkAttachment link;

        /**
         * 小程序附件
         */
        private MiniprogramAttachment miniprogram;

        /**
         * 视频附件
         */
        private VideoAttachment video;

        /**
         * 文件附件
         */
        private FileAttachment file;
    }

    /**
     * 图片附件
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageAttachment {
        @SerializedName("media_id")
        private String mediaId;

        @SerializedName("pic_url")
        private String picUrl;
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
         * 图文消息标题，最长为128字节
         */
        private String title;

        /**
         * 图文消息封面的url
         */
        private String picurl;

        /**
         * 图文消息的描述，最长为512字节
         */
        private String desc;

        /**
         * 图文消息的链接
         */
        private String url;
    }

    /**
     * 小程序附件
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MiniprogramAttachment {
        /**
         * 小程序消息标题，最长为64字节
         */
        private String title;

        /**
         * 小程序消息封面的mediaid
         */
        @SerializedName("pic_media_id")
        private String picMediaId;

        /**
         * 小程序appid
         */
        private String appid;

        /**
         * 小程序page路径
         */
        private String page;
    }

    /**
     * 视频附件
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VideoAttachment {
        @SerializedName("media_id")
        private String mediaId;
    }

    /**
     * 文件附件
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileAttachment {
        @SerializedName("media_id")
        private String mediaId;
    }
}

