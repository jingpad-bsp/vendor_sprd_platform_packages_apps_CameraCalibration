package com.sprd.cameracalibration;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.sprd.cameracalibration.itemstest.camera.CameraCaliAfterSalesSelectActivity;

public class CameraToolsBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "CameraToolsBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive action = " + intent.getAction());

        Uri uri = intent.getData();
        if (uri == null)
            return;
        String host = uri.getHost();

        Intent i = new Intent(Intent.ACTION_MAIN);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        Log.d(TAG, "onReceive host=" + host);
        Log.d(TAG, "onReceive getAction=" + intent.getAction());

        if("83788".equals(host)) {
            i.setClass(context, CameraCaliAfterSalesSelectActivity.class);
            context.startActivity(i);
        }
    }

}
