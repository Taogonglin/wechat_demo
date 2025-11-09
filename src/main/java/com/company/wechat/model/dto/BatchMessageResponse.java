package com.company.wechat.model.dto;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

/**
 * 批量发送消息响应
 * 
 * @author Company
 */
@Data
public class BatchMessageResponse {
    
    @SerializedName("errcode")
    private Integer errCode;
    
    @SerializedName("errmsg")
    private String errMsg;
    
    /**
     * 无效的客户ID列表
     */
    @SerializedName("fail_list")
    private List<String> failList;
    
    /**
     * 消息ID
     */
    @SerializedName("msgid")
    private String msgId;
    
    public boolean isSuccess() {
        return errCode != null && errCode == 0;
    }
}

