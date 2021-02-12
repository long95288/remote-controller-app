package com.longquanxiao.remotecontroller.utils;

import android.content.Context;
import android.net.InetAddresses;
import android.net.wifi.WifiManager;
import android.view.View;
//import org.apache.http.conn.util.InetAddressUtils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * 网络相关工具,包括嗅探局域网内的数据
 */
public class NetTool {
    public NetTool() {

    }

    /**
     * 获得本机的IP地址
     * @return
     * @throws SocketException
     */
    public static String getLocalAddress() throws SocketException {
        String ipAddress = "";
        Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
        while (en.hasMoreElements()) {
            NetworkInterface networkInterface = en.nextElement();
            Enumeration<InetAddress> address =  networkInterface.getInetAddresses();
            while (address.hasMoreElements()){
                InetAddress ip = address.nextElement();
                System.out.printf("ip = %s\n", ip.getHostAddress());
//                if (!ip.isLoopbackAddress()){
//                    System.out.printf("ip = %s\n", ip.getHostAddress());
//                }
            }
        }
        return ipAddress;
    }



}
