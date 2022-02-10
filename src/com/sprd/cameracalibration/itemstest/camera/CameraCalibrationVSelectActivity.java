package com.sprd.cameracalibration.itemstest.camera;
import android.app.Activity;
import android.widget.Button;
import android.content.Intent;
import android.os.Bundle;
import android.view.View.OnClickListener;
import com.sprd.cameracalibration.R;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.LinearLayout;
import android.view.Gravity;
import android.graphics.Color;
import android.view.ViewGroup;
import android.graphics.PixelFormat;

public class CameraCalibrationVSelectActivity extends Activity implements OnClickListener{
    private static final int TEXT_SIZE = 30;
    protected Button mVersion1;//7 times V1 calibration
    protected Button mVersion2;//2 times V2 calibration
    protected Button mExitButton;
    private Intent mStartCalibration;

    //com.sprd.cameracalibration/.camera.CameraCalibrationActivity
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_calibration_version);
        mStartCalibration = new Intent(this,CameraCalibrationVCMActivity.class);
        mVersion1 = (Button) findViewById(R.id.calibration_version_1);
        mVersion1.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                mStartCalibration.putExtra("camera_calibration_version","3");
                startActivity(mStartCalibration);
            }
        });
        mVersion2 = (Button) findViewById(R.id.calibration_version_2);
        mVersion2.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                mStartCalibration.putExtra("camera_calibration_version","4");
                startActivity(mStartCalibration);
            }
        });
        createButton();
    }

    public void createButton() {
        int buttonSize = getResources().getDimensionPixelSize(R.dimen.pass_fail_button_size);
        mExitButton = new Button(this);
        mExitButton.setText("Exit");
        mExitButton.setTextColor(Color.WHITE);
        mExitButton.setTextSize(TEXT_SIZE);
        mExitButton.setBackgroundColor(Color.RED);
        mExitButton.setOnClickListener(this);


        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION,
//                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.RIGHT | Gravity.BOTTOM;
        lp.flags =LayoutParams.FLAG_NOT_TOUCH_MODAL
                | LayoutParams.FLAG_NOT_FOCUSABLE;
        lp.width=buttonSize;
        lp.height=buttonSize;
        getWindowManager().addView(mExitButton, lp);
    }

    @Override
    public void onClick(View v) {
        finish();
    }

}