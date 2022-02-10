/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#define LOG_TAG "jniSpwCameraVerify"
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
#include "header/MonoCameraVerification_API.h"
#include "header/bmp_io.h"

#define DEBUG 0

#define ERRORLISTNUMBER 26

double mRMS = 0.0;

char CalibrationErrorList[ERRORLISTNUMBER][100] =
		{ { "Not all corners have been detected" }, {
				"points re-projection is larger than threshold" },
				{
						"points/epipolar line dot product error is larger than threshold" },
				{ "rectifying x angle of left camera is larger than threshold" },
				{ "rectifying y angle of left camera is larger than threshold" },
				{ "rectifying z angle of left camera is larger than threshold" },
				{ "rectifying x angle of right camera is larger than threshold" },
				{ "rectifying y angle of right camera is larger than threshold" },
				{ "rectifying z angle of right camera is larger than threshold" },
				{ "relative left/right  x angle is larger than threshold" },
				{ "relative left/right  y angle is larger than threshold" },
				{ "relative left/right  z angle is larger than threshold" },
				{
						"center x shift for left camera (before rectification) is larger than threshold" },
				{
						"center y shift for left camera (before rectification) is larger than threshold" },
				{
						"center x shift for right camera (before rectification) is larger than threshold" },
				{
						"center y shift for right camera (before rectification) is larger than threshold" },
				{
						"center x shift for left camera (after rectification) is larger than threshold" },
				{
						"center y shift for left camera (after rectification) is larger than threshold" },
				{
						"center x shift for right camera (after rectification) is larger than threshold" },
				{
						"center y shift for right camera (after rectification) is larger than threshold" },
				{
						"left camera lens Distortion coefficient is larger than threshold" },
				{
						"right camera lens Distortion coefficient is larger than threshold" },
				{ "left and right camera  interval is larger than threshold" },
				{ "cannot get vaid depth range" }, {
						"sensor alignment(vertcial or horizontal) is wrong " },
				{ "FOV is smaller than threshold" } };

void int2bytes(int i, BYTE* bytes, int size) {
	// byte[] bytes = new byte[4];
	memset(bytes, 0, sizeof(BYTE) * size);
	bytes[0] = (BYTE) (0xff & i);
	bytes[1] = (BYTE) ((0xff00 & i) >> 8);
	bytes[2] = (BYTE) ((0xff0000 & i) >> 16);
	bytes[3] = (BYTE) ((0xff000000 & i) >> 24);
}

int bytes2int(BYTE* bytes, int size) {
	int iRetVal = bytes[0] & 0xFF;
	iRetVal |= ((bytes[1] << 8) & 0xFF00);
	iRetVal |= ((bytes[2] << 16) & 0xFF0000);
	iRetVal |= ((bytes[3] << 24) & 0xFF000000);
	return iRetVal;
}

int TestNV21_Verification(monoCameraVerificationInputConfig* inputConfig) {
	clock_t start, stop;
	double time_spent;

	std::string filename_img = inputConfig->filename_img;
	std::string filename_otp = inputConfig->filename_otp;
	const char* otpMapVer = inputConfig->otpMapVer.c_str();
	const char* module = inputConfig->module.c_str();
	//std::string otpMapVer = inputConfig->otpMapVer;
	//std::string module = inputConfig->module;

	int nx = inputConfig->nx;
	int ny = inputConfig->ny;
	int square_size = inputConfig->square_size;
	double rms_th = inputConfig->rms_th;
	int ImgWidth = inputConfig->ImgWidth;
	int ImgHeight = inputConfig->ImgHeight;
	int calibrationWidth = inputConfig->calibrationWidth;
	int calibrationHeight = inputConfig->calibrationHeight;

	int Width = ImgWidth;
	int Height = ImgHeight;

	MonoCameraVerificationConfig monoCameraVerificationConfig;
	monoCameraVerificationConfig.pattern_size_row = nx;
	monoCameraVerificationConfig.pattern_size_col = ny;
	monoCameraVerificationConfig.square_size = square_size;
	monoCameraVerificationConfig.rms_th = rms_th;

	monoCameraVerificationConfig.Width = ImgWidth;
	monoCameraVerificationConfig.Height = ImgHeight;

	monoCameraVerificationConfig.image_format = IMAGE_NV21_FORMAT;
	monoCameraVerificationConfig.width_calibration = calibrationWidth;
	monoCameraVerificationConfig.height_calibration = calibrationHeight;

	monoCameraVerificationConfig.otpMapVer = otpMapVer;
	monoCameraVerificationConfig.module = module;

	const char* image_file = filename_img.c_str();
	const char* otp_file = filename_otp.c_str();

	start = clock();
	int result = MonoCameraVerificationNV21_New(image_file, otp_file,
			&monoCameraVerificationConfig);
	mRMS = monoCameraVerificationConfig.rms;
	ALOGD("MonoCameraVerification mRMS: %.5f", mRMS);
	stop = clock();
	time_spent = (double) (stop - start) / CLOCKS_PER_SEC;
	ALOGD("\nMonoCameraVerification time used = %fs\n", time_spent);

	if (result == ERR_NO_ERROR){
		ALOGD(
				"MonoCameraVerification success with return value %d,\nrms %.5f is less than rms threshold %.5f.\n",
				result, monoCameraVerificationConfig.rms,
				monoCameraVerificationConfig.rms_th);
	}
	else if (result == ERR_NOT_ALL_CONERS_FOUND){
		ALOGD(
				"MonoCameraVerification failed with return value %d, not all corners are detected\n",
				result);
	}
	else if (result == ERR_RMS_TOO_LARGE){
		ALOGD(
				"MonoCameraVerification failed with return value %d,\nrms %.5f is larger than rms threshold %.5f.\n",
				result, monoCameraVerificationConfig.rms,
				monoCameraVerificationConfig.rms_th);
	}
	else{
		ALOGD("MonoCameraVerification failed with return value: %d\n", result);
	}
	return result;
}

static jdouble JNIGetMonoCameraVerificationNV21_RMS(JNIEnv* env, jobject thiz) {
    return mRMS;
}

static jint JNIMonoCameraVerificationNV21_New(JNIEnv* env, jobject thiz,
		jstring image_dir, jstring otpDataPath,int imageWidth,int imageHeight) {
	const char *image_dir_path = env->GetStringUTFChars(image_dir, 0);
	const char *otp_data_path = env->GetStringUTFChars(otpDataPath, 0);
	ALOGD("JNIMonoCameraVerificationNV21_New image_dir_path: %s\n",image_dir_path);
	ALOGD("JNIMonoCameraVerificationNV21_New otp_data_path: %s\n",otp_data_path);
	ALOGD("JNIMonoCameraVerificationNV21_New imageWidth X imageHeight: %dx%d\n",imageWidth,imageHeight);

	//GetDualCameraVerificationVersion
	char VersionInfo[256];
	GetMonoCameraVerificationVersion(VersionInfo);
	ALOGD("MonoCameraVerificationVersion: %s\n", VersionInfo);

	//input arguments config in dualCameraVerificationInputConfig
	monoCameraVerificationInputConfig inputConfig;

	inputConfig.filename_img = image_dir_path;
	inputConfig.filename_otp = otp_data_path;
	inputConfig.module = "16M";
	inputConfig.otpMapVer = "v1.1";

	inputConfig.nx = 11;
	inputConfig.ny = 7;
	inputConfig.square_size = 36;
	inputConfig.rms_th = 1.5;
	inputConfig.ImgWidth = imageWidth;
	inputConfig.ImgHeight = imageHeight;
	inputConfig.calibrationWidth = 800;
	inputConfig.calibrationHeight = 600;

	int result = TestNV21_Verification(&inputConfig);
	ALOGD("JNIMonoCameraVerificationNV21_New result: %d\n", result);
	return result;
}

static const char *hardWareClassPathName =
        "com/sprd/cameracalibration/itemstest/camera/NativeCameraCalibration";

static JNINativeMethod getMethods[] = { {
		"native_MonoCameraVerificationNV21_New",
		"(Ljava/lang/String;Ljava/lang/String;II)I",
		(void*) JNIMonoCameraVerificationNV21_New },
        { "native_getMonoCameraVerificationNV21_RMS",
        "()D", (void*) JNIGetMonoCameraVerificationNV21_RMS },
};

//Not change code
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
