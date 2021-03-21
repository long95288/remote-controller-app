package com.longquanxiao.remotecontroller.manager;

import com.longquanxiao.remotecontroller.core.RCTLCore;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * 解码H264流
 */
public class DecodeH264Stream {

//    private RandomAccessFile rf;
    private BufferedInputStream inputStream;

    private static final int dataBufferMaxSize = 100000;
    private byte[] dataBuffer = new byte[dataBufferMaxSize];
    private int dataBufferSize = 0;

    //当前读到的帧位置
    private int curIndex;

    private StringBuilder builder = new StringBuilder();

    private String[] SLICE;

    private List<Byte> byteList = new ArrayList();

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
        if (size + dataBufferSize > dataBufferMaxSize) {
            this.dataBufferSize = 0;
            return false;
        }
        System.arraycopy(data,0, this.dataBuffer, this.dataBufferSize, size );
        this.dataBufferSize += size;
        return true;
    }

    private byte[] readFromBufferNotRemote(int size) {
        byte[] ret = new byte[size];
        if (size > dataBufferSize) {
            return null;
        }
        System.arraycopy(this.dataBuffer, 0, ret, 0, size);
        return ret;
    }

    private byte[] readFromBufferAndRemove(int size){
        byte[] ret = new byte[size];
        if (size > dataBufferSize) {
            return null;
        }
        System.arraycopy(this.dataBuffer, 0, ret, 0, size);
        System.arraycopy(this.dataBuffer, size, this.dataBuffer, 0, this.dataBufferSize - size);
        this.dataBufferSize -= size;
        return ret;
    }

    public void init() {
        initInputStream();
    }

    private void initInputStream() {
        try {
            String ip = RCTLCore.getInstance().getServerIP();
            ip = "192.168.0.2";
            int port = 1402;
            Socket socket = new Socket(ip, port);
            inputStream = new BufferedInputStream(socket.getInputStream());
            System.out.println("建立连接成功");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 读取每一帧数据
     *
     * @param buffer
     * @return
     */
    public int readSampleData(ByteBuffer buffer) {
        byte[] nal = getNALU();
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
    private byte[] getNALU() {
        //
        boolean readed = false;
        int startCode = -1;
        try {
            while(!readed) {
                byte[] startCodeBuf = readFromBufferNotRemote(4);
                byte[] buffer = new byte[10000];
                if (null == startCodeBuf || startCodeBuf.length == 0){
                    int readsize = inputStream.read(buffer);
                    if ((readsize <= 0) || !append2Buffer(buffer, readsize)) {
                        System.out.println("添加数据失败");
                        return null;
                    }else {
                        System.out.println("读取数据成功"+ readsize);
                    }
                    // 先算出startCode
                    startCodeBuf = readFromBufferNotRemote(4);
                    if (findStartCode4(startCodeBuf, 0)) {
                        System.out.println("StartCode 4");
                        startCode = 4;
                    }else if (findStartCode3(startCodeBuf, 0)){
                        System.out.println("StartCode 3");
                        startCode = 3;
                    }else{
                        // 找不到??开始删除数据,找到为止
                        if(this.dataBufferSize < 4) {
                            System.out.println("找不到startcode");
                            return null;
                        }
                        readFromBufferAndRemove(1);
                        continue;
                    }

                    // 先删除掉第一个startCodec
                    readFromBufferAndRemove(startCode);
                    byte[] next = readFromBufferNotRemote(this.dataBufferSize);
                    int nextStartCodeIndex = findNALUSplitIndex(next, startCode);
                    System.out.println("下一个StartCode " + nextStartCodeIndex);
                    if ( -1 == nextStartCodeIndex) {
                        startCode = -1;
                        continue;
                    }else{
                        // 找出下个startCode的索引
                        System.out.println("找到NALU");
                        return readFromBufferAndRemove(nextStartCodeIndex);
                    }
                }
            }
        }catch (Exception e) {
            e.printStackTrace();
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
        instance = null;
    }


}
