#define LOG_TAG "jniWTCameraVerification"
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
#include "header/WTCameraVerificationNV21.h"
#include "header/bmp_io.h"


static jint JNIWTCameraVerification(JNIEnv* env, jobject thiz,
		jstring teleImage, jstring wideImage,jstring otpDataPath,jint leftWidth,jint leftHeight,jint rightWidth,jint rightHeight) {
	const char *image_left_path = env->GetStringUTFChars(teleImage, 0);
	const char *image_rigth_path = env->GetStringUTFChars(wideImage, 0);
	const char *otp_data_path = env->GetStringUTFChars(otpDataPath, 0);

	clock_t start, stop;
    double time_spent;
    WTCameraVerificationConfig wtCameraVerificationConfig;

	//GetCameraVerificationVersion
	char VersionInfo[256];
	GetWTCameraVerificationVersion(VersionInfo);
	ALOGD("WTCameraVerificationVersion: %s\n", VersionInfo);

	wtCameraVerificationConfig.pattern_size_row = 11;
	wtCameraVerificationConfig.pattern_size_col = 7;

//	wtCameraVerificationConfig.leftWidth   = 3264; //tele  /  wide
//	wtCameraVerificationConfig.leftHeight  = 2448;
//	wtCameraVerificationConfig.rightWidth  = 3264; //wide  /  super_wide
//	wtCameraVerificationConfig.rightHeight = 2448;

	wtCameraVerificationConfig.leftWidth = leftWidth; //tele  /  wide
	wtCameraVerificationConfig.leftHeight = leftHeight;
	wtCameraVerificationConfig.rightWidth = rightWidth; //wide  /  super_wide
    wtCameraVerificationConfig.rightHeight = rightHeight;

	wtCameraVerificationConfig.width_calibration  = 800;
	wtCameraVerificationConfig.height_calibration = 600;

	start = clock();
	int result = WTCameraVerificationYUV(image_left_path, image_rigth_path,otp_data_path,&wtCameraVerificationConfig);
	stop = clock();
	time_spent = (double)(stop - start) / CLOCKS_PER_SEC;
	ALOGD("Calibration_Verification time used = %fs\n",time_spent);

	if (result == 0)
		ALOGD("WTCameraVerification success with return value %d \n", result);
	else
		ALOGD("WTCameraVerification failed with return value %d \n", result);

	return result;
}



static const char *hardWareClassPathName =
        "com/sprd/cameracalibration/itemstest/camera/NativeCameraCalibration";

static JNINativeMethod getMethods[] = { {
		"native_WTCameraVerification",
		"(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;IIII)I",
		(void*) JNIWTCameraVerification },
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