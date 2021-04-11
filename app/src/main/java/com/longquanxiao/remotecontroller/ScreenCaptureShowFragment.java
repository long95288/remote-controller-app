package com.longquanxiao.remotecontroller;

import android.annotation.SuppressLint;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.Bitmap;
import android.widget.ImageView;

import com.longquanxiao.remotecontroller.core.RCTLCore;
import com.longquanxiao.remotecontroller.utils.ComputerScreenCaptureThread;
import com.longquanxiao.remotecontroller.utils.ComputerScreenCaptureThreadInterface;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.Socket;

import static android.content.ContentValues.TAG;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ScreenCaptureShowFragment#newInstance} factory method to
 * create an instance of this fragment.
 */


public class ScreenCaptureShowFragment extends Fragment {
    ImageView imageView = null;
    private final Handler handler;
    ComputerScreenCaptureThread screenCaptureThread;
    private boolean isViewed = false;
    {
        handler = new Handler() {
            @SuppressLint("HandlerLeak")
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                if (msg.what == 233) {
                    loadImageData(msg);
                    System.out.println("收到更新消息");
                    // status的
//                    System.out.println("" + (String)msg.obj);
//                    System.out.println("" + msg.getData().getString("bundledata"));
                    // imageView.setImageDrawable((RoundedBitmapDrawable)msg.obj);
//                    statusView.setText(msg.getData().getString("bundledata"));
                }
            }

            @Override
            public void dispatchMessage(@NonNull Message msg) {
                super.dispatchMessage(msg);

            }
        };
    };

    private void loadImageData(Message msg) {
        RoundedBitmapDrawable drawable = (RoundedBitmapDrawable)msg.obj;
//        WindowManager manager = (WindowManager) this.getContext().getSystemService(Context.WINDOW_SERVICE);
//        DisplayMetrics metrics = new DisplayMetrics();
//        manager.getDefaultDisplay().getMetrics(metrics);
//        System.out.println("height:" + metrics.heightPixels + " width:"+metrics.widthPixels);
//        metrics.widthPixels = drawable.getBitmap().getHeight();
//        metrics.heightPixels = drawable.getBitmap().getWidth();
//        drawable.setTargetDensity(metrics);
        imageView.setImageDrawable(drawable);
    }

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public ScreenCaptureShowFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ScreenCaptureShowFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ScreenCaptureShowFragment newInstance(String param1, String param2) {
        ScreenCaptureShowFragment fragment = new ScreenCaptureShowFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    private Bitmap Byte2BitMap(byte[] b) {
        if (b.length  != 0) {
            return BitmapFactory.decodeByteArray(b, 0, b.length);
        }
        return null;
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }



    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_screen_capture_show, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // 获得电脑屏幕,读取输入流的图片数据
        isViewed = true;
        imageView = view.findViewById(R.id.imageView);
//        Context context = this.getContext();
//        if (null != context) {
//            ImageViewUtil.matchAll(context, imageView);
//        }
        screenCaptureThread = new ComputerScreenCaptureThread(new ComputerScreenCaptureThreadInterface() {
            @Override
            public void screenCaptureImageData(byte[] data) {
                // 接收图片数据,渲染到图片区
                // Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                Bitmap bitmap = BitmapFactory.decodeStream(new ByteArrayInputStream(data));
                try {
                    Message message = new Message();
                    message.obj = RoundedBitmapDrawableFactory.create(getResources(), bitmap);
                    message.what = 233;
                    if (null != handler){
                        handler.sendMessage(message);
                    }
                }catch (Exception e) {
                    e.printStackTrace();
                    screenCaptureThread.stopThread();
                }
            }

            @Override
            public void screenCaptureStatus(int status, String msg) {
                if (status == ComputerScreenCaptureThread.PC_SCREEN_CAPTURE_STOP) {
                    Log.d(TAG, "screenCaptureStatus: msg" + msg);
                }
            }
        });
        screenCaptureThread.start();

//        view.post(() -> new Thread(() -> {
//            System.out.println("开始显示电脑屏幕图像");
//            String ip = RCTLCore.getInstance().getServerIP();
//            int port = 1401;
//            while (isViewed) {
//                try {
//                    Socket socket = new Socket(ip, port);
//                    BufferedInputStream input = new BufferedInputStream(socket.getInputStream());
//                    Bitmap bitmap = BitmapFactory.decodeStream(input);
//                    if (isViewed) {
//                        RoundedBitmapDrawable bg = RoundedBitmapDrawableFactory.create(getResources(), bitmap);
//                        Message message = new Message();
//                        message.obj = bg;
//                        message.what = 233;
//                        handler.sendMessage(message);
//                    }
//                    // 30fps => 1000 / 30 =>
//                    Thread.sleep(10);
//                    socket.close();
//                }catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//            System.out.println("结束观看.....");
//        }).start());
    }



    @Override
    public void onDestroyView() {
        System.out.println("退出观看界面。。。。");
        isViewed = false;
        if (null != screenCaptureThread) {
            screenCaptureThread.stopThread();
        }
        super.onDestroyView();
    }
}