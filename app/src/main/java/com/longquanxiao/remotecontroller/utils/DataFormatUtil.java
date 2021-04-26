package com.longquanxiao.remotecontroller.utils;

public class DataFormatUtil {
    // byte转uint32
    public static int bytesToUint32BigEnd(byte[] bytes) {
        if (bytes.length < 4) {
            return -1;
        }
        // 大端模式
        int length = (int)((bytes[0] & 0xFF) << 24) ;
        length |= (int)((bytes[1]& 0xFF) << 16) ;
        length |= (int)((bytes[2]& 0xFF) << 8) ;
        length |= (int)bytes[3] & 0xFF;
        return length;
    }

    /**
     * uint32整数转大端bytes数据
     * @param data
     * @return
     */
    public static byte[] uint32ToBytesBigEnd(int data) {
        if (data < 0) {
            return null;
        }
        byte[] bytes = new byte[4];
        // 00 00 00 00
        bytes[0] = (byte)((data & 0xFF) >> 24);
        bytes[1] = (byte)((data & 0xFF) >> 16);
        bytes[2] = (byte)((data & 0xFF) >> 8);
        bytes[3] = (byte)((data & 0xFF));
        return bytes;
    }
}
