package com.sprd.cameracalibration.itemstest.camera;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.sprd.cameracalibration.Const;
import com.sprd.cameracalibration.R;

/**
 * Created by SPREADTRUM\emily.miao on 19-11-13.
 */

public class ZoomBaseActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "ZoomBaseActivity";
    private String mTestname = null;

    protected Button mPassButton;
    protected Button mFailButton;
    private static final int TEXT_SIZE = 30;
    protected boolean canPass = true;
    protected WindowManager mWindowManager;
    protected long time;

    protected boolean isSuccess;
    //send broadcast return phasecheck result
    private static final String ACTION_SAVE_PHASECHECK = "com.sprd.validationtools.SAVE_PHASECHECK";
    private static final String SAVE_PHASECHECK_STATION_NAME = "station_name";
    private static final String SAVE_PHASECHECK_RESULT = "station_result";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTestname = this.getIntent().getStringExtra(Const.INTENT_PARA_TEST_NAME);
        time = System.currentTimeMillis();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mWindowManager = getWindowManager();
        createButton(true);
        createButton(false);
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
    }

    @Override
    protected void onDestroy() {
        removeButton();
        if (mTestname != null) {
            Log.d("APK_MMI", "*********** " + mTestname + " Time: "
                    + (System.currentTimeMillis() - time) / 1000
                    + "s ***********");
        }
        super.onDestroy();
    }

    public void createButton(boolean isPassButton) {
        int buttonSize = getResources().getDimensionPixelSize(
                R.dimen.pass_fail_button_size);
        if (isPassButton) {
            mPassButton = new Button(this);
            mPassButton.setText(R.string.text_pass);
            mPassButton.setTextColor(Color.WHITE);
            mPassButton.setTextSize(TEXT_SIZE);
            mPassButton.setBackgroundColor(Color.GREEN);
            mPassButton.setOnClickListener(this);
        } else {
            mFailButton = new Button(this);
            mFailButton.setText(R.string.text_fail);
            mFailButton.setTextColor(Color.WHITE);
            mFailButton.setTextSize(TEXT_SIZE);
            mFailButton.setBackgroundColor(Color.RED);
            mFailButton.setOnClickListener(this);
        }

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION,
                // WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        lp.gravity = isPassButton ? Gravity.LEFT | Gravity.BOTTOM
                : Gravity.RIGHT | Gravity.BOTTOM;
        lp.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        lp.width = buttonSize;
        lp.height = buttonSize;
        mWindowManager.addView(isPassButton ? mPassButton : mFailButton, lp);
    }

    public void storeRusult(boolean isSuccess) {
    }


    @Override
    public void finish() {
        super.finish();
    }

    protected void removeButton() {
        if(mPassButton != null){
            mWindowManager.removeViewImmediate(mPassButton);
            mPassButton = null;
        }
        if(mFailButton != null){
            mWindowManager.removeViewImmediate(mFailButton);
            mFailButton = null;
        }
    }

    @Override
    public void onBackPressed() {
    }

    @Override
    public void onClick(View v) {
        if (v == mPassButton) {
            if (canPass) {
                Log.d("onclick", "pass.." + this);
                isSuccess = true;
                storeRusult(true);
                finish();
            } else {
                Toast.makeText(this, R.string.can_not_pass, Toast.LENGTH_SHORT)
                        .show();
            }
        } else if (v == mFailButton) {
            Log.d("onclick", "false.." + this);
            isSuccess = false;
            storeRusult(false);
            finish();
        }
    }
}
