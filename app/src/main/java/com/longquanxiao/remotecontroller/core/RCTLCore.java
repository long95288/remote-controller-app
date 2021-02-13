package com.longquanxiao.remotecontroller.core;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import static android.content.ContentValues.TAG;


class RCTLConnetion {
    private Socket socket;
    private boolean active;
    private Thread readThread = null;
    private OutputStream outputStream = null;
    private InputStream inputStream = null;
    private Thread writeThread = null;

    public RCTLConnetion(){}
    public RCTLConnetion(Socket socket) throws Exception {
        this.socket = socket;
        this.active = true;
        this.outputStream = this.socket.getOutputStream();
        this.inputStream = this.socket.getInputStream();
        this.readThread = new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] buffer = new byte[1024];
                while(active) {
                    try{
                         int ret = inputStream.read(buffer);

                    }catch (IOException e){
                        e.printStackTrace();
                        active = false;
                    }
                }
            }
        });
        this.readThread.start();

        this.writeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(active) {
                    try {

                        Thread.sleep(200);
                    }catch (Exception e){
                        active = false;
                    }
                }
            }
        });
        this.writeThread.start();

    }

}
/**
 * 核心类,单例
 */
public class RCTLCore {
    private static RCTLCore RCTLCore = null;
    private boolean start = false;
    private Queue<byte[]> readQueue = null;
    private Thread readThread = null;

    private Queue<byte[]> writeQueue = null;
    private Thread writeThread = null;
    private List<RCTLConnetion> connetionList = null;
    private RCTLCore() {
        init();
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

    /*
    * 初始化,完成如下功能
    * 1、登录平台
    * 2、与平台建立连接,维持心跳
    * 3、创建收发消息的队列
    */
    private void init() {
        readQueue = new ArrayBlockingQueue<byte[]>(250);
        writeQueue = new ArrayBlockingQueue<byte[]>(250);
        // 建立连接,建立成功之后为这个连接创建读写线程
        connetionList = new LinkedList<>();

        String host = "192.168.200.107";
        int port = 1399;

        try{
            // 建立socket
            Socket socket = new Socket(host, port);
            // 建立成功之后需要开启新的线程
            RCTLConnetion connection = new RCTLConnetion(socket);
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

    public int sendData(byte[] data){
        Log.d(TAG, "sendData: size = " + data.length);
        if (null != this.writeQueue && this.writeQueue.add(data)) {
            return 0;
        }
        return -1;
    }
    public int sendCMD(byte[] data){
        System.out.println("send CMD size = "+ data.length);
        if (null != this.writeQueue && this.writeQueue.add(data)) {
            return 0;
        }
        return -1;
    }
    public int receiveData(){
        return 0;
    }
}
