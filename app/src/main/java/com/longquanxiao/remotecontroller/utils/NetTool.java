package com.longquanxiao.remotecontroller.utils;

import android.content.Context;
import android.net.InetAddresses;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.view.View;
//import org.apache.http.conn.util.InetAddressUtils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;

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
        final int[] threadNum = {0};
        final String[] serverIP = {""};
        String ipPrefix = localIpv4.substring(0, localIpv4.lastIndexOf('.') + 1);
        System.out.println("IP Prefix :"+ ipPrefix);
        int serverPort = 1399;
        final boolean[] noticeServer = {false};
        for (int i = 1; i <= 255; i++) {
            if (noticeServer[0]){
                break;
            }
            String finalServerIP = ipPrefix + i;
            if (threadNum[0] > 20){
                i --;
                try{
                    Thread.sleep(300);
                }catch (Exception e){
                    e.printStackTrace();
                }
                continue;
            }
            if (!finalServerIP.equals(localIpv4)){
                // 尝试建立连接,发送Hello Server,接收到Hello Client算作成功找到对应的IP地址
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (noticeServer[0]){
                            return;
                        }
                        threadNum[0]++;
                        try {
                            System.out.println("Try Connect "+ finalServerIP + ":"+serverPort);
                            Socket socket = new Socket(finalServerIP, serverPort);
                            byte[] buffer = new byte[1024];
                            int readn = socket.getInputStream().read(buffer);
                            if (readn > 0) {
                                byte[] readvalue = new byte[readn];
                                System.arraycopy(buffer,0, readvalue,0, readn);
                                if ("SERVER SAY HELLO".equals(new String(readvalue))){
                                    System.out.println("getServerIp find ServerIP "+ finalServerIP);
                                    serverIP[0] = finalServerIP;
                                    noticeServer[0] = true;
                                }
                            }
                            if (socket != null) {
                                socket.close();
                            }
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                        threadNum[0] --;
                    }
                }).start();
            }
        }
        return serverIP[0];
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
