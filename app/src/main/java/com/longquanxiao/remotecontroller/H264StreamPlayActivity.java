package com.longquanxiao.remotecontroller;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.Toast;

import com.longquanxiao.remotecontroller.core.RCTLCore;
import com.longquanxiao.remotecontroller.manager.DecodeH264Stream;
import com.longquanxiao.remotecontroller.manager.DecoderManager;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

public class H264StreamPlayActivity extends AppCompatActivity {

    private static final String TAG = H264StreamPlayActivity.class.getSimpleName();

    private static final int INIT_MANAGER_MSG = 0x01;

    private static final int INIT_MANAGER_DELAY = 1 * 1000;

    public static SurfaceView surfaceView;

    private SurfaceHolder mSurfaceHolder;

    private Button mEndBtn;

    private boolean isPlayH264;

    private Thread mDecodeThread;
    private MediaCodec mCodec;

    private boolean mStopFlag = false;
    private DataInputStream mInputStream;
    private String FileName = "test.h264";
    private static final int VIDEO_WIDTH = 1920; //长宽应该关系不大
    private static final int VIDEO_HEIGHT = 1088;
    private int FrameRate = 15;
    private Boolean UseSPSandPPS = false;
    private String filePath = Environment.getExternalStorageDirectory() + "/" + FileName;

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_h264_stream_play);
        surfaceView = findViewById(R.id.surfaceview);
        mEndBtn = findViewById(R.id.end);
        mEndBtn.setOnClickListener((v) -> {
            DecoderManager.getInstance().close();
            // finish();
        });

        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
        }
        initSurface();
    }



    private void initSurface() {
        File f = new File(filePath);
        if (null == f || !f.exists() || f.length() == 0) {
            Toast.makeText(this, "视频文件不存在", Toast.LENGTH_LONG).show();
            return;
        }
        try {
            //获取文件输入流
            mInputStream = new DataInputStream(new FileInputStream(new File(filePath)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            try {
                mInputStream.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        if (null != surfaceView) {
            mSurfaceHolder = surfaceView.getHolder();
            mSurfaceHolder.addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder holder) {
//                    mHandler.sendEmptyMessageDelayed(INIT_MANAGER_MSG, INIT_MANAGER_DELAY);
                    try
                    {
                        //通过多媒体格式名创建一个可用的解码器
                        mCodec = MediaCodec.createDecoderByType("video/avc");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    //初始化编码器
                    final MediaFormat mediaformat = MediaFormat.createVideoFormat("video/avc", VIDEO_WIDTH, VIDEO_HEIGHT);
                    //获取h264中的pps及sps数据
                    if (UseSPSandPPS) {
                        byte[] header_sps = {0, 0, 0, 1, 103, 66, 0, 42, (byte) 149, (byte) 168, 30, 0, (byte) 137, (byte) 249, 102, (byte) 224, 32, 32, 32, 64};
                        byte[] header_pps = {0, 0, 0, 1, 104, (byte) 206, 60, (byte) 128, 0, 0, 0, 1, 6, (byte) 229, 1, (byte) 151, (byte) 128};
                        mediaformat.setByteBuffer("csd-0", ByteBuffer.wrap(header_sps));
                        mediaformat.setByteBuffer("csd-1", ByteBuffer.wrap(header_pps));
                    }
                    //设置帧率
                    mediaformat.setInteger(MediaFormat.KEY_FRAME_RATE, FrameRate);
                    //https://developer.android.com/reference/android/media/MediaFormat.html#KEY_MAX_INPUT_SIZE
                    //设置配置参数，参数介绍 ：
                    // format	如果为解码器，此处表示输入数据的格式；如果为编码器，此处表示输出数据的格式。
                    //surface	指定一个surface，可用作decode的输出渲染。
                    //crypto	如果需要给媒体数据加密，此处指定一个crypto类.
                    //   flags	如果正在配置的对象是用作编码器，此处加上CONFIGURE_FLAG_ENCODE 标签。
                    mCodec.configure(mediaformat, holder.getSurface(), null, 0);
                    startDecodingThread();
                }
                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

                }
                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {
                    mCodec.stop();
                    mCodec.release();
                }
            });
        }else{
            System.out.println("Surface is NULL");
        }

    }
    private void startDecodingThread() {
        mCodec.start();
        mDecodeThread = new Thread(new decodeThread());
        mDecodeThread.start();
    }

    /**
     * @author ldm
     * @description 解码线程
     * @time 2016/12/19 16:36
     */
    private class decodeThread implements Runnable {
        @Override
        public void run() {
            try {
                String ip = RCTLCore.getInstance().getServerIP();
//                ip = "192.168.0.2";
                int port = 1402;
                Socket socket = new Socket(ip, port);
                InputStream inputStream = new BufferedInputStream(socket.getInputStream());
                mInputStream.close();
                mInputStream = new DataInputStream(inputStream);
                decodeLoop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void decodeLoop() {
            //存放目标文件的数据
            ByteBuffer[] inputBuffers = mCodec.getInputBuffers();
            //解码后的数据，包含每一个buffer的元数据信息，例如偏差，在相关解码器中有效的数据大小
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            long startMs = System.currentTimeMillis();
            long timeoutUs = 10000;
            byte[] marker0 = new byte[]{0, 0, 0, 1};
            byte[] dummyFrame = new byte[]{0x00, 0x00, 0x01, 0x20};
            byte[] streamBuffer = null;
            try {
                streamBuffer = getBytes(mInputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
            int bytes_cnt = 0;
            while (mStopFlag == false) {
                bytes_cnt = streamBuffer.length;
                if (bytes_cnt == 0) {
                    streamBuffer = dummyFrame;
                }

                int startIndex = 0;
                int remaining = bytes_cnt;
                while (true) {
                    if (remaining == 0 || startIndex >= remaining) {
                        break;
                    }
                    int nextFrameStart = KMPMatch(marker0, streamBuffer, startIndex + 2, remaining);
                    if (nextFrameStart == -1) {
                        nextFrameStart = remaining;
                    } else {
                    }

                    int inIndex = mCodec.dequeueInputBuffer(timeoutUs);
                    if (inIndex >= 0) {
                        ByteBuffer byteBuffer = inputBuffers[inIndex];
                        byteBuffer.clear();
                        byteBuffer.put(streamBuffer, startIndex, nextFrameStart - startIndex);
                        //在给指定Index的inputbuffer[]填充数据后，调用这个函数把数据传给解码器
                        mCodec.queueInputBuffer(inIndex, 0, nextFrameStart - startIndex, 0, 0);
                        startIndex = nextFrameStart;
                    } else {
                        continue;
                    }

                    int outIndex = mCodec.dequeueOutputBuffer(info, timeoutUs);
                    if (outIndex >= 0) {
                        //帧控制是不在这种情况下工作，因为没有PTS H264是可用的
                        while (info.presentationTimeUs / 1000 > System.currentTimeMillis() - startMs) {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        boolean doRender = (info.size != 0);
                        //对outputbuffer的处理完后，调用这个函数把buffer重新返回给codec类。
                        mCodec.releaseOutputBuffer(outIndex, doRender);
                    } else {
                    }
                }
                mStopFlag = true;
            }
        }
    }

    public static byte[] getBytes(InputStream is) throws IOException {
        int len;
        int size = 1024;
        byte[] buf;
        if (is instanceof ByteArrayInputStream) {
            size = is.available();
            buf = new byte[size];
            len = is.read(buf, 0, size);
        } else {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            buf = new byte[size];
            while ((len = is.read(buf, 0, size)) != -1)
                bos.write(buf, 0, len);
            buf = bos.toByteArray();
        }
        return buf;
    }

    int KMPMatch(byte[] pattern, byte[] bytes, int start, int remain) {
        try {
            Thread.sleep(30);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        int[] lsp = computeLspTable(pattern);

        int j = 0;  // Number of chars matched in pattern
        for (int i = start; i < remain; i++) {
            while (j > 0 && bytes[i] != pattern[j]) {
                // Fall back in the pattern
                j = lsp[j - 1];  // Strictly decreasing
            }
            if (bytes[i] == pattern[j]) {
                // Next char matched, increment position
                j++;
                if (j == pattern.length)
                    return i - (j - 1);
            }
        }

        return -1;  // Not found
    }

    int[] computeLspTable(byte[] pattern) {
        int[] lsp = new int[pattern.length];
        lsp[0] = 0;  // Base case
        for (int i = 1; i < pattern.length; i++) {
            // Start by assuming we're extending the previous LSP
            int j = lsp[i - 1];
            while (j > 0 && pattern[i] != pattern[j])
                j = lsp[j - 1];
            if (pattern[i] == pattern[j])
                j++;
            lsp[i] = j;
        }
        return lsp;
    }
    private void initManager() {
        Intent intent = getIntent();
        isPlayH264 = intent.getBooleanExtra("isPlayH264", false);
        if (isPlayH264) {
            DecoderManager.getInstance().startH264Decode();
        }
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
//        DecodeH264Stream.getInstance().exitDecoder = true;
//        DecodeH264Stream.getInstance().close();
        Log.d(TAG, "onDestroy..");
    }

    public static Surface getSurface() {
        return surfaceView.getHolder().getSurface();
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == INIT_MANAGER_MSG) {
                initManager();
            }
        }
    };
}