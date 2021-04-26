package com.longquanxiao.remotecontroller.protcol;

/**
 * Length TYPE VALUE PROTO
 */
public class LTVData {
    public static final int PEER_CMD_NOTICE_SERVER = 1;
    public static final int PEER_CMD_MOUSE_CTL     = 2 ;

    int length;
    int type;
    byte[] value;

    public LTVData(int length, int type, byte[] value) {
        this.length = length;
        this.type = type;
        this.value = value;
    }

    public static byte[] encodeLTVDataByObject(LTVData data) {
        return null;
    }

    public static LTVData decodeLTVDataByBytes(byte[] data) {
        return null;
    }

}
