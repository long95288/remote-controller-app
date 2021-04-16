package com.longquanxiao.remotecontroller.cmd.DTO;

import com.alibaba.fastjson.annotation.JSONField;

public class BaseRequestDTO {

    @JSONField(name = "CMD")
    public int cmd;

    public BaseRequestDTO(int cmd) {
        this.cmd = cmd;
    }

    public int getCmd() {
        return cmd;
    }
    public void setCmd(int cmd) {
        this.cmd = cmd;
    }
}
