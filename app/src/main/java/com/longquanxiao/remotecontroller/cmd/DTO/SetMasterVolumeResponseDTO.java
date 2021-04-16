package com.longquanxiao.remotecontroller.cmd.DTO;

import com.alibaba.fastjson.annotation.JSONField;

public class SetMasterVolumeResponseDTO {

    @JSONField(name = "Volume")
    private int volume;

    public SetMasterVolumeResponseDTO(int volume) {
        this.volume = volume;
    }

    public SetMasterVolumeResponseDTO() {
    }

    public int getVolume() {
        return volume;
    }

    public void setVolume(int volume) {
        this.volume = volume;
    }
}
