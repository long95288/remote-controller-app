package com.longquanxiao.remotecontroller.utils;

public interface ProgressRequestListener {
    void onRequestProgress(long byteWritten, long contentLength, boolean done);
}