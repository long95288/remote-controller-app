package com.longquanxiao.remotecontroller.utils;

import android.annotation.SuppressLint;
import android.util.Log;

import com.longquanxiao.remotecontroller.core.RCTLCore;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

import static android.content.ContentValues.TAG;

public class H264StreamPullThread extends Thread {

    public static final int RUNNING = 1;
    public static final int STOP = 2;
    public static final int FAILED = 3;

    public static final int SCREEN_STREAM = 1;
    public static final int CAMERA_STREAM = 2;

    // 6M的缓冲区
    private static final int BUFFER_MAX_SIZE = 6 * 1024 * 1024;
    private byte[] buffer = new byte[BUFFER_MAX_SIZE];
    private int bufferSize = 0;

    H264StreamPullThreadCallbackInterface callback;
    volatile int status;

    private int streamType = SCREEN_STREAM;
    public H264StreamPullThread(H264StreamPullThreadCallbackInterface callback) {
        this.callback = callback;
    }
    public H264StreamPullThread(H264StreamPullThreadCallbackInterface callback, int streamType) {
        this.callback = callback;
        this.streamType = streamType;
    }
    public void stopH264ReceiveStream() {
        Log.d(TAG, "stopH264ReceiveStream ");
        this.status = STOP;
        this.status = STOP;
        this.status = STOP;
        this.status = STOP;
        this.status = STOP;
        this.status = STOP;

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


    @SuppressLint("DefaultLocale")
    @Override
    public void run() {
        status = RUNNING;
        String ip = RCTLCore.getInstance().getServerIP();
        int port = 1408;
        try {
            Socket socket = new Socket(ip, port);
            InputStream inputStream = socket.getInputStream();

            // 告诉服务器需要获取的流
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(String.format("%d", this.streamType).getBytes());

            BufferedInputStream bis = new BufferedInputStream(inputStream);
            int size = 2 * 1024 * 1024;
            byte[] buf = new byte[size];
            int readSize = 0;
            while (this.status == RUNNING ) {
                // Log.d(TAG, "run: .....");
                byte[] data = getSplitData();
                if (null != data) {
                    callback.receiveH264Data(data, data.length);
                    continue;
                }
                readSize = bis.read(buf);
                // 2M buf
                if (!addBufferData(buf, readSize)){
                    break;
                }
            }
            Log.d(TAG, "run: close socket");
            socket.close();
//            bis.close();
            if (readSize == -1) {
                status = STOP;
                callback.reportStatus(STOP, "NOT DATA FROM PEER");
            }
        }catch (Exception e) {
            status = FAILED;
            callback.reportStatus(FAILED, e.getMessage());
        }
    }
}
