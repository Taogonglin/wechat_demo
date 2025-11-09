package com.company.wechat.model.vo;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

/**
 * 获取客户列表响应
 * 
 * @author Company
 */
@Data
public class ExternalContactListResponse {

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
     * 外部联系人的userid列表
     */
    @SerializedName("external_userid")
    private List<String> externalUserid;
}

