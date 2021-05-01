package com.longquanxiao.remotecontroller;

import com.longquanxiao.remotecontroller.protcol.TLVData;

import org.junit.Test;

public class TLVDataTest {

    @Test
    public void testConv() {
        TLVData tlvData1 = new TLVData(TLVData.PEER_CMD_MOUSE_CTL, "HELLOWORLD".length(),"HELLOWORLD".getBytes());
        byte[] data = TLVData.encodeTLVDataByObject(tlvData1);
        TLVData tlvData2 = TLVData.decodeTLVDataByBytes(data);
        System.out.printf("%d %d %s", tlvData2.getType(), tlvData2.getLength(), new String(tlvData2.getValue()));
    }
}
