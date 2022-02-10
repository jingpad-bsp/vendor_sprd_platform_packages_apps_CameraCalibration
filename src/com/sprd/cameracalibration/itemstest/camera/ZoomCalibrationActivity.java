package com.sprd.cameracalibration.itemstest.camera;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.UserHandle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import com.sprd.cameracalibration.BaseActivity;
import com.sprd.cameracalibration.Const;
import com.sprd.cameracalibration.ListItemTestActivity;
import com.sprd.cameracalibration.utils.CameraUtil;
import com.sprd.cameracalibration.R;
/**
 * Created by SPREADTRUM\emily.miao on 19-11-9.
 */

public class ZoomCalibrationActivity extends ZoomCaliVeriActivity {
    private final static String TAG = "ZoomCalibrationActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(TAG,"onCreate");
        setContentView(R.layout.zoom_cali_stage_select);
        zoom_1 = findViewById(R.id.zoom_stage1);
        zoom_2 = findViewById(R.id.zoom_stage2);

        if (CameraUtil.getSupportZoomStage() == 1){
            zoom_2.setVisibility(View.GONE);
        }else if (CameraUtil.getSupportZoomStage() == 2){
            zoom_1.setVisibility(View.GONE);
        }

        zoom_1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ZoomCalibrationActivity.this,CameraWTCalibrationActivity.class);
                intent.putExtra("zoom_stage","1");
                startActivityForResult(intent,0);
            }
        });

        zoom_2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ZoomCalibrationActivity.this,CameraWTCalibrationActivity.class);
                intent.putExtra("zoom_stage","2");
                startActivityForResult(intent,0);
            }
        });
    }
}
