package com.longquanxiao.remotecontroller;

import com.longquanxiao.remotecontroller.utils.DataFormatUtil;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DataFormatUtilTest {
    @Test
    public void bytes2Uint32BigEnd() {
        int length = 240;
        byte[] lengthBytes = DataFormatUtil.uint32ToBytesBigEnd(length);
        int convertLength = DataFormatUtil.bytesToUint32BigEnd(lengthBytes);
        assertEquals(length, convertLength);
    }
}
