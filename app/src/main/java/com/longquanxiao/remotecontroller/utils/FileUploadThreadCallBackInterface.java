package com.longquanxiao.remotecontroller.utils;

/**
 * 文件上传回调接口
 */
public interface FileUploadThreadCallBackInterface {
    void reportStatus(int id, int status, String msg);
    void reportProgress(int id, long uploadSize, long totalSize);
}