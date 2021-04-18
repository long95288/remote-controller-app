package com.longquanxiao.remotecontroller.utils;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.RequiresApi;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static android.content.ContentValues.TAG;

public class H264Player {
    private String path;
    private MediaCodec mediaCodec;
    private H264StreamPullThread pullThread;
    private ByteArrayOutputStream buffer;
    private Surface surface;
    private String mimeType = "video/avc";
    private int width;
    private int height;
    private MediaCodec.BufferInfo mediaCodecBufferInfo = null;
    boolean isExit = false;
    private Lock lock;
    private volatile boolean isViewed;
    private int streamType = H264StreamPullThread.SCREEN_STREAM;
    private Thread inputThread;
    private Thread outputThread;


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public H264Player(Context context, String path, Surface surface, int streamType){
        lock = new ReentrantLock();
        this.path = path;
        width = 1920;
        height = 1080;
        mediaCodecBufferInfo = new MediaCodec.BufferInfo();
        isViewed = false;
        this.streamType = streamType;
        try {
            mediaCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            MediaFormat mediaFormat = MediaFormat.createVideoFormat( mimeType, width, height);
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
            mediaFormat.setInteger(MediaFormat.KEY_ROTATION, 90);
//            mediaFormat.setInteger();
            // 需要页面绘制完成才能进行config
            mediaCodec.configure(mediaFormat, surface, null, 0);
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void play() {
        mediaCodec.start();
        inputThread = new Thread(()->{
            pullThread = new H264StreamPullThread(new H264StreamPullThreadCallbackInterface() {
                @Override
                public void reportStatus(int status, String msg) {
                    if (status == H264StreamPullThread.STOP || status == H264StreamPullThread.FAILED){
                        Log.d(TAG, "reportStatus: " + msg);
                        isExit = true;
                    }
                }

                @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void receiveH264Data(byte[] data, int size) {
                    Log.d(TAG, "receiveH264Data: " + size);
                    try {
                        ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
                        int inputBufferIndex = mediaCodec.dequeueInputBuffer(0);
                        if (inputBufferIndex >= 0) {
                            ByteBuffer inputBuffer = mediaCodec.getInputBuffer(inputBufferIndex);
                            inputBuffer.put(data, 0, size);
                            mediaCodec.queueInputBuffer(inputBufferIndex, 0, size,0, 0);
                        }
                    }catch (Exception e) {
                        Log.d(TAG, "receiveH264Data: "+e.getMessage());
                    }
                }
            }, streamType);
            pullThread.start();
        });
        inputThread.start();

        outputThread = new Thread(() -> {
            while (!isExit){
                try{
                    int outIndex = mediaCodec.dequeueOutputBuffer(mediaCodecBufferInfo, 300 * 1000);
                    if (outIndex < 0) {
                        Log.d(TAG, "decodeFrame: dequeueOutputBuffer Timeout");
                        continue;
                    }
                    // 解码后的数据,一般自己做中间件处理的时候才会使用这个
//        ByteBuffer byteBuffer = mediaCodec.getOutputBuffer(outIndex);
                    // 直接渲染到surface
                    mediaCodec.releaseOutputBuffer(outIndex, true);
                    Thread.sleep(20);
                }catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        outputThread.start();
    }
    public void pause() {
        pullThread.stopH264ReceiveStream();
        mediaCodec.stop();
    }
    public void release() {
        mediaCodec.release();
    }
    private byte[] getBytes(String path) throws IOException {
        InputStream is = new DataInputStream(new FileInputStream(new File(path)));
        int len;
        int size = 1024;
        byte[] buf;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        buf = new byte[size];
        while ((len = is.read(buf, 0, size)) != -1){
            bos.write(buf, 0, len);
        }
        return bos.toByteArray();
    }
    private int findNextFrame(byte[] bytes, int start, int totalSize) {
        for (int i = start; i < totalSize - 4;i ++) {
            if ((bytes[i] == 0 && bytes[i + 1] == 0 && bytes[i + 2] == 1) ||
                (bytes[i] == 0 && bytes[i + 1] == 0 && bytes[i + 2] == 0 && bytes[i + 3] == 1) ){
                return i;
            }
        }
        return -1;
    }

    // 返回一帧数据
    private byte[] getOneFrame() throws Exception {
        boolean foundOneFrame = false;
        int firstIndex = -1;
        int nextIndex = -1;
        while (!foundOneFrame) {
            // 获得buffer
            lock.lock();
            byte[] totalBuf = buffer.toByteArray();
            lock.unlock();
            if (firstIndex == -1) {
                firstIndex = findNextFrame(totalBuf, 0, totalBuf.length);
                if (firstIndex == -1) {
                    Thread.sleep(20);
                }
            }else{
                // 找下一帧的起始
                nextIndex = findNextFrame(totalBuf, firstIndex + 4, totalBuf.length);
                if (nextIndex != -1) {
                    foundOneFrame = true;
                }else{
                    Thread.sleep(20);
                }
            }
        }
        lock.lock();
        // 获得帧数对应的数据
        byte[] totalBuf = buffer.toByteArray();
        byte[] oneFrame = Arrays.copyOf(totalBuf, nextIndex - firstIndex);
        // 处理剩余数据
        byte[] remain = Arrays.copyOfRange(totalBuf,nextIndex, totalBuf.length);
        buffer.reset();
        buffer.write(remain);
        lock.unlock();
        return oneFrame;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private boolean decodeFrame(byte[] nalu) {
        // 3秒钟解码等待应该不多吧？
        // 塞数据进入编码队列
        int inIndex = mediaCodec.dequeueInputBuffer(300 * 1000);
        if (inIndex < 0) {
            Log.d(TAG, "decodeFrame: dequeueInputBuffer Timeout\n");
            return false;
        }
        ByteBuffer inputBuffer = mediaCodec.getInputBuffer(inIndex);
        inputBuffer.put(nalu, 0, nalu.length);
        mediaCodec.queueInputBuffer(inIndex, 0, nalu.length, 0, 0);

        // 获得编码好的数据
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 300 * 1000);
        if (outIndex < 0) {
            Log.d(TAG, "decodeFrame: dequeueOutputBuffer Timeout");
            return false;
        }

        // 解码后的数据,一般自己做中间件处理的时候才会使用这个
//        ByteBuffer byteBuffer = mediaCodec.getOutputBuffer(outIndex);
        // 直接渲染到surface
        mediaCodec.releaseOutputBuffer(outIndex, true);
        return true;
    }

//    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
//    @Override
//    public void run() {
//        buffer = new ByteArrayOutputStream();
//        // 启动数据接收线程
//        pullThread = new H264StreamPullThread(new H264StreamPullThreadCallbackInterface() {
//            @Override
//            public void reportStatus(int status, String msg) {
//                if (status == H264StreamPullThread.STOP || status == H264StreamPullThread.FAILED){
//                    Log.d(TAG, "reportStatus: " + msg);
//                    isExit = true;
//                }
//            }
//
//            @Override
//            public void receiveH264Data(byte[] data, int size) {
//                Log.d(TAG, "receiveH264Data: " + size);
//                decodeFrame(data);
//            }
//        });
//        pullThread.start();

        // 解码主线程
//        while (!isExit) {
//            try {
//                byte[] nalu = getOneFrame();
//                if (null == nalu) {
//                    break;
//                }
//                Log.d(TAG, "run: get one frame, start decode");
//                if (!decodeFrame(nalu)){
//                    break;
//                }
//                Log.d(TAG, "run: get one frame, finish decode");
//                Thread.sleep(33);
//            }catch (Exception e) {
//                break;
//            }
//        }
//        isExit = true;
//        pullThread.stopH264ReceiveStream();
//    }
    //    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
//    @Override
//    public void run() {
//        byte[] bytes = null;
//        try {
//            bytes = getBytes(path);
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//
//        int startIndex = 0;
//        int totalSize = bytes.length;
//        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
//        while(true) {
//            byte[] oneFrame = getOneFrame();
//            if (null == oneFrame) {
//                break;
//            }
//            if (!decodeFrame(oneFrame)) {
//                break;
//            }
//            try {
//                Thread.sleep(33);
//            }catch (Exception e) {
//                e.printStackTrace();
//                break;
//            }
//
//
//            int nextFrameIndex = findNextFrame(bytes, startIndex + 1, totalSize);
//            if (nextFrameIndex == -1) {
//                break;
//            }
//            int inIndex = mediaCodec.dequeueInputBuffer(10000);
//            if (inIndex >= 0) {
//                if (totalSize == 0 || startIndex >= totalSize - 4) {
//                    break;
//                }
//                Log.d(TAG, "run: start decode frame");
//                ByteBuffer byteBuffer = mediaCodec.getInputBuffer(inIndex);
//                byteBuffer.put(bytes, startIndex, nextFrameIndex - startIndex);
//                mediaCodec.queueInputBuffer(inIndex, 0, nextFrameIndex - startIndex,0, 0);
//                startIndex = nextFrameIndex;
//            }else{
//                continue;
//            }
//
//            int outIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 10000);
//            if (outIndex >= 0) {
////                ByteBuffer byteBuffer = mediaCodec.getOutputBuffer(outIndex);
//                Log.d(TAG, "run: decode frame success");
//                mediaCodec.releaseOutputBuffer(outIndex, true);
//            }
//            try {
//                Thread.sleep(33);
//            }catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//    }
}
