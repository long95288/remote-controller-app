package com.longquanxiao.remotecontroller;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaCodec;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Button;

import com.longquanxiao.remotecontroller.core.RCTLCore;
import com.longquanxiao.remotecontroller.manager.DecoderManager;
import com.longquanxiao.remotecontroller.utils.H264Player;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
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
    private H264Player h264Player;
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

        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
        }
        initSurface();
    }

    private void initSurface() {
        final SurfaceHolder surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                h264Player = new H264Player( H264StreamPlayActivity.this, filePath, surfaceHolder.getSurface());
                h264Player.play();
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                try {
                    h264Player.pause();
                }catch (Exception e){
                    e.printStackTrace();
                }
                try {
                    h264Player.release();
                }catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy..");
        try{
            h264Player.release();
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Surface getSurface() {
        return surfaceView.getHolder().getSurface();
    }
}