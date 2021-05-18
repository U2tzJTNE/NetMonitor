package com.u2tzjtne.netmonitor.core;

import com.u2tzjtne.netmonitor.entity.NetInfo;
import com.u2tzjtne.netmonitor.entity.NetType;

/**
 * NetCallback interface by which observer is notified of network changes.
 */
public interface NetCallback {
    /**
     * Called when default network changes.
     */
    void onNetChanged(NetType newNetType);

    void onNetConnect(NetInfo netInfo);

    void onNetDisconnect(long networkHandle);
}
