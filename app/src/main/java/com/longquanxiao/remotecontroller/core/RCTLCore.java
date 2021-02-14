package com.longquanxiao.remotecontroller.core;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;



class RCTLConnetion {
    private RCTLConnetion instance;
    private Socket socket;
    private boolean active;
    private Thread readThread = null;
    private OutputStream outputStream = null;
    private InputStream inputStream = null;
    private Thread writeThread = null;
    private Queue<byte[]> readQueue = null;
    private Queue<byte[]> writeQueue = null;

    public RCTLConnetion(){}
    public RCTLConnetion(Socket socket) throws Exception {
        this.socket = socket;
        this.active = true;
        this.outputStream = this.socket.getOutputStream();
        this.inputStream = this.socket.getInputStream();
        this.readQueue = new LinkedBlockingQueue<>(125);
        this.readThread = new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] buffer = new byte[1024];
                while(active) {
                    try{
                         int ret = inputStream.read(buffer);
                         if (-1 == ret) {
                         }else if (ret > 0) {
//                             System.out.println("thread read data size = " + ret);
//                             System.out.println("thread read data "+ new String(buffer,0, ret));
                             byte[] e1 = new byte[ret];
                             for (int i = 0; i < ret; i++) {
                                 e1[i] = buffer[i];
                             }
                             try{
                                 readQueue.add(e1);
                             }catch (IllegalStateException e){
                                 readQueue.poll();
                                 e.printStackTrace();
                             }
                         }
                        Thread.sleep(200);
                    }catch (IOException | InterruptedException e){
                        e.printStackTrace();
                        active = false;
                    }
                }
                RCTLCore.getInstance().destroyRCTLConnection(instance);
            }
        });
        this.readThread.start();
        this.writeQueue = new LinkedBlockingQueue<>(125);
        this.writeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(active) {
                    try {
                        byte[] sendbuffer = writeQueue.poll();
                        if (null == sendbuffer || sendbuffer.length <= 0) {
                            sendbuffer = "CLIENT SAY HELLO WORLD".getBytes();
                        }
                        outputStream.write(sendbuffer);
//                        System.out.println("thread write data size "+sendbuffer.length);
                        Thread.sleep(500);
                    }catch (Exception e){
                        e.printStackTrace();
                        active = false;
                    }
                }
            }
        });
        this.writeThread.start();
        this.instance = this;
    }

    public byte[] readDate() {
        if (null != this.readQueue){
            return this.readQueue.poll();
        }
        return null;
    }

    public boolean writeData(byte[] data) {
        if (null != this.writeQueue){
            try {
                return this.writeQueue.add(data);
            }catch (IllegalArgumentException e) {
                this.writeQueue.poll();
                e.printStackTrace();
                return false;
            }catch (Exception e){
                return false;
            }
        }
        return false;
    }

    public void closeSocket(){
        if (null != this.socket){
            try {
                socket.close();
                active = false;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
/**
 * 核心类,单例
 */
public class RCTLCore {
    private static RCTLCore RCTLCore = null;
    private boolean start = false;
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
    // 销毁
    public void destroyRCTLConnection(RCTLConnetion connetion) {
           connetionList.remove(connetion);
           connetion.closeSocket();
    }
    /*
    * 初始化,完成如下功能
    * 1、登录平台
    * 2、与平台建立连接,维持心跳
    * 3、创建收发消息的队列
    */
    private void init() {
        // 建立连接,建立成功之后为这个连接创建读写线程
        connetionList = new LinkedList<>();
        String host = "192.168.200.115";
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

    public boolean sendData(byte[] data){
        for (RCTLConnetion conn :
                connetionList) {
            return conn.writeData(data);
        }
        return false;
    }
    public byte[] readData() {
        for (RCTLConnetion conn : this.connetionList) {
            return conn.readDate();
        }
        return null;
    }
}
