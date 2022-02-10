package com.sprd.cameracalibration.itemstest.camera;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;

import com.sprd.cameracalibration.R;

public class CameraCaliAfterSalesSelectActivity extends Activity implements
        OnClickListener {
    private static final int TEXT_SIZE = 30;
    protected Button mVersion1;// V1 calibration
    protected Button mVersion2;// V2 calibration
    protected Button mExitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_cali_version_aftersales);
        mVersion1 = (Button) findViewById(R.id.calibration_version_1);
        mVersion1.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Intent intent = new Intent(CameraCaliAfterSalesSelectActivity.this, CameraCalibrationAftersalesActivity.class);
                startActivity(intent);
            }
        });
        mVersion2 = (Button) findViewById(R.id.calibration_version_2);
        mVersion2.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                Intent intent = new Intent(CameraCaliAfterSalesSelectActivity.this, CameraCalibrationAftersales2Activity.class);
                startActivity(intent);
            }
        });
        createButton();
    }

    public void createButton() {
        int buttonSize = getResources().getDimensionPixelSize(
                R.dimen.pass_fail_button_size);
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
                // WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.RIGHT | Gravity.BOTTOM;
        lp.flags = LayoutParams.FLAG_NOT_TOUCH_MODAL
                | LayoutParams.FLAG_NOT_FOCUSABLE;
        lp.width = buttonSize;
        lp.height = buttonSize;
        getWindowManager().addView(mExitButton, lp);
    }

    @Override
    public void onClick(View v) {
        finish();
    }

}