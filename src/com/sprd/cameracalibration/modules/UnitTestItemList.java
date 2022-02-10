package com.sprd.cameracalibration.modules;

import android.content.Context;
import com.sprd.cameracalibration.itemstest.camera.CameraCalibrationVCMActivity;
import com.sprd.cameracalibration.itemstest.camera.CameraSTL3DCalibrationActivity;
import com.sprd.cameracalibration.itemstest.camera.CameraSTL3DVerificationActivity;
import com.sprd.cameracalibration.itemstest.camera.CameraVerificationVCMActivity;
import com.sprd.cameracalibration.itemstest.camera.CameraSPWVerificationActivity;
import com.sprd.cameracalibration.itemstest.camera.CameraSPWCalibrationActivity;
import com.sprd.cameracalibration.itemstest.camera.ZoomCalibrationActivity;
import com.sprd.cameracalibration.itemstest.camera.ZoomVerificationActivity;

public class UnitTestItemList extends TestItemList {
    private static final String TAG = "UnitTestItemList";

    /**
     * This array define the order of test items.
     */
    private static final String[] FILTER_CLASS_NAMES = {
            CameraCalibrationVCMActivity.class.getName(),
            CameraVerificationVCMActivity.class.getName(),
            CameraSPWCalibrationActivity.class.getName(),
            CameraSPWVerificationActivity.class.getName(),
            ZoomCalibrationActivity.class.getName(),
            ZoomVerificationActivity.class.getName(),
            CameraSTL3DCalibrationActivity.class.getName(),
            CameraSTL3DVerificationActivity.class.getName()
    };

    private static UnitTestItemList mTestItemListInstance = null;

    public static TestItemList getInstance(Context context) {
        if (mTestItemListInstance == null) {
        	mTestItemListInstance = new UnitTestItemList(context);
        }
        return mTestItemListInstance;
    }

    private UnitTestItemList(Context context) {
        super(context);
    }

    @Override
    public String[] getfilterClassName() {
        return FILTER_CLASS_NAMES;
    }

}
