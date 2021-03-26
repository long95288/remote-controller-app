package com.longquanxiao.remotecontroller.manager;

import com.longquanxiao.remotecontroller.core.RCTLCore;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 解码H264流
 */
public class DecodeH264Stream {

//    private RandomAccessFile rf;
    private BufferedInputStream inputStream;

    private static final int dataBufferMaxSize = 10 * 1024 * 1024;

    private byte[] dataBuffer = new byte[dataBufferMaxSize];

    private int dataBufferSize = 0;

    public volatile boolean exitDecoder = false;

    Lock lock = new ReentrantLock();

    Lock naluListLock = new ReentrantLock();
    List<byte[]> naluList = new LinkedList<>();

    //当前读到的帧位置
    private int curIndex;

    private StringBuilder builder = new StringBuilder();

    private String[] SLICE;

    private static DecodeH264Stream instance;

    private boolean isStartCode4;

    public static DecodeH264Stream getInstance() {
        if (instance == null) {
            instance = new DecodeH264Stream();
        }
        return instance;
    }

    public DecodeH264Stream() {
        init();
    }

    private boolean append2Buffer(byte[] data, int size) {
        lock.lock();
        if (size + dataBufferSize > dataBufferMaxSize) {
            this.dataBufferSize = 0;
            lock.unlock();
            return false;
        }
        System.arraycopy(data,0, this.dataBuffer, this.dataBufferSize, size );
        this.dataBufferSize += size;
        lock.unlock();
        return true;
    }

    private byte[] readFromBufferNotRemote(int size) {
        lock.lock();
        byte[] ret = new byte[size];
        if (size > dataBufferSize) {
            lock.unlock();
            return null;
        }
        System.arraycopy(this.dataBuffer, 0, ret, 0, size);
        lock.unlock();
        return ret;
    }

    private byte[] readFromBufferAndRemove(int size){
        lock.lock();
        byte[] ret = new byte[size];
        if (size > dataBufferSize) {
            lock.unlock();
            return null;
        }
        System.arraycopy(this.dataBuffer, 0, ret, 0, size);
        System.arraycopy(this.dataBuffer, size, this.dataBuffer, 0, this.dataBufferSize - size);
        this.dataBufferSize -= size;
        lock.unlock();
        return ret;
    }

    public void init() {
        initInputStream();
    }

    private void initInputStream() {
        new Thread(() -> {
            try {
                String ip = RCTLCore.getInstance().getServerIP();
                System.out.println("开启数据接收线程");
                ip = "192.168.0.2";
                int port = 1402;
                Socket socket = new Socket(ip, port);
                inputStream = new BufferedInputStream(socket.getInputStream());
                int n = 0;
                byte[] buf = new byte[1024];
                while((n =inputStream.read(buf)) != -1 && !exitDecoder) {
                    // System.out.printf("读取到数据 %d\n byte", n);
                    append2Buffer(buf, n);
                    Thread.sleep(20);
                }
                inputStream.close();
                socket.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("结束连接收线程");
            exitDecoder = true;
        }).start();

        // 切割NALU线程
        /*
        new Thread(() -> {
            while(!exitDecoder) {
                try {
                    int startCode = -1;
                    byte[] startCodeBuf = readFromBufferNotRemote(4);
                    if (null == startCodeBuf || startCodeBuf.length == 0){
                        Thread.sleep(20);
                        continue;
                    }else{
                        // 先算出startCode
                        startCodeBuf = readFromBufferNotRemote(4);
                        if (findStartCode4(startCodeBuf, 0)) {
                            System.out.println("StartCode 4");
                            startCode = 4;
                        }else if (findStartCode3(startCodeBuf, 0)){
                            System.out.println("StartCode 3");
                            startCode = 3;
                        }else{
                            readFromBufferAndRemove(1);
                            continue;
                        }

                        // 先删除掉第一个startCodec
                        readFromBufferAndRemove(startCode);
                        lock.lock();
                        int bufSize = this.dataBufferSize;
                        lock.unlock();
                        byte[] next = readFromBufferNotRemote(bufSize);
                        int nextStartCodeIndex = findNALUSplitIndex(next, startCode);
                        if ( -1 == nextStartCodeIndex) {
                            // System.out.println("没有找到NALU");
                            Thread.sleep(20);
                            continue;
                        }else{
                            // 找出下个startCode的索引
                            // System.out.println("找到NALU");
                            System.out.println("下一个StartCode " + nextStartCodeIndex);
                            byte[] nalu = readFromBufferAndRemove(nextStartCodeIndex);
                            naluListLock.lock();
                            naluList.add(nalu);
                            naluListLock.unlock();
                        }
                    }
                }catch (Exception e) {
                    e.printStackTrace();
                }
            }
            System.out.println("结束解析线程");
        }).start();
         */
    }

    public byte[] readData(int size) {
        byte[] buf = readFromBufferAndRemove(size);
        while (null == buf) {
            try{
                Thread.sleep(30);
            }catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }
        System.out.println("返回数据....");
        return buf;
    }
    /**
     * 读取每一帧数据
     *
     * @param buffer
     * @return
     */
    public int readSampleData(ByteBuffer buffer) {
        byte[] nal = getNALU();
        if (null == nal) {
            return -1;
        }
        buffer.put(nal);
        return nal.length;
    }

    // 找到第一个NALU的分割点
    private int findNALUSplitIndex(byte[] data, int startCodeSize) {
        byte[] startCodeBuf = new byte[startCodeSize];
        startCodeBuf[startCodeSize - 1] = 1;
        byte[] windows = new byte[startCodeSize];
        for (int i = 0; i < startCodeSize; i++) {
            windows[i] = data[i];
        }
        for(int i = startCodeSize; i < data.length;i ++) {
            boolean foundStartCode = true;
            for (int j = 0; j < startCodeSize; j++) {
                if (windows[j] != startCodeBuf[j]) {
                    foundStartCode = false;
                    break;
                }
            }
            if (!foundStartCode) {
                // 滑动窗口
                System.arraycopy(windows, 1, windows, 0, startCodeSize - 1);
                windows[startCodeSize - 1] = data[i];
            }else{
                // 找到了 0 0 0 1 x
                return i - startCodeSize;
            }
        }
        return -1;
    }

    // getNaul
    public byte[] getNALU() {
        while(!exitDecoder) {
            naluListLock.lock();
            if (naluList.isEmpty()) {
                naluListLock.unlock();
                try {
                    // System.out.println("NALU 队列为空");
                    Thread.sleep(20);
                }catch (Exception e){
                    e.printStackTrace();
                }
                continue;
            }
            byte[] nalu = naluList.remove(0);
            System.out.println("取到NALU");
            naluListLock.unlock();
            return nalu;
        }
        return null;
    }

    //find match "00 00 00 01"
    private boolean findStartCode4(byte[] bb, int offSet) {
        if (offSet < 0) {
            return false;
        }
        if (bb[offSet] == 0 && bb[offSet + 1] == 0 && bb[offSet + 2] == 0 && bb[offSet + 3] == 1) {
            return true;
        }
        return false;
    }

    //find match "00 00 01"
    private boolean findStartCode3(byte[] bb, int offSet) {
        if (offSet <= 0) {
            return false;
        }
        if (bb[offSet] == 0 && bb[offSet + 1] == 0 && bb[offSet + 2] == 1) {
            return true;
        }
        return false;
    }

    public void close() {
        exitDecoder = true;
        instance = null;
    }


}
