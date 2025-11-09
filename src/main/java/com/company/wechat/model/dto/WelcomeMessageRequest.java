package com.company.wechat.model.dto;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 发送欢迎语请求
 * 
 * @author Company
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WelcomeMessageRequest {

    /**
     * 欢迎语code，通过添加企业客户事件获取
     */
    @SerializedName("welcome_code")
    private String welcomeCode;

    /**
     * 文本消息
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

