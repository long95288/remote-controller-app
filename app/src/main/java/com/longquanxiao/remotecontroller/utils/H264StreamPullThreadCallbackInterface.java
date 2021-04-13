package com.longquanxiao.remotecontroller.utils;

public interface H264StreamPullThreadCallbackInterface {
    void reportStatus(int status, String msg);
    void receiveH264Data(byte[] data, int size);
}
