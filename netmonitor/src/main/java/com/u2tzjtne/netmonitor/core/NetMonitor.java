package com.u2tzjtne.netmonitor.core;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.util.Log;

import com.u2tzjtne.netmonitor.core.delegate.ConnectivityManagerDelegate;
import com.u2tzjtne.netmonitor.core.delegate.WifiManagerDelegate;
import com.u2tzjtne.netmonitor.entity.NetType;
import com.u2tzjtne.netmonitor.entity.NetInfo;
import com.u2tzjtne.netmonitor.entity.NetState;
import com.u2tzjtne.netmonitor.util.NetUtils;

import java.util.List;

/**
 * Borrowed from Chromium's
 * src/net/android/java/src/org/chromium/net/NetworkChangeNotifierAutoDetect.java
 * <p>
 * Used by the NetworkMonitor to listen to platform changes in connectivity.
 * Note that use of this class requires that the app have the platform
 * ACCESS_NETWORK_STATE permission.
 */
public class NetMonitor extends BroadcastReceiver {

    public static final long INVALID_NET_ID = -1;
    public static final String TAG = "NetMonitor";
    // NetCallback for the connection type change.
    private final NetCallback netCallback;
    private final IntentFilter intentFilter;
    private final Context context;
    // Used to request mobile network. It does not do anything except for keeping
    // the callback for releasing the request.
    private final NetworkCallback mobileNetworkCallback;
    // Used to receive updates on all networks.
    private final NetworkCallback allNetworkCallback;
    // connectivityManagerDelegate and wifiManagerDelegate are only non-final for testing.
    private ConnectivityManagerDelegate connectivityManagerDelegate;
    private WifiManagerDelegate wifiManagerDelegate;
    private boolean isRegistered;
    private NetType netType;
    private String wifiSSID;

    /**
     * Constructs a NetMonitor. Should only be called on UI thread.
     */
    @SuppressLint("NewApi")
    public NetMonitor(NetCallback netCallback, Context context) {
        this.netCallback = netCallback;
        this.context = context;
        connectivityManagerDelegate = new ConnectivityManagerDelegate(context);
        wifiManagerDelegate = new WifiManagerDelegate(context);
        final NetState netState = connectivityManagerDelegate.getNetworkState();
        netType = NetUtils.getConnectionType(netState);
        wifiSSID = getWifiSSID(netState);
        intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver();
        if (connectivityManagerDelegate.supportNetworkCallback()) {
            // On Android 6.0.0, the WRITE_SETTINGS permission is necessary for
            // requestNetwork, so it will fail. This was fixed in Android 6.0.1.
            NetworkCallback tempNetworkCallback = new NetworkCallback();
            try {
                connectivityManagerDelegate.requestMobileNetwork(tempNetworkCallback);
            } catch (SecurityException e) {
                Log.w(TAG, "Unable to obtain permission to request a cellular network.");
                tempNetworkCallback = null;
            }
            mobileNetworkCallback = tempNetworkCallback;
            allNetworkCallback = new SimpleNetworkCallback();
            connectivityManagerDelegate.registerNetworkCallback(allNetworkCallback);
        } else {
            mobileNetworkCallback = null;
            allNetworkCallback = null;
        }
    }

    public List<NetInfo> getActiveNetworkList() {
        return connectivityManagerDelegate.getActiveNetworkList();
    }

    /**
     * Registers a BroadcastReceiver in the given context.
     */
    private void registerReceiver() {
        if (isRegistered)
            return;
        isRegistered = true;
        context.registerReceiver(this, intentFilter);
    }

    /**
     * Unregisters the BroadcastReceiver in the given context.
     */
    private void unregisterReceiver() {
        if (!isRegistered)
            return;
        isRegistered = false;
        context.unregisterReceiver(this);
    }

    public NetState getCurrentNetworkState() {
        return connectivityManagerDelegate.getNetworkState();
    }

    /**
     * Returns NetID of device's current default connected network used for
     * communication.
     * Only implemented on Lollipop and newer releases, returns INVALID_NET_ID
     * when not implemented.
     */
    public long getDefaultNetId() {
        return connectivityManagerDelegate.getDefaultNetId();
    }



    private String getWifiSSID(NetState netState) {
        if (NetUtils.getConnectionType(netState) != NetType.NET_WIFI)
            return "";
        return wifiManagerDelegate.getWifiSSID();
    }

    // BroadcastReceiver
    @Override
    public void onReceive(Context context, Intent intent) {
        final NetState netState = getCurrentNetworkState();
        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
            connectionTypeChanged(netState);
        }
    }

    private void connectionTypeChanged(NetState netState) {
        NetType newNetType = NetUtils.getConnectionType(netState);
        String newWifiSSID = getWifiSSID(netState);
        if (newNetType == netType && newWifiSSID.equals(wifiSSID)) return;
        netType = newNetType;
        wifiSSID = newWifiSSID;
        Log.d(TAG, "Network connectivity changed, type is: " + netType);
        netCallback.onNetChanged(newNetType);
    }

    /**
     * Extracts NetID of network on Lollipop and NetworkHandle (which is mungled
     * NetID) on Marshmallow and newer releases. Only available on Lollipop and
     * newer releases. Returns long since getNetworkHandle returns long.
     */
    @SuppressLint("NewApi")
    public static long networkToNetId(Network network) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return network.getNetworkHandle();
        }
        // NOTE(honghaiz): This depends on Android framework implementation details.
        // These details cannot change because Lollipop has been released.
        return Integer.parseInt(network.toString());
    }

    /**
     * The methods in this class get called when the network changes if the callback
     * is registered with a proper network request. It is only available in Android Lollipop
     * and above.
     */
    @SuppressLint("NewApi")
    private class SimpleNetworkCallback extends NetworkCallback {
        @Override
        public void onAvailable(Network network) {
            Log.d(TAG, "Network becomes available: " + network.toString());
            onNetworkChanged(network);
        }

        @Override
        public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
            // A capabilities change may indicate the NetType has changed,
            // so forward the new NetInfo along to the netCallback.
            Log.d(TAG, "capabilities changed: " + networkCapabilities.toString());
            onNetworkChanged(network);
        }

        @Override
        public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
            // A link property change may indicate the IP address changes.
            // so forward the new NetInfo to the netCallback.
            Log.d(TAG, "link properties changed: " + linkProperties.toString());
            onNetworkChanged(network);
        }

        @Override
        public void onLosing(Network network, int maxMsToLive) {
            // Tell the network is going to lose in MaxMsToLive milliseconds.
            // We may use this signal later.
            Log.d(TAG, "Network " + network.toString() + " is about to lose in " + maxMsToLive + "ms");
        }

        @Override
        public void onLost(Network network) {
            Log.d(TAG, "Network " + network.toString() + " is disconnected");
            netCallback.onNetDisconnect(networkToNetId(network));
        }

        private void onNetworkChanged(Network network) {
            NetInfo netInfo = connectivityManagerDelegate.networkToInfo(network);
            if (netInfo != null) {
                netCallback.onNetConnect(netInfo);
            }
        }
    }

    public void destroy() {
        if (allNetworkCallback != null) {
            connectivityManagerDelegate.releaseCallback(allNetworkCallback);
        }
        if (mobileNetworkCallback != null) {
            connectivityManagerDelegate.releaseCallback(mobileNetworkCallback);
        }
        unregisterReceiver();
    }
}