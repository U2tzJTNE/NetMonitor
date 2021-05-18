package com.u2tzjtne.netmonitor.util;

import android.net.ConnectivityManager;
import android.telephony.TelephonyManager;

import com.u2tzjtne.netmonitor.entity.NetState;
import com.u2tzjtne.netmonitor.entity.NetType;

public class NetUtils {

    public static NetType getConnectionType(NetState netState) {
        if (!netState.isConnected()) {
            return NetType.NET_NONE;
        }
        switch (netState.getNetworkType()) {
            case ConnectivityManager.TYPE_ETHERNET:
                return NetType.NET_ETHERNET;
            case ConnectivityManager.TYPE_WIFI:
                return NetType.NET_WIFI;
            case ConnectivityManager.TYPE_WIMAX:
                return NetType.NET_4G;
            case ConnectivityManager.TYPE_BLUETOOTH:
                return NetType.NET_BLUETOOTH;
            case ConnectivityManager.TYPE_MOBILE:
                // Use information from TelephonyManager to classify the connection.
                switch (netState.getNetworkSubType()) {
                    case TelephonyManager.NETWORK_TYPE_GPRS:
                    case TelephonyManager.NETWORK_TYPE_EDGE:
                    case TelephonyManager.NETWORK_TYPE_CDMA:
                    case TelephonyManager.NETWORK_TYPE_1xRTT:
                    case TelephonyManager.NETWORK_TYPE_IDEN:
                        return NetType.NET_2G;
                    case TelephonyManager.NETWORK_TYPE_UMTS:
                    case TelephonyManager.NETWORK_TYPE_EVDO_0:
                    case TelephonyManager.NETWORK_TYPE_EVDO_A:
                    case TelephonyManager.NETWORK_TYPE_HSDPA:
                    case TelephonyManager.NETWORK_TYPE_HSUPA:
                    case TelephonyManager.NETWORK_TYPE_HSPA:
                    case TelephonyManager.NETWORK_TYPE_EVDO_B:
                    case TelephonyManager.NETWORK_TYPE_EHRPD:
                    case TelephonyManager.NETWORK_TYPE_HSPAP:
                        return NetType.NET_3G;
                    case TelephonyManager.NETWORK_TYPE_LTE:
                        return NetType.NET_4G;
                    default:
                        return NetType.NET_UNKNOWN_CELLULAR;
                }
            default:
                return NetType.NET_UNKNOWN;
        }
    }
}
