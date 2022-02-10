package com.sprd.cameracalibration.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;


/**
 * Created by SPREADTRUM\emily.miao on 19-10-11.
 */

public class SharedPreferencesUtil {
    private static String TAG = "SharedPreferencesUtil";

    private static SharedPreferences mPrefs = null;

    public static void init(Context context){
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static String getPrefString(String key,String defaultValue) {
        return mPrefs.getString(key, defaultValue);
    }

    public static void setPrefString(String key, String value) {
        boolean saved = mPrefs.edit().putString(key, value).commit();
        Log.i(TAG,"key = " + key + " value = " + value + " saved = " + saved);
    }
}
