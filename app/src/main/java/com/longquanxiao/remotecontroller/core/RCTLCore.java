package com.longquanxiao.remotecontroller.core;


import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.longquanxiao.remotecontroller.cmd.RemoteControlCMD;
import com.longquanxiao.remotecontroller.utils.FileUploadThreadCallBackInterface;
import com.longquanxiao.remotecontroller.utils.FileUploadThread;
import com.longquanxiao.remotecontroller.utils.SendMsgCallbackInterface;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.longquanxiao.remotecontroller.utils.FileUploadThread.UPLOAD_FAILED;

/**
 * 核心类,单例
 */
public class RCTLCore {
    private static RCTLCore RCTLCore = null;

    public static final int RCTLCORE_STATUS_RUNNING = 1;
    public static final int RCTLCORE_STATUS_STOP = 2;

    private int coreStatus = RCTLCORE_STATUS_STOP;

    private boolean start = false;
    private String serverIP = null;
    private int serverPort = 1399;
    private List<RCTLConnection> connetionList = null;
    private Handler uiHandler = null;

    private List<FileUploadThread> uploadFileThreadList = null;
    private Map<Integer, Integer> uploadFileStatusMap = null;

    FileUploadThreadCallBackInterface fileUploadThreadCallBack = null;

    private RCTLCore() {
        connetionList = new LinkedList<>();
        uploadFileThreadList = new LinkedList<>();
        uploadFileStatusMap = new HashMap<>();
        // init();
    }
    private void insertMessage(String s) {
        if (null != this.uiHandler) {
            Message msg = new Message();
            msg.what = 1;
            msg.obj = s;
            this.uiHandler.sendMessage(msg);
        }
    }
    private void insertMessegeTest(String s) {
        if (null != this.uiHandler){
            Message msg = new Message();
            msg.what = 1;
            msg.obj = s;
            Bundle bundle = new Bundle();
            bundle.putString("bundledata", "Hello World");
            msg.setData(bundle);
            this.uiHandler.sendMessage(msg);
        }
    }

    public static RCTLCore getInstance() {
        if (null == RCTLCore) {
            synchronized (RCTLCore.class) {
                if (null == RCTLCore){
                    RCTLCore = new RCTLCore();
                }
            }
        }
        return RCTLCore;
    }
    // 销毁
    public void destroyRCTLConnection(RCTLConnection connetion) {
           connetionList.remove(connetion);
           connetion.closeSocket();
    }

    public String getServerIP() {
        return serverIP;
    }

    public void setServerIP(String serverIP) {
        insertMessage("Core设置IP:"+serverIP);
        coreStatus = RCTLCORE_STATUS_RUNNING;
        this.serverIP = serverIP;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    /**
     * 获得文件上传的URL
     * @return url
     */
    @SuppressLint("DefaultLocale")
    public String getFileUploadURL() {
        if (coreStatus == RCTLCORE_STATUS_RUNNING) {
            String fileUploadServiceURLFormat = "http://%s:%d/file";
            return String.format(fileUploadServiceURLFormat, this.serverIP, this.serverPort);
        }
        return null;
    }

    /*
    * 初始化,完成如下功能
    * 1、登录平台
    * 2、与平台建立连接,维持心跳
    * 3、创建收发消息的队列
    */
    public void createServerConnection() {
        // 建立连接,建立成功之后为这个连接创建读写线程
        try{
            if (connetionList.size() > 0){
                return;
            }
            // 建立socket
            Socket socket = new Socket(this.serverIP, this.serverPort);
            // 建立成功之后需要开启新的线程
            RCTLConnection connection = new RCTLConnection(socket);
            connetionList.add(connection);
            this.start = true;
        }catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean sendData(byte[] data){
        for (RCTLConnection conn :
                connetionList) {
            return conn.writeData(data);
        }
        return false;
    }
    public byte[] readData() {
        for (RCTLConnection conn : this.connetionList) {
            return conn.readDate();
        }
        return null;
    }

    public void setUiHandler(Handler uiHandler) {
        this.uiHandler = uiHandler;
    }

    public int getRCTLCoreStatus(){
        return this.coreStatus;
    }

    public void addFileUploadTask(int taskId, String filepath, FileUploadThreadCallBackInterface fileUploadThreadCallBack) {
        FileUploadThread task = new FileUploadThread(filepath, taskId,fileUploadThreadCallBack);
        if(null == task && null != fileUploadThreadCallBack) {
            fileUploadThreadCallBack.reportStatus(taskId, UPLOAD_FAILED, "Create new FileUploadThread failed.");
            return;
        }
        task.start();
    }

    public void sendMsg(String msg, SendMsgCallbackInterface callback) {
        // 发送HTTP请求
        new Thread(() -> {
            try {
                if (RemoteControlCMD.sendMsg(msg)) {
                    // 处理回调
                    if (null != callback) {
                        callback.sendMsgStatusCallBack(1, "Send Success");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (null != callback) {
                    callback.sendMsgStatusCallBack(2, e.getMessage());
                }
            }
        }).start();
    }
}
