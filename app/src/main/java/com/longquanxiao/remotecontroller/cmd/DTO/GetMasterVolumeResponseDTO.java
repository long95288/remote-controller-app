package com.longquanxiao.remotecontroller.cmd.DTO;

import com.alibaba.fastjson.annotation.JSONField;

public class GetMasterVolumeResponseDTO extends BaseResponseDTO {

    @JSONField(name = "Volume")
    private int volume;

    public GetMasterVolumeResponseDTO(int volume) {
        this.volume = volume;
    }

    public GetMasterVolumeResponseDTO() {
    }

    public int getVolume() {
        return volume;
    }

    public void setVolume(int volume) {
        this.volume = volume;
    }
}
