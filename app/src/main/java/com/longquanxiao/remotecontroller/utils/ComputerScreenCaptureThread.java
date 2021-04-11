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

    private volatile boolean isExit;
    private ComputerScreenCaptureThreadInterface callback = null;

    public ComputerScreenCaptureThread(ComputerScreenCaptureThreadInterface callback) {
        this.callback = callback;
        this.isExit = false;
    }

    private int bytes2Uint32BigEnd(byte[] bytes) {
        if (bytes.length < 4) {
            return -1;
        }

        int length = (bytes[0] << 24) & 0xFF;
        length |= (bytes[1] << 16) & 0xFF;
        length |= (bytes[2] << 8) & 0xFF;
        length |= bytes[3] & 0xFF;

        return length;
    }

    @Override
    public void run() {
        super.run();
        // 创建连接,接收数据,回调
        String ip = RCTLCore.getInstance().getServerIP();
        int port = 1401;
        Socket socket = null;
        try {
            callback.screenCaptureStatus(PC_SCREEN_CAPTURE_RUNNING, "START CAPTURE");
            while (!isExit) {
                socket = new Socket(ip, port);
                BufferedInputStream input = new BufferedInputStream(socket.getInputStream());
                // 2M buf
                byte[] buf = new byte[1024 * 1024 * 2];
                int readSize = input.read(buf);
                byte[] data = new byte[readSize];
                System.arraycopy(buf, 0, data, 0, readSize);
                callback.screenCaptureImageData(data);
                socket.close();
                Thread.sleep(20);
            }
        }catch (UnknownHostException e) {
            isExit = true;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        callback.screenCaptureStatus(PC_SCREEN_CAPTURE_STOP, "Thread STOP");
    }

    public void stopThread() {
        this.isExit = true;
    }
}
