package com.sprd.cameracalibration.itemstest.camera;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.sprd.cameracalibration.BaseActivity;
import com.sprd.cameracalibration.Const;
import com.sprd.cameracalibration.utils.CameraUtil;
import com.sprd.cameracalibration.R;
/**
 * Created by SPREADTRUM\emily.miao on 19-11-14.
 */

public class ZoomCaliVeriActivity extends BaseActivity {
    private final static String TAG = "ZoomCaliVeriActivity";
    private final static String UnTest = "UnTest";
    private final static String Pass = "Pass";
    private final static String Fail = "Fail";

    private String zoomResult_stage1 = UnTest;
    private String zoomResult_stage2 = UnTest;


    protected Button zoom_1;
    protected Button zoom_2;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (mPassButton != null) {
            mPassButton.setVisibility(View.GONE);
        }
        if (mFailButton != null) {
            mFailButton.setText("Exit");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        int result = data.getIntExtra(Const.ZOOM_CALIBRATION_RESULT,-1);
        Log.d(TAG,"result from zoom stage " + resultCode + " is " +result);
        if (resultCode == Const.ZOOM_STAGE_1_RESULTCODE){
            if (result == Const.SUCCESS ){
                zoomResult_stage1 = Pass;
                zoom_1.setTextColor(Color.GREEN);
            }else if (result == Const.FAIL){
                zoomResult_stage1 = Fail;
                zoom_1.setTextColor(Color.RED);
            }
        }
        if (resultCode == Const.ZOOM_STAGE_2_RESULTCODE){
            if (result == Const.SUCCESS ){
                zoomResult_stage2 = Pass;
                zoom_2.setTextColor(Color.GREEN);
            }else if (result == Const.FAIL){
                zoomResult_stage2 = Fail;
                zoom_2.setTextColor(Color.RED);
            }
        }
        checkButtonState();
    }
    private void checkButtonState(){
        Log.d(TAG,"after test checkButtonState zoomResult_stage1 = " + zoomResult_stage1 + " zoomResult_stage2 = " + zoomResult_stage2);
        if (CameraUtil.getSupportZoomStage() == 3 && (zoomResult_stage1.equals(UnTest) || zoomResult_stage2.equals(UnTest))){
            return;
        }
        if ((CameraUtil.getSupportZoomStage() == 3 && (zoomResult_stage1.equals(Fail) || zoomResult_stage2.equals(Fail)))
                || (CameraUtil.getSupportZoomStage() == 1 && zoomResult_stage1.equals(Fail))
                || (CameraUtil.getSupportZoomStage() == 2 && zoomResult_stage2.equals(Fail))){
            if (mFailButton != null) {
                mFailButton.setText(R.string.text_fail);
            }
        }

        if ((CameraUtil.getSupportZoomStage() == 3 && zoomResult_stage1.equals(Pass) && zoomResult_stage2.equals(Pass))
                || (CameraUtil.getSupportZoomStage() == 1 && zoomResult_stage1.equals(Pass))
                || (CameraUtil.getSupportZoomStage() == 2 && zoomResult_stage2.equals(Pass))){
            if (mPassButton != null) {
                mPassButton.setVisibility(View.VISIBLE);
            }
            if (mFailButton != null) {
                mFailButton.setVisibility(View.GONE);
            }
        }
    }
}
