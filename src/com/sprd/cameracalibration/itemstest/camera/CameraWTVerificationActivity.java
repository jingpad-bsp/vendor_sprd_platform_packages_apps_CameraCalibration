package com.sprd.cameracalibration.itemstest.camera;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaActionSound;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemProperties;
import android.util.Log;
import android.util.Range;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.sprd.cameracalibration.BaseActivity;
import com.sprd.cameracalibration.Const;
import com.sprd.cameracalibration.R;
import com.sprd.cameracalibration.itemstest.camera.CameraCalibrationResultActivity;
import com.sprd.cameracalibration.itemstest.camera.NativeCameraCalibration;
import com.sprd.cameracalibration.utils.CameraUtil;
import com.sprd.cameracalibration.utils.Counter;
import com.sprd.cameracalibration.utils.FileUtils;
import com.sprd.cameracalibration.utils.ImageUtils;
import com.sprd.cameracalibration.utils.StorageUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Created by SPREADTRUM\emily.miao on 19-8-19.
 */

/*
    UW+W(otp data put on W):W+T(otp data put on T)
    SPRD_OPTICSZOOM_W_ID = 40
    SPRD_OPTICSZOOM_T_ID = 41
    SPRD_ULTRA_WIDE_ID = 35
*/
public class CameraWTVerificationActivity extends ZoomBaseActivity {
    private static final String TAG = "CameraWTVerificationActivity";
    public static final String WTCAMERA_VERIFICATION_TEST_NAME = "OpticsZoom Verification";
    private static final String YUV_TMP_PATH = StorageUtil.getInternalStoragePath() + "/cali/";

    private static String W_YUV_PATH;
    private static String T_YUV_PATH;

    private static String DST_RESULT_BIN_FILE;

    private static String W_CAMERA_ID = "35"; //UW  W
    private static String T_CAMERA_ID = "40"; //W   T

    private static final int W_CAMERA_CAPTURE_WIDTH = 3264;
    private static final int W_CAMERA_CAPTURE_HEIGHT = 2448;
    private static final int T_CAMERA_CAPTURE_WIDTH = 3264;
    private static final int T_CAMERA_CAPTURE_HEIGHT = 2448;

    private int mWCaptureWidth = W_CAMERA_CAPTURE_WIDTH;
    private int mWCaptureHeight = W_CAMERA_CAPTURE_HEIGHT;
    private int mTCaptureWidth = T_CAMERA_CAPTURE_WIDTH;
    private int mTCaptureHeight = T_CAMERA_CAPTURE_HEIGHT;

    private Handler mHandler_w;
    private HandlerThread mThreadHandler_w;
    private Handler mHandler_t;
    private HandlerThread mThreadHandler_t;


    private MediaActionSound mCameraSound;

    private Button mTakePhotoBtn = null;


    private Semaphore mCameraOpenCloseLock_w = new Semaphore(1);
    private Semaphore mCameraOpenCloseLock_t = new Semaphore(1);

    private int mState_w;
    private int mState_t;
    private static final int STATE_PREVIEW = 0;
    private static final int STATE_WAITING_CAPTURE = 1;
    private static final int STATE_START_CAPTURE = 2;


    private TextureView mPreviewView_w;  // wide
    Surface mSurface_w;
    private TextureView mPreviewView_t; // tele
    Surface mSurface_t;

    private TextView text_left;
    private TextView text_right;

    CountDownLatch countDownLatch_t = new CountDownLatch(1);
    CountDownLatch countDownLatch_w = new CountDownLatch(1);

    private CameraDevice mCameraDevice_w;
    private CameraDevice mCameraDevice_t;

    private CameraCaptureSession mSession_w;
    private CameraCaptureSession mSession_t;

    private ImageReader mImageReader_w;
    private ImageReader mImageReader_t;

    private CaptureRequest.Builder mPreviewBuilder_w;
    private CaptureRequest.Builder mPreviewBuilder_t;

    private boolean mWCameraAFSupprt;
    private boolean mTCameraAFSupprt;

    private boolean mFlagOne = false;
    private String zoomStage;
    private int otpFlag;

    /*
        private Counter mCounterCpatureOnce:
            add for ensure only 2 yuv pictures all are saved we can excute startCalibrationAfterCapture.
    */
    private Counter mCounterCpatureOnce;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.e(TAG,"onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_3d_calibration);
        if (!checkPermissions()) {
            finish();
            return;
        }

        text_left = findViewById(R.id.txt_left);
        text_right = findViewById(R.id.txt_right);
        //Determine camera ID according to intent
        Intent intent = getIntent();
        zoomStage = intent.getStringExtra("zoom_stage");
        Log.d(TAG,"zoom stage selected is " + zoomStage);
        if (zoomStage.equals("1")){
            W_CAMERA_ID = "35"; //UW
            T_CAMERA_ID = "40"; //W
            otpFlag = Const.OTP_FLAG_ZOOM_STAGE_1;
            text_right.setText("ultrawide");
            text_left.setText("wide");
            W_YUV_PATH = YUV_TMP_PATH + "stage1_uw_verify.yuv";
            T_YUV_PATH = YUV_TMP_PATH + "stage1_w_verify.yuv";
            DST_RESULT_BIN_FILE = YUV_TMP_PATH + "stage1_uw_w_otpdata.bin";
        }else if (zoomStage.equals("2")){
            W_CAMERA_ID = "40"; //W
            T_CAMERA_ID = "41"; //T
            otpFlag = Const.OTP_FLAG_ZOOM_STAGE_2;
            text_right.setText("wide");
            text_left.setText("tele");
            W_YUV_PATH = YUV_TMP_PATH + "stage2_w_verify.yuv";
            T_YUV_PATH = YUV_TMP_PATH + "stage2_t_verify.yuv";
            DST_RESULT_BIN_FILE = YUV_TMP_PATH + "stage2_w_t_otpdata.bin";
        }
        Log.d(TAG,"W_CAMERA_ID = " + W_CAMERA_ID + " T_CAMERA_ID = " + T_CAMERA_ID + " otpFlag = " + otpFlag);

        if (mCameraSound == null) {
            mCameraSound = new MediaActionSound();
            mCameraSound.load(MediaActionSound.SHUTTER_CLICK);
            mCameraSound.load(MediaActionSound.FOCUS_COMPLETE);
        }

        mTakePhotoBtn = (Button) findViewById(R.id.start_take_picture);
        mTakePhotoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "mTakePhotoBtn take picture is start");
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
                mCounterCpatureOnce = new Counter(2);
                autoFocusIfSupported();
                new Thread(){
                    @Override
                    public void run() {
                        startCalibrationAfterCapture();
                    }
                }.start();
            }
        });

        mFlagOne = false;
        mTakePhotoBtn.setEnabled(true);
        mPreviewView_t = (TextureView) findViewById(R.id.sur_left);
        mPreviewView_w = (TextureView) findViewById(R.id.sur_right);

        mPreviewView_t.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(
                    SurfaceTexture surface, int width, int height) {
                Log.d(TAG, "tele TextureView available");
                Log.i(TAG, " tele SurfaceTexture width = " + width + " height = " + height);

                surface.setDefaultBufferSize(T_CAMERA_CAPTURE_WIDTH, T_CAMERA_CAPTURE_HEIGHT);
                mSurface_t = new Surface(surface);
                //if (width == PREVIEW_WIDTH) {
                countDownLatch_t.countDown();
                //}
            }

            @Override
            public void onSurfaceTextureSizeChanged(
                    SurfaceTexture surface, int width, int height) {
            }

            @Override
            public boolean onSurfaceTextureDestroyed(
                    SurfaceTexture surface) {
                Log.i(TAG,"tele TextureView destoryed");
                mSurface_t = null;
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            }
        });

        mPreviewView_w.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(
                    SurfaceTexture surface, int width, int height) {
                Log.d(TAG, "wide TextureView available");
                Log.i(TAG, "wide SurfaceTexture width = " + width + " height = " + height);

                surface.setDefaultBufferSize(W_CAMERA_CAPTURE_WIDTH, W_CAMERA_CAPTURE_HEIGHT);
                mSurface_w = new Surface(surface);
                //if (width == PREVIEW_WIDTH) {
                countDownLatch_w.countDown();
                //}
            }

            @Override
            public void onSurfaceTextureSizeChanged(
                    SurfaceTexture surface, int width, int height) {
            }

            @Override
            public boolean onSurfaceTextureDestroyed(
                    SurfaceTexture surface) {
                Log.i(TAG,"wide TextureView destoryed");
                mSurface_w = null;
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            }
        });


        if (mPassButton != null) {
            mPassButton.setVisibility(View.GONE);
        }
        if (mFailButton != null) {
            mFailButton.setText("Exit");
        }

        //get captureSize from tag "android.scaler.availableStreamConfigurations"
        int[] captureSize_w = CameraUtil.getLargestYuvSize(this,W_CAMERA_ID);
        if (captureSize_w != null && captureSize_w[0] != 0 && captureSize_w[1] != 0){
            mWCaptureWidth = captureSize_w[0];
            mWCaptureHeight = captureSize_w[1];
        }
        Log.e(TAG,"camera id " + W_CAMERA_ID + " mWCaptureWidth = " + mWCaptureWidth + " mWCaptureHeight = " + mWCaptureHeight);
        int[] captureSize_t = CameraUtil.getLargestYuvSize(this,T_CAMERA_ID);
        if (captureSize_t != null && captureSize_t[0] != 0 && captureSize_t[1] != 0){
            mTCaptureWidth = captureSize_t[0];
            mTCaptureHeight = captureSize_t[1];
        }
        Log.e(TAG,"camera id " + T_CAMERA_ID + " mTCaptureWidth = " + mTCaptureWidth + " mTCaptureHeight = " + mTCaptureHeight);
        mImageReader_w = ImageReader.newInstance(mWCaptureWidth, mWCaptureHeight, ImageFormat.YUV_420_888, 1);
        mImageReader_w.setOnImageAvailableListener(mOnImageAvailableListener_w, mHandler_w);

        mImageReader_t = ImageReader.newInstance(mTCaptureWidth, mTCaptureHeight, ImageFormat.YUV_420_888, 1);
        mImageReader_t.setOnImageAvailableListener(mOnImageAvailableListener_t, mHandler_t);

        try {
            FileUtils.deleteFile(new File(DST_RESULT_BIN_FILE));
            File cali = new File(YUV_TMP_PATH);
            if(cali != null && !cali.exists()){
                boolean res = cali.mkdirs();
                if(!res){
                    Log.d(TAG, "mkdirs fail:"+YUV_TMP_PATH);
                }
            }
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
    }


    private boolean mPause = false;
    @Override
    public void onResume() {
        Log.e(TAG,"onResume");
        super.onResume();
        if (!mHasPermissions) {
            Log.i(TAG, "onResume: Missing permissions.");
            return;
        }
        mPause = false;
        initLooper();
        //open camera
        if (zoomStage.equals("1")){
            openCamera(T_CAMERA_ID);
            openCamera(W_CAMERA_ID);
        }else if (zoomStage.equals("2")){
            openCamera(W_CAMERA_ID);
            openCamera(T_CAMERA_ID);
        }
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause start!");
        mPause = true;
        if (!mHasPermissions) {
            Log.i(TAG, "onPause: Missing permissions.");
            return;
        }
        closeCamera();
        stopLooper();
        super.onPause();
        Log.d(TAG, "onPause end!");
    }

    @Override
    public void onDestroy() {
        Log.d(TAG,"onDestroy");
        super.onDestroy();
        if (!mHasPermissions) {
            Log.i(TAG, "onDestroy: Missing permissions.");
            return;
        }
        if (mCameraSound != null) {
            mCameraSound.release();
            mCameraSound = null;
        }

        if (mHandler != null) {
            mHandler.removeMessages(MSG_DISMISS_DIALOG);
            mHandler.removeMessages(MSG_FAIL);
            mHandler.removeMessages(MSG_PASS);
            mHandler = null;
        }
    }


    @Override
    public void onBackPressed() {
    }



    private boolean isWidecameraReady = true;
    private boolean isTelecameraReady = true;

    private void checkWideCameraState(CaptureResult result) {
        switch (mState_w) {
            case STATE_PREVIEW:
                // NOTHING
                break;
            case STATE_WAITING_CAPTURE:
                int aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                if (aeState != CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                    Log.e(TAG,"checkWideCameraState aeState = " + aeState);
                    //return;
                }
                Integer afStateMaybe = result.get(CaptureResult.CONTROL_AF_STATE);
                Log.d(TAG, "checkWideCameraState afStateMaybe =" + afStateMaybe);
                if (afStateMaybe == CameraMetadata.CONTROL_AF_STATE_FOCUSED_LOCKED){
                    mState_w = STATE_START_CAPTURE;
                    isWidecameraReady = true;
                    if (mCameraSound != null) {
                        mCameraSound.play(MediaActionSound.FOCUS_COMPLETE);
                    }
                }else if (afStateMaybe == CameraMetadata.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED){
                    Log.d(TAG, "checkWideCameraState afStateMaybe ="+afStateMaybe);
                    mState_w = STATE_PREVIEW;
                    final int afState = afStateMaybe;
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            // TODO Auto-generated method stub
                            StringBuffer buffer = new StringBuffer();
                            buffer.append("AutoFucos fail ,afState : "  + afState + "\n");
                            try {
                                dismissDialog();
                                showRmsDialog("AutoFucos fail!", buffer.toString());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
                break;
            case STATE_START_CAPTURE:
                if (isWidecameraReady && isTelecameraReady){
                    takePictureByWideCamera();
                    mState_w = STATE_PREVIEW;
                }
                break;
        }
    }

    private void checkTeleCameraState(CaptureResult result) {
        switch (mState_t) {
            case STATE_PREVIEW:
                // NOTHING
                break;
            case STATE_WAITING_CAPTURE:
                int aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                if (aeState != CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                    Log.e(TAG,"checkTeleCameraState aeState = " + aeState);
                    //return;
                }
                Integer afStateMaybe = result.get(CaptureResult.CONTROL_AF_STATE);
                Log.d(TAG, "checkTeleCameraState afStateMaybe =" + afStateMaybe);
                if (afStateMaybe == CameraMetadata.CONTROL_AF_STATE_FOCUSED_LOCKED){
                    mState_t = STATE_START_CAPTURE;
                    isTelecameraReady = true;
                    if (mCameraSound != null) {
                        mCameraSound.play(MediaActionSound.FOCUS_COMPLETE);
                    }
                }else if (afStateMaybe == CameraMetadata.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED){
                    Log.d(TAG, "checkTeleCameraState afStateMaybe ="+afStateMaybe);
                    mState_t = STATE_PREVIEW;
                    final int afState = afStateMaybe;
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            // TODO Auto-generated method stub
                            StringBuffer buffer = new StringBuffer();
                            buffer.append("AutoFucos fail ,afState : "  + afState + "\n");
                            try {
                                dismissDialog();
                                showRmsDialog("AutoFucos fail!", buffer.toString());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
                break;
            case STATE_START_CAPTURE:
                if (isWidecameraReady && isTelecameraReady){
                    takePictureByTeleCamera();
                    mState_t = STATE_PREVIEW;
                }
                break;
        }
    }

    private void takePictureByWideCamera(){
        try {
            final CaptureRequest.Builder captureBuilder = mCameraDevice_w.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader_w.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
            CameraCaptureSession.CaptureCallback CaptureCallback = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session,
                                               CaptureRequest request, TotalCaptureResult result) {
                }
            };
            Log.d(TAG, "capture in wide camera");
            // Make sure camera output frame rate is set to correct value.
            captureBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range.create(20, 20));
            mSession_w.capture(captureBuilder.build(), CaptureCallback, mHandler_w);
        } catch (Exception e) {
            Log.d(TAG, "wide camera capture a picture1 fail" + e.toString());
        }
    }

    private void takePictureByTeleCamera(){
        try {
            final CaptureRequest.Builder captureBuilder = mCameraDevice_t.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader_t.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
            CameraCaptureSession.CaptureCallback CaptureCallback = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session,
                                               CaptureRequest request, TotalCaptureResult result) {
                }
            };
            Log.d(TAG, "capture in tele camera");
            // Make sure camera output frame rate is set to correct value.
            captureBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range.create(20, 20));
            mSession_t.capture(captureBuilder.build(), CaptureCallback, mHandler_t);
        } catch (Exception e) {
            Log.d(TAG, "wide camera capture a picture1 fail" + e.toString());
        }
    }


    private boolean checkAutoFocusSupport(CameraManager manager,String cameraId){
        try {
            int[] mSupportFocus= manager.getCameraCharacteristics(cameraId).get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
            for (int i=0;i<mSupportFocus.length;i++) {
                Log.d(TAG,"Camera " + cameraId + " support focus mode is "+mSupportFocus[i]);
                if (mSupportFocus[i] == CameraMetadata.CONTROL_AF_MODE_AUTO) {
                    return true;
                }
            }
            return false;
        } catch (CameraAccessException e) {
            e.printStackTrace();
            return false;
        }
    }
    private void autoFocusIfSupported(){
        mState_t = STATE_START_CAPTURE;
        mState_w = STATE_START_CAPTURE;
        if (mTCameraAFSupprt){
            mState_t = STATE_WAITING_CAPTURE;
            isTelecameraReady = false;
            Log.e(TAG,"tele camera auto focus");
            mPreviewBuilder_t.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
            mPreviewBuilder_t.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
            try {
                mSession_t.capture(mPreviewBuilder_t.build(), deferredCallbackSetter_t, mHandler_t);
                mSession_t.setRepeatingRequest(mPreviewBuilder_t.build(), mSessionCaptureCallback_t, mHandler_t);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
            mPreviewBuilder_t.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_IDLE);

        }
        if (mWCameraAFSupprt){
            mState_w = STATE_WAITING_CAPTURE;
            isWidecameraReady = false;
            Log.e(TAG,"wide camera auto focus");
            mPreviewBuilder_w.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
            mPreviewBuilder_w.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
            try {
                mSession_w.capture(mPreviewBuilder_w.build(), deferredCallbackSetter_w, mHandler_w);
                mSession_w.setRepeatingRequest(mPreviewBuilder_w.build(), mSessionCaptureCallback_w, mHandler_w);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
            mPreviewBuilder_w.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_IDLE);
        }
    }


    private void openCamera(String cameraId) {
        Log.d(TAG, "openCamera cameraId=" + cameraId);
        CameraManager manager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
        try {
            if (W_CAMERA_ID.equals(cameraId)) {
                if (!mCameraOpenCloseLock_w.tryAcquire(2500,
                        TimeUnit.MILLISECONDS)) {
                    throw new RuntimeException(
                            "Time out waiting to lock camera opening.");
                }
                manager.openCamera(cameraId, mCameraDeviceStateCallback_w, mHandler_w);
                mWCameraAFSupprt = checkAutoFocusSupport(manager,cameraId);
            } else if (T_CAMERA_ID.equals(cameraId)) {
                if (!mCameraOpenCloseLock_t.tryAcquire(2500,
                        TimeUnit.MILLISECONDS)) {
                    throw new RuntimeException(
                            "Time out waiting to lock camera opening.");
                }
                manager.openCamera(cameraId, mCameraDeviceStateCallback_t, mHandler_t);
                mTCameraAFSupprt = checkAutoFocusSupport(manager,cameraId);
            } else {
                Log.e(TAG, "Wrong camera ID");
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e){
            e.printStackTrace();
        }
    }

    private void startWidePreview(){
        Log.i(TAG, "start wide preview");
        try {
            mPreviewBuilder_w = mCameraDevice_w.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewBuilder_w.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, new Range(20, 20));
            mPreviewBuilder_w.addTarget(mSurface_w);
            Log.i(TAG,"setRepeatingRequest");
            mSession_w.setRepeatingRequest(mPreviewBuilder_w.build(),mSessionCaptureCallback_w,mHandler_w);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startTelePreview(){
        Log.i(TAG, "start tele preview");
        try {
            mPreviewBuilder_t = mCameraDevice_t.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewBuilder_t.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, new Range(20, 20));
            mPreviewBuilder_t.addTarget(mSurface_t);
            Log.i(TAG,"setRepeatingRequest");
            mSession_t.setRepeatingRequest(mPreviewBuilder_t.build(),mSessionCaptureCallback_t,mHandler_t);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }



    private void closeCamera() {
        if (zoomStage.equals("1")){
            closeTeleCamera();
            closeWideCamera();
        }else if (zoomStage.equals("2")){
            closeWideCamera();
            closeTeleCamera();
        }
    }
    private void closeTeleCamera(){
        try {
            mCameraOpenCloseLock_t.acquire();
            if (null != mSession_t) {
                mSession_t.stopRepeating();
                mSession_t.abortCaptures();
                mSession_t = null;
            }
            if (null != mCameraDevice_t) {
                mCameraDevice_t.close();
                mCameraDevice_t = null;
            }
            if (null != mImageReader_t) {
                mImageReader_t.close();
                mImageReader_t = null;
            }
        } catch (Exception e) {
            Log.d(TAG, e.toString());
            e.printStackTrace();
        } finally {
            mCameraOpenCloseLock_t.release();
        }
    }
    private void closeWideCamera(){
        try {
            mCameraOpenCloseLock_w.acquire();
            if (null != mSession_w) {
                mSession_w.stopRepeating();
                mSession_w.abortCaptures();
                mSession_w = null;
            }
            if (null != mCameraDevice_w) {
                mCameraDevice_w.close();
                mCameraDevice_w = null;
            }
            if (null != mImageReader_w) {
                mImageReader_w.close();
                mImageReader_w = null;
            }
        } catch (Exception e) {
            Log.d(TAG, e.toString());
            e.printStackTrace();
        } finally {
            mCameraOpenCloseLock_w.release();
        }
    }


    CameraCaptureSession.CaptureCallback deferredCallbackSetter_w = new CameraCaptureSession.CaptureCallback() {

        @Override
        public void onCaptureProgressed(CameraCaptureSession session,
                                        CaptureRequest request, CaptureResult result) {
            Log.d(TAG, "deferredCallbackSetter_w onCaptureProgressed");
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession session,
                                       CaptureRequest request, TotalCaptureResult result) {
            Log.d(TAG, "deferredCallbackSetter_w onCaptureCompleted");
        }

        @Override
        public void onCaptureFailed(CameraCaptureSession session,
                                    CaptureRequest request, CaptureFailure failure) {
            Log.e(TAG, "wide camera Focusing failed with reason " + failure.getReason());
        }
    };

    CameraCaptureSession.CaptureCallback deferredCallbackSetter_t = new CameraCaptureSession.CaptureCallback() {

        @Override
        public void onCaptureProgressed(CameraCaptureSession session,
                                        CaptureRequest request, CaptureResult result) {
            Log.d(TAG, "deferredCallbackSetter_t onCaptureProgressed");
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession session,
                                       CaptureRequest request, TotalCaptureResult result) {
            Log.d(TAG, "deferredCallbackSetter_t onCaptureCompleted");
        }

        @Override
        public void onCaptureFailed(CameraCaptureSession session,
                                    CaptureRequest request, CaptureFailure failure) {
            Log.e(TAG, "tele camera Focusing failed with reason " + failure.getReason());
        }
    };


    private CameraDevice.StateCallback mCameraDeviceStateCallback_w = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            Log.i(TAG, "CameraDevice_w onOpened");
            mCameraOpenCloseLock_w.release();
            mCameraDevice_w = camera;
            try {
                countDownLatch_w.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //createSession
            try {
                camera.createCaptureSession(Arrays.asList(mSurface_w, mImageReader_w.getSurface()), mCameraCaptureSessionStateCallback_w, mHandler_w);
            }catch (CameraAccessException e) {
                Log.i(TAG, e.toString());
            }
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            mCameraOpenCloseLock_w.release();
            camera.close();
            Log.i(TAG, "CameraDevice_w onDisconnected");
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            mCameraOpenCloseLock_w.release();
            camera.close();
            Log.i(TAG, "CameraDevice_w onError");
        }
    };

    private CameraDevice.StateCallback mCameraDeviceStateCallback_t = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            Log.i(TAG, "CameraDevice_t onOpened");
            mCameraOpenCloseLock_t.release();
            mCameraDevice_t = camera;
            try {
                countDownLatch_t.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //createSession
            try {
                camera.createCaptureSession(Arrays.asList(mSurface_t, mImageReader_t.getSurface()), mCameraCaptureSessionStateCallback_t, mHandler_t);
            }catch (CameraAccessException e) {
                Log.i(TAG, e.toString());
            }
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            mCameraOpenCloseLock_t.release();
            camera.close();
            Log.i(TAG, "CameraDevice_t onDisconnected");
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            mCameraOpenCloseLock_t.release();
            camera.close();
            Log.i(TAG, "CameraDevice_t onError");
        }
    };

    private CameraCaptureSession.StateCallback mCameraCaptureSessionStateCallback_w = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession session) {
            Log.e(TAG,"CameraCaptureSession mCameraCaptureSessionStateCallback_w onConfigured");
            if (isFinishing() || isDestroyed() || mPause) {
                Log.e(TAG,
                        "onConfigured mCameraCaptureSessionStateCallback_w isFinishing is "+isFinishing()
                                +", isDestroyed is "+isDestroyed()
                                +", mPause is "+mPause);
                return;
            }
            mSession_w = session;
            //startPreview
            startWidePreview();
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {

        }

        @Override
        public void onActive(CameraCaptureSession session) {

        }
    };

    private CameraCaptureSession.StateCallback mCameraCaptureSessionStateCallback_t = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession session) {
            Log.e(TAG,"CameraCaptureSession mCameraCaptureSessionStateCallback_t onConfigured");
            if (isFinishing() || isDestroyed() || mPause) {
                Log.e(TAG,
                        "onConfigured mCameraCaptureSessionStateCallback_t isFinishing is "+isFinishing()
                                +", isDestroyed is "+isDestroyed()
                                +", mPause is "+mPause);
                return;
            }
            mSession_t = session;
            //startPreview
            startTelePreview();
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {

        }

        @Override
        public void onActive(CameraCaptureSession session) {

        }
    };


    private CameraCaptureSession.CaptureCallback mSessionCaptureCallback_w = new CameraCaptureSession.CaptureCallback() {

        @Override
        public void onCaptureCompleted(CameraCaptureSession session,
                                       CaptureRequest request, TotalCaptureResult result) {
            mSession_w = session;
            //get vcm
            checkWideCameraState(result);
        }

        @Override
        public void onCaptureProgressed(CameraCaptureSession session,
                                        CaptureRequest request, CaptureResult partialResult) {
            mSession_w = session;
        }

    };

    private CameraCaptureSession.CaptureCallback mSessionCaptureCallback_t = new CameraCaptureSession.CaptureCallback() {

        @Override
        public void onCaptureCompleted(CameraCaptureSession session,
                                       CaptureRequest request, TotalCaptureResult result) {
            mSession_t = session;
            //获取vcm
            checkTeleCameraState(result);
            saveOtpFile(result);
        }

        @Override
        public void onCaptureProgressed(CameraCaptureSession session,
                                        CaptureRequest request, CaptureResult partialResult) {
            mSession_t = session;
        }

    };

    private void saveOtpFile(TotalCaptureResult result){
        try {
            int mDualOtpFlag = result.get(CaptureResult.ANDROID_SPRD_DUAL_OTP_FLAG);
            if(mDualOtpFlag != otpFlag){
                Log.e(TAG, "saveOtpFile mDualOtpFlag = " + mDualOtpFlag);
                if(!mFlagOne){
                    mFlagOne = true;
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            // TODO Auto-generated method stub
                            showAlertDialog("Camera verifying", "Camera verifying FAIL!\nOTP flag != " + otpFlag);
                        }
                    });
                }
                return;
            }
            boolean hasDstResultBin = checkFileExists(DST_RESULT_BIN_FILE);
            if(hasDstResultBin){
                return;
            }
            byte[] otpByte = result.get(CaptureResult.CONTROL_OTP_VALUE);
            dumpData(otpByte, DST_RESULT_BIN_FILE);
        } catch (Exception e) {
            Log.i(TAG, e.toString());
            e.printStackTrace();
        }
    }

//    private boolean mTeleYuvPrepare = false;
//    private boolean mWideYuvPrepare = false;

    private ImageReader.OnImageAvailableListener mOnImageAvailableListener_w = new ImageReader.OnImageAvailableListener(){
        @Override
        public void onImageAvailable(ImageReader reader) {
//            mWideYuvPrepare = true;
            Log.d(TAG, "mOnImageAvailableListener_w mWideYuvPrepare!");
            mHandler_w.post(new CameraWTVerificationActivity.ImageSaver(reader.acquireNextImage(), new File(W_YUV_PATH)));
        }
    };
    private ImageReader.OnImageAvailableListener mOnImageAvailableListener_t = new ImageReader.OnImageAvailableListener(){
        @Override
        public void onImageAvailable(ImageReader reader) {
//            mTeleYuvPrepare = true;
            Log.d(TAG, "mOnImageAvailableListener_t mTeleYuvPrepare!");
            mHandler_t.post(new CameraWTVerificationActivity.ImageSaver(reader.acquireNextImage(), new File(T_YUV_PATH)));
        }
    };

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


//    private boolean mCalibrationBegin = false;
//    private Object mWorkLock = new Object();
//    synchronized private void startCalibrationWork() {
//        synchronized (mWorkLock) {
//            Log.d(TAG, "startCalibrationWork mTeleYuvPrepare =" + mTeleYuvPrepare
//                    + ",mWideYuvPrepare =" + mWideYuvPrepare + ",mCalibrationBegin=" + mCalibrationBegin);
//            if (mTeleYuvPrepare && mWideYuvPrepare && !mCalibrationBegin) {
//                mCalibrationBegin = true;
//                startCalibrationAfterCapture();
//                mCalibrationBegin = false;
//                mTeleYuvPrepare = false;
//                mWideYuvPrepare = false;
//            }
//        }
//    }

    private void startCalibrationAfterCapture(){
        mCounterCpatureOnce.waitCount();
        Log.d(TAG, "startCalibrationAfterCapture");
        //1. stop preview
        try {
            if(mSession_t != null){
                mSession_t.stopRepeating();
            }
            if(mSession_w != null){
                mSession_w.stopRepeating();
            }
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
        if(mPreviewView_t != null){
            mPreviewView_t.setVisibility(View.INVISIBLE);
        }
        if(mPreviewView_w != null){
            mPreviewView_w.setVisibility(View.INVISIBLE);
        }
        if (mCameraSound != null) {
            mCameraSound.play(MediaActionSound.SHUTTER_CLICK);
        }
        // 2.do verify work
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                showCalibrationAlermDialog("Camera Verification...", "Calibration need some time,Please take away the phone.", Color.BLUE, Color.WHITE);
            }
        });


        long begin = System.currentTimeMillis();
        Log.e(TAG,"start Verification");
        int result = NativeCameraCalibration.native_WTCameraVerification(T_YUV_PATH,W_YUV_PATH,DST_RESULT_BIN_FILE,mTCaptureWidth,mTCaptureHeight,mWCaptureWidth,mWCaptureHeight);
        long costTime = System.currentTimeMillis() - begin;
        Log.d("APK_CameraCalibration", "*********** Time: " + costTime + "ms ***********");

        try {
//            final StringBuffer text = new StringBuffer();
//            double rms = NativeCameraCalibration.native_getWTCameraVerificationRMS();
//            Log.d(TAG, "startVerification rms =" + rms);
//            text.append(rms);
//            showCalibrationResult(result, "\n RMS  = " + text.toString(), costTime);
            //w+t verification will not show any data
            showCalibrationResult(result,"verification result is " + result, costTime);
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
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


    private void showCalibrationResult(int testResult, String testData, long costTime) {
        Log.d(TAG, "showCalibrationResult testResult=" + testResult);
        Log.d(TAG, "showCalibrationResult testData=" + testData);
        isSuccess = testResult == 0 ? true : false;
        try {
            Intent intent = new Intent(CameraWTVerificationActivity.this, ZoomCalibrationResultActivity.class);
            intent.putExtra(ZoomCalibrationResultActivity.KEY_TEST_RESULT, testResult);
            intent.putExtra(ZoomCalibrationResultActivity.KEY_TEST_DATA, testData);
            intent.putExtra(Const.INTENT_PARA_TEST_NAME, WTCAMERA_VERIFICATION_TEST_NAME);
            intent.putExtra(ZoomCalibrationResultActivity.KEY_TEST_TIME, costTime);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void finish() {
        if (zoomStage.equals("1")){
            Intent intent = new Intent();
            intent.putExtra(Const.ZOOM_CALIBRATION_RESULT,isSuccess?Const.SUCCESS:Const.FAIL);
            setResult(Const.ZOOM_STAGE_1_RESULTCODE,intent);
        }else if (zoomStage.equals("2")){
            Intent intent = new Intent();
            intent.putExtra(Const.ZOOM_CALIBRATION_RESULT,isSuccess?Const.SUCCESS:Const.FAIL);
            setResult(Const.ZOOM_STAGE_2_RESULTCODE,intent);
        }
        super.finish();
    }

    @Override
    public void storeRusult(boolean isSuccess) {}

    private boolean checkFileExists(String filePath){
        File file = new File(filePath);
        boolean exists = file.exists();
        return exists;
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


    private void initLooper() {
        Log.i(TAG, "initLooper in");
        mThreadHandler_t = new HandlerThread("calibration_t");
        mThreadHandler_t.start();
        mHandler_t = new Handler(mThreadHandler_t.getLooper());
        mThreadHandler_w = new HandlerThread("calibration_w");
        mThreadHandler_w.start();
        mHandler_w = new Handler(mThreadHandler_w.getLooper());
        Log.i(TAG, "initLooper out");
    }


    private void stopLooper() {

        try {
            mThreadHandler_t.quit();
            mThreadHandler_t.join();
            mThreadHandler_t = null;
            mHandler_t = null;

            mThreadHandler_w.quit();
            mThreadHandler_w.join();
            mThreadHandler_w = null;
            mHandler_w = null;
        } catch (Exception e) {
            Log.d(TAG, "StopLooper" + e.toString());
            e.printStackTrace();
        }

    }


    private boolean mHasPermissions;
    private boolean checkPermissions() {
        if ((checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
                || (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
            //Intent intent = new Intent(this, PermissionGrant.class);
            //startActivity(intent);
            mHasPermissions = false;
            Log.e(TAG,"oncreate check Permissions false");
            return false;
        }
        mHasPermissions = true;
        Log.e(TAG,"oncreate check Permissions true");
        return true;
    }


    private static final int MSG_PASS = 1;
    private static final int MSG_FAIL = 2;
    private static final int MSG_DISMISS_DIALOG = 3;

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_PASS:
                    int result = msg.arg1;
                    Toast.makeText(CameraWTVerificationActivity.this,
                            "Camera STL3D Verification PASS!\n result=" + result,
                            Toast.LENGTH_SHORT).show();
                    storeRusult(true);
                    // finish();
                    break;
                case MSG_FAIL:
                    result = msg.arg1;
                    Toast.makeText(CameraWTVerificationActivity.this,
                            "Camera STL3D Verification FAIL!\n result=" + result,
                            Toast.LENGTH_SHORT).show();
                    storeRusult(false);
                    // finish();
                    break;
                case MSG_DISMISS_DIALOG:
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
        }
    };


    private void dismissDialog() {
        Message message = Message.obtain();
        message.what = MSG_DISMISS_DIALOG;
        mHandler.sendMessage(message);
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

        LayoutInflater inflater = LayoutInflater.from(CameraWTVerificationActivity.this);
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
                    CameraWTVerificationActivity.this)
                    .setView(mCusDialogView)
                    .setCancelable(false);
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                // TODO Auto-generated method stub
            }
        });
        mCaptureAlermDialog = dialog.create();
        mCaptureAlermDialog.show();
    }

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

        LayoutInflater inflater = LayoutInflater.from(CameraWTVerificationActivity.this);
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
                    CameraWTVerificationActivity.this)
                    .setView(mCusDialogView)
                    .setCancelable(false);
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                // TODO Auto-generated method stub
            }
        });
        mCalibrationAlermDialog = dialog.create();
        mCalibrationAlermDialog.show();
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

        LayoutInflater inflater = LayoutInflater.from(CameraWTVerificationActivity.this);
        View mCusDialogView = inflater.inflate(R.layout.camera_calibration_dialog_layout, null);
        TextView tileView = (TextView) mCusDialogView.findViewById(R.id.camera_calibration_dialog_title);
        TextView textView = (TextView) mCusDialogView.findViewById(R.id.camera_calibration_dialog_text);
        tileView.setText(title);
        textView.setText(text);
        AlertDialog.Builder dialog  = new AlertDialog.Builder(CameraWTVerificationActivity.this)
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
//                                closeCamera();
//                                openCamera(T_CAMERA_ID);
//                                openCamera(W_CAMERA_ID);
//                                if(mPreviewView_t != null){
//                                    mPreviewView_t.setVisibility(View.VISIBLE);
//                                }
//                                if(mPreviewView_w != null){
//                                    mPreviewView_w.setVisibility(View.VISIBLE);
//                                }
                            }
                        });
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                // TODO Auto-generated method stub
            }
        });
        mAlermDialog = dialog.create();
        mAlermDialog.show();
    }

    private AlertDialog mRmsDialog = null;

    private void showRmsDialog(String title,String text) {
        if (mRmsDialog != null && mRmsDialog.isShowing()) {
            mRmsDialog.dismiss();
            mRmsDialog = null;
        }
        if (isFinishing() || isDestroyed()) {
            Log.w(TAG, "showDialog activity isDestroyed!");
            return;
        }
        AlertDialog.Builder dialog = new AlertDialog.Builder(
                CameraWTVerificationActivity.this)
                .setTitle(title)
                .setMessage(text)
                .setCancelable(false)
                .setNegativeButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                // TODO Auto-generated method stub
                                CameraWTVerificationActivity.this.finish();
                            }
                        });
        mRmsDialog = dialog.create();
        mRmsDialog.show();
    }

}
