package com.longquanxiao.remotecontroller.cmd.DTO;

import com.alibaba.fastjson.annotation.JSONField;

/**
 * 远程控制指令
 *
 */



public class SetMasterVolumeRequestDTO extends BaseRequestDTO {

    @JSONField(name = "Volume")
    private int volume;

    public SetMasterVolumeRequestDTO(int cmd, int volume) {
        super(cmd);
        this.volume = volume;
    }

    public int getVolume() {
        return volume;
    }

    public void setVolume(int volume) {
        this.volume = volume;
    }
}
