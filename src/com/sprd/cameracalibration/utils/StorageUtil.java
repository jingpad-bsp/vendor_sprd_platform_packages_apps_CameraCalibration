package com.sprd.cameracalibration.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import android.content.Context;
import android.os.Environment;
//import android.os.EnvironmentEx;
import android.os.storage.StorageManager;
//import com.sprd.validationtools.nonpublic.EnvironmentExProxy;
import android.os.EnvironmentEx;
import android.util.Log;

public class StorageUtil {
    public static final String TAG = "StorageUtil";
    public static String getExternalStoragePathState(){
    	return EnvironmentEx.getExternalStoragePathState();
    }

    public static String getInternalStoragePath(){
    	return EnvironmentEx.getInternalStoragePath().getAbsolutePath();
    }
}