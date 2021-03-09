package com.longquanxiao.remotecontroller.utils;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.view.View;
//import org.apache.http.conn.util.InetAddressUtils;

import com.longquanxiao.remotecontroller.core.RCTLCore;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
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
            DatagramSocket udpSocket = new DatagramSocket();
            for (int i = 1; i <= 255; i++) {
                String findServerIP = ipPrefix + i;
                if (!findServerIP.equals(localIpv4)){
                    try {
                        udpSocket.send(new DatagramPacket("123".getBytes(), "123".length(), InetAddress.getByName(findServerIP), serverPort));
                        System.out.println("探测:"+ findServerIP);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
            // 接收数据
            for(int i = 0;i < 255;i ++) {
                byte[] recieveData = new byte[1024];
                System.out.println("等待响应");
                DatagramPacket receivePacket = new DatagramPacket(recieveData, recieveData.length);
                udpSocket.receive(receivePacket);
                String receiveStr =  new String(recieveData).substring(0, receivePacket.getLength());
                System.out.println("收到响应:"+ receiveStr);
                // 收到数据,读取出
                if ("321".equals(receiveStr)) {
                    // 是服务器响应,取出这个packet的IP和PORT
                    String peerIp = receivePacket.getAddress().getHostAddress();
                    System.out.println("探测到服务器IP:" + peerIp);
                    return peerIp;
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
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
