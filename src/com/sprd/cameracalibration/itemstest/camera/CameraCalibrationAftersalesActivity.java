package com.sprd.cameracalibration.itemstest.camera;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaActionSound;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.sprd.cameracalibration.BaseActivity;
import com.sprd.cameracalibration.R;
import com.sprd.cameracalibration.utils.CameraUtil;
import com.sprd.cameracalibration.utils.Counter;
import com.sprd.cameracalibration.utils.FileUtils;
import com.sprd.cameracalibration.utils.ImageUtils;
import com.sprd.cameracalibration.utils.OtpDataUtil;
import com.sprd.cameracalibration.utils.StorageUtil;

import android.os.SystemProperties;
import android.preference.PreferenceManager;

public class CameraCalibrationAftersalesActivity extends BaseActivity {

    private static final String TAG = "CameraCalibrationAftersalesActivity";
    // /storage/emulated/0/cali/aftersales/
    private static final String YUV_TMP_PATH = StorageUtil.getInternalStoragePath() + "/cali/aftersales/";
    private static final String LEFT_YUV_PATH_VCM = YUV_TMP_PATH
            + "yuv_left_cali_aftersales_vcm";
    private static final String RIGHT_YUV_PATH_VCM = YUV_TMP_PATH
            + "yuv_right_cali_aftersales_vcm";
    private static final String CALIBRATION_RESULT = YUV_TMP_PATH
            + "calibration_aftersales_result.txt";

    private static final String LEFT_CAMERA_ID = "20";
    private static String RIGHT_CAMERA_ID = "2";

    private static final int LEFT_CAMERA_CAPTURE_WIDTH = 4000;
    private static final int LEFT_CAMERA_CAPTURE_HEIGHT = 3000;
    private static final int RIGHT_CAMERA_CAPTURE_WIDTH = 800;
    private static final int RIGHT_CAMERA_CAPTURE_HEIGHT = 600;

    private int mLeftCaptureWidth = LEFT_CAMERA_CAPTURE_WIDTH;
    private int mLeftCaptureHeight = LEFT_CAMERA_CAPTURE_HEIGHT;
    private int mRightCaptureWidth = RIGHT_CAMERA_CAPTURE_WIDTH;
    private int mRightCaptureHeight = RIGHT_CAMERA_CAPTURE_HEIGHT;

    private Handler mHandler1;
    private HandlerThread mThreadHandler1;
    private Handler mHandler2;
    private HandlerThread mThreadHandler2;

    private CaptureRequest.Builder mPreviewBuilder1;
    private CaptureRequest.Builder mPreviewBuilder2;

    private TextureView mPreviewView1;
    private TextureView mPreviewView2;

    private CameraDevice mCameraDevice1;
    private CameraDevice mCameraDevice2;

    private CameraCaptureSession mSession1;
    private CameraCaptureSession mSession2;

    private Size mPreviewSize;

    private ImageReader mImageReader1;
    private ImageReader mImageReader2;

    private int mState;
    private static final int STATE_PREVIEW = 0;
    private static final int STATE_WAITING_CAPTURE = 1;
    private static final int STATE_SRART_CAPTURE = 2;
    private static final int STATE_SRART_CHECK_VCM = 3;
    private static final int STATE_START_CHECK_SENDRESULT = 4;

    private TextView mTextView1;

    private boolean picReady1 = false;
    private boolean picReady2 = false;

    private MediaActionSound mCameraSound;

    private Button mTakePhotoBtn = null;

    private OtpDataUtil mOtpDataUtil = new OtpDataUtil();
    /**
     * A {@link Semaphore} to prevent the app from exiting before closing the
     * camera.
     */
    private Semaphore mCameraOpenCloseLock1 = new Semaphore(1);
    private Semaphore mCameraOpenCloseLock2 = new Semaphore(1);

    public static final CaptureRequest.Key<byte[]> ANDROID_SPRD_CALIBRATION_OTPDATA = new CaptureRequest.Key<byte[]>(
            "com.addParameters.otpData",byte[].class);

    public static final CaptureResult.Key<Integer> ANDROID_SPRD_CALIBRATION_OTP_SENDRESULT = new CaptureResult.Key<Integer>(
            "com.addParameters.otpDataSendResult",Integer.class);

    public static final CaptureResult.Key<Integer> ANDROID_SPRD_DUAL_OTP_FLAG = new CaptureResult.Key<Integer>("com.addParameters.sprdDualOtpFlag", Integer.class);
    public static final CaptureResult.Key<byte[]> CONTROL_OTP_VALUE = new CaptureResult.Key<byte[]>("com.addParameters.OtpValue", byte[].class);

    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener1 = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
//            mLeftYuvPrepare = true;
            Log.d(TAG, "mOnImageAvailableListener1 mLeftYuvPrepare!");
            mHandler1.post(new ImageSaver(reader.acquireNextImage(), new File(
                    LEFT_YUV_PATH_VCM + mVCMIndex + ".yuv")));
        }

    };

    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener2 = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
//            mRightYuvPrepare = true;
            Log.d(TAG, "mOnImageAvailableListener2 mRightYuvPrepare!");
            mHandler2.post(new ImageSaver(reader.acquireNextImage(), new File(
                    RIGHT_YUV_PATH_VCM + mVCMIndex + ".yuv")));
        }

    };

    public final static String TEST_COUNT_FAIL = "test_count_fail";
    private SharedPreferences mPrefs = null;
    public int getTestCountFail(){
        int testCount = mPrefs.getInt(TEST_COUNT_FAIL, 0);
        Log.d(TAG, "getTestCount testCount="+testCount);
        return testCount;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_3d_calibration);
        mTextView1 = (TextView) findViewById(R.id.txt_left);
        mTextView1.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // startCalibration();
            }
        });
        mTakePhotoBtn = (Button) findViewById(R.id.start_take_picture);
        mTakePhotoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "mTakePhotoBtn take picture 1 is start");
                if(mHandler != null){
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mTakePhotoBtn.setEnabled(false);
                            showCaptureAlermDialog("Capture picture...", "Don't move phone!", Color.YELLOW, Color.RED);
                        }
                    });
                    mHandler.removeMessages(MSG_DISMISS_DIALOG);
                    Message message = Message.obtain();
                    message.what = MSG_DISMISS_DIALOG;
                    mHandler.sendMessageDelayed(message, 120000);
                }
                mState = STATE_WAITING_CAPTURE;
            }
        });
        mTakePhotoBtn.setEnabled(true);
        if (mCameraSound == null) {
            mCameraSound = new MediaActionSound();
            mCameraSound.load(MediaActionSound.SHUTTER_CLICK);
            mCameraSound.load(MediaActionSound.FOCUS_COMPLETE);
        }

        String type = SystemProperties.get("persist.sys.cam3.type", "unknown");
        Log.d(TAG, "onCreate cam3.type=" + type);
        if ("back_bokeh".equals(type) || "front_bokeh".equals(type)) {
            RIGHT_CAMERA_ID = SystemProperties.get(
                    "persist.sys.cam3.multi.cam.id", "2");
        }
        Log.d(TAG, "onCreate RIGHT_CAMERA_ID =" + RIGHT_CAMERA_ID);

        /* SPRD bug 857711:Maybe cause ANR */
        mPreviewView1 = (TextureView) findViewById(R.id.sur_left);
        mPreviewView2 = (TextureView) findViewById(R.id.sur_right);
        /* @} */
        if (mPassButton != null) {
            mPassButton.setVisibility(View.GONE);
        }
        if (mFailButton != null) {
            mFailButton.setText("Exit");
        }
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
//        mCalibrationBegin = false;
        try {
            FileUtils.deleteDir(YUV_TMP_PATH);
            File mmi = new File(YUV_TMP_PATH);
            if(mmi != null && !mmi.exists()){
                boolean res = mmi.mkdirs();
                if(!res){
                    Log.d(TAG, "mkdirs fail:"+YUV_TMP_PATH);
                }
            }
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
        initVCM();
        getCaliAfterSalesFromConf();
        Log.d(TAG, "I'm 10.6");
    }

    private final static String CALI_AFTER_SALES_CONF_FILE = "/system/etc/otpdata/sell_aft_cali.txt";
    private int[] mCaliAfterSalesValues = null;
    private int mAftCalibrationCount = 0;
    private int mAftCalibrationFlag = 0;
    private int [] mAftCalibrationVcmIndexs = null;
    private int getCaliAfterSalesFromConf(){
        int count = 0;
        String text = null;
        File file = null;
        InputStream fIn = null;
        InputStreamReader isr = null;
        try {
            file = new File(CALI_AFTER_SALES_CONF_FILE);
            fIn = new FileInputStream(file);
            isr = new InputStreamReader(fIn,Charset.defaultCharset());
            InputStreamReader inputreader = new InputStreamReader(fIn);
            BufferedReader buffreader = new BufferedReader(inputreader);
            String line;
            int lineInt = -1;
            ArrayList<String> tmp = new ArrayList<String>();
            while (( line = buffreader.readLine()) != null) {
                Log.d(TAG, "getCaliAfterSalesFromConf line="+line);
                tmp.add(line);
                count ++;
            }
            Log.d(TAG, "getCaliAfterSalesFromConf count="+count);
            int i = 0;
            if(count > 0){
                mCaliAfterSalesValues = new int[count];
                for(String tmpLine : tmp){
                    mCaliAfterSalesValues[i++] = Integer.valueOf(tmpLine);
                }
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return 0;
        }finally{
            try {
                isr.close();
                fIn.close();
                isr = null;
                fIn = null;
            } catch (Exception e2) {
                // TODO: handle exception
                e2.printStackTrace();
            }
        }
        if(mCaliAfterSalesValues != null){
            for(int aftValue : mCaliAfterSalesValues){
                Log.d(TAG, "getCaliAfterSalesFromConf aftValue="+aftValue + "\n");
            }
        }
        return count;
    }

    private boolean checkIsUseGoldenOtp(){
        if(mCaliAfterSalesValues != null){
            int userGoldenOtp = mCaliAfterSalesValues[0];
            Log.d(TAG, "checkIsUseGoldenOtp userGoldenOtp="+userGoldenOtp + "\n");
            return userGoldenOtp == 1;
        }
        Log.d(TAG, "checkIsUseGoldenOtp return false!");
        return false;
    }

    private boolean parseVCMData(byte[] otpdata) {
        if(otpdata == null){
            Log.d(TAG, "otpdata == NULL!");
            return false;
        }
        try {
            int otpLength = otpdata.length;
            int vcmCount = (otpLength - 16) / 256;
            Log.d(TAG, "parseVCMDataFromOtpd vcmCount="+vcmCount+",otpLength="+otpLength);
            mVCMValues = new int[vcmCount];
            mVCMCount = vcmCount;
            for(int i=0;i<vcmCount;i++){
                int offset = 16 + 252 * (i + 1) + 4 * i;
                mVCMValues[i] = OtpDataUtil.bytesToInt(otpdata, offset);
                Log.d(TAG, "parseVCMDataFromOtpd mVCMValues[i]="+mVCMValues[i]+",i="+i+",offset="+offset);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static final String DST_OTP_BIN_FILE = YUV_TMP_PATH + "dualcamera.bin";
    private void saveOtpdData(byte[] srcData){
        try {
            if(srcData == null){
                return;
            }
            int length = srcData.length;
            Log.d(TAG, "saveOtpdData length="+length);
            if(length <= 32){
                return;
            }
            byte[] dstData = new byte[length];
            System.arraycopy(srcData, 0, dstData, 0, length);
            for(int i=0;i<length;i += 4){
                int tempInt  = OtpDataUtil.bytesToInt(dstData, i);
                Log.d(TAG, "saveOtpdData tempInt="+tempInt +"\n");
            }
            dumpData(dstData, DST_OTP_BIN_FILE);
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
    }

    private byte[] otpByte = null;
    private boolean mFlagOne = false;
    private void saveOtpFile(CaptureResult result){
        int mDualOtpFlag = result.get(ANDROID_SPRD_DUAL_OTP_FLAG);
        if(mDualOtpFlag != 1){
            if(!mFlagOne){
                Log.i(TAG, "saveOtpFile 2 mDualOtpFlag = "
                        + mDualOtpFlag);
                mFlagOne = true;
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        showAlertDialog("Camera verifying", "Camera verifying FAIL!\nOTP flag == 0!");
                    }
                });
            }
            return;
        }
        boolean hasDstResultBin = checkFileExists(DST_OTP_BIN_FILE);
        if(hasDstResultBin){
            return;
        }
        Log.i(TAG, "saveOtpFile 2 mDualOtpFlag = "+ mDualOtpFlag);
        otpByte = result.get(CONTROL_OTP_VALUE);
        saveOtpdData(otpByte);
    }
    /*
        private Counter mCounterCpatureAll:
            add for ensure mAftCalibrationCount*2 yuv pictures all are saved before start calibration
        private Counter mCounterCpatureOnce:
            add for ensure 2 yuv pictures all are saved with every vcm,only 2 yuv pictures all are saved we can set next vcm.
            mCounterCpatureOnce.count() should excute after yuv file saved,because name yuv file needs mVCMIndex,so we can't mCounterCpatureOnce.count() in onImageAvailable
     */
    private Counter mCounterCpatureAll;
    private Counter mCounterCpatureOnce;
    class VCMAsyncTask extends AsyncTask<Void, Void, Void>{

        @Override
        protected Void doInBackground(Void... params) {
            // TODO Auto-generated method stub
            Log.d(TAG, "VCMAsyncTask doInBackground");
            mVCMIndex = 0;
            Log.d(TAG, "VCMAsyncTask doInBackground mVCMCount="+mVCMCount);
            if(mCaliAfterSalesValues == null){
                 Log.d(TAG, "VCMAsyncTask mCaliAfterSalesValues == null!");
                return null;
            }
            if(checkIsUseGoldenOtp()){
                calibrationByGoldenOtp();
                return null;
            }else{
                mAftCalibrationCount = mCaliAfterSalesValues[1];
                Log.d(TAG, "VCMAsyncTask doInBackground mAftCalibrationCount="+mAftCalibrationCount);
                if(mAftCalibrationCount > 0){
                    mAftCalibrationVcmIndexs = new int[mAftCalibrationCount];
                    for(int i = 0; i < mAftCalibrationCount; i++){
                        mAftCalibrationVcmIndexs[i] = mCaliAfterSalesValues[2 + i];
                    }

                    boolean ret = parseVCMData(otpByte);
                    Log.d(TAG, "VCMAsyncTask readCalibrationOtp ret="+ret+",mVCMCount="+mVCMCount);
                    if(ret == false || mVCMCount <= 0){
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                showComfirmDialog(getString(R.string.camera_calibration_aftersales_diaglog_text));
                            }
                        });
                        return null;
                    }
                    if(mVCMCount > 0){
                        for(int i=0;i<mVCMCount;i++){
                            if(mVCMValues[i] <= 0){
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        showComfirmDialog(getString(R.string.camera_calibration_aftersales_diaglog_text));
                                    }
                                });
                                return null;
                            }
                        }
                    }
                }else{
                    return null;
                }
            }

            Log.d(TAG, "VCMAsyncTask doInBackground start!");
            mCounterCpatureAll = new Counter(mAftCalibrationCount*2);
            for(int j=0;j<mAftCalibrationCount;j++){
                int dstVCMindex = mAftCalibrationVcmIndexs[j];
                Log.d(TAG, "VCMAsyncTask doInBackground22 dstVCMindex="+dstVCMindex);
                for(int i=0;i< mVCMCount;i++){
                    if(mPause){
                        Log.d(TAG, "VCMAsyncTask doInBackground22 mPause!");
                        return null;
                    }
                    if(i != dstVCMindex){
                        Log.d(TAG, "VCMAsyncTask doInBackground22 NOT dstVCMindex="+dstVCMindex);
                        continue;
                    }
                    mAftCalibrationFlag = j;
                    setVCMTimeout = 50;
//                    mCalibrationDone = false;
                    mCounterCpatureOnce = new Counter(2);
                    mState = STATE_SRART_CAPTURE;
                    mVCMIndex = i;
                    setFocusOff();
                    Log.d(TAG, "VCMAsyncTask doInBackground22 i="+i);
                    try {
                        Thread.sleep(100);
                    } catch (Exception e) {
                        // TODO: handle exception
                    }
                    checkVCMstate();
                    Log.d(TAG, "VCMAsyncTask doInBackground223333 i="+i);
//                    while (!mCalibrationDone) {
//                        try {
//                            Thread.sleep(10);
//                        } catch (Exception e) {
//                            // TODO: handle exception
//                        }
//                        continue;
//                    }
                    mCounterCpatureOnce.waitCount();
                    Log.d(TAG, "VCMAsyncTask mVCMIndex="+mVCMIndex);
                }
            }
            mCounterCpatureAll.waitCount();
            startCalibrationAfterCapture();
            return null;
        }
    }

    static class CompareSizesByArea implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight()
                    - (long) rhs.getWidth() * rhs.getHeight());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mPause = false;
        initLooper();
        // When the screen is turned off and turned back on, the SurfaceTexture
        // is already
        // available, and "onSurfaceTextureAvailable" will not be called. In
        // that case, we can open a camera and start preview from here
        // (otherwise, we wait until the
        // surface is ready in the SurfaceTextureListener).
        /* SPRD bug 857711:Maybe cause ANR */
        if (mPreviewView1.isAvailable()) {
            openCamera(LEFT_CAMERA_ID);
        } else {
            startTestPreviewView1();
        }
        if (mPreviewView2.isAvailable()) {
            openCamera(RIGHT_CAMERA_ID);
        } else {
            startTestPreviewView2();
        }
        /* @} */
    }

    private void startTestPreviewView2() {
        mPreviewView2
                .setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {

                    @Override
                    public void onSurfaceTextureAvailable(
                            SurfaceTexture surface, int width, int height) {
                        Log.d(TAG, "p2 in");
                        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
                        CameraCharacteristics characteristics = null;
                        try {
                            characteristics = cameraManager
                                    .getCameraCharacteristics(RIGHT_CAMERA_ID);
                        } catch (CameraAccessException e) {
                            Log.d(TAG, "p2 in" + e.toString());
                            Toast.makeText(getApplicationContext(),
                                    R.string.text_fail, Toast.LENGTH_SHORT)
                                    .show();
                            storeRusult(false);
                            finish();
                            return;
                        }

                        StreamConfigurationMap map = characteristics
                                .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                        mPreviewSize = Collections.max(Arrays.asList(map
                                .getOutputSizes(ImageFormat.JPEG)),
                                new CompareSizesByArea());
                        openCamera(RIGHT_CAMERA_ID);
                    }

                    @Override
                    public void onSurfaceTextureSizeChanged(
                            SurfaceTexture surface, int width, int height) {

                    }

                    @Override
                    public boolean onSurfaceTextureDestroyed(
                            SurfaceTexture surface) {
                        Log.i(TAG,
                                "mPreviewView2.setSurfaceTextureListener stay onSurfaceTextureDestroyed");
                        return false;
                    }

                    @Override
                    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
                    }
                });
    }

    public void startTestPreviewView1() {
        mPreviewView1
                .setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {

                    @Override
                    public void onSurfaceTextureAvailable(
                            SurfaceTexture surface, int width, int height) {
                        Log.d(TAG, "p1 in");
                        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
                        CameraCharacteristics characteristics = null;
                        try {
                            characteristics = cameraManager
                                    .getCameraCharacteristics(LEFT_CAMERA_ID);
                        } catch (CameraAccessException e) {
                            Log.d(TAG, "p1 in" + e.toString());
                            Toast.makeText(getApplicationContext(),
                                    R.string.text_fail, Toast.LENGTH_SHORT)
                                    .show();
                            storeRusult(false);
                            finish();
                            return;
                        }
                        StreamConfigurationMap map = characteristics
                                .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                        mPreviewSize = Collections.max(Arrays.asList(map
                                .getOutputSizes(ImageFormat.JPEG)),
                                new CompareSizesByArea());
                        openCamera(LEFT_CAMERA_ID);
                    }

                    @Override
                    public void onSurfaceTextureSizeChanged(
                            SurfaceTexture surface, int width, int height) {

                    }

                    @Override
                    public boolean onSurfaceTextureDestroyed(
                            SurfaceTexture surface) {
                        Log.i(TAG,
                                "mPreviewView1.setSurfaceTextureListener stay onSurfaceTextureDestroyed");
                        return false;
                    }

                    @Override
                    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
                    }
                });
    }

    /* SPRD bug 857711:Maybe cause ANR */
    private void openCamera(String cameraId) {
        Log.d(TAG, "openCamera cameraId=" + cameraId);
        CameraManager manager = (CameraManager) this
                .getSystemService(Context.CAMERA_SERVICE);
        try {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(), "not permission",
                        Toast.LENGTH_LONG).show();
                return;
            }
            if (LEFT_CAMERA_ID.equals(cameraId)) {
                if (!mCameraOpenCloseLock1.tryAcquire(2500,
                        TimeUnit.MILLISECONDS)) {
                    throw new RuntimeException(
                            "Time out waiting to lock camera opening.");
                }
                manager.openCamera(cameraId, mCameraDeviceStateCallback1,
                        mHandler1);
            } else if (RIGHT_CAMERA_ID.equals(cameraId)) {
                if (!mCameraOpenCloseLock2.tryAcquire(2500,
                        TimeUnit.MILLISECONDS)) {
                    throw new RuntimeException(
                            "Time out waiting to lock camera opening.");
                }
                manager.openCamera(cameraId, mCameraDeviceStateCallback2,
                        mHandler2);
            } else {
                Log.e(TAG, "Wrong camera ID");
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private boolean mPause = false;
    @Override
    public void onPause() {
        Log.d(TAG, "onPause start!");
        mPause = true;
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.cancel();
            mDialog = null;
        }
        dismissDialog();
        Log.d(TAG, "onPause start! 111");
        /* SPRD bug 857711:Maybe cause ANR */
        closeCamera();
        /* @} */
        Log.d(TAG, "onPause start! 222");
        stopLooper();
        Log.d(TAG, "onPause end!");
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (mCameraSound != null) {
            mCameraSound.release();
            mCameraSound = null;
        }
        /* SPRD bug 857711:Maybe cause ANR */
        if (mHandler != null) {
            mHandler.removeMessages(MSG_DISMISS_DIALOG);
            mHandler.removeMessages(MSG_FAIL);
            mHandler.removeMessages(MSG_PASS);
            mHandler = null;
        }
        /* @} */
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
    }

    private void initLooper() {
        Log.i(TAG, "initLooper in");
        mThreadHandler1 = new HandlerThread("calibration1");
        mThreadHandler1.start();
        mHandler1 = new Handler(mThreadHandler1.getLooper());
        mThreadHandler2 = new HandlerThread("calibration2");
        mThreadHandler2.start();
        mHandler2 = new Handler(mThreadHandler2.getLooper());
        Log.i(TAG, "initLooper out");
    }

    private void stopLooper() {

        try {
            mThreadHandler1.quit();
            mThreadHandler1.join();
            mThreadHandler1 = null;
            mHandler1 = null;

            mThreadHandler2.quit();
            mThreadHandler2.join();
            mThreadHandler2 = null;
            mHandler2 = null;
        } catch (Exception e) {
            Log.d(TAG, "StopLooper" + e.toString());
            e.printStackTrace();
        }

    }

    private void closeCamera() {
        try {
            mCameraOpenCloseLock1.acquire();
            if (null != mSession1) {
                mSession1.close();
                mSession1 = null;
            }
            if (null != mCameraDevice1) {
                mCameraDevice1.close();
                mCameraDevice1 = null;
            }
            if (null != mImageReader1) {
                mImageReader1.close();
                mImageReader1 = null;
            }
        } catch (Exception e) {
            Log.d(TAG, e.toString());
            e.printStackTrace();
        } finally {
            if(mCameraOpenCloseLock1 != null){
                mCameraOpenCloseLock1.release();
            }
        }
        try {
            mCameraOpenCloseLock2.acquire();
            if (null != mSession2) {
                mSession2.close();
                mSession2 = null;
            }
            if (null != mCameraDevice2) {
                mCameraDevice2.close();
                mCameraDevice2 = null;
            }
            if (null != mImageReader2) {
                mImageReader2.close();
                mImageReader2 = null;
            }
        } catch (Exception e) {
            Log.d(TAG, e.toString());
            e.printStackTrace();
        } finally {
            if(mCameraOpenCloseLock2 != null){
                mCameraOpenCloseLock2.release();
            }
        }
    }

    private CameraDevice.StateCallback mCameraDeviceStateCallback1 = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(CameraDevice camera) {
            Log.i(TAG, "CameraDevice.StateCallback1 onOpened in");
            mCameraOpenCloseLock1.release();
            mCameraDevice1 = camera;
            startPreview1(camera);
            Log.i(TAG, "CameraDevice.StateCallback1 onOpened out");
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            mCameraOpenCloseLock1.release();
            camera.close();
            Log.i(TAG, "CameraDevice.StateCallback1 stay onDisconnected");
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            mCameraOpenCloseLock1.release();
            camera.close();
            Log.i(TAG, "CameraDevice.StateCallback1 stay onError");
        }
    };

    private CameraDevice.StateCallback mCameraDeviceStateCallback2 = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(CameraDevice camera) {
            Log.i(TAG, "CameraDevice.StateCallback2 onOpened in");
            mCameraOpenCloseLock2.release();
            mCameraDevice2 = camera;
            //Wait 200ms for Main camera preview OK.
            try {
                Thread.sleep(200);
            } catch (Exception e) {
                // TODO: handle exception
            }
            startPreview2(camera);
            Log.i(TAG, "CameraDevice.StateCallback2 onOpened out");
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            mCameraOpenCloseLock2.release();
            camera.close();
            Log.i(TAG, "mCameraDeviceStateCallback2 stay onDisconnected");
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            mCameraOpenCloseLock2.release();
            camera.close();
            Log.i(TAG, "mCameraDeviceStateCallback2 stay onError");
        }
    };

    private int setVCMTimeout = 20; //times
    private void checkState(CaptureResult result) {
        switch (mState) {
        case STATE_PREVIEW:
            // NOTHING
            break;
        case STATE_WAITING_CAPTURE:
            mState = STATE_SRART_CHECK_VCM;
            break;
        case STATE_SRART_CAPTURE: {
            int vcmResult = 0;
            try {
                vcmResult = result.get(ANDROID_SPRD_CALIBRATION_VCM_RESULT);
            } catch (Exception e) {
                e.printStackTrace();
            }
            Log.d(TAG, "checkState vcmResult="+vcmResult);
            final int vcmRes = vcmResult;
            if(vcmResult == 0x2){
                mState = STATE_PREVIEW;
                try {
                    int timeoutInt = 500;
                    try {
                        String timeout = SystemProperties.get(PROP_CAPTURE_TIMEOUT , "500");
                        Log.d(TAG, "take picture timeout="+timeout);
                        timeoutInt = Integer.valueOf(timeout);
                    } catch (Exception e) {
                        // TODO: handle exception
                        e.printStackTrace();
                    }
                    Log.d(TAG, "take picture timeoutInt="+timeoutInt);
                    Thread.sleep(timeoutInt);
                } catch (Exception e) {
                    // TODO: handle exception
                    e.printStackTrace();
                }
                Log.d(TAG, "take picture mVCMIndex="+mVCMIndex);
                Log.d(TAG, "take picture 2 is start");
                captureStillPicture2();
                Log.d(TAG, "take picture 1 is start");
                captureStillPicture();
            }else{
                setVCMTimeout --;
                if(setVCMTimeout <= 0){
                    mState = STATE_PREVIEW;
                    try {
                        Thread.sleep(50);
                    } catch (Exception e) {
                        // TODO: handle exception
                    }
                    Log.d(TAG, "checkVCMCallbackSetter onCaptureProgressed set VCM not done yet!");
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            // TODO Auto-generated method stub
                            StringBuffer buffer = new StringBuffer();
                            buffer.append("Calibration fail vcm result : "  + vcmRes + "\n");
                            int i = 0;
                            for(int vcm : mVCMValues){
                                buffer.append("vcm"  + i + " = " + vcm + "\n");
                                i ++;
                            }
                            saveCalibrationResult(buffer.toString());
                            try {
                                // Go to display activity
                                showCalibrationResult(-1, buffer.toString());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }
        }
            break;
        case STATE_SRART_CHECK_VCM:
            mState = STATE_PREVIEW;
            VCMAsyncTask asyncTask = new VCMAsyncTask();
            asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
            break;
        case STATE_START_CHECK_SENDRESULT:
            // check send otp data result
            Integer sendResult = result.get(ANDROID_SPRD_CALIBRATION_OTP_SENDRESULT);
            Log.e(TAG,"get result from hal sendResult = " + sendResult);
            if (sendResult != null && sendResult == 1){
                showCalibrationResult(0, "Calibration pass");
                mState = STATE_PREVIEW;
            }
            if (sendResult != null && sendResult == 2){
                showCalibrationResult(-1, "Calibration fail");
                mState = STATE_PREVIEW;
            }

            break;
        }
    }

    private static final String PROP_CAPTURE_TIMEOUT = "persist.vendor.camera.capture_timeout";
    //SPRD: Add for dualcamera calibration
    public static final CaptureResult.Key<Integer> ANDROID_SPRD_CALIBRATION_VCM_RESULT = new CaptureResult.Key<Integer>(
            "com.addParameters.sprdCalibrationVCMResult", Integer.class);
	// SPRD:Add for Feature:3DCalibration
    public static final android.hardware.camera2.CaptureRequest.Key<Integer> ANDROID_SPRD_3DCALIBRATION_ENABLED = new android.hardware.camera2.CaptureRequest.Key<Integer>(
            "com.addParameters.srpd3dCalibrationEnable",int.class);
    private void checkVCMstate() {
        try {
            final CaptureRequest.Builder captureBuilder = mPreviewBuilder1;
            float vcm = (float)mVCMValues[mVCMIndex];
            captureBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE,
                    vcm);
            Log.d(TAG, "checkVCMstate capture  in camera vcm=" + vcm);
            // Make sure camera output frame rate is set to correct value.
            Range<Integer> fpsRange = Range.create(20, 20);
            captureBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                    fpsRange);
            mSession1.setRepeatingRequest(captureBuilder.build(),
                    mSessionCaptureCallback1, mHandler1);
        } catch (Exception e) {
            Log.d(TAG, "autoFocus capture a picture1 fail" + e.toString());
        }
    }
    private void setFocusOff() {
        try {
            Log.d(TAG, "setFocusOff");
            final CaptureRequest.Builder captureBuilder = mPreviewBuilder1;
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_OFF);
            // Make sure camera output frame rate is set to correct value.
            Range<Integer> fpsRange = Range.create(20, 20);
            captureBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                    fpsRange);
            mSession1.setRepeatingRequest(captureBuilder.build(),
                    mSessionCaptureCallback1, mHandler1);
        } catch (Exception e) {
            Log.d(TAG, "autoFocus capture a picture1 fail" + e.toString());
        }
    }

    private CameraCaptureSession.CaptureCallback mSessionCaptureCallback1 = new CameraCaptureSession.CaptureCallback() {

        @Override
        public void onCaptureCompleted(CameraCaptureSession session,
                CaptureRequest request, TotalCaptureResult result) {
            mSession1 = session;
            saveOtpFile(result);
            checkState(result);
        }

        @Override
        public void onCaptureProgressed(CameraCaptureSession session,
                CaptureRequest request, CaptureResult partialResult) {
            mSession1 = session;
        }

    };

    private CameraCaptureSession.CaptureCallback mSessionCaptureCallback2 = new CameraCaptureSession.CaptureCallback() {

        @Override
        public void onCaptureProgressed(CameraCaptureSession session,
                CaptureRequest request, CaptureResult partialResult) {
            mSession2 = session;
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession session,
                CaptureRequest request, TotalCaptureResult result) {
            mSession2 = session;
        }
    };

    private CameraCaptureSession.StateCallback mCameraCaptureSessionStateCallback1 = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession session) {
            if (isFinishing() || isDestroyed() || mPause) {
                Log.e(TAG, "onConfigured mCameraCaptureSessionStateCallback1 isFinishing or isDestroyed");
                return;
            }
            try {
                mSession1 = session;
                mSession1.setRepeatingRequest(mPreviewBuilder1.build(),
                        mSessionCaptureCallback1, mHandler1);
            } catch (CameraAccessException e) {
                Log.i(TAG, e.toString());
            }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {

        }

        @Override
        public void onActive(CameraCaptureSession session) {

        }
    };

    private CameraCaptureSession.StateCallback mCameraCaptureSessionStateCallback2 = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession session) {
            if (isFinishing() || isDestroyed() || mPause) {
                Log.e(TAG, "onConfigured mCameraCaptureSessionStateCallback2 isFinishing or isDestroyed");
                return;
            }
            try {
                mSession2 = session;
                mSession2.setRepeatingRequest(mPreviewBuilder2.build(),
                        mSessionCaptureCallback2, mHandler2);
            } catch (CameraAccessException e) {
                Log.i(TAG, e.toString());
            }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {

        }

        @Override
        public void onActive(CameraCaptureSession session) {

        }
    };

    /* SPRD bug 759782 : Display RMS value */
    private AlertDialog mCalibrationAlermDialog = null;
    private void showCalibrationAlermDialog(String title, String text,int bg_color, int text_color) {
        if (mCalibrationAlermDialog != null && mCalibrationAlermDialog.isShowing()) {
            mCalibrationAlermDialog.dismiss();
            mCalibrationAlermDialog = null;
        }
        if (isFinishing() || isDestroyed()) {
            Log.w(TAG, "showDialog activity isDestroyed!");
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(CameraCalibrationAftersalesActivity.this);
        View mCusDialogView = inflater.inflate(R.layout.camera_calibration_dialog_layout, null);
        mCusDialogView.setBackgroundColor(bg_color);
        TextView tileView = (TextView) mCusDialogView.findViewById(R.id.camera_calibration_dialog_title);
        TextView textView = (TextView) mCusDialogView.findViewById(R.id.camera_calibration_dialog_text);
        tileView.setText(title);
        tileView.setTextColor(text_color);
        tileView.setBackgroundColor(bg_color);
        textView.setText(text);
        textView.setTextColor(text_color);
        tileView.setBackgroundColor(bg_color);
        AlertDialog.Builder dialog = new AlertDialog.Builder(
                    CameraCalibrationAftersalesActivity.this)
                    .setView(mCusDialogView)
                    .setCancelable(false);
        dialog.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                // TODO Auto-generated method stub
            }
        });
        mCalibrationAlermDialog = dialog.create();
        mCalibrationAlermDialog.show();
    }

    private AlertDialog mCaptureAlermDialog = null;
    private void showCaptureAlermDialog(String title, String text,int bg_color, int text_color) {
        if (mCaptureAlermDialog != null && mCaptureAlermDialog.isShowing()) {
            mCaptureAlermDialog.dismiss();
            mCaptureAlermDialog = null;
        }
        if (isFinishing() || isDestroyed()) {
            Log.w(TAG, "showDialog activity isDestroyed!");
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(CameraCalibrationAftersalesActivity.this);
       View mCusDialogView = inflater.inflate(R.layout.camera_calibration_dialog_layout, null);
        mCusDialogView.setBackgroundColor(bg_color);
        TextView tileView = (TextView) mCusDialogView.findViewById(R.id.camera_calibration_dialog_title);
        TextView textView = (TextView) mCusDialogView.findViewById(R.id.camera_calibration_dialog_text);
        tileView.setText(title);
        tileView.setTextColor(text_color);
        tileView.setBackgroundColor(bg_color);
        textView.setText(text);
        textView.setTextColor(text_color);
        tileView.setBackgroundColor(bg_color);

        AlertDialog.Builder dialog = new AlertDialog.Builder(
                    CameraCalibrationAftersalesActivity.this)
                    .setView(mCusDialogView)
                    .setCancelable(false);
        dialog.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                // TODO Auto-generated method stub
            }
        });
        mCaptureAlermDialog = dialog.create();
        mCaptureAlermDialog.show();
    }

    private AlertDialog mAlermDialog = null;
    private void showAlertDialog(String title, String text) {
        if (mAlermDialog != null && mAlermDialog.isShowing()) {
            mAlermDialog.dismiss();
            mAlermDialog = null;
        }
        if (isFinishing() || isDestroyed()) {
            Log.w(TAG, "showDialog activity isDestroyed!");
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(CameraCalibrationAftersalesActivity.this);
        View mCusDialogView = inflater.inflate(R.layout.camera_calibration_dialog_layout, null);
        TextView tileView = (TextView) mCusDialogView.findViewById(R.id.camera_calibration_dialog_title);
        TextView textView = (TextView) mCusDialogView.findViewById(R.id.camera_calibration_dialog_text);
        tileView.setText(title);
        textView.setText(text);

        AlertDialog.Builder dialog = new AlertDialog.Builder(
                    CameraCalibrationAftersalesActivity.this)
                    .setView(mCusDialogView)
                    .setCancelable(false)
                    .setNegativeButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                        int which) {
                                    // TODO Auto-generated method stub
                                    if (mAlermDialog != null && mAlermDialog.isShowing()) {
                                        mAlermDialog.dismiss();
                                        mAlermDialog = null;
                                    }
                                }
                            });
        dialog.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                // TODO Auto-generated method stub
            }
        });
        mAlermDialog = dialog.create();
        mAlermDialog.show();
    }

    /* @} */
    private static boolean ENABLE_KEY_CAPTURE = false;
    private long mPreCurrentTime = 0l;
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            // pass
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            Log.d(TAG, "onKeyDown keyCode=" + keyCode + ",ENABLE_KEY_CAPTURE="
                    + ENABLE_KEY_CAPTURE);
            if (!ENABLE_KEY_CAPTURE) {
                return super.onKeyDown(keyCode, event);
            }
            long curentTime = SystemClock.currentThreadTimeMillis();
            Log.d(TAG, "onKeyDown curentTime=" + curentTime
                    + ",mPreCurrentTime=" + mPreCurrentTime);
            if (curentTime - mPreCurrentTime < 10) {
                return true;
            }
            mPreCurrentTime = curentTime;
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void startPreview1(CameraDevice camera) {
        Log.i(TAG, "start preview 1");

        if (isFinishing() || isDestroyed() || mPause) {
            Log.e(TAG, "start preview 1 isFinishing or isDestroyed");
            return;
        }
        SurfaceTexture texture = mPreviewView1.getSurfaceTexture();
        texture.setDefaultBufferSize(800, 600);
        Surface surface = new Surface(texture);
        try {

            mPreviewBuilder1 = camera
                    .createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewBuilder1.set(
                    ANDROID_SPRD_3DCALIBRATION_ENABLED, 1);
            Log.i(TAG, "start preview set ANDROID_SPRD_3DCALIBRATION_ENABLED");
            /* SPRD bug 866105:Fix preview issue */
            // Make sure camera output frame rate is set to correct value.
            Range<Integer> fpsRange = Range.create(20, 20);
            mPreviewBuilder1.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                    fpsRange);
        } catch (CameraAccessException e) {
            Log.i(TAG, e.toString());
        }
        mPreviewBuilder1.addTarget(surface);
        try {
            int[] capturesize = CameraUtil.getSupportBokehSize(this,LEFT_CAMERA_ID);
            if (capturesize != null && capturesize[0] != 0 && capturesize[1] != 0) {
                mLeftCaptureWidth = capturesize[0];
                mLeftCaptureHeight = capturesize[1];
            }
            Log.d(TAG, "startPreview1 mLeftCaptureWidth=" + mLeftCaptureWidth
                    + ",mLeftCaptureHeight=" + mLeftCaptureHeight);
            mImageReader1 = ImageReader.newInstance(mLeftCaptureWidth,
                    mLeftCaptureHeight, ImageFormat.YUV_420_888, 1);
            mImageReader1.setOnImageAvailableListener(
                    mOnImageAvailableListener1, mHandler1);
            if (isFinishing() || isDestroyed() || mPause) {
                Log.e(TAG, "start preview 11 isFinishing or isDestroyed");
                return;
            }
            camera.createCaptureSession(
                    Arrays.asList(surface, mImageReader1.getSurface()),
                    mCameraCaptureSessionStateCallback1, mHandler1);

        } catch (CameraAccessException e) {
            Log.i(TAG, e.toString());
        }
    }

    private void startPreview2(CameraDevice camera) {
        Log.i(TAG, "start preview 2");
        if (isFinishing() || isDestroyed() || mPause) {
            Log.e(TAG, "start preview 2 isFinishing or isDestroyed");
            return;
        }
        SurfaceTexture texture = mPreviewView2.getSurfaceTexture();
        texture.setDefaultBufferSize(800, 600);
        Surface surface = new Surface(texture);
        try {
            mPreviewBuilder2 = camera
                    .createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewBuilder2.set(
                    ANDROID_SPRD_3DCALIBRATION_ENABLED, 1);
            /* SPRD bug 866105:Fix preview issue */
            // Make sure camera output frame rate is set to correct value.
            Range<Integer> fpsRange = Range.create(20, 20);
            mPreviewBuilder2.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                    fpsRange);
        } catch (CameraAccessException e) {
            Log.i(TAG, e.toString());
        }

        mPreviewBuilder2.addTarget(surface);
        try {
            Log.d(TAG, "startPreview2 mRightCaptureWidth=" + mRightCaptureWidth
                    + ",mRightCaptureHeight=" + mRightCaptureHeight);
            mImageReader2 = ImageReader.newInstance(mRightCaptureWidth,
                    mRightCaptureHeight, ImageFormat.YUV_420_888, 1);
            mImageReader2.setOnImageAvailableListener(
                    mOnImageAvailableListener2, mHandler2);
            if (isFinishing() || isDestroyed() || mPause) {
                Log.e(TAG, "start preview 22 isFinishing or isDestroyed");
                return;
            }
            camera.createCaptureSession(
                    Arrays.asList(surface, mImageReader2.getSurface()),
                    mCameraCaptureSessionStateCallback2, mHandler2);

        } catch (CameraAccessException e) {
            Log.i(TAG, e.toString());
        }
    }

    private void captureStillPicture() {
        try {
            final CaptureRequest.Builder captureBuilder = mCameraDevice1
                    .createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader1.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_OFF);
            CameraCaptureSession.CaptureCallback CaptureCallback = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(CameraCaptureSession session,
                        CaptureRequest request, TotalCaptureResult result) {
                    picReady1 = true;
                    if (picReady2 == true) {
                        // startCalibration();
                    }
                }
            };
            while (!capture2IsSend) {
                try {
                    Thread.sleep(300);
                } catch (Exception e) {

                }
            }
            capture2IsSend = false;
            Log.d(TAG, "capture  in camera 1 ");
            // Make sure camera output frame rate is set to correct value.
            Range<Integer> fpsRange = Range.create(20, 20);
            captureBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                    fpsRange);
            mSession1.capture(captureBuilder.build(), CaptureCallback,
                    mHandler1);
            capture1IsSend = true;
        } catch (Exception e) {
            Log.d(TAG, "capture a picture1 fail" + e.toString());
        }
    }

    private boolean capture1IsSend = false;
    private boolean capture2IsSend = false;

    private void captureStillPicture2() {
        try {
            final CaptureRequest.Builder captureBuilder = mCameraDevice2
                    .createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader2.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_OFF);
            CameraCaptureSession.CaptureCallback CaptureCallback = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session,
                        CaptureRequest request, TotalCaptureResult result) {
                    picReady2 = true;
                    if (picReady1 == true) {
                        // startCalibration();
                    }
                }

            };
            Log.d(TAG, "capture  in camera 2 ");
            // Make sure camera output frame rate is set to correct value.
            Range<Integer> fpsRange = Range.create(20, 20);
            captureBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                    fpsRange);
            mSession2.capture(captureBuilder.build(), CaptureCallback,
                    mHandler2);
            // capture1IsSend = false;
            capture2IsSend = true;
        } catch (Exception e) {
            Log.d(TAG, "capture a picture2 fail" + e.toString());
        }
    }

    private class ImageSaver implements Runnable {
        private final Image mImage;
        private final File mFile;

        public ImageSaver(Image image, File file) {
            mImage = image;
            mFile = file;
        }

        @Override
        public void run() {
            FileOutputStream output = null;
            try {
                if (!mFile.getParentFile().exists()) {
                    mFile.getParentFile().mkdirs();
                }
                if (!mFile.exists()) {
                    mFile.createNewFile();
                }
                Log.d(TAG, "ImageSaver mFile=" + mFile.getPath());
                byte[] data = ImageUtils.getNV21FromImage(mImage);
                ImageUtils.createFileWithByte(data, mFile);
//                startCalibrationWork();
                mCounterCpatureOnce.count();
                mCounterCpatureAll.count();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mImage.close();
                if (null != output) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }

//    private boolean mLeftYuvPrepare = false;
//    private boolean mRightYuvPrepare = false;
//
//    private boolean mCalibrationBegin = false;
//    private boolean mCalibrationDone = false;
//    private Object mWorkLock = new Object();
//    synchronized private void startCalibrationWork() {
//        synchronized (mWorkLock) {
//            Log.d(TAG, "startCalibrationWork mLeftYuvPrepare=" + mLeftYuvPrepare
//                    + ",mRightYuvPrepare=" + mRightYuvPrepare + ",mCalibrationBegin=" + mCalibrationBegin);
//            if (mLeftYuvPrepare && mRightYuvPrepare && !mCalibrationBegin) {
//                mCalibrationBegin = true;
//                Log.d(TAG, "startCalibrationWork mCalibrationDone11=" + mCalibrationDone);
//                startCalibrationAfterCapture();
//                Log.d(TAG, "startCalibrationWork mCalibrationDone22=" + mCalibrationDone);
//                mCalibrationDone = true;
//                mCalibrationBegin = false;
//                mLeftYuvPrepare = false;
//                mRightYuvPrepare = false;
//            }
//            Log.d(TAG, "startCalibrationWork mCalibrationDone2233333=" + mCalibrationDone);
//        }
//    }

    private long mCalibrationBeginTime = 0;
    private long mCalibrationEndTime = 0;
    private long mCalibrationTime = 0;

    private void startCalibrationAfterCapture(){
        Log.d(TAG, "startCalibrationAfterCapture mVCMIndex="+mVCMIndex);
        Log.d(TAG, "startCalibrationAfterCapture mAftCalibrationFlag="+mAftCalibrationFlag+",mAftCalibrationCount="+mAftCalibrationCount);
        if(mAftCalibrationFlag == mAftCalibrationCount - 1){
            if (mCameraSound != null) {
                mCameraSound.play(MediaActionSound.SHUTTER_CLICK);
            }
            // 2.do verify work
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    showCalibrationAlermDialog("Camera calibration...", "Calibration need some time,Please take away the phone.", Color.BLUE, Color.WHITE);
                }
            });
            mCalibrationBeginTime = System.currentTimeMillis();
            Log.d(TAG, "startCalibrationAfterCapture begin mCalibrationBeginTime=" + mCalibrationBeginTime);
            Log.d(TAG, "startCalibrationAfterCapture begin mVCMCount=" + mVCMCount);

            int result = NativeCameraCalibration.native_dualCameraCalibrationYUV_afterSales(YUV_TMP_PATH, DST_OTP_BIN_FILE, mLeftCaptureWidth,
                    mLeftCaptureHeight, RIGHT_CAMERA_CAPTURE_WIDTH, RIGHT_CAMERA_CAPTURE_HEIGHT, 1);

            Log.d(TAG, "startCalibration end result=" + result);
            mCalibrationEndTime = System.currentTimeMillis();
            Log.d(TAG, "startCalibration end mCalibrationEndTime=" + mCalibrationEndTime);
            mCalibrationTime = (mCalibrationEndTime - mCalibrationBeginTime);
            Log.d("APK_MMI", "*********** Time: " + mCalibrationTime + "ms ***********");

            if(result == 0){
                int otpData[] = NativeCameraCalibration.native_getdualCameraCalibrationOTP_afterSales();
                if(otpData == null || (otpData != null && otpData.length <= 0)){
                    StringBuffer buffer = new StringBuffer();
                    buffer.append("Calibration fail");
                    showCalibrationResult(-1, buffer.toString());
                    return;
                }
                for(int otp_data : otpData){
                    Log.d(TAG, "startCalibration end otp_data=" + otp_data + "\n");
                }
                sendOtpDataToHAL(otpData);
            }else{
                int resultCode = -1;
                try {
                    resultCode = result;
                    final String resultText = String.format("0x%08x", resultCode);
                    Log.d(TAG, "startCalibration end resultCode=" + resultCode);
                    Log.d(TAG, "startCalibration end resultText=" + resultText);
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            // TODO Auto-generated method stub
                            StringBuffer buffer = new StringBuffer();
                            buffer.append("Calibration time : " + mCalibrationTime + " ms\n");
                            buffer.append("Calibration fail result code : "  + resultText + "\n");
                            showAlertDialog("Calibration fail!", buffer.toString());
                            int i = 0;
                            for(int vcm : mVCMValues){
                                buffer.append("vcm"  + i + " = " + vcm + "\n");
                                i ++;
                            }
                            saveCalibrationResult(buffer.toString());
                        }
                    });
                }catch (Exception e) {
                    // TODO: handle exception
                    e.printStackTrace();
                }
            }
            dismissDialog();
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    mTakePhotoBtn.setEnabled(true);
                }
            });
        }
    }

    private void saveCalibrationResult(String result) {
        try {
            Log.d(TAG, "saveCalibrationResult result="+result);
            dumpData(result.getBytes(), CALIBRATION_RESULT);
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }catch (Throwable e) {
            // TODO: handle exception
            e.printStackTrace();
        }
    }

    private void sendOtpDataToHAL(int[] data){
        mOtpDataUtil.setOtpDataFull(data);
        byte[] otpDataByteArray = mOtpDataUtil.getFinalOtpDataByteArray();

        // send data to HAL by tag
        mPreviewBuilder1.set(ANDROID_SPRD_CALIBRATION_OTPDATA,otpDataByteArray);
        try {
            mSession1.setRepeatingRequest(mPreviewBuilder1.build(),mSessionCaptureCallback1, mHandler1);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.e(TAG,"send otp data to Hal");
        mState = STATE_START_CHECK_SENDRESULT;
    }

    private void calibrationByGoldenOtp(){
        byte[] otpDataByteArray = mOtpDataUtil.getGoldenOtpByteArray();
        // send data to HAL by tag
        mPreviewBuilder1.set(ANDROID_SPRD_CALIBRATION_OTPDATA,otpDataByteArray);
        try {
            mSession1.setRepeatingRequest(mPreviewBuilder1.build(),mSessionCaptureCallback1, mHandler1);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private void showCalibrationResult(int testResult, String testData) {
        Log.d(TAG, "showCalibrationResult testResult=" + testResult);
        Log.d(TAG, "showCalibrationResult testData=" + testData);
        try {
            Intent intent = new Intent(CameraCalibrationAftersalesActivity.this,
                    CameraCalibrationResultActivity.class);
            intent.putExtra(CameraCalibrationResultActivity.KEY_TEST_RESULT,
                    testResult);
            intent.putExtra(CameraCalibrationResultActivity.KEY_TEST_DATA,
                    testData);
            intent.putExtra(CameraCalibrationResultActivity.KEY_TEST_TIME,
                    mCalibrationTime);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ArrayList<int[]> mOtpDataVCMArray = new ArrayList<int[]>();
    private ArrayList<String> mTestStereoInfoVCMArray = new ArrayList<String>();
    private int[] mVCMValues = null;
    private int mVCMIndex = 0;
    private int mVCMCount = 0;

    private void initVCM(){
        try {
            mVCMValues = null;
            mOtpDataVCMArray.clear();
            mTestStereoInfoVCMArray.clear();
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
    }

    private static final int MSG_PASS = 1;
    private static final int MSG_FAIL = 2;
    private static final int MSG_DISMISS_DIALOG = 3;


    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
            case MSG_PASS:
                int result = msg.arg1;
                Toast.makeText(CameraCalibrationAftersalesActivity.this,
                        "Camera Calibration PASS!\n result=" + result,
                        Toast.LENGTH_SHORT).show();
                storeRusult(true);
                // finish();
                break;
            case MSG_FAIL:
                result = msg.arg1;
                Toast.makeText(CameraCalibrationAftersalesActivity.this,
                        "Camera Calibration FAIL!\n result=" + result,
                        Toast.LENGTH_SHORT).show();
                storeRusult(false);
                // finish();
                break;
            case MSG_DISMISS_DIALOG:
                if (mDialog != null && mDialog.isShowing()) {
                    mDialog.cancel();
                }
                if (mCaptureAlermDialog != null && mCaptureAlermDialog.isShowing()) {
                    mCaptureAlermDialog.cancel();
                }
                if (mCalibrationAlermDialog != null && mCalibrationAlermDialog.isShowing()) {
                    mCalibrationAlermDialog.cancel();
                }
                break;
            default:
                break;
            }
        };
    };

    private AlertDialog mDialog = null;
    private void showComfirmDialog(String text) {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
            mDialog = null;
        }
        if (isFinishing() || isDestroyed()) {
            Log.w(TAG, "showDialog activity isDestroyed!");
            return;
        }
        AlertDialog.Builder dialog = new AlertDialog.Builder(
                CameraCalibrationAftersalesActivity.this)
                .setMessage(text).setCancelable(false)
                .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(CameraCalibrationAftersalesActivity.this, CameraCalibrationAftersales2Activity.class);
                startActivity(intent);
                CameraCalibrationAftersalesActivity.this.finish();
            }
        })
        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                CameraCalibrationAftersalesActivity.this.finish();
            }
        })
        ;
        mDialog = dialog.create();
        mDialog.show();
    }

    private void dismissDialog() {
        Message message = Message.obtain();
        message.what = MSG_DISMISS_DIALOG;
        mHandler.sendMessage(message);
    }

    private void dumpData(byte[] data, String path) {
        FileOutputStream fileOutput = null;
        try {
            fileOutput = new FileOutputStream(path);
            fileOutput.write(data);
            fileOutput.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fileOutput != null) {
                try {
                    fileOutput.close();
                } catch (Exception t) {
                    t.printStackTrace();
                }
            }
        }
    }
    private boolean checkFileExists(String filePath) {
        File file = new File(filePath);
        boolean exists = file.exists();
        return exists;
    }
}
