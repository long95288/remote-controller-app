package com.longquanxiao.remotecontroller.cmd.DTO;

import com.alibaba.fastjson.annotation.JSONField;

public class SendMsgRequestDTO extends BaseRequestDTO {

        @JSONField(name = "msg")
        private String msg;

        public SendMsgRequestDTO(int cmd, String msg) {
            super(cmd);
            this.msg = msg;
        }

        public String getMsg() {
            return msg;
        }

        public void setMsg(String msg) {
            this.msg = msg;
        }
}
