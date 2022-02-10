package com.sprd.cameracalibration.itemstest.camera;

import android.Manifest;
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
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaActionSound;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.EnvironmentEx;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Range;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.glframe.GLFrameSurface;
import com.sprd.cameracalibration.BaseActivity;
import com.sprd.cameracalibration.Const;
import com.sprd.cameracalibration.R;
import com.sprd.cameracalibration.itemstest.camera.CameraCalibrationResultActivity;
import com.sprd.cameracalibration.itemstest.camera.NativeCameraCalibration;
import com.sprd.cameracalibration.utils.OtpDataUtil;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Created by SPREADTRUM\emily.miao on 19-8-15.
 */

public class CameraSTL3DCalibrationActivity extends BaseActivity {

    private static final String TAG = "CameraSTL3DCalibrationActivity";
    public static final String CAMERA_STL3D_CALI_TEST_NAME = "STL3D Calibration";
    private static final String SAVE_PATH = EnvironmentEx
            .getInternalStoragePath().getAbsolutePath() + "/cali/";
    private static final String YUV_PATH = SAVE_PATH + "yuv_preview_cali.yuv";
    private static final String IR_LEFT_PATH = SAVE_PATH + "raw_ir_left_cali.raw";
    private static final String IR_RIGHT_PATH = SAVE_PATH + "raw_ir_right_cali.raw";


    public static final int CALLBACK_WIDTH = 2592;
    public static final int CALLBACK_HEIGTH = 1944;

    public static final int PREVIEW_WIDTH = 1440;
    public static final int PREVIEW_HEIGTH = 1080;

    private static final int IR_WIDTH = 640;
    private static final int IR_HEIGTH = 480;

    private ImageReader mImageReader;

    private int mIRSize = IR_HEIGTH * IR_WIDTH;
    private int mYUVSize = PREVIEW_HEIGTH * PREVIEW_WIDTH * 3 / 2;

    private boolean mFlagOne = false;

    CountDownLatch countDownLatch = new CountDownLatch(1);
    //private static final String STL3D_CAMERA_ID = SystemProperties.get("persist.sys.stl3d.cameraid","17");
    private static String STL3D_CAMERA_ID = "17";

    private MediaActionSound mCameraSound;
    private Button mTakePhotoBtn = null;

    private CameraDevice mCameraDevice;
    private CaptureRequest.Builder mYuvPreviewBuilder;
    private CameraCaptureSession mSession;


    TextureView mYuvPreview;
    Surface mSurface;
    GLFrameSurface mIRLeftPreview;
    GLFrameSurface mIRRigthPreview;

    private boolean mSaveCallback = false;

    private Semaphore mCameraOpenCloseLock = new Semaphore(1);
    private SharedPreferences mPrefs = null;
    private OtpDataUtil mOtpDataUtil = new OtpDataUtil();

    private int mState;
    private static final int STATE_PREVIEW = 0;
    private static final int STATE_START_CHECK_SENDRESULT = 1;

    public static final CaptureRequest.Key<byte[]> ANDROID_SPRD_CALIBRATION_OTPDATA = new CaptureRequest.Key<byte[]>(
            "com.addParameters.otpData",byte[].class);

    public static final CaptureResult.Key<Integer> ANDROID_SPRD_CALIBRATION_OTP_SENDRESULT = new CaptureResult.Key<Integer>(
            "com.addParameters.otpDataSendResult",Integer.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!checkPermissions()) {
            finish();
            return;
        }

        setContentView(R.layout.camera_stl3d);
        if (mCameraSound == null) {
            mCameraSound = new MediaActionSound();
            mCameraSound.load(MediaActionSound.SHUTTER_CLICK);
        }
        mTakePhotoBtn = (Button) findViewById(R.id.start_take_picture);
        mTakePhotoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "mTakePhotoBtn onClick");
                if(mHandlerForShow != null){
                    mHandlerForShow.post(new Runnable() {
                        @Override
                        public void run() {
                            mTakePhotoBtn.setEnabled(false);
                            //showCaptureAlermDialog("Capture picture...", "Don't move phone!", Color.YELLOW, Color.RED);
                        }
                    });
                }

                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    // TODO: handle exception
                }
                Log.i(TAG, " mShutterButton onClick mSaveCallback = " + mSaveCallback);
                if (mCameraSound != null) {
                    mCameraSound.play(MediaActionSound.SHUTTER_CLICK);
                }
                if (!mSaveCallback) {
                    mSaveCallback = true;
                } else {
                    mSaveCallback = false;
                }
            }
        });

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        mFlagOne = false;
        mYuvPreview = (TextureView) findViewById(R.id.surfaceView);
        mYuvPreview.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(
                    SurfaceTexture surface, int width, int height) {
                Log.d(TAG, "TextureView available");
                Log.i(TAG, " SurfaceTexture width = " + width + " height = " + height);

                surface.setDefaultBufferSize(PREVIEW_WIDTH, PREVIEW_HEIGTH);
                mSurface = new Surface(surface);
                //if (width == PREVIEW_WIDTH) {
                countDownLatch.countDown();
                //}
            }

            @Override
            public void onSurfaceTextureSizeChanged(
                    SurfaceTexture surface, int width, int height) {
            }

            @Override
            public boolean onSurfaceTextureDestroyed(
                    SurfaceTexture surface) {
                Log.i(TAG,"TextureView destoryed");
                mSurface = null;
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            }
        });

        mIRLeftPreview = (GLFrameSurface) findViewById(R.id.glsurfaceviewleft);
        mIRRigthPreview = (GLFrameSurface) findViewById(R.id.glsurfaceviewRight);
        mIRLeftPreview.setPreviewSize(IR_WIDTH,IR_HEIGTH);
        mIRRigthPreview.setPreviewSize(IR_WIDTH,IR_HEIGTH);

        if(mPassButton != null){
            mPassButton.setVisibility(View.GONE);
        }
        if (mFailButton != null) {
            mFailButton.setText("Exit");
        }

        mImageReader = ImageReader.newInstance(CALLBACK_WIDTH, CALLBACK_HEIGTH, ImageFormat.RAW_SENSOR, 1);
        mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mHandler);
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
        // mkdir cali
        try {
            File cali = new File(SAVE_PATH);
            if(cali != null && !cali.exists()){
                boolean res = cali.mkdirs();
                if(!res){
                    Log.d(TAG, "mkdirs fail:"+SAVE_PATH);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //open camera
        openCamera(STL3D_CAMERA_ID);
    }


    @Override
    public void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
        mPause = true;
        if (!mHasPermissions) {
            Log.i(TAG, "onPause: Missing permissions.");
            return;
        }
        Log.d(TAG, "onPause end!");
    }


    @Override
    public void onStop(){
        Log.d(TAG, "onStop");
        super.onStop();

        closeCamera();
        stopLooper();
        if (!mHasPermissions) {
            Log.i(TAG, "onStop: Missing permissions.");
            return;
        }
        Log.d(TAG, "onStop end!");
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

        if (mHandlerForShow != null) {
            mHandlerForShow.removeMessages(MSG_DISMISS_DIALOG);
            mHandlerForShow.removeMessages(MSG_FAIL);
            mHandlerForShow.removeMessages(MSG_PASS);
            mHandlerForShow = null;
        }
        Log.d(TAG, "onDestroy end!");

    }

    @Override
    public void onBackPressed() {
    }

    private void openCamera(String cameraId) {
        Log.d(TAG, "openCamera cameraId=" + cameraId);
        CameraManager manager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500,
                    TimeUnit.MILLISECONDS)) {
                throw new RuntimeException(
                        "Time out waiting to lock camera opening.");
            }
            manager.openCamera(cameraId,mCameraDeviceStateCallback,mHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e){
            e.printStackTrace();
        }
    }

    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mSession) {
                Log.d(TAG,"stop preview");
                mSession.stopRepeating();
                mSession.close();
                mSession = null;
            }
            if (null != mCameraDevice) {
                Log.d(TAG,"close camera");
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

    private void startPreview(){
        Log.i(TAG, "start preview");
        try {
            mYuvPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mYuvPreviewBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, new Range(30, 30));
            mYuvPreviewBuilder.addTarget(mSurface);
            Surface callback = mImageReader.getSurface();
            if (callback != null){
                mYuvPreviewBuilder.addTarget(callback);
            }
            Log.i(TAG,"setRepeatingRequest");
            mSession.setRepeatingRequest(mYuvPreviewBuilder.build(),mCaptureCallback,mHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    private long mCalibrationTime = 0;
    private String showdata;
    private void startCalibration(){
        mHandlerForShow.post(new Runnable() {
            @Override
            public void run() {
                showCalibrationAlermDialog("Camera calibration...", "Calibration need some time,Please take away the phone.", Color.BLUE, Color.WHITE);
            }
        });

        long begin = System.currentTimeMillis();
        Log.e(TAG,"start stl3d Calibration");
        int result = NativeCameraCalibration.native_STL3DCameraCalibration(IR_LEFT_PATH,IR_RIGHT_PATH,YUV_PATH);
        mCalibrationTime = System.currentTimeMillis() - begin;
        Log.d("APK_CameraCalibration", "*********** Time: " + mCalibrationTime + "ms ***********");
        Log.d(TAG, "STL3D Calibration result=" + result);

        int[] otpData = NativeCameraCalibration.native_getSTL3DCameraCalibrationOTP();
        int[] headerData = NativeCameraCalibration.native_getSTL3DOTPHeader();
        float[] coordinate = NativeCameraCalibration.native_getSTL3DCoordinate();
        showdata = otpDataToStr(otpData,coordinate);

        if (result == 0) {
            sendOtpDataToHAL(otpData,headerData);
        } else {
            accumulateTestFailCount();
            final String resultText = String.format("0x%08x", result);
            Log.d(TAG, "startCalibration end resultText=" + resultText);
            mHandlerForShow.post(new Runnable() {
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
        }
        dismissDialog();
        mHandlerForShow.post(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                mTakePhotoBtn.setEnabled(true);
            }
        });

    }

    private void sendOtpDataToHAL(int[] data,int[] header){
        mOtpDataUtil.writeSTL3DOtpAndHeaderData(data,header);
        byte[] otpDataByteArray = mOtpDataUtil.getFinalOtpDataByteArray();
        // send data to HAL by tag
        mYuvPreviewBuilder.set(ANDROID_SPRD_CALIBRATION_OTPDATA,otpDataByteArray);
        try {
            mSession.setRepeatingRequest(mYuvPreviewBuilder.build(),mCaptureCallback, mHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.e(TAG,"send otp data to Hal");
        mState = STATE_START_CHECK_SENDRESULT;
    }

    private String otpDataToStr(int[] data,float[] coordinate){
        String s = null;
        try {
            StringBuffer stringBuffer = new StringBuffer();
            DecimalFormat df = new DecimalFormat("0.00");
            double base = Math.pow(2.0, 19.0);
            stringBuffer.append("roll: " + df.format(data[57]/base) + "\n");
            stringBuffer.append("pitch: " + df.format(data[58]/base) + "\n");
            stringBuffer.append("yaw: " + df.format(data[59]/base) + "\n");
            stringBuffer.append("L相机的Cx: " + df.format(data[0]/base) + "\n");
            stringBuffer.append("L相机的Cy: " + df.format(data[1]/base) + "\n");
            stringBuffer.append("R相机的Cx: " + df.format(data[25]/base) + "\n");
            stringBuffer.append("R相机的Cy: " + df.format(data[26]/base) + "\n");
            stringBuffer.append("point cx :" + coordinate[0] + "\n");
            stringBuffer.append("point cy :" + coordinate[1] + "\n");
            s = stringBuffer.toString();
        }catch (Exception e){
            e.printStackTrace();
        }

        return s;
    }


    private void showCalibrationResultWorker(String data) {
        int testResult = mSaveOtpSuccess ? 0 : -1;
        try {
            showCalibrationResult(testResult,data,mCalibrationTime);
        }catch (Exception e){
            e.printStackTrace();
        }
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

    private ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener(){
        @Override
        public void onImageAvailable(ImageReader reader) {
            Log.d(TAG, "onImageAvailable");
                //flush left/rigth ir preview
                try (Image image = reader.acquireNextImage()) {

                    final byte[] pixels;
                    java.nio.ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    pixels = new byte[buffer.remaining()];
                    buffer.get(pixels);
                    byte[] leftImageBytes = new byte[mIRSize];
                    System.arraycopy(pixels, mYUVSize, leftImageBytes, 0, mIRSize);
                    if (mIRLeftPreview != null) {
                        mIRLeftPreview.update(leftImageBytes, null, null);
                    }
                    byte[] rightImageBytes = new byte[mIRSize];
                    System.arraycopy(pixels, mYUVSize+mIRSize, rightImageBytes, 0, mIRSize);
                    if (mIRRigthPreview != null) {
                        mIRRigthPreview.update(rightImageBytes, null, null);
                    }

                    // dumpImageData and calibrate
                    if (mSaveCallback) {
                        mSaveCallback = false;

                        if (mCameraSound != null) {
                            mCameraSound.play(MediaActionSound.SHUTTER_CLICK);
                        }

                        (new AsyncTask<Void, Void, Integer>() {
                            @Override
                            protected Integer doInBackground(Void... arg) {
                                dumpImageData(pixels);
                                int result = 0;
                                //calibrate
                                startCalibration();
                                return result;
                            }

                            @Override
                            protected void onPostExecute( Integer result) {

                            }
                        }).execute();
                    }
                }
            }

    };

    private CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(CameraDevice camera) {
            Log.i(TAG, "CameraDevice onOpened");
            mCameraOpenCloseLock.release();
            mCameraDevice = camera;
            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            //createSession
            try {
                camera.createCaptureSession(Arrays.asList(mSurface, mImageReader.getSurface()), mCameraCaptureSessionStateCallback, mHandler);
            }catch (CameraAccessException e) {
                Log.i(TAG, e.toString());
            }

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

    private CameraCaptureSession.StateCallback mCameraCaptureSessionStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession session) {
            Log.e(TAG,"CameraCaptureSession onConfigured");
            if (isFinishing() || isDestroyed() || mPause) {
                Log.e(TAG,
                        "onConfigured mCameraCaptureSessionStateCallback isFinishing is "+isFinishing()
                                +", isDestroyed is "+isDestroyed()
                                +", mPause is "+mPause);
                return;
            }
            mSession = session;
            //startPreview
            startPreview();
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {

        }

        @Override
        public void onActive(CameraCaptureSession session) {

        }
    };

    private CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {

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

    boolean mSaveOtpSuccess = false;
    private void checkState(CaptureResult result) {
        switch (mState) {
            case STATE_PREVIEW:
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


    private Handler mHandler;
    private HandlerThread mThreadHandler;
    private void initLooper() {
        Log.i(TAG, "initLooper in");
        mThreadHandler = new HandlerThread("stl3dcameraverification");
        mThreadHandler.start();
        mHandler = new Handler(mThreadHandler.getLooper());
        Log.i(TAG, "initLooper out");
    }

    private void stopLooper() {
        try {
            mThreadHandler.quit();
            mThreadHandler.join();
            mThreadHandler = null;
            mHandler = null;
        } catch (Exception e) {
            Log.d(TAG, "StopLooper" + e.toString());
            e.printStackTrace();
        }

    }

    private static final int MSG_PASS = 1;
    private static final int MSG_FAIL = 2;
    private static final int MSG_DISMISS_DIALOG = 3;

    private Handler mHandlerForShow = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_PASS:
                    int result = msg.arg1;
                    Toast.makeText(CameraSTL3DCalibrationActivity.this,
                            "Camera STL3D Verification PASS!\n result=" + result,
                            Toast.LENGTH_SHORT).show();
                    storeRusult(true);
                    // finish();
                    break;
                case MSG_FAIL:
                    result = msg.arg1;
                    Toast.makeText(CameraSTL3DCalibrationActivity.this,
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
        mHandlerForShow.sendMessage(message);
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

        LayoutInflater inflater = LayoutInflater.from(CameraSTL3DCalibrationActivity.this);
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
                    CameraSTL3DCalibrationActivity.this)
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

        LayoutInflater inflater = LayoutInflater.from(CameraSTL3DCalibrationActivity.this);
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
                    CameraSTL3DCalibrationActivity.this)
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

        LayoutInflater inflater = LayoutInflater.from(CameraSTL3DCalibrationActivity.this);
        View mCusDialogView = inflater.inflate(R.layout.camera_calibration_dialog_layout, null);
        TextView tileView = (TextView) mCusDialogView.findViewById(R.id.camera_calibration_dialog_title);
        TextView textView = (TextView) mCusDialogView.findViewById(R.id.camera_calibration_dialog_text);
        tileView.setText(title);
        textView.setText(text);

        AlertDialog.Builder dialog  = new AlertDialog.Builder(CameraSTL3DCalibrationActivity.this);
        dialog.setView(mCusDialogView);

        dialog.setCancelable(false)
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


    private void showCalibrationResult(int testResult, String testData, long costTime) {
        Log.d(TAG, "showCalibrationResult testResult=" + testResult);
        Log.d(TAG, "showCalibrationResult testData=" + testData);
        isSuccess = testResult == 0 ? true:false;
        try {
            Intent intent = new Intent(CameraSTL3DCalibrationActivity.this,
                    CameraCalibrationResultActivity.class);
            intent.putExtra(CameraCalibrationResultActivity.KEY_TEST_RESULT,
                    testResult);
            intent.putExtra(CameraCalibrationResultActivity.KEY_TEST_DATA,
                    testData);
            intent.putExtra(Const.INTENT_PARA_TEST_NAME,
                    CAMERA_STL3D_CALI_TEST_NAME);
            intent.putExtra(CameraCalibrationResultActivity.KEY_TEST_TIME,
                    costTime);
            startActivity(intent);
            storeRusult(isSuccess);
            finish();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void dumpImageData(byte[] pixels) {
        Log.d(TAG,"dumpImageData");
        try (DataOutputStream out_yuv = new DataOutputStream(new FileOutputStream(YUV_PATH));
             DataOutputStream out_irleft = new DataOutputStream(new FileOutputStream(IR_LEFT_PATH));
             DataOutputStream out_irright = new DataOutputStream(new FileOutputStream(IR_RIGHT_PATH))){
            out_yuv.write(pixels,0,mYUVSize);
            out_irleft.write(pixels,mYUVSize,mIRSize);
            out_irright.write(pixels,mYUVSize + mIRSize,mIRSize);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}


