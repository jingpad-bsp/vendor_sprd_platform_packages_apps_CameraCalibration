#define LOG_TAG "jniSTL3DCameraVerify"
#include "utils/Log.h"

#include <jni.h>
#include <stdio.h>
#include <string.h>
#include <string>
#include <unistd.h>
#include <malloc.h>
#include <dlfcn.h>
#include <time.h>
#include <memory.h>
#include <stdlib.h>

#include "header/typedef.h"
#include "header/DualCameraVerification3D_API.h"
#include "header/bmp_io.h"

double rms,rms_th,rms_x,rms_y;

static jdoubleArray JNIGetSTL3DCameraVerification_RMS(JNIEnv* env, jobject thiz) {
    jdoubleArray jdouble_arr = env->NewDoubleArray(4);
    jdouble *elems = env->GetDoubleArrayElements(jdouble_arr, NULL);

    elems[0] = rms_th;
    elems[1] = rms;
    elems[2] = rms_x;
    elems[3] = rms_y;

    env->ReleaseDoubleArrayElements(jdouble_arr, elems, 0);
    return jdouble_arr;
}

static jint JNISTL3DCameraVerification(JNIEnv* env, jobject thiz,
		jstring irleft_dir,jstring irright_dir,jstring yuv_dir,jstring otpDataPath) {
    const  char *ir_left_path = env->GetStringUTFChars(irleft_dir, 0);
    const  char *ir_rigth_path = env->GetStringUTFChars(irright_dir, 0);
    const  char *yuv_path = env->GetStringUTFChars(yuv_dir, 0);
	const char *otp_data_path = env->GetStringUTFChars(otpDataPath, 0);
	ALOGD("JNISTL3DCameraVerification yuv_path: %s\n",yuv_path);
	ALOGD("JNISTL3DCameraVerification ir_left_path: %s\n",ir_left_path);
	ALOGD("JNISTL3DCameraVerification ir_rigth_path: %s\n",ir_rigth_path);
	ALOGD("JNISTL3DCameraVerification otp_data_path: %s\n",otp_data_path);

	//GetDualCameraVerificationVersion
	char VersionInfo[256];
	GetDualCameraVerificationVersion(VersionInfo);
	ALOGD("STL3DCameraVerificationVersion: %s\n", VersionInfo);

    clock_t start, stop;
    double time_spent;

    DualCameraVerificationConfig dualCameraVerificationConfig;
    dualCameraVerificationConfig.pattern_size_row = 11;
    dualCameraVerificationConfig.pattern_size_col = 7;
    dualCameraVerificationConfig.rms_th = 1.5;
    dualCameraVerificationConfig.isverticalstereo = true;

	dualCameraVerificationConfig.width = 640;//imageSize.width;
	dualCameraVerificationConfig.height = 480;//imageSize.height;
	dualCameraVerificationConfig.image_format = IMAGE_NV21_FORMAT;
	dualCameraVerificationConfig.width_calibration = 640;//calibratedSize.width;
	dualCameraVerificationConfig.height_calibration = 480;//calibratedSize.height;

	dualCameraVerificationConfig.stInputPara.LeftImageCFA.nWidth  = 640;//imageSize.width; //1600; //3264; //4224; //4224; //4224;//800;
	dualCameraVerificationConfig.stInputPara.LeftImageCFA.nHeight = 480;//imageSize.height; //1200; //2448; //3136; //3136; //3136;//600;
	dualCameraVerificationConfig.stInputPara.LeftImageCFA.BlackLevel = 16;
	dualCameraVerificationConfig.stInputPara.LeftImageCFA.DataBits   = 8; //8; //10;
	dualCameraVerificationConfig.stInputPara.LeftImageCFA.BayerPattern = 3;

	dualCameraVerificationConfig.stInputPara.RightImageCFA.nWidth  = 640;//imageSize.width;//1600; //4160; //2592;//800;
	dualCameraVerificationConfig.stInputPara.RightImageCFA.nHeight = 480;//imageSize.height; //1200; //3120; //1944;//600;
	dualCameraVerificationConfig.stInputPara.RightImageCFA.BlackLevel = 16;
	dualCameraVerificationConfig.stInputPara.RightImageCFA.DataBits   = 8; //8; //10;
	dualCameraVerificationConfig.stInputPara.RightImageCFA.BayerPattern = 3;

	dualCameraVerificationConfig.stInputPara.YuvImageCFA.nWidth  = 1440;//imageSize.width;//1600; //4160; //2592;//800;
	dualCameraVerificationConfig.stInputPara.YuvImageCFA.nHeight = 1080;//imageSize.height; //1200; //3120; //1944;//600;
	dualCameraVerificationConfig.stInputPara.YuvImageCFA.BlackLevel = 16;
	dualCameraVerificationConfig.stInputPara.YuvImageCFA.DataBits   = 8; //8; //10;
	dualCameraVerificationConfig.stInputPara.YuvImageCFA.BayerPattern = 3;

	dualCameraVerificationConfig.stInputPara.LeftImage_nWidth   = dualCameraVerificationConfig.stInputPara.LeftImageCFA.nWidth;
	dualCameraVerificationConfig.stInputPara.LeftImage_nHeight  = dualCameraVerificationConfig.stInputPara.LeftImageCFA.nHeight;
	dualCameraVerificationConfig.stInputPara.RightImage_nWidth  = dualCameraVerificationConfig.stInputPara.RightImageCFA.nWidth;
	dualCameraVerificationConfig.stInputPara.RightImage_nHeight = dualCameraVerificationConfig.stInputPara.RightImageCFA.nHeight;
	dualCameraVerificationConfig.stInputPara.YuvImage_nWidth  = dualCameraVerificationConfig.stInputPara.YuvImageCFA.nWidth;
	dualCameraVerificationConfig.stInputPara.YuvImage_nHeight = dualCameraVerificationConfig.stInputPara.YuvImageCFA.nHeight;

	start = clock();
	int result = DualCameraVerificationRawExt(ir_left_path,ir_rigth_path,yuv_path,otp_data_path,&dualCameraVerificationConfig);
    stop = clock();
    time_spent = (double)(stop - start) / CLOCKS_PER_SEC;
    ALOGD("STL3DCameraVerification time used = %fs\n", time_spent);
    ALOGD("STL3DCameraVerification result: %d", result);
    rms = dualCameraVerificationConfig.rms;
    rms_th = dualCameraVerificationConfig.rms_th;
    rms_x = dualCameraVerificationConfig.rms_x;
    rms_y = dualCameraVerificationConfig.rms_y;

	if(result == 0)
		ALOGD("STL3DCameraVerification success with return value %d,\nrms %.5f is less than rms threshold %.5f.\n", result,
			dualCameraVerificationConfig.rms, dualCameraVerificationConfig.rms_th);
	else if(result == 1)
		ALOGD("STL3DCameraVerification failed with return value %d, not all corners are detected\n", result);
	else if(result == 2)
		ALOGD("STL3DCameraVerification failed with return value %d,\nrms %.5f is larger than rms threshold %.5f.\n", result,
			dualCameraVerificationConfig.rms, dualCameraVerificationConfig.rms_th);
	else if((result == 3))
		ALOGD("STL3DCameraVerification failed with return value %d fov is small \n",result);

	else if(result == 4)
		ALOGD("STL3DCameraVerification failed with return value %d, H_rms is too big,H_diff_point x,y= %.5f, %.5f",result,dualCameraVerificationConfig.rms_x,dualCameraVerificationConfig.rms_y);

	else if(result == 5)
		ALOGD("STL3DCameraVerification failed with return value %d, Y_L:not all corners are detected\n", result);

    return result;
}

static const char *hardWareClassPathName =
        "com/sprd/cameracalibration/itemstest/camera/NativeCameraCalibration";

static JNINativeMethod getMethods[] = {
        {"native_STL3DCameraVerification",
        "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)I",
        (void*) JNISTL3DCameraVerification},
        { "native_getSTL3DCameraVerification_RMS",
        "()[D", (void*) JNIGetSTL3DCameraVerification_RMS },
};

static int registerNativeMethods(JNIEnv* env, const char* className,
        JNINativeMethod* gMethods, int numMethods) {
    jclass clazz;
    clazz = env->FindClass(className);
    if (clazz == NULL) {
        ALOGE("Native registration unable to find class '%s'", className);
        return JNI_FALSE;
    }
    if (env->RegisterNatives(clazz, gMethods, numMethods) < 0) {
        ALOGE("RegisterNatives failed for '%s'", className);
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    JNIEnv* env;
    //use JNI1.6
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        ALOGE("Error: GetEnv failed in JNI_OnLoad");
        return -1;
    }
    if (!registerNativeMethods(env, hardWareClassPathName, getMethods,
            sizeof(getMethods) / sizeof(getMethods[0]))) {
        ALOGE("Error: could not register native methods for HardwareFragment");
        return -1;
    }
    return JNI_VERSION_1_6;
}
