package com.company.wechat.model.vo;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

/**
 * 企业微信OAuth响应
 * 
 * @author Company
 */
@Data
public class WechatOAuthResponse {

    /**
     * 错误码
     */
    @SerializedName("errcode")
    private Integer errcode;

    /**
     * 错误信息
     */
    @SerializedName("errmsg")
    private String errmsg;

    /**
     * 成员UserID（企业内部成员）
     */
    @SerializedName("userid")
    private String userId;

    /**
     * 外部联系人ID（客户）
     */
    @SerializedName("external_userid")
    private String externalUserId;

    /**
     * 非企业成员的标识（个人微信用户）
     */
    @SerializedName("openid")
    private String openId;

    /**
     * 设备ID
     */
    @SerializedName("deviceid")
    private String deviceId;
}

