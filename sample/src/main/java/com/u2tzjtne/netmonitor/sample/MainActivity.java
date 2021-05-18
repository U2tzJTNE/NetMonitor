package com.u2tzjtne.netmonitor.sample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.u2tzjtne.netmonitor.core.NetMonitor;
import com.u2tzjtne.netmonitor.core.NetCallback;
import com.u2tzjtne.netmonitor.entity.NetInfo;
import com.u2tzjtne.netmonitor.entity.NetType;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private NetMonitor netMonitor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        netMonitor = new NetMonitor(new NetCallback() {
            @Override
            public void onNetChanged(NetType newNetType) {
                Log.d(TAG, "onNetChanged: " + newNetType.name());
            }

            @Override
            public void onNetConnect(NetInfo netInfo) {
                Log.d(TAG, "onNetConnect: " + netInfo.name);
            }

            @Override
            public void onNetDisconnect(long networkHandle) {
                Log.d(TAG, "onNetDisconnect: " + networkHandle);
            }
        }, this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        netMonitor.destroy();
    }
}
