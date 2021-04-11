package com.longquanxiao.remotecontroller.utils;

public interface ComputerScreenCaptureThreadInterface {
    void screenCaptureImageData(byte[] data);
    void screenCaptureStatus(int status, String msg);
}
