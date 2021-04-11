package com.longquanxiao.remotecontroller.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class RCTLConnection {
    private RCTLConnection instance;
    private Socket socket;
    private boolean active;
    private Thread readThread = null;
    private OutputStream outputStream = null;
    private InputStream inputStream = null;
    private Thread writeThread = null;
    private Queue<byte[]> readQueue = null;
    private Queue<byte[]> writeQueue = null;

    public RCTLConnection(){}
    public RCTLConnection(Socket socket) throws Exception {
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
