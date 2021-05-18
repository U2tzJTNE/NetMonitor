package com.u2tzjtne.netmonitor.entity;

/**
 * Java version of NetworkMonitor.NetInfo
 */
public class NetInfo {
    public final String name;
    public final NetType type;
    public final long handle;
    public final IPAddress[] ipAddresses;

    public NetInfo(
            String name, NetType type, long handle, IPAddress[] addresses) {
        this.name = name;
        this.type = type;
        this.handle = handle;
        this.ipAddresses = addresses;
    }
}
