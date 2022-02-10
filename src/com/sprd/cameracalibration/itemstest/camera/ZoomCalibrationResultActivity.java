package com.sprd.cameracalibration.itemstest.camera;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

import com.sprd.cameracalibration.BaseActivity;
import com.sprd.cameracalibration.Const;
import com.sprd.cameracalibration.R;


/**
 * Created by SPREADTRUM\emily.miao on 19-11-11.
 */

public class ZoomCalibrationResultActivity extends ZoomBaseActivity {
    private static final String TAG = "ZoomCalibrationResultActivity";

    public static final String KEY_TEST_RESULT = "calibration_test_result";
    public static final String KEY_TEST_DATA = "calibration_test_data";
    public static final String KEY_TEST_TIME = "calibration_test_time";

    private TextView mTestResultView = null;
    private TextView mTestDataView = null;
    private TextView mTestTimeView = null;

    private void showPassOrFail(boolean isPass) {
        Log.d(TAG, "showPassOrFail isPass=" + isPass);
        if(mPassButton != null){
            mPassButton.setVisibility(isPass ? View.VISIBLE : View.INVISIBLE);
        }
        if(mFailButton != null){
            mFailButton.setVisibility(isPass ? View.INVISIBLE : View.VISIBLE);
        }
    }
    private void initView() {
        mTestResultView = (TextView) findViewById(R.id.calibration_test_result);
        mTestDataView = (TextView) findViewById(R.id.calibration_test_data);
        mTestTimeView = (TextView) findViewById(R.id.calibration_test_time);
    }

    private void showCalibrationRsult() {
        if (mTestResultView == null || mTestDataView == null) {
            Log.w(TAG, "mTestResultView == null || mTestDataView == null");
            return;
        }
        Intent intent = getIntent();
        if (intent == null) {
            Log.w(TAG, "intent == null");
            return;
        }
        int testResult = intent.getIntExtra(KEY_TEST_RESULT, -1);
        String testData = intent.getStringExtra(KEY_TEST_DATA);
        long testTime = intent.getLongExtra(KEY_TEST_TIME,0);
        String testName = intent.getStringExtra(Const.INTENT_PARA_TEST_NAME);
        Log.d(TAG, "showCalibrationRsult testResult=" + testResult
                + "\n testData=" + testData + "\n testTime="+ testTime);
        showPassOrFail(testResult == 0);
        if (testResult == 0) {
            mTestResultView.setTextColor(Color.GREEN);
            mTestResultView.setText(getString(R.string.TestResultTitleString)
                    + ": PASS");
        } else {
            mTestResultView.setTextColor(Color.RED);
            mTestResultView.setText(getString(R.string.TestResultTitleString)
                    + ": FAIL");
        }
        mTestDataView.setText(testData);
        if (testName != null && testName.contains("Verification")) {
            mTestTimeView.setText("Verification time : " + testTime + " ms");
        } else {
            mTestTimeView.setText("Calibration time : " + testTime + " ms");
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_calibration);
        initView();
        showCalibrationRsult();
    }

}
