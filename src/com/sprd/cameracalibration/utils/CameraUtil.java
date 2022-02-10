package com.sprd.cameracalibration.utils;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfiguration;
import android.os.Build;
import android.util.Log;

import com.sprd.cameracalibration.Const;

import java.util.Arrays;


/**
 * Created by SPREADTRUM\emily.miao on 19-10-12.
 */

public class CameraUtil {
    private static String TAG = "CameraUtil";

    private enum FeatureTagID {
        BEAUTYVERSION,
        BACKBLURVERSION,
        FRONTBLURVERSION,
        BLURCOVEREDID,
        FRONTFLASHMODE,
        BACKWPLUSTMODEENABLE,
        AUTOTRACKINGENABLE,
        BACKULTRAWIDEANGLEENABLE,
        GDEPTHENABLE,
        BACKPORTRAITEENABLE,
        FRONTPORTRAITENABLE,
        MONTIONENABLE,
        DEFAULTQUARTERSIZE,
    };
    public final static int ZOOM_CALI_FEATURELIST_NUM = 16;
    public final static int ZOOM_1_STAGE = 1;
    public final static int ZOOM_2_STAGE = 2;
    public final static int ZOOM_1_2_STAGE = 3;

    public final static int BLUR_REFOCUS_VERSION_6 = 6;
    public final static int BLUR_REFOCUS_VERSION_9 = 9;//support hdr + bokeh

    public final static int STL3DENABLE = 21;

    private static final  CameraCharacteristics.Key<Integer> ANDROID_SPRD_SPW_CAMERA_ID = new CameraCharacteristics.Key<Integer>("com.addParameters.sprdUltraWideId", Integer.class);
    private static final CameraCharacteristics.Key<int[]> ANDROID_SPRD_FEATURE_LIST = new CameraCharacteristics.Key<int[]>("com.addParameters.sprdCamFeatureList", int[].class);
    private static final CameraCharacteristics.Key<Integer> ANDROID_SPRD_3DCALIBRATION_CAPTURE_SIZE = new CameraCharacteristics.Key<Integer>("com.addParameters.srpd3dCalibrationSize", Integer.class);

    private static final CameraCharacteristics.Key<Integer> ANDROID_SPRD_STL3D_ID = new CameraCharacteristics.Key<Integer>("com.addParameters.sprdStl3dId",Integer.class);

    private static int[] featureList = null;
    private static CameraManager cameraManager = null;
    // init feature list
    public static void initFeatureList(Context context){
        cameraManager = getCameraManager(context);
        try {
            featureList = cameraManager.getCameraCharacteristics(Integer.toString(0)).get(ANDROID_SPRD_FEATURE_LIST);
            Log.i(TAG,"get capability from hal featureList ="+ Arrays.toString(featureList));
            if (featureList == null || featureList.length == 0) {
                Log.d(TAG,"HAL does not have feature list");
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private static CameraManager getCameraManager(Context context){
        boolean isLOrHigher = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                || "L".equals(Build.VERSION.CODENAME) || "LOLLIPOP".equals(Build.VERSION.CODENAME);
        Log.d(TAG,"isLOrHigher is "+isLOrHigher);

        Object service = isLOrHigher ?  context.getSystemService(Context.CAMERA_SERVICE) : null;
        return (CameraManager) service;
    }

    public static boolean isEnableSPW() {
        if (featureList == null || featureList.length == 0) {
            return false;
        }
        boolean isEnableSPW = featureList[FeatureTagID.BACKULTRAWIDEANGLEENABLE.ordinal()] == 1 ? true : false;
        Log.d(TAG,"SPW feature enable from HAL is "+isEnableSPW);
        Log.d(TAG,"BACKULTRAWIDEANGLEENABLE from featurelist is " + featureList[FeatureTagID.BACKULTRAWIDEANGLEENABLE.ordinal()]);

        try {
            if (isEnableSPW) {
                int cameraId = cameraManager.getCameraCharacteristics(Integer.toString(0)).get(ANDROID_SPRD_SPW_CAMERA_ID);
                SharedPreferencesUtil.setPrefString("spw_cameraid",String.valueOf(cameraId));
            }
        }catch (CameraAccessException e) {
            e.printStackTrace();
            return false;
        }
        return isEnableSPW;
    }


    public static boolean isEnableDual() {
        if (featureList == null || featureList.length == 0) {
            return false;
        }
        int blurversion = featureList[FeatureTagID.BACKBLURVERSION.ordinal()];
        boolean isEnableDual = blurversion == BLUR_REFOCUS_VERSION_6 || blurversion == BLUR_REFOCUS_VERSION_9 ? true : false;
        Log.d(TAG,"Dual feature enable from HAL is "+isEnableDual);
        Log.d(TAG,"BACKBLURVERSION from featurelist is " + featureList[FeatureTagID.BACKBLURVERSION.ordinal()]);
        return isEnableDual;
    }

    public static boolean isEnableZoom(){
        if (featureList == null || featureList.length == 0){
            return false;
        }
        int SupportZoom = featureList[ZOOM_CALI_FEATURELIST_NUM];
        Log.d(TAG,"Zoom stage from HAL is "+ SupportZoom);
        boolean isEnableZoom = SupportZoom == ZOOM_1_STAGE || SupportZoom == ZOOM_2_STAGE || SupportZoom == ZOOM_1_2_STAGE ? true : false;
        Log.d(TAG,"Zoom feature enable from HAL is "+isEnableZoom);
        return isEnableZoom;
    }

    public static int getSupportZoomStage(){
        if (featureList == null || featureList.length == 0){
            return 0;
        }
        return featureList[ZOOM_CALI_FEATURELIST_NUM];
    }

    public static boolean isEnableSTL3D(){
        if (featureList == null || featureList.length == 0) {
            return false;
        }
        boolean isEnableSTL3D = featureList[STL3DENABLE] == 1 ? true : false;
        Log.d(TAG,"STL3D feature enable from HAL is "+isEnableSTL3D);
        Log.d(TAG,"STL3DENABLE from featurelist is " + featureList[STL3DENABLE]);
        return isEnableSTL3D;
    }

    public static int[] getLargestYuvSize(Context context,String cameraId){
        CameraManager cameraManager = getCameraManager(context);
        int[] widthAndHeight = new int[]{0,0};
        if (cameraManager == null){
            Log.e(TAG,"CameraManager is null");
            return null;
        }
        try {
            StreamConfiguration[] fullSize = cameraManager.getCameraCharacteristics(cameraId).get(CameraCharacteristics.SCALER_AVAILABLE_STREAM_CONFIGURATIONS);
            StreamConfiguration largestYuvSize = null;
            for (int i = 0;i<fullSize.length;i++){
                StreamConfiguration configuration = fullSize[i];
                if (configuration.getFormat() == 35 && (float)configuration.getWidth()/configuration.getHeight() == 4f/3f){
                    if (largestYuvSize == null){
                        largestYuvSize = configuration;
                        continue;
                    }
                    if(configuration.getWidth()>largestYuvSize.getWidth()){
                        largestYuvSize = configuration;
                    }
                }
            }
            if (largestYuvSize != null){
                widthAndHeight[0] = largestYuvSize.getWidth();
                widthAndHeight[1] = largestYuvSize.getHeight();
                Log.d(TAG, "getLargestYuvSize mCaptureWidth=" + widthAndHeight[0] + ",mCaptureHeight=" + widthAndHeight[1]);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
            return widthAndHeight;
        }

        return widthAndHeight;
    }

    public static int[] getSupportBokehSize(Context context,String cameraId){
        CameraManager cameraManager = getCameraManager(context);
        int[] widthAndHeight = new int[]{0,0};
        if (cameraManager == null){
            Log.e(TAG,"CameraManager is null");
            return null;
        }
        try {
            int type = cameraManager.getCameraCharacteristics(cameraId).get(ANDROID_SPRD_3DCALIBRATION_CAPTURE_SIZE);
            Log.d(TAG, "get vendor prop persist.vendor.cam.res.bokeh = " + type + "M");
            widthAndHeight = getSupportBokehSize(type);
            Log.d(TAG, "get vendor prop mCaptureWidth=" + widthAndHeight[0] + ",mCaptureHeight=" + widthAndHeight[1]);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            return widthAndHeight;
        }

        return widthAndHeight;
    }
    private static int[] getSupportBokehSize(int type){
        switch (type){
            case 8:
                return new int[]{3264, 2448};
            case 12:
                return new int[]{4000, 3000};
            default:
                return new int[]{0,0};
        }
    }
}
