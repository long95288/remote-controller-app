package com.longquanxiao.remotecontroller.cmd.DTO;

import com.alibaba.fastjson.annotation.JSONField;

public class SetShutdownPlanRequestDTO extends BaseRequestDTO {

    @JSONField(name = "ShutdownTime")
    private int shutdownTime;

    public SetShutdownPlanRequestDTO(int cmd) {
        super(cmd);
    }

    public SetShutdownPlanRequestDTO(int cmd, int shutdownTime) {
        super(cmd);
        this.shutdownTime = shutdownTime;
    }

    public int getShutdownTime() {
        return shutdownTime;
    }

    public void setShutdownTime(int shutdownTime) {
        this.shutdownTime = shutdownTime;
    }
}
