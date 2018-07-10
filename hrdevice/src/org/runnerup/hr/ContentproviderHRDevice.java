package org.runnerup.hr;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class ContentproviderHRDevice implements HRProvider {

    private Context ctx;
    private HRClient hrClient = null;
    private Handler hrClientHandler = null;
    public static final String NAME = "ContentproviderHRDevice";
    private static final Uri GADGETBRIDGE_AUTHORITY = Uri.parse("content://nodomain.freeyourgadget.gadgetbridge.realtimesamples.provider");
    private static final Uri realtime_uri = GADGETBRIDGE_AUTHORITY.buildUpon().appendPath("realtime").build();
    private static final Uri devices = GADGETBRIDGE_AUTHORITY.buildUpon().appendPath("devices").build();
    private static final Uri start_uri = GADGETBRIDGE_AUTHORITY.buildUpon().appendPath("activity_start").build();
    private static final Uri stop_uri = GADGETBRIDGE_AUTHORITY.buildUpon().appendPath("activity_stop").build();


    /**
     * This is registered with the ContentResolver updates the hrValue and hrTimestamp
     */
    private ContentObserver mObserver = new ContentObserver(null) {

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            Log.e(ContentproviderHRDevice.class.getName(), "Changed " + uri.toString());

            Cursor cursor = ctx.getContentResolver().query(realtime_uri, null, null, null, null);
            if (cursor.moveToFirst()) {
                do {
                    String status = cursor.getString(0);
                    if (!status.equals("OK"))
                        continue;

                    hrValue = cursor.getInt(1);
                    stepsValue = cursor.getInt(2);
                    batteryLevel = cursor.getInt(3);
                    hrTimestamp = System.currentTimeMillis();
                    Log.i(ContentproviderHRDevice.class.getName(), String.format("HeartRate %d Steps %d Battery %d", hrValue, stepsValue, batteryLevel));
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
    };

    ContentproviderHRDevice(Context ctx) {
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

        hrClient.onOpenResult(true);
    }

    private void startGBRealtime(HRDeviceRef ref) {

        String deviceAddress = ref != null ? ref.getAddress() : "";
        // TODO if there is no device, what should I do here??
        Cursor cursor = ctx.getContentResolver().query(start_uri, null, null, new String[]{ref.getAddress()}, null);
        if (cursor == null)
            return;

        cursor.close();
    }

    private void stopGBRealtime() {
        Cursor cursor = ctx.getContentResolver().query(stop_uri, null, null, null, null);
        if (cursor == null)
            return;

        cursor.close();

    }

    private List<HRDeviceRef> getDevices() {
        List<HRDeviceRef> ret = new ArrayList<>();
        ContentResolver resolver = ctx.getContentResolver();

        Log.i(getClass().getName(), "Sending Query to " + devices.toString());

        Cursor cursor = resolver.query(devices, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String deviceName = cursor.getString(0);
                String deviceAddress = cursor.getString(2);
                ret.add(HRDeviceRef.create(getProviderName(), deviceName, deviceAddress));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return ret;
    }

    @Override
    public void close() {
        Log.e(getClass().getName(), "close");
    }

    private boolean mIsScanning = false;

    @Override
    public boolean isScanning() {
        return mIsScanning;
    }

    @Override
    public void startScan() {
        mIsScanning = true;

        Log.e(getClass().getName(), "StartScan");

        final List<HRDeviceRef> devices = getDevices();
        hrClientHandler.post(new Runnable() {
            @Override
            public void run() {
                for (HRDeviceRef dev : devices)
                    hrClient.onScanResult(dev);
            }
        });

    }

    @Override
    public void stopScan() {
        Log.e(getClass().getName(), "StopScan");
        mIsScanning = false;
    }

    private boolean mIsConnecting = false;
    private boolean mIsConnected = false;

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
        Log.e(getClass().getName(), String.format("connect to %s", ref.toString()));

        if (mIsConnected)
            return;

        if (mIsConnecting)
            return;

        mIsConnecting = true;

        // TODO: If there is no device, there is no point in returning OK

        // Notify on client's thread
        hrClientHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mIsConnecting) {
                    mIsConnected = true;
                    mIsConnecting = false;
                    hrClient.onConnectResult(true);
                }
            }
        });
        ctx.getContentResolver().registerContentObserver(realtime_uri, true, mObserver);

        startGBRealtime(ref);
    }

    @Override
    public void disconnect() {
        Log.e(getClass().getName(), "disconnect");

        ctx.getContentResolver().unregisterContentObserver(mObserver);

        stopGBRealtime();

        mIsConnecting = false;
        mIsConnected = false;
    }

    private int hrValue = 0;
    private long hrTimestamp = 0;
    private int batteryLevel = 0;
    private int stepsValue = 0;

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
        //TODO should I start gadgetbridge here??
        return false;
    }
}
