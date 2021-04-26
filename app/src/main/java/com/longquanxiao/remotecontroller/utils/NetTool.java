package com.longquanxiao.remotecontroller.utils;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.view.View;
//import org.apache.http.conn.util.InetAddressUtils;

import com.longquanxiao.remotecontroller.core.RCTLCore;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;

import static android.content.ContentValues.TAG;

/**
 * 网络相关工具,包括嗅探局域网内的数据
 */
public class NetTool {
    public NetTool() {

    }

    /**
     * 获得服务器的IP地址,跟据本机的IP地址去嗅探服务器的地址
     * @param localIpv4
     * @return
     */
    public static String getServerIp(String localIpv4) {
        if (null == localIpv4 || "0.0.0.0".equals(localIpv4)) {
            return null;
        }
        String ipPrefix = localIpv4.substring(0, localIpv4.lastIndexOf('.') + 1);
        System.out.println("IP Prefix :"+ ipPrefix);
        int serverPort = 1400;
        // 使用UDP向每个端口发个UDP包,然后查看那个端口返回数据
        try{
            for (int i = 1; i <= 255; i++) {
                String findServerIP = ipPrefix + i;
                if (!findServerIP.equals(localIpv4)){
                    try {
                        Log.d(TAG, "getServerIp: check ip " + findServerIP);
                        if (checkServerIp(findServerIP)) {
                            return findServerIP;
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] getNoticeServerCMDRequestData() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        // 请求长度: 4(length) + ("123")
        String requestData = "123";
        byte[] dataLengthBytes = DataFormatUtil.uint32ToBytesBigEnd(4 + 4 + requestData.getBytes().length);
        try{
            outputStream.write(dataLengthBytes);
            byte[] type = DataFormatUtil.uint32ToBytesBigEnd(1);
            outputStream.write(type);
            outputStream.write(requestData.getBytes(), 0, requestData.getBytes().length);
            return outputStream.toByteArray();
        }catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean checkNoticeServerCMDResponse(byte[] data) {
        if (null == data) {
            return false;
        }
        String expectResponse = "321";
        String response = new String(data);
        return expectResponse.equals(response);
    }

    public static boolean checkServerIp(String ip) throws Exception {
        int serverPort = 1400;
        DatagramSocket udpSocket = new DatagramSocket();
        udpSocket.setSoTimeout(1000);
        byte[] requestData = getNoticeServerCMDRequestData();
        if (null == requestData) {
            return false;
        }
        udpSocket.send(new DatagramPacket(requestData, requestData.length, InetAddress.getByName(ip), serverPort));
        byte[] buf = new byte[1024];
        DatagramPacket receivePacket = new DatagramPacket(buf, buf.length);
        udpSocket.receive(receivePacket);
        if (ip.equals(receivePacket.getAddress().getHostAddress())) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byteArrayOutputStream.write(receivePacket.getData(),4, receivePacket.getLength() - 4);
            return NetTool.checkNoticeServerCMDResponse(byteArrayOutputStream.toByteArray());
        }else{
            Log.d(TAG, "checkServerIp: UNKNOWN response package "+ receivePacket.getAddress().getHostAddress());
        }
        return false;
    }


    public static String geLocalWifiAddress(View view) {
        String ipv4 = "";
        WifiManager wifiManager = (WifiManager)view.getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        // 检查并开启wifi
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipAddress = wifiInfo.getIpAddress();
        ipv4 = (ipAddress & 0xFF) + "." + ((ipAddress >> 8) & 0xFF) + "." + ((ipAddress >> 16) & 0xFF) + "." + ((ipAddress >> 24) & 0xFF);
        return ipv4;
    }
    public static String getLocal4GAddress() {
        String ip = "";
        try {
            ArrayList<NetworkInterface> networkInterfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface n : networkInterfaces) {
                ArrayList<InetAddress> inetAddress = Collections.list(n.getInetAddresses());
                for (InetAddress address : inetAddress) {
                    if (!address.isLoopbackAddress() && !address.isLinkLocalAddress()) {
                        ip = address.getHostAddress();
                        return ip;
                    }
                }
            }
        }catch (Exception e){
            return null;
        }
        return ip;
    }
}
