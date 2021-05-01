package com.longquanxiao.remotecontroller;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.longquanxiao.remotecontroller.cmd.RemoteControlCMD;
import com.longquanxiao.remotecontroller.core.RCTLCore;
import com.longquanxiao.remotecontroller.utils.NetTool;

import static android.content.ContentValues.TAG;


public class FirstFragment extends Fragment {
    TextView statusView = null;
    EditText ipEditText = null;

    boolean hasNoticeServerIP = false;
    String serverIP = null;

    SeekBar masterVolumeSeekBar = null;
    TextView masterVolumeTextView = null;

    private final Handler handler;
    {
        handler = new Handler() {
            @SuppressLint("HandlerLeak")
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                if (msg.what == 1) {
                    System.out.println("收到消息");
                    // status的
//                    System.out.println("" + (String)msg.obj);
//                    System.out.println("" + msg.getData().getString("bundledata"));
                    statusView.setText("handler 更新UI"+(String)msg.obj);
//                    statusView.setText(msg.getData().getString("bundledata"));
                }
            }

            @Override
            public void dispatchMessage(@NonNull Message msg) {
                super.dispatchMessage(msg);

            }
        };
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_first, container, false);
    }

    @SuppressLint("SetTextI18n")
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // 初始化页面数据,当前电脑的状态,
        Toast toast = Toast.makeText(getContext(), "检查成功", Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.TOP, 0, 0);

        statusView = view.findViewById(R.id.statusText);
        ipEditText = view.findViewById(R.id.ipInputText);

         masterVolumeTextView = ((TextView)view.findViewById(R.id.masterVolumeTextView));
         masterVolumeSeekBar = ((SeekBar)view.findViewById(R.id.masterSeekBar));
         masterVolumeSeekBar.setOnSeekBarChangeListener(new MasterVolumeOnSeekBarChangeListener(this));


         RCTLCore.getInstance().setUiHandler(handler);

        // 获得服务器IP地址和设备音量信息
        new Thread(() -> {
            String ipv4 = NetTool.geLocalWifiAddress(view);
            String getServerIP = NetTool.getServerIp(ipv4);
            if (getServerIP == null || "0.0.0.0".equals(getServerIP)){
                hasNoticeServerIP = false;
            }else{
                hasNoticeServerIP = true;
                serverIP = getServerIP;
                // 使用post可以安全的更新UI数据
                ipEditText.post(() -> ipEditText.setText(serverIP));
                // 创建连接
                RCTLCore.getInstance().setServerIP(serverIP);
                RCTLCore.getInstance().setServerPort(9999);
                hasNoticeServerIP = true;
                toast.setText("探测服务器IP"+serverIP);
                toast.show();
                // 获得音量
                try {
                    int volume = RemoteControlCMD.getMasterVolume();
                    masterVolumeSeekBar.post(() -> masterVolumeSeekBar.setProgress(volume));
                    masterVolumeTextView.post(() -> masterVolumeTextView.setText(""+volume));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

        Button shutdownBtn = view.findViewById(R.id.shutdownBtn);
        Button cancelShutdownBtn = view.findViewById(R.id.cancelShutdownBtn);

        // 检查服务器IP地址有效性
        view.findViewById(R.id.testServerIpBtn).setOnClickListener((v -> {
            //
            String serverIp = ipEditText.getText().toString();
            v.setClickable(false);
            new Thread(() -> {
                try {
                    if (NetTool.checkServerIp(serverIp)) {
                        statusView.post(() -> {statusView.setText("检查IP成功");});
                        serverIP = ipEditText.getText().toString();
                        Log.d(TAG, "onViewCreated: checkout success ip" + serverIp);
                        RCTLCore.getInstance().setServerIP(serverIP);
                        toast.setText("检查成功");
                        toast.show();
                    }else {
                        statusView.post(() -> {statusView.setText("检查IP失败");});
                        toast.setText("检查失败");
                        toast.show();
                    }
                }catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
            v.setClickable(true);
        }));

        view.findViewById(R.id.button_first).setOnClickListener((v ->{
            Intent intent = new Intent();
            intent.putExtra("isPlayH264", true);
            intent.putExtra("StreamType", 1);
            intent.setClass(this.getActivity(), H264StreamPlayActivity.class);
            startActivity(intent);
        }));

        view.findViewById(R.id.showPcCameraBtn).setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.putExtra("StreamType", 2);
            intent.setClass(this.getActivity(), H264StreamPlayActivity.class);
            startActivity(intent);
        });
        view.findViewById(R.id.goToFileTransferActivityBtn).setOnClickListener((v) -> {
            Intent intent = new Intent();
            intent.setClass(this.getActivity(), FileTransferActivity.class);
            startActivity(intent);
        });



        // 获得电脑屏幕
        view.findViewById(R.id.showPcScreenBtn).setOnClickListener(v -> NavHostFragment.findNavController(FirstFragment.this)
                .navigate(R.id.action_FirstFragment_to_ScreenCaptureShow));

        shutdownBtn.setOnClickListener(this::onClickShutdownBtn);

        String ip = NetTool.geLocalWifiAddress(view);
        if ("0.0.0.0".equals(ip)){ ip = NetTool.getLocal4GAddress();}

        ((EditText)view.findViewById(R.id.localIPEditText)).setText(ip);

        cancelShutdownBtn.setOnClickListener(this::onClickCancelShutdownBtn);

    }

    private void onClickCancelShutdownBtn(View v) {
        if (hasNoticeServerIP) {
            new Thread(() -> {
                boolean ret = RemoteControlCMD.cancelShutdownPlan();
                if (ret) {
                    this.statusView.post(() -> statusView.setText("取消关机成功"));
                } else {
                    this.statusView.post(() -> statusView.setText("取消关机失败"));
                }
            }).start();
        } else {
            statusView.post(() -> statusView.setText("未设置服务器"));
        }
    }

    @SuppressLint("SetTextI18n")
    private void onClickShutdownBtn(View v) {
        if(!hasNoticeServerIP){
            statusView.post(() -> statusView.setText("未查询到服务器"));
            return;
        }
        new Thread(() -> {
            try {
                if (RemoteControlCMD.setShutdownPlan(30)) {
                    statusView.post(() -> statusView.setText("电脑将在30s后关机"));
                }
            } catch (Exception e) {
                statusView.post(() -> statusView.setText(e.getMessage()));
                e.printStackTrace();
            }
        }).start();
    }

    static class MasterVolumeOnSeekBarChangeListener implements OnSeekBarChangeListener {
        FirstFragment firstFragment;
        public MasterVolumeOnSeekBarChangeListener(FirstFragment fragment) {
            this.firstFragment = fragment;
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            System.out.println("Seekbar progress "+progress);
            firstFragment.masterVolumeTextView.setText(""+progress);
            RCTLCore.getInstance().setServerIP(firstFragment.ipEditText.getText().toString());
            RCTLCore.getInstance().setServerPort(9999);
            System.out.println("结束滑块"+seekBar.getProgress());
            if (firstFragment.hasNoticeServerIP) {
                new Thread(() -> {
                    try {
                        int volume = RemoteControlCMD.setMasterVolume(seekBar.getProgress());
                        firstFragment.masterVolumeTextView.setText(Integer.toString(volume));
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }).start();
            }else{
                System.out.println("unconnected server");
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            System.out.println("开始滑块...");
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            if (!firstFragment.hasNoticeServerIP) {
                return;
            }
            new Thread(() -> {
                try {
                    int volume = RemoteControlCMD.setMasterVolume(seekBar.getProgress());
                    firstFragment.masterVolumeTextView.setText(Integer.toString(volume));
                }catch (Exception e){
                    e.printStackTrace();
                }
            }).start();
        }
    }

}