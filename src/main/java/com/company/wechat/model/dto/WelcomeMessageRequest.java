package com.company.wechat.model.dto;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 发送欢迎语请求（客服欢迎语接口）
 * 根据官方文档：https://developer.work.weixin.qq.com/document/path/95122
 * 
 * @author Company
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WelcomeMessageRequest {

    /**
     * 事件响应消息对应的code（即welcome_code）
     * 通过事件回调下发，仅可使用一次
     */
    @SerializedName("code")
    private String code;

    /**
     * 消息ID（可选）
     * 如果请求参数指定了msgid，则原样返回，否则系统自动生成并返回
     * 不多于32字节，字符串取值范围(正则表达式)：[0-9a-zA-Z_-]*
     */
    @SerializedName("msgid")
    private String msgid;

    /**
     * 消息类型
     * 对不同的msgtype，有相应的结构描述
     */
    @SerializedName("msgtype")
    private String msgtype;

    /**
     * 文本消息内容（当msgtype为text时使用）
     */
    private TextContent text;

    /**
     * 文本内容
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TextContent {
        /**
         * 消息内容
         */
        private String content;
    }
}

