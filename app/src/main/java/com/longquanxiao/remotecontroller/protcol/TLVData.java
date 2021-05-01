package com.longquanxiao.remotecontroller.protcol;

import com.longquanxiao.remotecontroller.utils.DataFormatUtil;

import java.io.ByteArrayOutputStream;

/**
 * Length TYPE VALUE PROTO
 */
public class TLVData {
    public static final int PEER_CMD_NOTICE_SERVER = 1;
    public static final int PEER_CMD_MOUSE_CTL     = 2 ;

    int type;
    int length;
    byte[] value;

    public TLVData() {
    }
    public TLVData(int type,int length,  byte[] value) {
        this.length = length;
        this.type = type;
        this.value = value;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public byte[] getValue() {
        return value;
    }

    public void setValue(byte[] value) {
        this.value = value;
    }



    public static byte[] encodeTLVDataByObject(TLVData data) {
        // T + L + V
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try{
            os.write(DataFormatUtil.uint32ToBytesBigEnd(data.type));
            os.write(DataFormatUtil.uint32ToBytesBigEnd(data.length));
            os.write(data.value);
            return os.toByteArray();
        }catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static TLVData decodeTLVDataByBytes(byte[] data) {
        TLVData tlvData = new TLVData();
        int type = DataFormatUtil.bytesToUint32BigEnd(data);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        os.write(data, 4, 4);
        int length = DataFormatUtil.bytesToUint32BigEnd(os.toByteArray());
        os.reset();

        os.write(data, 8, data.length - 8);
        tlvData.setType(type);
        tlvData.setLength(length);
        tlvData.setValue(os.toByteArray());

        return tlvData;
    }

}
