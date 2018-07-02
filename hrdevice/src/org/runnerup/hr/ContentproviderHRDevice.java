package org.runnerup.hr;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.BaseColumns;
import android.util.Log;

public class ContentproviderHRDevice implements HRProvider {

    private HRClient hrClient = null;
    private Handler hrClientHandler = null;
    public static final String NAME = "ContentproviderHRDevice";

    private Context ctx;

    public ContentproviderHRDevice(Context ctx) {
        super();
        this.ctx = ctx;
    }

    @Override
    public String getName() {
        // indicate to the user which provider we use
        return "Gadgetbridge";
    }

    @Override
    public String getProviderName() {
        return NAME;
    }

    @Override
    public void open(Handler handler, HRClient hrClient) {
        this.hrClient = hrClient;
        this.hrClientHandler = handler;

        Log.e(getClass().getName(), "open");
        ContentResolver resolver = ctx.getContentResolver();
        String[] projection = new String[]{BaseColumns._ID};
        Uri provider = Uri.parse("content://com.gadgetbridge.heartrate.provider/devices");
        Log.e(getClass().getName(), "Sending Query");

        Cursor cursor = resolver.query(provider, projection, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(0);
                // do something meaningful
            } while (cursor.moveToNext());
        } else {
            Log.e(getClass().getName(), "No Reults from Curosr");
        }




                //TODO: Subscribe to Contentprovider
        hrClient.onOpenResult(true);
    }

    @Override
    public void close() {
        Log.e(getClass().getName(), "close");
    }

    boolean mIsScanning = false;

    @Override
    public boolean isScanning() {
        return mIsScanning;
    }

    @Override
    public void startScan() {
        mIsScanning = true;

        Log.e(getClass().getName(), "StartScan");

        //TODO ask Gadgetbridge for all devices
        hrClient.onScanResult(HRDeviceRef.create(getProviderName(), "device-"+getProviderName(), "addr-"+NAME));
    }

    @Override
    public void stopScan() {
        Log.e(getClass().getName(), "StopScan");
        mIsScanning = false;
    }

    boolean mIsConnecting = false;
    boolean mIsConnected = false;

    @Override
    public boolean isConnected() {
        return mIsConnected;
    }

    @Override
    public boolean isConnecting() {
        return mIsConnecting;
    }

    @Override
    public void connect(HRDeviceRef ref) {
        Log.e(getClass().getName(), "connect");

        if (mIsConnected)
            return;

        if (mIsConnecting)
            return;

        mIsConnecting = true;
        hrClientHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mIsConnecting) {
                    mIsConnected = true;
                    mIsConnecting = false;
                    hrClient.onConnectResult(true);
                    hrClientHandler.postDelayed(hrUpdate, 750);
                }
            }
        }, 3000);
    }

    final Runnable hrUpdate = new Runnable() {
        @Override
        public void run() {
            hrValue = (int) (150 + 40 * Math.random());
            hrTimestamp = System.currentTimeMillis();
            if (mIsConnected == true) {
                hrClientHandler.postDelayed(hrUpdate, 750);
            }
        }
    };

    @Override
    public void disconnect() {
        Log.e(getClass().getName(), "disconnect");

        mIsConnecting = false;
        mIsConnected = false;
    }

    int hrValue = 0;
    long hrTimestamp = 0;

    @Override
    public int getHRValue() {
        return hrValue;
    }

    @Override
    public long getHRValueTimestamp() {
        return hrTimestamp;
    }

    @Override
    public HRData getHRData() {
        if (hrValue <= 0) {
            return null;
        }

        return new HRData().setHeartRate(hrValue).setTimestampEstimate(hrTimestamp);
    }

    @Override
    public int getBatteryLevel() {
        return (int) (100 * Math.random());
    }

    @Override
    public boolean isBondingDevice() {
        return false;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean startEnableIntent(Activity activity, int requestCode) {
        return false;
    }
}