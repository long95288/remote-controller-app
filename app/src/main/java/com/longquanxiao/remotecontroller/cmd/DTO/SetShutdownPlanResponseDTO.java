package com.longquanxiao.remotecontroller.cmd.DTO;

import com.alibaba.fastjson.annotation.JSONField;

public class SetShutdownPlanResponseDTO {
    @JSONField(name = "ShutdownTime")
    private int shutdownTime;

    public SetShutdownPlanResponseDTO() {
    }

    public SetShutdownPlanResponseDTO(int shutdownTime) {
        this.shutdownTime = shutdownTime;
    }

    public int getShutdownTime() {
        return shutdownTime;
    }

    public void setShutdownTime(int shutdownTime) {
        this.shutdownTime = shutdownTime;
    }
}
