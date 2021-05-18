package com.u2tzjtne.netmonitor.core.delegate;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.util.Log;

import com.u2tzjtne.netmonitor.entity.NetInfo;
import com.u2tzjtne.netmonitor.entity.NetType;
import com.u2tzjtne.netmonitor.entity.IPAddress;
import com.u2tzjtne.netmonitor.entity.NetState;
import com.u2tzjtne.netmonitor.util.NetUtils;

import java.util.ArrayList;
import java.util.List;

import static android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET;
import static android.net.NetworkCapabilities.TRANSPORT_CELLULAR;
import static com.u2tzjtne.netmonitor.core.NetMonitor.INVALID_NET_ID;
import static com.u2tzjtne.netmonitor.core.NetMonitor.TAG;
import static com.u2tzjtne.netmonitor.core.NetMonitor.networkToNetId;

/**
 * Queries the ConnectivityManager for information about the current connection.
 */
public class ConnectivityManagerDelegate {
    /**
     * Note: In some rare Android systems connectivityManager is null.  We handle that
     * gracefully below.
     */
    private final ConnectivityManager connectivityManager;

    public ConnectivityManagerDelegate(Context context) {
        connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    /**
     * Returns connection type and status information about the current
     * default network.
     */
    public NetState getNetworkState() {
        if (connectivityManager == null) {
            return new NetState(false, -1, -1);
        }
        return getNetworkState(connectivityManager.getActiveNetworkInfo());
    }

    /**
     * Returns connection type and status information about |network|.
     * Only callable on Lollipop and newer releases.
     */
    @SuppressLint("NewApi")
    public NetState getNetworkState(Network network) {
        if (connectivityManager == null) {
            return new NetState(false, -1, -1);
        }
        return getNetworkState(connectivityManager.getNetworkInfo(network));
    }

    /**
     * Returns connection type and status information gleaned from networkInfo.
     */
    public NetState getNetworkState(android.net.NetworkInfo networkInfo) {
        if (networkInfo == null || !networkInfo.isConnected()) {
            return new NetState(false, -1, -1);
        }
        return new NetState(true, networkInfo.getType(), networkInfo.getSubtype());
    }

    /**
     * Returns all connected networks.
     * Only callable on Lollipop and newer releases.
     */
    @SuppressLint("NewApi")
    public Network[] getAllNetworks() {
        if (connectivityManager == null) {
            return new Network[0];
        }
        return connectivityManager.getAllNetworks();
    }

    public List<NetInfo> getActiveNetworkList() {
        if (!supportNetworkCallback()) {
            return null;
        }
        ArrayList<NetInfo> netInfoList = new ArrayList<>();
        for (Network network : getAllNetworks()) {
            NetInfo info = networkToInfo(network);
            if (info != null) {
                netInfoList.add(info);
            }
        }
        return netInfoList;
    }

    /**
     * Returns the NetID of the current default network. Returns
     * INVALID_NET_ID if no current default network connected.
     * Only callable on Lollipop and newer releases.
     */
    @SuppressLint({"NewApi", "Assert"})
    public long getDefaultNetId() {
        if (!supportNetworkCallback()) {
            return INVALID_NET_ID;
        }
        // Android Lollipop had no API to get the default network; only an
        // API to return the NetInfo for the default network. To
        // determine the default network one can find the network with
        // type matching that of the default network.
        final android.net.NetworkInfo defaultNetworkInfo = connectivityManager.getActiveNetworkInfo();
        if (defaultNetworkInfo == null) {
            return INVALID_NET_ID;
        }
        final Network[] networks = getAllNetworks();
        long defaultNetId = INVALID_NET_ID;
        for (Network network : networks) {
            if (!hasInternetCapability(network)) {
                continue;
            }
            final android.net.NetworkInfo networkInfo = connectivityManager.getNetworkInfo(network);
            if (networkInfo != null && networkInfo.getType() == defaultNetworkInfo.getType()) {
                // There should not be multiple connected networks of the
                // same type. At least as of Android Marshmallow this is
                // not supported. If this becomes supported this assertion
                // may trigger. At that point we could consider using
                // ConnectivityManager.getDefaultNetwork() though this
                // may give confusing results with VPNs and is only
                // available with Android Marshmallow.
                assert defaultNetId == INVALID_NET_ID;
                defaultNetId = networkToNetId(network);
            }
        }
        return defaultNetId;
    }

    @SuppressLint("NewApi")
    public NetInfo networkToInfo(Network network) {
        LinkProperties linkProperties = connectivityManager.getLinkProperties(network);
        // getLinkProperties will return null if the network is unknown.
        if (linkProperties == null) {
            Log.w(TAG, "Detected unknown network: " + network.toString());
            return null;
        }
        if (linkProperties.getInterfaceName() == null) {
            Log.w(TAG, "Null interface name for network " + network.toString());
            return null;
        }
        NetState netState = getNetworkState(network);
        NetType netType = NetUtils.getConnectionType(netState);
        if (netType == NetType.NET_NONE) {
            // This may not be an error. The OS may signal a network event with connection type
            // NONE when the network disconnects.
            Log.d(TAG, "Network " + network.toString() + " is disconnected");
            return null;
        }
        // Some android device may return a NET_UNKNOWN_CELLULAR or NET_UNKNOWN type,
        // which appears to be usable. Just log them here.
        if (netType == NetType.NET_UNKNOWN
                || netType == NetType.NET_UNKNOWN_CELLULAR) {
            Log.d(TAG, "Network " + network.toString() + " connection type is " + netType
                    + " because it has type " + netState.getNetworkType() + " and subtype "
                    + netState.getNetworkSubType());
        }
        return new NetInfo(linkProperties.getInterfaceName(), netType,
                networkToNetId(network), getIPAddresses(linkProperties));
    }

    /**
     * Returns true if {@code network} can provide Internet access. Can be used to
     * ignore specialized networks (e.g. IMS, FOTA).
     */
    @SuppressLint("NewApi")
    public boolean hasInternetCapability(Network network) {
        if (connectivityManager == null) {
            return false;
        }
        final NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
        return capabilities != null && capabilities.hasCapability(NET_CAPABILITY_INTERNET);
    }

    /**
     * Only callable on Lollipop and newer releases.
     */
    @SuppressLint("NewApi")
    public void registerNetworkCallback(ConnectivityManager.NetworkCallback networkCallback) {
        connectivityManager.registerNetworkCallback(
                new NetworkRequest.Builder().addCapability(NET_CAPABILITY_INTERNET).build(),
                networkCallback);
    }

    /**
     * Only callable on Lollipop and newer releases.
     */
    @SuppressLint("NewApi")
    public void requestMobileNetwork(ConnectivityManager.NetworkCallback networkCallback) {
        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        builder.addCapability(NET_CAPABILITY_INTERNET).addTransportType(TRANSPORT_CELLULAR);
        connectivityManager.requestNetwork(builder.build(), networkCallback);
    }

    @SuppressLint("NewApi")
    public IPAddress[] getIPAddresses(LinkProperties linkProperties) {
        IPAddress[] ipAddresses = new IPAddress[linkProperties.getLinkAddresses().size()];
        int i = 0;
        for (LinkAddress linkAddress : linkProperties.getLinkAddresses()) {
            ipAddresses[i] = new IPAddress(linkAddress.getAddress().getAddress());
            ++i;
        }
        return ipAddresses;
    }

    @SuppressLint("NewApi")
    public void releaseCallback(ConnectivityManager.NetworkCallback networkCallback) {
        if (supportNetworkCallback()) {
            Log.d(TAG, "Unregister network callback");
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
    }

    public boolean supportNetworkCallback() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && connectivityManager != null;
    }
}
