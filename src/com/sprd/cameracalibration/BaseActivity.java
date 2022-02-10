package com.sprd.cameracalibration;

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
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.Toast;

public class BaseActivity extends Activity implements OnClickListener {
    private static final String TAG = "BaseActivity";
    private String mTestname = null;
    public static boolean shouldCanceled = true;

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

    public void hideNavigationBar() {
        View decorView = getWindow().getDecorView();
        if (Build.VERSION.SDK_INT > 11 && Build.VERSION.SDK_INT < 19) {
            decorView.setSystemUiVisibility(View.GONE);
        } else if (Build.VERSION.SDK_INT >= 19) {
            // for new api versions. View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
        }
    }

    public int getHeight(Context context) {
        WindowManager windowManager = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        DisplayMetrics dm = new DisplayMetrics();
        display.getMetrics(dm);
        int height = dm.heightPixels;
        return height;
    }

    public int getRealHeight(Context context) {
        WindowManager windowManager = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        DisplayMetrics dm = new DisplayMetrics();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            display.getRealMetrics(dm);
        } else {
            display.getMetrics(dm);
        }
        int realHeight = dm.heightPixels;
        return realHeight;
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
        lp.flags = LayoutParams.FLAG_NOT_TOUCH_MODAL
                | LayoutParams.FLAG_NOT_FOCUSABLE;
        lp.width = buttonSize;
        lp.height = buttonSize;
        mWindowManager.addView(isPassButton ? mPassButton : mFailButton, lp);
    }

    public void storeRusult(boolean isSuccess) {
        Log.d(TAG, "storeResult" + mTestname);
        Log.d(TAG, "storeResult" + getClass().getName());
        String phasecheckName = Const.getPhasecheckName(getClass().getName());
        if (phasecheckName == null){
            Log.e(TAG,"phasecheckName is null");
            return;
        }
        Intent intent = new Intent(ACTION_SAVE_PHASECHECK);
        intent.putExtra(SAVE_PHASECHECK_STATION_NAME,Const.getPhasecheckName(getClass().getName()));
        intent.putExtra(SAVE_PHASECHECK_RESULT,(isSuccess ? "0" : "1"));
        intent.setPackage("com.sprd.validationtools");
        sendBroadcast(intent);
        Log.d(TAG,"send broadcast return phasecheck result");
    }


    @Override
    public void finish() {
        //removeButton();
        // return result intent to ListItemTestActivity
        Intent intent = getIntent();
        if (isSuccess)
            intent.putExtra("isSuccess",Const.SUCCESS);
        else
            intent.putExtra("isSuccess",Const.FAIL);
        this.setResult(Const.TEST_ITEM_DONE, intent);
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
