package com.longquanxiao.remotecontroller.core;


import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.longquanxiao.remotecontroller.cmd.RemoteControlCMD;
import com.longquanxiao.remotecontroller.protcol.TLVData;
import com.longquanxiao.remotecontroller.utils.DataFormatUtil;
import com.longquanxiao.remotecontroller.utils.FileUploadThreadCallBackInterface;
import com.longquanxiao.remotecontroller.utils.FileUploadThread;
import com.longquanxiao.remotecontroller.utils.SendMsgCallbackInterface;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import static android.content.ContentValues.TAG;
import static com.longquanxiao.remotecontroller.utils.FileUploadThread.UPLOAD_FAILED;

/**
 * 核心类,单例
 */
public class RCTLCore {

    private static final int MOUSE_CTL_TYPE_MOVE_ABSOLUTE      = 0;  // 指针移动
    private static final int MOUSE_CTL_TYPE_LEFT_SINGLE_CLICK  = 1;  // 左键单击
    private static final int MOUSE_CTL_TYPE_LEFT_DOUBLE_CLICK  = 2;  // 左键双击
    private static final int MOUSE_CTL_TYPE_RIGHT_SINGLE_CLICK = 3;  // 右键单击
    private static final int MOUSE_CTL_TYPE_MOVE_RELATIVE      = 4;  // 指针相对移动

    // EventType
    private static final int MOUSEEVENTF_MOVE       = 0x0001; /* mouse move */
    private static final int MOUSEEVENTF_LEFTDOWN   = 0x0002; /* left button down */
    private static final int MOUSEEVENTF_LEFTUP     = 0x0004; /* left button up */
    private static final int MOUSEEVENTF_RIGHTDOWN  = 0x0008; /* right button down */
    private static final int MOUSEEVENTF_RIGHTUP    = 0x0010; /* right button up */
    private static final int MOUSEEVENTF_MIDDLEDOWN = 0x0020; /* middle button down */
    private static final int MOUSEEVENTF_MIDDLEUP   = 0x0040; /* middle button up */
    private static final int MOUSEEVENTF_ABSOLUTE   = 0x8000; /* absolute move */
    
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

    DatagramSocket cmdUpdSocket = null;

    private Thread cmdUdpSocketThread = null;
    private static final int cmdUdpSocketThreadRunning = 1;
    private int cmdUdpSocketThreadStatus = 0;

    private List<TLVData> cmdList = null;


    private RCTLCore() {
        connetionList = new LinkedList<>();
        uploadFileThreadList = new LinkedList<>();
        uploadFileStatusMap = new HashMap<>();
        cmdList = new LinkedList<>();
        cmdUdpSocketThread = new Thread(() -> {
            int port = 1405;
            try{
                cmdUpdSocket = new DatagramSocket();
            }catch (Exception e){
                e.printStackTrace();
                return;
            }
            while (cmdUdpSocketThreadStatus == cmdUdpSocketThreadRunning) {
                try {
                    if (! cmdList.isEmpty()) {
                        TLVData data = cmdList.remove(0);
                        byte[] dataStream  = TLVData.encodeTLVDataByObject(data);
                        cmdUpdSocket.send(new DatagramPacket(dataStream, dataStream.length, InetAddress.getByName(serverIP), port));
                        Log.d(TAG, "RCTLCore: send CMD Data size: "+dataStream.length+" type:"+data.getType() + "length:"+ data.getLength());
                    }
                    // Thread.sleep(10);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
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

    public void sendMouseMoveRelativeCMD(int x, int y) {
        if (cmdUdpSocketThreadStatus != cmdUdpSocketThreadRunning) {
            cmdUdpSocketThread.start();
            cmdUdpSocketThreadStatus = cmdUdpSocketThreadRunning;
        }
        // 鼠标相对移动
        Log.d(TAG, "sendMouseMoveRelativeCMD: x = "+ x +"; y = " + y);
        try {
            // ByteArrayOutputStream value = new ByteArrayOutputStream();
            // value.write(DataFormatUtil.uint32ToBytesBigEnd(MOUSE_CTL_TYPE_MOVE_RELATIVE));
            // value.write(DataFormatUtil.uint32ToBytesBigEnd(x));
            // value.write(DataFormatUtil.uint32ToBytesBigEnd(y));

            @SuppressLint("DefaultLocale")
            byte[] data = String.format("%d:%d:%d", MOUSE_CTL_TYPE_MOVE_RELATIVE, -y, x).getBytes();
            Log.d(TAG, "sendMouseMoveRelativeCMD:  data " + new String(data));
            TLVData tlvData = new TLVData(TLVData.PEER_CMD_MOUSE_CTL, data.length, data);
            cmdList.add(tlvData);
        }catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void sendMouseLeftClickCMD(int x, int y) {
        // 左键单击
        if (cmdUdpSocketThreadStatus != cmdUdpSocketThreadRunning) {
            cmdUdpSocketThread.start();
            cmdUdpSocketThreadStatus = cmdUdpSocketThreadRunning;
        }
        try {
            @SuppressLint("DefaultLocale")
            byte[] data = String.format("%d:%d:%d", MOUSEEVENTF_LEFTDOWN | MOUSEEVENTF_LEFTUP, -y, x).getBytes();
            Log.d(TAG, "sendMouseMoveRelativeCMD:  data " + new String(data));
            TLVData tlvData = new TLVData(TLVData.PEER_CMD_MOUSE_CTL, data.length, data);
            cmdList.add(tlvData);
        }catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void sendMouseRigthClickCMD(int x, int y) {
        // 右键单击
        if (cmdUdpSocketThreadStatus != cmdUdpSocketThreadRunning) {
            cmdUdpSocketThread.start();
            cmdUdpSocketThreadStatus = cmdUdpSocketThreadRunning;
        }
        try {
            @SuppressLint("DefaultLocale")
            byte[] data = String.format("%d:%d:%d", MOUSEEVENTF_RIGHTDOWN | MOUSEEVENTF_RIGHTUP, -y, x).getBytes();
            Log.d(TAG, "sendMouseMoveRelativeCMD:  data " + new String(data));
            TLVData tlvData = new TLVData(TLVData.PEER_CMD_MOUSE_CTL, data.length, data);
            cmdList.add(tlvData);
        }catch (Exception e) {
            e.printStackTrace();
        }
    }
}
