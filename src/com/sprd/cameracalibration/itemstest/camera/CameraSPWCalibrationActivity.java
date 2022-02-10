package com.sprd.cameracalibration.itemstest.camera;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.hardware.camera2.params.StreamConfiguration;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaActionSound;
import android.os.Build;
import android.os.Bundle;
import android.os.EnvironmentEx;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.os.SystemProperties;

import com.sprd.cameracalibration.BaseActivity;
import com.sprd.cameracalibration.Const;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import android.Manifest;
import com.sprd.cameracalibration.R;
import com.sprd.cameracalibration.utils.CameraUtil;
import com.sprd.cameracalibration.utils.ImageUtils;
import com.sprd.cameracalibration.utils.OtpDataUtil;
import com.sprd.cameracalibration.utils.SharedPreferencesUtil;

/**
 * Created by SPREADTRUM\emily.miao on 19-5-23.
 */

public class CameraSPWCalibrationActivity extends BaseActivity {

    private static final String TAG = "CameraSPWCalibrationActivity";
    public static final String SPW_CAMERA_CALIBRATION_TEST_NAME = "Ultrawide Calibration";

    // /storage/emulated/0/cali
    private static final String YUV_TMP_PATH = EnvironmentEx
            .getInternalStoragePath().getAbsolutePath() + "/cali/";


    private static final String YUV_PATH_SPW = YUV_TMP_PATH
            + "spw_cali.yuv";

    private static final String SPW_CAMERA_ID = SharedPreferencesUtil.getPrefString("spw_cameraid","35");

    private static final int CAMERA_CAPTURE_WIDTH = 3264;
    private static final int CAMERA_CAPTURE_HEIGHT = 2448;

    private int mCaptureWidth = CAMERA_CAPTURE_WIDTH;
    private int mCaptureHeight = CAMERA_CAPTURE_HEIGHT;


    private Handler mHandler_cam;
    private HandlerThread mThreadHandler;

    private CaptureRequest.Builder mPreviewBuilder;

    private Size mPreviewSize;

    private TextureView mPreviewView;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mSession;
    private ImageReader mImageReader;

    private int mState;
    private static final int STATE_PREVIEW = 0;
    private static final int STATE_WAITING_CAPTURE = 1;
    private static final int STATE_SRART_CAPTURE = 2;
    private static final int STATE_START_CHECK_SENDRESULT = 3;

    private boolean picReady1 = false;

    private MediaActionSound mCameraSound;

    private Button mTakePhotoBtn = null;

    private OtpDataUtil mOtpDataUtil = new OtpDataUtil();
    /**
     * A {@link Semaphore} to prevent the app from exiting before closing the
     * camera.
     */
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    private SharedPreferences mPrefs = null;


//    private boolean mLeftYuvPrepare = false;
//    private boolean mRightYuvPrepare = false;
//    private boolean mCalibrationBegin = false;
//    private boolean mCalibrationDone = false;

    private int[] mSupportFocus;

    public static final CaptureRequest.Key<byte[]> ANDROID_SPRD_CALIBRATION_OTPDATA = new CaptureRequest.Key<byte[]>(
            "com.addParameters.otpData",byte[].class);

    public static final CaptureResult.Key<Integer> ANDROID_SPRD_CALIBRATION_OTP_SENDRESULT = new CaptureResult.Key<Integer>(
            "com.addParameters.otpDataSendResult",Integer.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_spw);
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
                if (isSupportAutoFocus()) {
                    mState = STATE_WAITING_CAPTURE;
                    autoFocus();
                } else {
                    mState = STATE_SRART_CAPTURE;
                }
            }
        });
        mTakePhotoBtn.setEnabled(true);
        if (mCameraSound == null) {
            mCameraSound = new MediaActionSound();
            mCameraSound.load(MediaActionSound.SHUTTER_CLICK);
            mCameraSound.load(MediaActionSound.FOCUS_COMPLETE);
        }

        mPreviewView = (TextureView) findViewById(R.id.sur);

        if (mPassButton != null) {
            mPassButton.setVisibility(View.GONE);
        }
        if (mFailButton != null) {
            mFailButton.setText("Exit");
        }
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
//        mCalibrationBegin = false;
    }


    private boolean mPause = false;
    @Override
    public void onResume() {
        super.onResume();
        mPause = false;
        initLooper();

        if (mPreviewView.isAvailable()) {
            openCamera(SPW_CAMERA_ID);
        } else {
            startTestPreviewView();
        }
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause start!");
        mPause = true;
        /* SPRD bug 857711:Maybe cause ANR */
        closeCamera();
        /* @} */
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

    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mSession) {
                mSession.close();
                mSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mImageReader) {
                mImageReader.close();
                mImageReader = null;
            }
        } catch (Exception e) {
            Log.d(TAG, e.toString());
            e.printStackTrace();
        } finally {
            mCameraOpenCloseLock.release();
        }
    }
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

            if (SPW_CAMERA_ID.equals(cameraId)){
                if (!mCameraOpenCloseLock.tryAcquire(2500,
                        TimeUnit.MILLISECONDS)) {
                    throw new RuntimeException(
                            "Time out waiting to lock camera opening.");
                }
                manager.openCamera(cameraId, mCameraDeviceStateCallback,
                        mHandler_cam);
                mSupportFocus = manager.getCameraCharacteristics(cameraId).get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
            }else{
                Log.e(TAG, "Wrong camera ID");
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void startPreview(CameraDevice camera) {
        Log.i(TAG, "start preview");

        if (isFinishing() || isDestroyed() || mPause) {
            Log.e(TAG, "start preview isFinishing or isDestroyed");
            return;
        }
        SurfaceTexture texture = mPreviewView.getSurfaceTexture();
        texture.setDefaultBufferSize(800, 600);
        Surface surface = new Surface(texture);
        try {

            mPreviewBuilder = camera
                    .createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewBuilder.set(
                    CaptureRequest.ANDROID_SPRD_3DCALIBRATION_ENABLED, 1);
            Log.i(TAG, "start preview set ANDROID_SPRD_3DCALIBRATION_ENABLED");
            /* SPRD bug 866105:Fix preview issue */
            // Make sure camera output frame rate is set to correct value.
            Range<Integer> fpsRange = Range.create(20, 20);
            mPreviewBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                    fpsRange);
        } catch (CameraAccessException e) {
            Log.i(TAG, e.toString());
        }
        mPreviewBuilder.addTarget(surface);
        try {
            //getLargestYuvSize(SPW_CAMERA_ID); //bugfix 1155742
            int[] widthAndHeight = CameraUtil.getLargestYuvSize(this,SPW_CAMERA_ID);
            if (widthAndHeight != null && widthAndHeight[0] != 0 && widthAndHeight[1] != 0){
                mCaptureWidth = widthAndHeight[0];
                mCaptureHeight = widthAndHeight[1];
            }
            Log.d(TAG, "startPreview mCaptureWidth=" + mCaptureWidth + ",mCaptureHeight=" + mCaptureHeight);
            mImageReader = ImageReader.newInstance(mCaptureWidth,
                    mCaptureHeight, ImageFormat.YUV_420_888, 1);
            mImageReader.setOnImageAvailableListener(
                    mOnImageAvailableListener, mHandler_cam);
            if (isFinishing() || isDestroyed() || mPause) {
                Log.e(TAG, "start preview isFinishing or isDestroyed");
                return;
            }
            camera.createCaptureSession(
                    Arrays.asList(surface, mImageReader.getSurface()),
                    mCameraCaptureSessionStateCallback, mHandler_cam);

        } catch (CameraAccessException e) {
            Log.i(TAG, e.toString());
        }
    }


    private boolean mFocusing = false;
    private void autoFocus() {
        try {
            Log.d(TAG, "autoFocus start mFocusing =" + mFocusing);
            if (mFocusing) {
                Log.d(TAG, "autoFocus mFocusing=" + mFocusing);
                return;
            }
            final CaptureRequest.Builder captureBuilder = mPreviewBuilder;
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_AUTO);
            captureBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CaptureRequest.CONTROL_AF_TRIGGER_START);
            Log.d(TAG, "autoFocus capture  in camera");
            // Add start focus
            if (mCameraSound != null) {
                mCameraSound.play(MediaActionSound.FOCUS_COMPLETE);
            }
            mSession.capture(captureBuilder.build(), deferredCallbackSetter,
                    mHandler);
            mSession.setRepeatingRequest(captureBuilder.build(),
                    mSessionCaptureCallback, mHandler_cam);
            captureBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_IDLE);
            mFocusing = true;
        } catch (Exception e) {
            Log.d(TAG, "autoFocus capture a picture fail" + e.toString());
        }
    }

    boolean mSaveOtpSuccess = false;
    private void checkState(CaptureResult result) {
        switch (mState) {
            case STATE_PREVIEW:
                break;
            case STATE_WAITING_CAPTURE:
                int aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                if (aeState != CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                    Log.d(TAG, "checkState start aeState="+aeState);
                    return;
                }
                Integer afStateMaybe = result.get(CaptureResult.CONTROL_AF_STATE);
                Log.d(TAG, "checkState afStateMaybe=" + afStateMaybe);
                if (afStateMaybe == CameraMetadata.CONTROL_AF_STATE_FOCUSED_LOCKED
                        || afStateMaybe == CameraMetadata.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED) {
                    mState = STATE_SRART_CAPTURE;
                }
                break;
            case STATE_SRART_CAPTURE:
                mState = STATE_PREVIEW;
                captureStillPicture();
                break;
            case STATE_START_CHECK_SENDRESULT:
                // check send otp data result
                Integer sendResult = result.get(ANDROID_SPRD_CALIBRATION_OTP_SENDRESULT);
                Log.e(TAG,"get result from hal sendResult = " + sendResult);
                if (sendResult != null && sendResult == 1){
                    mSaveOtpSuccess = true;
                    showCalibrationResultWorker(showdata);
                    mState = STATE_PREVIEW;
                }
                if (sendResult != null && sendResult == 2){
                    mSaveOtpSuccess = false;
                    showCalibrationResultWorker(showdata);
                    mState = STATE_PREVIEW;
                }
                break;
        }


    }

    private void captureStillPicture() {
        try {
            final CaptureRequest.Builder captureBuilder = mCameraDevice
                    .createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());
            if (isSupportAutoFocus()) {
                captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_OFF);
            }
            CameraCaptureSession.CaptureCallback CaptureCallback = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(CameraCaptureSession session,
                                               CaptureRequest request, TotalCaptureResult result) {
                    picReady1 = true;
                }
            };
            Log.d(TAG, "capture  in camera");
            mSession.capture(captureBuilder.build(), CaptureCallback,
                    mHandler_cam);
        } catch (Exception e) {
            Log.d(TAG, "capture a picture fail" + e.toString());
        }
    }

    public void startTestPreviewView() {
        mPreviewView
                .setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {

                    @Override
                    public void onSurfaceTextureAvailable(
                            SurfaceTexture surface, int width, int height) {
                        Log.d(TAG, "onSurfaceTextureAvailable");
                        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
                        CameraCharacteristics characteristics = null;
                        try {
                            characteristics = cameraManager
                                    .getCameraCharacteristics(SPW_CAMERA_ID);
                        } catch (CameraAccessException e) {
                            Log.d(TAG, "onSurfaceTextureAvailable" + e.toString());
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
                        openCamera(SPW_CAMERA_ID);
                    }

                    @Override
                    public void onSurfaceTextureSizeChanged(
                            SurfaceTexture surface, int width, int height) {

                    }

                    @Override
                    public boolean onSurfaceTextureDestroyed(
                            SurfaceTexture surface) {
                        Log.i(TAG,
                                "mPreviewView.setSurfaceTextureListener stay onSurfaceTextureDestroyed");
                        return false;
                    }

                    @Override
                    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
                    }
                });
    }



    private CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(CameraDevice camera) {
            Log.i(TAG, "CameraDevice.StateCallback onOpened in");
            mCameraOpenCloseLock.release();
            mCameraDevice = camera;
            startPreview(camera);
            Log.i(TAG, "CameraDevice.StateCallback onOpened out");
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            mCameraOpenCloseLock.release();
            camera.close();
            Log.i(TAG, "CameraDevice.StateCallback stay onDisconnected");
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            mCameraOpenCloseLock.release();
            camera.close();
            Log.i(TAG, "CameraDevice.StateCallback stay onError");
        }
    };

    private CameraCaptureSession.CaptureCallback mSessionCaptureCallback = new CameraCaptureSession.CaptureCallback() {

        @Override
        public void onCaptureCompleted(CameraCaptureSession session,
                                       CaptureRequest request, TotalCaptureResult result) {
            mSession = session;
            checkState(result);
        }

        @Override
        public void onCaptureProgressed(CameraCaptureSession session,
                                        CaptureRequest request, CaptureResult partialResult) {
            mSession = session;
        }

    };

    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
//            mLeftYuvPrepare = true;
            Log.d(TAG, "mOnImageAvailableListener mLeftYuvPrepare!");
            mHandler_cam.post(new ImageSaver(reader.acquireNextImage(), new File(YUV_PATH_SPW)));
        }

    };

    private CameraCaptureSession.StateCallback mCameraCaptureSessionStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession session) {
            if (isFinishing() || isDestroyed() || mPause) {
                Log.e(TAG,"onConfigured mCameraCaptureSessionStateCallback isFinishing or isDestroyed");
                return;
            }
            try {
                mSession = session;
                mSession.setRepeatingRequest(mPreviewBuilder.build(),
                        mSessionCaptureCallback, mHandler_cam);
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

    CameraCaptureSession.CaptureCallback deferredCallbackSetter = new CameraCaptureSession.CaptureCallback() {

        @Override
        public void onCaptureProgressed(CameraCaptureSession session,
                                        CaptureRequest request, CaptureResult result) {
            Log.d(TAG, "deferredCallbackSetter onCaptureProgressed");
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession session,
                                       CaptureRequest request, TotalCaptureResult result) {
            Log.d(TAG, "deferredCallbackSetter onCaptureCompleted");
            mFocusing = false;
        }

        @Override
        public void onCaptureFailed(CameraCaptureSession session,
                                    CaptureRequest request, CaptureFailure failure) {
            Log.e(TAG, "Focusing failed with reason " + failure.getReason());
        }
    };


    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight()
                    - (long) rhs.getWidth() * rhs.getHeight());
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
                startCalibrationAfterCapture();
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

//    synchronized private void startCalibrationWork() {
//        Log.d(TAG, "startCalibrationWork mLeftYuvPrepare=" + mLeftYuvPrepare
//                + ",mRightYuvPrepare=" + mRightYuvPrepare + ",mCalibrationBegin=" + mCalibrationBegin);
//        if (mLeftYuvPrepare &&!mCalibrationBegin) {
//            mCalibrationBegin = true;
//            Log.d(TAG, "before startCalibrationAfterCapture mCalibrationDone=" + mCalibrationDone);
//            startCalibrationAfterCapture();
//            Log.d(TAG, "after startCalibrationAfterCapture mCalibrationDone=" + mCalibrationDone);
//            mCalibrationDone = true;
//            mCalibrationBegin = false;
//            mLeftYuvPrepare = false;
//            mRightYuvPrepare = false;
//        }
//        Log.d(TAG, "in the end startCalibrationWork mCalibrationDone=" + mCalibrationDone);
//    }

    private long mCalibrationBeginTime = 0;
    private long mCalibrationEndTime = 0;
    private long mCalibrationTime = 0;
    private String showdata = null;

    private void startCalibrationAfterCapture() {
        if (mCameraSound != null) {
            mCameraSound.play(MediaActionSound.SHUTTER_CLICK);
        }
        // 2.do verify work
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                showCalibrationAlermDialog("Camera calibration...", "Calibration need some time,Please take away the phone.", Color.BLUE, Color.WHITE);
            }
        });
        mCalibrationBeginTime = System.currentTimeMillis();
        Log.d(TAG, "startCalibrationAfterCapture begin mCalibrationBeginTime=" + mCalibrationBeginTime);

        int result = NativeCameraCalibration.native_SpwCameraCalibrationYUV(YUV_PATH_SPW, mCaptureWidth, mCaptureHeight);

        Log.d(TAG, "startCalibration end result=" + result);
        mCalibrationEndTime = System.currentTimeMillis();
        Log.d(TAG, "startCalibration end mCalibrationEndTime=" + mCalibrationEndTime);
        mCalibrationTime = (mCalibrationEndTime - mCalibrationBeginTime);
        Log.d("APK_MMI", "*********** Time: " + mCalibrationTime + "ms ***********");
        int[] otpData = NativeCameraCalibration.native_getSpwCameraCalibrationOTP();
        getOtpHeader();
        showdata = otpDataToStr(otpData);
        if (result == 0) {
            sendOtpDataToHAL(otpData);
            storeRusult(true);
        } else {
            accumulateTestFailCount();
            try {
                final String resultText = String.format("0x%08x", result);
                Log.d(TAG, "startCalibration end resultText=" + resultText);
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        StringBuffer buffer = new StringBuffer();
                        buffer.append("Calibration time : " + mCalibrationTime + " ms\n");
                        buffer.append("Calibration fail result code : " + resultText + "\n");
                        buffer.append("Calibration fail count : " + getTestCountFail() + "\n");
                        buffer.append("Calibration otp data : " + "\n");
                        buffer.append(showdata + "\n");
                        showAlertDialog("Calibration fail!", buffer.toString());
                    }
                });

                storeRusult(false);
            } catch (Exception e) {
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
    public final static String TEST_COUNT_FAIL = "test_count_fail";
    public void accumulateTestFailCount() {
        try {
            SharedPreferences.Editor editor = mPrefs.edit();
            editor.putInt(TEST_COUNT_FAIL, getTestCountFail() + 1);
            editor.apply();
            editor.commit();
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
    }

    public int getTestCountFail(){
        int testCount = mPrefs.getInt(TEST_COUNT_FAIL, 0);
        Log.d(TAG, "getTestCount testCount="+testCount);
        return testCount;
    }
    private String otpDataToStr(int[] data){
        String s = null;
        try {
            StringBuffer stringBuffer = new StringBuffer();
            DecimalFormat df = new DecimalFormat("0.00");
            double base = Math.pow(2.0, 19.0);
            stringBuffer.append("Cx: " + df.format(data[0]/base) + "\n");
            stringBuffer.append("Cy: " + df.format(data[1]/base) + "\n");
            stringBuffer.append("Fx: " + df.format(data[2]/base) + "\n");
            stringBuffer.append("Fy: " + df.format(data[3]/base) + "\n");
            s = stringBuffer.toString();
        }catch (Exception e){
            e.printStackTrace();
        }

        return s;
    }

    private void showCalibrationResultWorker(String data) {
        int testResult = mSaveOtpSuccess ? 0 : -1;
        try {
            showCalibrationResult(testResult, data);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void showCalibrationResult(int testResult, String testData) {
        Log.d(TAG, "showCalibrationResult testResult=" + testResult);
        Log.d(TAG, "showCalibrationResult testData=" + testData);
        isSuccess = testResult == 0 ? true:false;
        try {
            Intent intent = new Intent(CameraSPWCalibrationActivity.this,
                    CameraCalibrationResultActivity.class);
            intent.putExtra(CameraCalibrationResultActivity.KEY_TEST_RESULT,
                    testResult);
            intent.putExtra(CameraCalibrationResultActivity.KEY_TEST_DATA,
                    testData);
            intent.putExtra(Const.INTENT_PARA_TEST_NAME,
                    SPW_CAMERA_CALIBRATION_TEST_NAME);
            intent.putExtra(CameraCalibrationResultActivity.KEY_TEST_TIME,
                    mCalibrationTime);
            startActivity(intent);
            storeRusult(isSuccess);
            finish();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendOtpDataToHAL(int[] data){
        mOtpDataUtil.setSpwHeadData(mOtpHeader);
        mOtpDataUtil.setSpwOtpData(data);
        byte[] otpDataByteArray = mOtpDataUtil.getFinalOtpDataByteArray();

        // send data to HAL by tag
        mPreviewBuilder.set(ANDROID_SPRD_CALIBRATION_OTPDATA,otpDataByteArray);
        try {
            mSession.setRepeatingRequest(mPreviewBuilder.build(),mSessionCaptureCallback, mHandler_cam);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.e(TAG,"send otp data to Hal");
        mState = STATE_START_CHECK_SENDRESULT;
    }
    private int[] mOtpHeader;
    private void getOtpHeader(){
        mOtpHeader = NativeCameraCalibration.native_getSpwOTPHeader();
    }
    private void dismissDialog(){
        if (mCaptureAlermDialog != null && mCaptureAlermDialog.isShowing()) {
            mCaptureAlermDialog.cancel();
        }
        if (mCalibrationAlermDialog != null && mCalibrationAlermDialog.isShowing()) {
            mCalibrationAlermDialog.cancel();
        }
    }

    private void initLooper() {
        Log.i(TAG, "initLooper in");
        mThreadHandler = new HandlerThread("spwcameracalibration");
        mThreadHandler.start();
        mHandler_cam = new Handler(mThreadHandler.getLooper());
        Log.i(TAG, "initLooper out");
    }

    private void stopLooper() {

        try {
            mThreadHandler.quit();
            mThreadHandler.join();
            mThreadHandler = null;
            mHandler_cam = null;
        } catch (Exception e) {
            Log.d(TAG, "StopLooper" + e.toString());
            e.printStackTrace();
        }

    }

    private static final int MSG_PASS = 1;
    private static final int MSG_FAIL = 2;
    private static final int MSG_DISMISS_DIALOG = 3;

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_PASS:
                    int result = msg.arg1;
                    Toast.makeText(CameraSPWCalibrationActivity.this,
                            "Camera Calibration PASS!\n result=" + result,
                            Toast.LENGTH_SHORT).show();
                    storeRusult(true);
                    // finish();
                    break;
                case MSG_FAIL:
                    result = msg.arg1;
                    Toast.makeText(CameraSPWCalibrationActivity.this,
                            "Camera Calibration FAIL!\n result=" + result,
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

        LayoutInflater inflater = LayoutInflater.from(CameraSPWCalibrationActivity.this);
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
                    CameraSPWCalibrationActivity.this)
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

        LayoutInflater inflater = LayoutInflater.from(CameraSPWCalibrationActivity.this);
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
                    CameraSPWCalibrationActivity.this)
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

        LayoutInflater inflater = LayoutInflater.from(CameraSPWCalibrationActivity.this);
        View mCusDialogView = inflater.inflate(R.layout.camera_calibration_dialog_layout, null);
        TextView tileView = (TextView) mCusDialogView.findViewById(R.id.camera_calibration_dialog_title);
        TextView textView = (TextView) mCusDialogView.findViewById(R.id.camera_calibration_dialog_text);
        tileView.setText(title);
        textView.setText(text);

        AlertDialog.Builder dialog = new AlertDialog.Builder(
                    CameraSPWCalibrationActivity.this)
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
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                // TODO Auto-generated method stub
            }
        });
        mAlermDialog = dialog.create();
        mAlermDialog.show();
    }

    private boolean isSupportAutoFocus() {
        for (int i=0;i<mSupportFocus.length;i++) {
            Log.d(TAG,"support focus mode is "+mSupportFocus[i]);
            if (mSupportFocus[i] == CameraMetadata.CONTROL_AF_MODE_AUTO) {
                return true;
            }
        }
        return false;
    }
}
