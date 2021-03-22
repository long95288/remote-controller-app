package com.longquanxiao.remotecontroller;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Button;

import com.longquanxiao.remotecontroller.manager.DecodeH264Stream;
import com.longquanxiao.remotecontroller.manager.DecoderManager;

public class H264StreamPlayActivity extends AppCompatActivity {

    private static final String TAG = H264StreamPlayActivity.class.getSimpleName();

    private static final int INIT_MANAGER_MSG = 0x01;

    private static final int INIT_MANAGER_DELAY = 1 * 1000;

    public static SurfaceView surfaceView;

    private SurfaceHolder mSurfaceHolder;

    private Button mEndBtn;

    private boolean isPlayH264;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_h264_stream_play);
        surfaceView = findViewById(R.id.surfaceview);
        mEndBtn = findViewById(R.id.end);
        mEndBtn.setOnClickListener((v) -> {
            DecoderManager.getInstance().close();
            finish();
        });

        initSurface();
    }

    private void initSurface() {
        if (null != surfaceView) {
            mSurfaceHolder = surfaceView.getHolder();
            mSurfaceHolder.addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                    mHandler.sendEmptyMessageDelayed(INIT_MANAGER_MSG, INIT_MANAGER_DELAY);
                }
                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

                }
                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {
                }
            });
        }else{
            System.out.println("Surface is NULL");
        }

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
        DecodeH264Stream.getInstance().exitDecoder = true;
        DecodeH264Stream.getInstance().close();
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