package com.longquanxiao.remotecontroller.utils;

import android.content.Context;
import android.net.InetAddresses;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.view.View;
//import org.apache.http.conn.util.InetAddressUtils;

import java.net.InetAddress;
import java.net.NetworkInterface;
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
