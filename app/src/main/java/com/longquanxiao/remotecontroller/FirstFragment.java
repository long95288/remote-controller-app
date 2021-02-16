package com.longquanxiao.remotecontroller;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.JsonReader;
import android.util.TimeUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.alibaba.fastjson.JSON;
import com.longquanxiao.remotecontroller.core.RCTLCore;
import com.longquanxiao.remotecontroller.utils.NetTool;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Timer;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
class ResponseStatus {
    String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

public class FirstFragment extends Fragment {
    TextView statusView = null;
    String statusViewText = "";
    boolean updateText = false;

    EditText ipEditText = null;
    String ipEditTextValue = null;
    boolean updateEditTextValue = false;

    boolean hasNoticeServerIP = false;
    String serverIP = null;
    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_first, container, false);
    }
    public void setStatusViewText(String text) {
        updateText = true;
        this.statusViewText = text;
    }
    public void setEditTextText(String text) {
        if (null != this.ipEditText) {
           updateEditTextValue = true;
           ipEditTextValue= text;
        }
    }
    public String getStatusViewText() {
        return this.statusViewText;
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.setBackgroundResource(R.drawable.bg);
        view.getBackground().setAlpha(180);

        // 初始化页面数据,当前电脑的状态,
        statusView = view.findViewById(R.id.statusText);
        ipEditText = ((EditText)view.findViewById(R.id.ipInputText));

        // 获得服务器IP地址
        new Thread(new Runnable() {
            @Override
            public void run() {
                String ipv4 = NetTool.geLocalWifiAddress(view);
                String getServerIP = NetTool.getServerIp(ipv4);
                if (getServerIP == null || "0.0.0.0".equals(getServerIP)){
                    hasNoticeServerIP = false;
                }else{
                    hasNoticeServerIP = true;
                    serverIP = getServerIP;
                    setEditTextText(serverIP);
                    // 创建连接
                    RCTLCore.getInstance().setServerIP(serverIP);
                    RCTLCore.getInstance().setServerPort(1399);
                    RCTLCore.getInstance().createServerConnection();
                }
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true){
                    try {
                        Thread.sleep(500);
                    }catch (Exception e){
                        setStatusViewText(e.getMessage());
                    }
                    if (updateText){
                        try {
                            statusView.setText(statusViewText);
                        }catch (Exception e){
                            setStatusViewText(e.getMessage());
                            e.printStackTrace();
                        }
                        updateText = false;
                    }else{
                        byte[] readbuf = RCTLCore.getInstance().readData();
                        if (null != readbuf){
                            String msg = System.currentTimeMillis() + ":" + new String(readbuf);
                            statusView.setText(msg);
                        }
                    }

                    if (updateEditTextValue) {
                        if (null != ipEditText){
                            try {
                                ipEditText.setText(ipEditTextValue);
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                            updateEditTextValue = false;
                        }
                    }
                }
            }
        }).start();

        Button shutdownBtn = view.findViewById(R.id.shutdownBtn);
        Button cancelShutdownBtn = view.findViewById(R.id.cancelShutdownBtn);
        view.findViewById(R.id.button_first).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(FirstFragment.this)
                        .navigate(R.id.action_FirstFragment_to_SecondFragment);
            }
        });

        shutdownBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 发送一个关闭电脑的同步请求
//                new Thread(new Runnable() {
//                    @Override
//                    public void run() {
//                        try {
//                            OkHttpClient client = new OkHttpClient();
//                            int port = 9999;
//                            String url = "http://192.168.200.107" +":"+ port + "?id=2";
//                            System.out.println("请求IP : " + url);
//                            Request request = new Request.Builder()
//                                    .url(url)
//                                    .get()
//                                    .build();
//                            Call call = client.newCall(request);
//                            Response response = call.execute();
//                            if (response.isSuccessful()) {
////                                statusView.setText(response.body().string());
//                                System.out.printf("Response %s%n", response.body().string());
//                                statusView.setText("请求成功");
//                            }else{
//                                System.out.println("请求失败");
//                            }
//                        }catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }).start();
                // 发送一个异步请求
                OkHttpClient client = new OkHttpClient();
                String ip = ipEditText.getText().toString();
                String url = "http://"+ ip + ":9999/cmd?id=3";
                System.out.println("请求ip "+url);
                setStatusViewText("请求ip "+url);
                RCTLCore.getInstance().sendData((getStatusViewText().getBytes()));
                Request request = new Request.Builder().url(url).build();
                Call call = client.newCall(request);
                call.enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        setStatusViewText(e.getMessage());
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        String body = response.body().string();
                        try {
                            ResponseStatus responseStatus = JSON.parseObject(body, ResponseStatus.class);
                            setStatusViewText(responseStatus.message);
                            updateText = true;
                        }catch (Exception e){
                            setStatusViewText(e.getMessage());
                            e.printStackTrace();
                        }
                    }
                });
            }
        });

        String ip = NetTool.geLocalWifiAddress(view);
        setStatusViewText("WIFI连接IP:"+ip);
        if ("0.0.0.0".equals(ip)){
            ip = NetTool.getLocal4GAddress();
        }
        ((EditText)view.findViewById(R.id.localIPEditText)).setText(ip);
        cancelShutdownBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OkHttpClient client = new OkHttpClient();
                String ip = ipEditText.getText().toString();
                String url = "http://"+ip+":9999/cmd?id=2";
                System.out.println("请求ip "+url);
                setStatusViewText("请求ip "+url);
                Request request = new Request.Builder().url(url).build();

                Call call = client.newCall(request);
                call.enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        setStatusViewText(e.getMessage());
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        String body = response.body().string();
                        try {
                            ResponseStatus responseStatus = JSON.parseObject(body, ResponseStatus.class);
                            setStatusViewText(responseStatus.message);
                        }catch (Exception e){
                            setStatusViewText(e.getMessage());
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }
}