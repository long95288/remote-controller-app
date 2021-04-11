package com.longquanxiao.remotecontroller.utils;


import android.util.Log;

import com.longquanxiao.remotecontroller.core.RCTLCore;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static android.content.ContentValues.TAG;

public class ComputerScreenCaptureThread extends Thread {

    public static final int PC_SCREEN_CAPTURE_RUNNING = 1;
    public static final int PC_SCREEN_CAPTURE_STOP = 2;

    // 6M的缓冲区
    private static final int BUFFER_MAX_SIZE = 6 * 1024 * 1024;
    private byte[] buffer = new byte[BUFFER_MAX_SIZE];
    private int bufferSize = 0;

    private volatile boolean isExit;
    private ComputerScreenCaptureThreadInterface callback = null;

    public ComputerScreenCaptureThread(ComputerScreenCaptureThreadInterface callback) {
        this.callback = callback;
        this.isExit = false;
    }

    private boolean addBufferData(byte[] data, int size) {
        if (bufferSize + size > BUFFER_MAX_SIZE) {
            bufferSize = 0;
            return false;
        }
        System.arraycopy(data, 0, buffer, bufferSize, size);
        bufferSize += size;
        return true;
    }

    private byte[] getSplitData() {
        // 获得长度
        if (bufferSize < 4) {
            return null;
        }
        int dataSize = bytes2Uint32BigEnd(buffer);
        if (bufferSize < dataSize) {
            return null;
        }
        byte[] data = new byte[dataSize - 4];

        System.arraycopy(buffer, 4, data, 0, dataSize - 4);
        System.arraycopy(buffer, dataSize, buffer,0, bufferSize - dataSize);
        bufferSize -= dataSize;
        return data;
    }


    private int bytes2Uint32BigEnd(byte[] bytes) {
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

    @Override
    public void run() {
        super.run();
        // 创建连接,接收数据,回调
        String ip = RCTLCore.getInstance().getServerIP();
        int port = 1404;
        Socket socket = null;
        try {
            callback.screenCaptureStatus(PC_SCREEN_CAPTURE_RUNNING, "START CAPTURE");
            socket = new Socket(ip, port);
            BufferedInputStream input = new BufferedInputStream(socket.getInputStream());
            while (!isExit) {
                byte[] data = getSplitData();
                if (null != data) {
                    callback.screenCaptureImageData(data);
                    continue;
                }
                // 2M buf
                byte[] buf = new byte[1024 * 1024 * 2];
                int readSize = input.read(buf);
                if (!addBufferData(buf, readSize)){
                    break;
                }
                // Thread.sleep(10);
            }
            socket.close();
            input.close();
        }catch (UnknownHostException e) {
            isExit = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        callback.screenCaptureStatus(PC_SCREEN_CAPTURE_STOP, "Thread STOP");
    }

    public void stopThread() {
        this.isExit = true;
    }
}
