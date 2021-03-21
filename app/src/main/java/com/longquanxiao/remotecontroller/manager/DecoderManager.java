package com.longquanxiao.remotecontroller.manager;

import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;
import android.view.Surface;


import androidx.annotation.RequiresApi;

import com.longquanxiao.remotecontroller.H264StreamPlay;

import java.io.IOException;
import java.nio.ByteBuffer;

public class DecoderManager {

    private static final String TAG = DecoderManager.class.getSimpleName();
    private static DecoderManager instance;
    private MediaCodec mediaCodec;
    private MediaFormat mediaFormat;
    private long frameIndex;
    private volatile boolean isDecodeFinish = false;
    private MediaExtractor mediaExtractor;
    private SpeedManager mSpeedController = new SpeedManager();
    private DecoderMP4Thread mDecodeMp4Thread;
    private DecoderH264Thread mDecodeH264Thread;
    private DecoderManager() {
    }

    public static DecoderManager getInstance() {
        if (instance == null) {
            instance = new DecoderManager();
        }
        return instance;
    }

    /**
     * * Synchronized callback decoding
     */
    private void initMediaCodecSys() {
        try {
            mediaCodec = MediaCodec.createDecoderByType("video/avc");
            mediaFormat = MediaFormat.createVideoFormat("video/avc", 1080, 1920);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Surface surface = H264StreamPlay.getSurface();
        mediaCodec.configure(mediaFormat, surface, null, 0);
        mediaCodec.start();
    }

    /**
     * Play the MP4 file Thread
     */
    private class DecoderMP4Thread extends Thread {
        long pts = 0;

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void run() {
            super.run();
            while (!isDecodeFinish) {
                int inputIndex = mediaCodec.dequeueInputBuffer(-1);
                Log.d(TAG, " inputIndex: " + inputIndex);
                if (inputIndex >= 0) {
                    ByteBuffer byteBuffer = mediaCodec.getInputBuffer(inputIndex);
                    //读取一片或者一帧数据
                    int sampSize = mediaExtractor.readSampleData(byteBuffer, 0);
                    //读取时间戳
                    long time = mediaExtractor.getSampleTime();
                    if (sampSize > 0 && time > 0) {
                        mediaCodec.queueInputBuffer(inputIndex, 0, sampSize, time, 0);
                        //读取一帧后必须调用，提取下一帧
                        //控制帧率在30帧左右
                        mSpeedController.preRender(time);
                        mediaExtractor.advance();
                    }
                }
                BufferInfo bufferInfo = new BufferInfo();
                int outIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
                if (outIndex >= 0) {
                    mediaCodec.releaseOutputBuffer(outIndex, true);
                }
            }
        }

    }

    /**
     * 解析播放H264码流
     */
    private class DecoderH264Thread extends Thread {
        long pts = 0;

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void run() {
            super.run();
            long startTime = System.nanoTime();
            while (!isDecodeFinish) {
                if (mediaCodec != null) {
                    int inputIndex = mediaCodec.dequeueInputBuffer(-1);
                    if (inputIndex >= 0) {
                        ByteBuffer byteBuffer = mediaCodec.getInputBuffer(inputIndex);
                        int sampSize = DecodeH264Stream.getInstance().readSampleData(byteBuffer);
                        long time = (System.nanoTime() - startTime) / 1000;
                        if (sampSize > 0 && time > 0) {
                            mediaCodec.queueInputBuffer(inputIndex, 0, sampSize, time, 0);
                            mSpeedController.preRender(time);
                        }

                    }
                }
                BufferInfo bufferInfo = new BufferInfo();
                int outIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
                if (outIndex >= 0) {
                    mediaCodec.releaseOutputBuffer(outIndex, true);
                }
            }
        }

    }


    public void close() {
        try {
            Log.d(TAG, "close start");
            if (mediaCodec != null) {
                isDecodeFinish = true;
                try {
                    if (mDecodeMp4Thread != null) {
                        mDecodeMp4Thread.join(2000);
                    }
                    if (mDecodeH264Thread != null) {
                        mDecodeH264Thread.join(2000);
                    }
                } catch (InterruptedException e) {
                    Log.e(TAG, "InterruptedException " + e);
                }
                boolean isAlive = mDecodeMp4Thread.isAlive();
                Log.d(TAG, "close end isAlive :" + isAlive);
                mediaCodec.stop();
                mediaCodec.release();
                mediaCodec = null;
                mSpeedController.reset();
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
        DecodeH264Stream.getInstance().close();
        instance = null;
    }


    public void startMP4Decode() {
        initMediaCodecSys();
        mDecodeMp4Thread = new DecoderMP4Thread();
        mDecodeMp4Thread.setName("DecoderMP4Thread");
        mDecodeMp4Thread.start();

    }

    public void startH264Decode() {
        initMediaCodecSys();
        mDecodeH264Thread = new DecoderH264Thread();
        mDecodeH264Thread.setName("DecoderH264Thread");
        mDecodeH264Thread.start();
    }

}
