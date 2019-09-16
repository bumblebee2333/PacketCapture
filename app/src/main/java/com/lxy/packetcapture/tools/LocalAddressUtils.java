package com.lxy.packetcapture.tools;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.widget.Toast;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * created by 李昕怡 on 2019/8/27
 * ps:根据是wif连接还是连接的是网络数据 来得到本机地址
 */

public class LocalAddressUtils {
    public static final String TAG = "LoaclAddressUtils";

    public static String getIPAddress(Context context){
        NetworkInfo info = ((ConnectivityManager)context.
                getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if(info != null && info.isConnected()){
            //若当前网络使用2G/3G/4G网络
            if(info.getType() == ConnectivityManager.TYPE_MOBILE){
                try {
                    for(Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();en.hasMoreElements();){
                        NetworkInterface intf = en.nextElement();
                        for(Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses();enumIpAddr.hasMoreElements();){
                            InetAddress inetAddress = enumIpAddr.nextElement();
                            if(!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address){
                                Log.d(TAG,"手机连接移动网络："+inetAddress.getHostAddress());
                                return inetAddress.getHostAddress();
                            }
                        }
                    }
                } catch (SocketException e) {
                    e.printStackTrace();
                }
            }else if(info.getType() == ConnectivityManager.TYPE_WIFI){//若使用无线网络
                WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                String ipAddress = intIP2StringIP(wifiInfo.getIpAddress());//获取IPv4地址
                Log.d(TAG,"手机连接无线网络："+ipAddress);
                return ipAddress;
            }
        }else {
            Toast.makeText(context,"当前无网络连接，请在设置中打开链接",Toast.LENGTH_SHORT).show();
        }
        return null;
    }


    public static String intIP2StringIP(int ip){
        return (ip & 0xFF) + "."
                + ((ip >> 8) & 0xFF) + "."
                + ((ip >> 16) & 0xFF) + "."
                + ((ip >> 24) & 0xFF);
    }
}
