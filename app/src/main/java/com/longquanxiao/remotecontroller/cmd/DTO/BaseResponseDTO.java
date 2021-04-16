package com.longquanxiao.remotecontroller.cmd.DTO;

import com.alibaba.fastjson.annotation.JSONField;

public class BaseResponseDTO {

    @JSONField(name = "OptionStatus")
    public int optionStatus;

    @JSONField(name = "Message")
    public String message;

    @JSONField(name = "Data")
    public Object data;

    public BaseResponseDTO() {
    }
    public BaseResponseDTO(int optionStatus, String message, Object data) {
        this.optionStatus = optionStatus;
        this.message = message;
        this.data = data;
    }
    public int getOptionStatus() {
        return optionStatus;
    }
    public void setOptionStatus(int optionStatus) {
        this.optionStatus = optionStatus;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
    public Object getData() {
        return data;
    }
    public void setData(Object data) {
        this.data = data;
    }
}

