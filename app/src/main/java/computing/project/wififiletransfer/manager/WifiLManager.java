package computing.project.wififiletransfer.manager;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class WifiLManager {

    private static final String TAG = "WifiLManager";

    /**
     * 获取连接的 Wifi 的网关地址
     *
     * @param context 上下文
     * @return IP地址
     */
    public static String getGatewayIpAddress(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiinfo = wifiManager == null ? null : wifiManager.getConnectionInfo();
        if (wifiinfo != null) {
            DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
            if (dhcpInfo != null) {
                int address = dhcpInfo.gateway;
                return ((address & 0xFF)
                        + "." + ((address >> 8) & 0xFF)
                        + "." + ((address >> 16) & 0xFF)
                        + "." + ((address >> 24) & 0xFF));
            }
        }
        return "";
    }

    /**
     * 获取连接 WiFi 后设备自身的 IP 地址（作为客户端、或作为热点提供者）
     *
     * @param context 上下文
     * @return IP地址
     */
    public static String getLocalIpAddress(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager == null ? null : wifiManager.getConnectionInfo();
        int address = 0;
        if (wifiInfo != null)
            address = wifiInfo.getIpAddress();
        if (address > 0)
            return ((address & 0xFF)
                    + "." + ((address >> 8) & 0xFF)
                    + "." + ((address >> 16) & 0xFF)
                    + "." + ((address >> 24) & 0xFF));

        // 作为热点时，通过网络接口查询本机 IP
        try {
            for (Enumeration<NetworkInterface> enumNetIF = NetworkInterface.getNetworkInterfaces();
                    enumNetIF.hasMoreElements();) {
                NetworkInterface netIF = enumNetIF.nextElement();
                for (Enumeration<InetAddress> enumInetAddress = netIF.getInetAddresses();
                        enumInetAddress.hasMoreElements();) {
                    InetAddress inetAddress = enumInetAddress.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}
