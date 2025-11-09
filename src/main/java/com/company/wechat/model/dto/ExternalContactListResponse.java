package com.company.wechat.model.dto;

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
    
    @SerializedName("errcode")
    private Integer errCode;
    
    @SerializedName("errmsg")
    private String errMsg;
    
    /**
     * 外部联系人ID列表
     */
    @SerializedName("external_userid")
    private List<String> externalUserid;
    
    public boolean isSuccess() {
        return errCode != null && errCode == 0;
    }
}

