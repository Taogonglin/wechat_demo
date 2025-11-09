package com.company.wechat.model.vo;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

/**
 * 企业微信API响应
 * 
 * @author Company
 */
@Data
public class WechatResponse<T> {

    /**
     * 错误码
     */
    @SerializedName("errcode")
    private Integer errCode;

    /**
     * 错误信息
     */
    @SerializedName("errmsg")
    private String errMsg;

    /**
     * Access Token
     */
    @SerializedName("access_token")
    private String accessToken;

    /**
     * Token过期时间（秒）
     */
    @SerializedName("expires_in")
    private Integer expiresIn;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 判断是否成功
     */
    public boolean isSuccess() {
        return errCode != null && errCode == 0;
    }
}
