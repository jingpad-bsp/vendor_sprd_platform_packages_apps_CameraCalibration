package com.sprd.cameracalibration.itemstest.camera;

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
import android.hardware.camera2.params.StreamConfiguration;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaActionSound;
import android.os.Build;
import android.os.Bundle;
import android.os.EnvironmentEx;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import com.sprd.cameracalibration.R;
import com.sprd.cameracalibration.utils.CameraUtil;
import com.sprd.cameracalibration.utils.ImageUtils;
import com.sprd.cameracalibration.utils.SharedPreferencesUtil;
import com.sprd.cameracalibration.utils.FileUtils;

import android.Manifest;


/**
 * Created by SPREADTRUM\emily.miao on 19-5-23.
 */

public class CameraSPWVerificationActivity extends BaseActivity {
    private static final String TAG = "CameraSPWVerificationActivity";
    public static final String CAMERA_SPW_VERIFY_TEST_NAME = "Ultrawide Verification";
    private static final String YUV_TMP_PATH = EnvironmentEx
            .getInternalStoragePath().getAbsolutePath() + "/cali/";
    private static final String YUV_PATH = YUV_TMP_PATH + "spwVerification.yuv";

    private int mCaptureWidth = 1600;
    private int mCaptureHeight = 1200;

    private Handler mHandler_cam;
    private HandlerThread mThreadHandler;

    private CaptureRequest.Builder mPreviewBuilder;

    private TextureView mPreviewView;

    private CameraDevice mCameraDevice;

    private CameraCaptureSession mSession;

    private ImageReader mImageReader;

    private int mState;
    private static final int STATE_PREVIEW = 0;
    private static final int STATE_WAITING_CAPTURE = 1;
    private static final int STATE_SRART_CAPTURE = 2;

    private MediaActionSound mCameraSound;

    private Button mTakePhotoBtn = null;

    private String mCameraID;

    /**
     * A {@link Semaphore} to prevent the app from exiting before closing the
     * camera.
     */
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);


    private static final String DST_RESULT_BIN_FILE = YUV_TMP_PATH + "spw_otp_dump.bin";
    
    private boolean mFlagOne = false;
    private int[] mSupportFocus;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_spw);
        if (mCameraSound == null) {
            mCameraSound = new MediaActionSound();
            mCameraSound.load(MediaActionSound.SHUTTER_CLICK);
        }
        mTakePhotoBtn = (Button) findViewById(R.id.start_take_picture);
        mTakePhotoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "mTakePhotoBtn onClick");
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
        mFlagOne = false;
        mPreviewView = (TextureView) findViewById(R.id.sur);
        if(mPassButton != null){
            mPassButton.setVisibility(View.GONE);
        }
        if (mFailButton != null) {
            mFailButton.setText("Exit");
        }
        Log.d(TAG, "I'm 10.5");
        mCameraID = SharedPreferencesUtil.getPrefString("spw_cameraid","35");;
        Log.e(TAG,"SPW CameraID is "+mCameraID);
        try {
            FileUtils.deleteFile(new File(DST_RESULT_BIN_FILE));
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
        if (mPreviewView.isAvailable()) {
            openCamera(mCameraID);
        } else {
            startTestPreviewView();
        }
        /* @} */
    }

    private boolean mPause = false;

    @Override
    public void onPause() {
        Log.d(TAG, "onPause");
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

    private void initLooper() {
        Log.i(TAG, "initLooper in");
        mThreadHandler = new HandlerThread("spwcameraverification");
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
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                    throw new RuntimeException(
                            "Time out waiting to lock camera opening.");
            }
            manager.openCamera(cameraId, mCameraDeviceStateCallback,
                        mHandler_cam);
            mSupportFocus = manager.getCameraCharacteristics(cameraId).get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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


    public static final CaptureRequest.Key<Integer> CONTROL_VERIFICATION_FLAG = new CaptureRequest.Key<Integer>(
            "com.addParameters.sprdSetVerificationFlag", Integer.class);

    private void startPreview(CameraDevice camera) {
        Log.i(TAG, "start preview");

        if (isFinishing() || isDestroyed() || mPause) {
            Log.e(TAG, "start preview before isFinishing is "+isFinishing()+", isDestroyed is "+isDestroyed()+", mPause is "+mPause);
            return;
        }
        SurfaceTexture texture = mPreviewView.getSurfaceTexture();
        texture.setDefaultBufferSize(1600, 1200);
        Surface surface = new Surface(texture);
        try {
            mPreviewBuilder = camera
                    .createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        } catch (CameraAccessException e) {
            Log.i(TAG, e.toString());
        }
        mPreviewBuilder.addTarget(surface);
        try {
            //getLargestYuvSize(mCameraID); //bugfix 1155742
            int[] widthAndHeight = CameraUtil.getLargestYuvSize(this,mCameraID);
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
                Log.e(TAG, "start preview after isFinishing is "+isFinishing()+", isDestroyed is "+isDestroyed()+", mPause is "+mPause);
                return;
            }
            camera.createCaptureSession(
                    Arrays.asList(surface, mImageReader.getSurface()),
                    mCameraCaptureSessionStateCallback, mHandler_cam);

        } catch (CameraAccessException e) {
            Log.i(TAG, e.toString());
        }
    }

    private CameraCaptureSession.StateCallback mCameraCaptureSessionStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession session) {
            if (isFinishing() || isDestroyed() || mPause) {
                Log.e(TAG, "onConfigured mCameraCaptureSessionStateCallback isFinishing is "+isFinishing()
                        +", isDestroyed is "+isDestroyed()
                                +", mPause is "+mPause);
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

    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            Log.d(TAG, "onImageAvailable");
            if (mCameraSound != null) {
                mCameraSound.play(MediaActionSound.SHUTTER_CLICK);
            }
            dismissDialog();
            // 2.do verify work
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    showCalibrationAlermDialog("Camera Verification...", "Verification need some time,Please take away the phone.", Color.BLUE, Color.WHITE);
                }
            });
            mHandler_cam.post(new ImageSaver(reader.acquireNextImage(), new File(YUV_PATH)));
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

    private void startCalibrationAfterCapture(){
        try {
            if(mSession != null){
                mSession.stopRepeating();
            }
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
        if(mPreviewView != null){
            mPreviewView.setVisibility(View.INVISIBLE);
        }
        long begin = System.currentTimeMillis();
        Log.e(TAG,"start Verification");
        final int result = NativeCameraCalibration.native_MonoCameraVerificationNV21_New(YUV_PATH,DST_RESULT_BIN_FILE,mCaptureWidth,mCaptureHeight);
        long costTime = System.currentTimeMillis() - begin;
        Log.d("APK_MMI", "*********** Time: " + costTime + "ms ***********");
        try {
            final StringBuffer text = new StringBuffer();
            double rms = NativeCameraCalibration.native_getMonoCameraVerificationNV21_RMS();
            Log.d(TAG, "startCalibration rms =" + rms);
            text.append(rms);
            showCalibrationResult(result, "\n RMS  = " + text.toString(), costTime);
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
        isSuccess = testResult == 0 ? true:false;
        try {
            Intent intent = new Intent(CameraSPWVerificationActivity.this,
                    CameraCalibrationResultActivity.class);
            intent.putExtra(CameraCalibrationResultActivity.KEY_TEST_RESULT,
                    testResult);
            intent.putExtra(CameraCalibrationResultActivity.KEY_TEST_DATA,
                    testData);
            intent.putExtra(Const.INTENT_PARA_TEST_NAME,
                    CAMERA_SPW_VERIFY_TEST_NAME);
            intent.putExtra(CameraCalibrationResultActivity.KEY_TEST_TIME,
                    costTime);
            startActivity(intent);
            storeRusult(isSuccess);
            finish();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(CameraDevice camera) {
            Log.i(TAG, "CameraDevice onOpened");
            mCameraOpenCloseLock.release();
            mCameraDevice = camera;
            startPreview(camera);
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            mCameraOpenCloseLock.release();
            camera.close();
            Log.i(TAG, "CameraDevice onDisconnected");
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            mCameraOpenCloseLock.release();
            camera.close();
            Log.i(TAG, "CameraDevice onError");
        }
    };

    public void startTestPreviewView() {
        mPreviewView
                .setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {

                    @Override
                    public void onSurfaceTextureAvailable(
                            SurfaceTexture surface, int width, int height) {
                        Log.d(TAG, "TextureView available");
//                        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
//                        CameraCharacteristics characteristics = null;
//                        try {
//                            characteristics = cameraManager
//                                    .getCameraCharacteristics(mCameraID);
//                        } catch (CameraAccessException e) {
//                            Log.d(TAG, "TextureView available" + e.toString());
//                            Toast.makeText(getApplicationContext(),
//                                    R.string.text_fail, Toast.LENGTH_SHORT)
//                                    .show();
//                            storeRusult(false);
//                            finish();
//                            return;
//                        }
                        openCamera(mCameraID);
                    }

                    @Override
                    public void onSurfaceTextureSizeChanged(
                            SurfaceTexture surface, int width, int height) {

                    }

                    @Override
                    public boolean onSurfaceTextureDestroyed(
                            SurfaceTexture surface) {
                        Log.i(TAG,
                                "TextureView destoryed");
                        return false;
                    }

                    @Override
                    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
                    }
                });
    }

    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight()
                    - (long) rhs.getWidth() * rhs.getHeight());
        }

    }
    private boolean mFocusing = false;
    private void autoFocus() {
        try {
            if (mFocusing) {
                Log.e(TAG, "autoFocus is focusing return and not take picture");
                return;
            }
            final CaptureRequest.Builder captureBuilder = mPreviewBuilder;
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_AUTO);
            captureBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CaptureRequest.CONTROL_AF_TRIGGER_START);
            // Add start focus
            if (mCameraSound != null) {
                mCameraSound.play(MediaActionSound.FOCUS_COMPLETE);
            }
            Log.d(TAG, "set auto focus");
            mSession.capture(captureBuilder.build(), deferredCallbackSetter,
                    mHandler);
            mSession.setRepeatingRequest(captureBuilder.build(),
                    mSessionCaptureCallback, mHandler_cam);
            mFocusing = true;
        } catch (Exception e) {
            Log.d(TAG, "set auto focus failed" + e.toString());
        }
    }

    private void saveOtpFile(TotalCaptureResult result){
        try {
            int mDualOtpFlag = result
                    .get(CaptureResult.ANDROID_SPRD_DUAL_OTP_FLAG);
            if(mDualOtpFlag != 3){
                Log.i(TAG, "saveOtpFile mDualOtpFlag = "
                        + mDualOtpFlag);
                if(!mFlagOne){
                    mFlagOne = true;
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            // TODO Auto-generated method stub
                            showAlertDialog("Camera verifying", "Camera verifying FAIL!\nOTP flag == 3!");
                        }
                    });
                }
                return;
            }
            boolean hasDstResultBin = checkFileExists(DST_RESULT_BIN_FILE);
            if(hasDstResultBin){
                return;
            }
            Log.i(TAG, "saveOtpFile mDualOtpFlag = "
                    + mDualOtpFlag);
            byte[] otpByte = result
                    .get(CaptureResult.CONTROL_OTP_VALUE);
            dumpData(otpByte, DST_RESULT_BIN_FILE);
        } catch (Exception e) {
            Log.i(TAG, e.toString());
            e.printStackTrace();
        }
    }

    private void checkState(CaptureResult result) {
        switch (mState) {
            case STATE_PREVIEW:
                // NOTHING
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
            case STATE_SRART_CAPTURE: {
                mState = STATE_PREVIEW;
                Log.d(TAG, "take picture start");
                captureStillPicture();
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
            mSession.capture(captureBuilder.build(), null,
                    mHandler_cam);
        } catch (Exception e) {
            Log.d(TAG, "capture failed" + e.toString());
        }
    }

    private boolean checkFileExists(String filePath){
        File file = new File(filePath);
        boolean exists = file.exists();
        return exists;
    }
    private CameraCaptureSession.CaptureCallback mSessionCaptureCallback = new CameraCaptureSession.CaptureCallback() {

        @Override
        public void onCaptureCompleted(CameraCaptureSession session,
                                       CaptureRequest request, TotalCaptureResult result) {
            mSession = session;
            saveOtpFile(result);
            checkState(result);
        }

        @Override
        public void onCaptureProgressed(CameraCaptureSession session,
                                        CaptureRequest request, CaptureResult partialResult) {
            mSession = session;
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

    private static final int MSG_PASS = 1;
    private static final int MSG_FAIL = 2;
    private static final int MSG_DISMISS_DIALOG = 3;

    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case MSG_PASS:
                    int result = msg.arg1;
                    Toast.makeText(CameraSPWVerificationActivity.this,
                            "Camera SPW Verification PASS!\n result=" + result,
                            Toast.LENGTH_SHORT).show();
                    storeRusult(true);
                    // finish();
                    break;
                case MSG_FAIL:
                    result = msg.arg1;
                    Toast.makeText(CameraSPWVerificationActivity.this,
                            "Camera SPW Verification FAIL!\n result=" + result,
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

        LayoutInflater inflater = LayoutInflater.from(CameraSPWVerificationActivity.this);
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
                    CameraSPWVerificationActivity.this)
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

        LayoutInflater inflater = LayoutInflater.from(CameraSPWVerificationActivity.this);
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
                    CameraSPWVerificationActivity.this)
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

        LayoutInflater inflater = LayoutInflater.from(CameraSPWVerificationActivity.this);
        View mCusDialogView = inflater.inflate(R.layout.camera_calibration_dialog_layout, null);
        TextView tileView = (TextView) mCusDialogView.findViewById(R.id.camera_calibration_dialog_title);
        TextView textView = (TextView) mCusDialogView.findViewById(R.id.camera_calibration_dialog_text);
        tileView.setText(title);
        textView.setText(text);

        AlertDialog.Builder dialog = new AlertDialog.Builder(
                    CameraSPWVerificationActivity.this)
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
                                    closeCamera();
                                    if (mPreviewView.isAvailable()) {
                                        openCamera(mCameraID);
                                    } else {
                                        startTestPreviewView();
                                    }
                                    if(mPreviewView != null){
                                        mPreviewView.setVisibility(View.VISIBLE);
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

    private boolean isSupportAutoFocus() {
        for (int i=0;i<mSupportFocus.length;i++) {
            if (mSupportFocus[i] == CameraMetadata.CONTROL_AF_MODE_AUTO) {
                return true;
            }
        }
        return false;
    }
}
