package com.longquanxiao.remotecontroller.core;

import android.util.Log;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import static android.content.ContentValues.TAG;

/**
 * 核心类,单例
 */
public class RCTLCore {
    private static RCTLCore RCTLCore = null;
    private boolean start = false;
    private Queue<byte[]> readQueue = null;
    private Queue<byte[]> writeQueue = null;

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
    private void init(){
        readQueue = new ArrayBlockingQueue<byte[]>(250);
        writeQueue = new ArrayBlockingQueue<byte[]>(250);
        this.start = true;
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
