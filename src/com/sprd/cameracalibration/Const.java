package com.sprd.cameracalibration;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.util.Log;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest.Key;

import com.sprd.cameracalibration.itemstest.*;
import com.sprd.cameracalibration.itemstest.camera.CameraCalibrationVCMActivity;
import com.sprd.cameracalibration.itemstest.camera.CameraVerificationVCMActivity;
import com.sprd.cameracalibration.itemstest.camera.CameraSPWVerificationActivity;
import com.sprd.cameracalibration.itemstest.camera.CameraSPWCalibrationActivity;
import com.sprd.cameracalibration.itemstest.camera.ZoomCalibrationActivity;
import com.sprd.cameracalibration.itemstest.camera.ZoomVerificationActivity;
import com.sprd.cameracalibration.itemstest.camera.CameraSTL3DCalibrationActivity;
import com.sprd.cameracalibration.itemstest.camera.CameraSTL3DVerificationActivity;
import com.sprd.cameracalibration.utils.CameraUtil;
import com.sprd.cameracalibration.utils.SharedPreferencesUtil;
import android.os.Build;

public class Const {
    private static String TAG = "Const";

    public static boolean DEBUG = false;

    public static final String INTENT_PARA_TEST_NAME = "testname";
    public static final String INTENT_PARA_TEST_INDEX = "testindex";

    public final static int TEST_ITEM_DONE = 0;


    // add status for test item
    public static final int FAIL = 0;
    public static final int SUCCESS = 1;
    public static final int DEFAULT = 2;

    //PhasecheckName
    public static final String DUAL_CAMERA_CALIBRATION = "DuaCamCal"; // dual camera calibration
    public static final String DUAL_CAMERA_VERIFICATION = "DuaCamVfy"; // dual camera verification
    public static final String SPW_CAMERA_CALIBRATION = "UWACal"; // spw camera calibration
    public static final String SPW_CAMERA_VERIFICATION = "UWAVfy"; // spw camera verification
    public static final String STL3D_CAMERA_CALIBRATION = "3DStLCal"; // 3DStL camera calibration
    public static final String STL3D_CAMERA_VERIFICATION = "3DStLVfy"; // 3DStL camera verification
    public static final String ZOOM_CALIBRATION = "ZOOMCal"; // zoom calibration
    public static final String ZOOM_VERIFICATION = "ZOOMVfy"; // zoom verification


    //zoom cali veri result code define
    public final static int ZOOM_STAGE_1_RESULTCODE = 1;
    public final static int ZOOM_STAGE_2_RESULTCODE = 2;
    public static final String ZOOM_CALIBRATION_RESULT = "zoom_calibration_result";
    //zoom verify otp data dual flag
    public final static int OTP_FLAG_ZOOM_STAGE_1 = 5;
    public final static int OTP_FLAG_ZOOM_STAGE_2 = 6;

    // add the filter here
    public static boolean isSupport(String className, Context context) {
        Log.d(TAG, "isSupport className="+className);

        if (CameraVerificationVCMActivity.class.getName().equals(className)) {
            if (DEBUG){
                return true;
            }else {
//                String dualEnable = SharedPreferencesUtil.getPrefString("dual_enable","");
//                Log.d(TAG,"dual get dual_enable is "+dualEnable);
//                if (!dualEnable.equals("")) {
//                    return dualEnable.equals("1") ? true : false;
//                } else if (!CameraUtil.isEnableDual()) {
//                    Log.d(TAG,"dual feature enable from HAL is false");
//                    return false;
//                }
                return CameraUtil.isEnableDual();
            }
        }
        else if (CameraCalibrationVCMActivity.class.getName().equals(className)) {
            if (DEBUG){
                return true;
            }else {
//                String dualEnable = SharedPreferencesUtil.getPrefString("dual_enable","");
//                Log.d(TAG,"dual get dual_enable is "+dualEnable);
//                if (!dualEnable.equals("")) {
//                    return dualEnable.equals("1") ? true : false;
//                } else if (!CameraUtil.isEnableDual()) {
//                    Log.d(TAG,"dual feature enable from HAL is false");
//                    return false;
//                }
                return CameraUtil.isEnableDual();
            }
        }
        else if (CameraSPWVerificationActivity.class.getName().equals(className)) {
//            String spwEnable = SharedPreferencesUtil.getPrefString("spw_enable","");
//            Log.d(TAG,"SPW get spw_enable is "+spwEnable);
//            if (!spwEnable.equals("")) {
//                return spwEnable.equals("1") ? true : false;
//            } else if (!CameraUtil.isEnableSPW()) {
//                Log.d(TAG,"SPW feature enable from HAL is false");
//                return false;
//            }

            return CameraUtil.isEnableSPW();
        }
        else if (CameraSPWCalibrationActivity.class.getName().equals(className)) {
//            String spwEnable = SharedPreferencesUtil.getPrefString("spw_enable","");
//            Log.d(TAG,"SPW get spw_enable is "+spwEnable);
//            if (!spwEnable.equals("")) {
//                return spwEnable.equals("1") ? true : false;
//            } else if (!CameraUtil.isEnableSPW()) {
//                Log.d(TAG,"SPW feature enable from HAL is false");
//                return false;
//            }
            return CameraUtil.isEnableSPW();
        }else if (ZoomCalibrationActivity.class.getName().equals(className) || ZoomVerificationActivity.class.getName().equals(className)){
            return CameraUtil.isEnableZoom();
        }else if (CameraSTL3DCalibrationActivity.class.getName().equals(className)||CameraSTL3DVerificationActivity.class.getName().equals(className)){
            return CameraUtil.isEnableSTL3D();
        }
        return true;
    }

    public static String getPhasecheckName(String className){
        Log.d(TAG, "getPhasecheckName className="+className);
        if (CameraVerificationVCMActivity.class.getName().equals(className)) {

                return DUAL_CAMERA_VERIFICATION;

        }
        else if (CameraCalibrationVCMActivity.class.getName().equals(className)) {

                return DUAL_CAMERA_CALIBRATION;

        }
        else if (CameraSPWVerificationActivity.class.getName().equals(className)) {


            return SPW_CAMERA_VERIFICATION;
        }
        else if (CameraSPWCalibrationActivity.class.getName().equals(className)) {

            return SPW_CAMERA_CALIBRATION;
        }else if (ZoomCalibrationActivity.class.getName().equals(className)){
            return ZOOM_CALIBRATION;
        }else if (ZoomVerificationActivity.class.getName().equals(className)){
            return ZOOM_VERIFICATION;
        }else if (CameraSTL3DCalibrationActivity.class.getName().equals(className)){
            return STL3D_CAMERA_CALIBRATION;
        }else if (CameraSTL3DVerificationActivity.class.getName().equals(className)){
            return STL3D_CAMERA_VERIFICATION;
        }
        return null;
    }
}
