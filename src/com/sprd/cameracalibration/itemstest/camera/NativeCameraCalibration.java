package com.sprd.cameracalibration.itemstest.camera;

import android.util.Log;

public class NativeCameraCalibration {
	private static final String TAG = "NativeCameraCalibration";

    static {
        try {
            System.loadLibrary("jni_dualcameraverify");
        } catch (UnsatisfiedLinkError e) {
            Log.d(TAG, " #loadLibrary jni_dualcameraverify failed  ");
            e.printStackTrace();
        }
    }

    static {
        try {
            System.loadLibrary("jni_dualcameracalibration");
        } catch (UnsatisfiedLinkError e) {
            Log.d(TAG, " #loadLibrary libjni_dualcameracalibration failed  ");
            e.printStackTrace();
        }
    }

    static {
        try {
            System.loadLibrary("jni_spwcameraverify");
        } catch (UnsatisfiedLinkError e) {
            Log.d(TAG, " #loadLibrary libjni_spwcameraverify failed  ");
            e.printStackTrace();
        }
    }

    static {
        try {
            System.loadLibrary("jni_spwcameracalibration");
        } catch (UnsatisfiedLinkError e) {
            Log.d(TAG, " #loadLibrary libjni_spwcameracalibration failed  ");
            e.printStackTrace();
        }
    }


    static {
        try {
            System.loadLibrary("jni_wtcameracalibration");
        } catch (UnsatisfiedLinkError e) {
            Log.d(TAG, " #loadLibrary libjni_wtcameracalibration failed  ");
            e.printStackTrace();
        }
    }

    static {
        try {
            System.loadLibrary("jni_wtcameraverification");
        } catch (UnsatisfiedLinkError e) {
            Log.d(TAG, " #loadLibrary libjni_wtcameraverification failed  ");
            e.printStackTrace();
        }
    }

    static {
        try {
            System.loadLibrary("jni_stl3dcameracalibration");
        } catch (UnsatisfiedLinkError e) {
            Log.d(TAG, " #loadLibrary libjni_stl3dcameracalibration failed  ");
            e.printStackTrace();
        }
    }


    static {
        try {
            System.loadLibrary("jni_stl3dcameraverification");
        } catch (UnsatisfiedLinkError e) {
            Log.d(TAG, " #loadLibrary libjni_stl3dcameraverification failed  ");
            e.printStackTrace();
        }
    }

    public static native int native_dualCameraVerfication(String leftImage, String rightImage, String otpPath);

    public static native int native_dualCameraVerfication_vcm(String leftImage, String rightImage, String otpPath);

    /*SPRD bug 759782 : Display RMS value*/
    public static native double native_getCameraVerficationRMS();
    /*@}*/

    public static native int native_dualCameraCalibrationYUV(String leftImage, String rightImage, String calibration_version);
    public static native int native_dualCameraCalibrationYUV(String leftImage, String rightImage, String calibration_version, int vcmIndex,int leftWidth,int leftHeight,int rightWidth,int rightHeight);

    public static native int[] native_getdualCameraCalibrationOTP();
    public static native int[] native_getdualCameraCalibrationOTP(int vcmIndex);

    public static native String native_getStereoInfo();

    public static native int native_getModulueLocation();

    public static native int native_getOtpVersion();

    public static native int native_MonoCameraVerificationNV21_New(String image_dir, String otpDataPath, int imageWidth, int imageHeight);

    public static native double native_getMonoCameraVerificationNV21_RMS();


    public static native int native_SpwCameraCalibrationYUV(String image_dir,int imageWidth, int imageHeight);

    public static native int[] native_getSpwCameraCalibrationOTP();
    public static native int[] native_getSpwOTPHeader();

    public static native int native_dualCameraCalibrationYUV_afterSales(String iamgeDir, String otpDataPath ,int leftWidth,int leftHeight,int rightWidth,int rightHeight,int isAfterSales);
    public static native int[] native_getdualCameraCalibrationOTP_afterSales();
    public static native int native_dualCameraCalibrationYUV_afterSales_v2(String iamgeDir, String otpDataPath ,int leftWidth,int leftHeight,int rightWidth,int rightHeight);

    public static native int native_WTCameraCalibration(String teleImage,String wideImage,String stage,int wVCM,int tVCM,int leftWidth,int leftHeight,int rightWidth,int rightHeight);
    public static native int[] native_getWTCameraCalibrationOTP();
    public static native int[] native_getWTOTPHeader();
    public static native int native_WTCameraVerification(String teleImage,String wideImage,String otpDataPath,int leftWidth,int leftHeight,int rightWidth,int rightHeight);

    public static native int native_STL3DCameraCalibration(String irleft,String irright,String yuv);
    public static native int[] native_getSTL3DCameraCalibrationOTP();
    public static native int[] native_getSTL3DOTPHeader();
    public static native float[] native_getSTL3DCoordinate();

    public static native int native_STL3DCameraVerification(String irleft,String irright,String yuv,String otpDataPath);
    public static native double[] native_getSTL3DCameraVerification_RMS();
}