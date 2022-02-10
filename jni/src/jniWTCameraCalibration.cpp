
#define LOG_TAG "jniWTCameraCalibration"
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
#include "header/WTCameraCalibrationApi.h"
#include "header/bmp_io.h"

WT_OTP_DATA_T stOutOTP;

static jint JNIWTCameraCalibration(JNIEnv* env, jobject thiz,jstring tele_image_path,jstring wide_image_path,jstring stage,jint wVCM,jint tVCM,jint leftWidth,jint leftHeight,jint rightWidth,jint rightHeight)
{
    const  char *filename_left = env->GetStringUTFChars(tele_image_path, 0);
    const  char *filename_right = env->GetStringUTFChars(wide_image_path, 0);
    const  char *zoom_stage = env->GetStringUTFChars(stage, 0);

    ALOGD("JNIWTCameraCalibration filename_left: %s", filename_left);
    ALOGD("JNIWTCameraCalibration filename_right: %s", filename_right);
    ALOGD("JNIWTCameraCalibration zoom_stage: %s", zoom_stage);

    int wvcm = wVCM;
    int tvcm = tVCM;

    clock_t start, stop;
    double time_spent;
    INPUT_WT_PARAM_DATA_T stInputPara;

    char InputParams_Filename[500];

	stInputPara.Calibration_nWidth  = 800;//800
	stInputPara.Calibration_nHeight = 600;//600

//	stInputPara.LeftImage_nWidth    = 3264; //tele  /  wide
//	stInputPara.LeftImage_nHeight   = 2448;
//	stInputPara.RightImage_nWidth  = 3264; //wide  /  super_wide
//    stInputPara.RightImage_nHeight = 2448;

	stInputPara.LeftImage_nWidth    = leftWidth; //tele  /  wide
	stInputPara.LeftImage_nHeight   = leftHeight;
	stInputPara.RightImage_nWidth  = rightWidth; //wide  /  super_wide
    stInputPara.RightImage_nHeight = rightHeight;

    const  char *outputname = "/storage/emulated/0/cali/wt_otp.txt";

    if(strcmp(zoom_stage,"1") == 0) {
     //Load stage1 Input Parameters
        if(access("/system/etc/otpdata/oz1_input_parameters_values.txt", F_OK) == 0){
            ALOGD("JNIWTCameraCalibration oz1_input_parameters_values!");
            strcpy(InputParams_Filename,"/system/etc/otpdata/oz1_input_parameters_values.txt");
        }
        outputname = "/storage/emulated/0/cali/stage1_uw_u_otp.txt";
    }else if(strcmp(zoom_stage,"2") == 0){
     //Load stage2 Input Parameters
        if(access("/system/etc/otpdata/oz2_input_parameters_values.txt", F_OK) == 0){
            ALOGD("JNIWTCameraCalibration oz2_input_parameters_values!");
            strcpy(InputParams_Filename,"/system/etc/otpdata/oz2_input_parameters_values.txt");
        }
        outputname = "/storage/emulated/0/cali/stage2_w_t_otp.txt";
    }else {

        ALOGD("JNIWTCameraCalibration zoom stage wrong!");
    }

	WT_load_parameter(InputParams_Filename, (float *)&stInputPara.agParam[0], INPUT_WT_PARAMETER_SIZE);

	char versionInfo[256];
	WT_GetCalibLibVersion(versionInfo);
	ALOGD("Calibration library version: %s\n", versionInfo);
	WT_ReadMe();

	start = clock();
	int error = WT_Calibration_VerificationYUV(filename_left, filename_right, stInputPara,&stOutOTP, wvcm, tvcm);
	stop = clock();
	time_spent = (double)(stop - start) / CLOCKS_PER_SEC;
	ALOGD("Calibration_Verification time used = %fs\n",time_spent);


	if (error == 0)
	{
		ALOGD("Calibration Success \n");

		//Print out and Save into a File
		FILE *fp_w = fopen(outputname,"w");
		for (int i = 0; i < PRODUCT_WT_OTP_OUTPUT_HEADER_SIZE; i ++)
		{
			fprintf(fp_w,"%d\n",stOutOTP.agHeader[i]);
		}
		for (int i = 0; i < PRODUCT_WT_OTP_OUTPUT_PARAMETER_SIZE; i ++)
		{
			fprintf(fp_w,"%d\n",stOutOTP.agOTP[i]);
		}
		fclose(fp_w);
	}
	else
	{
		ALOGD("Calibration Fail \n");
		ALOGD("Not all corners are detected \n");
	}

    return error;
}



JNIEXPORT jintArray JNIGetWTCameraCalibrationOTP(JNIEnv* env, jobject thiz)
{
    jintArray jint_arr = env->NewIntArray(PRODUCT_WT_OTP_OUTPUT_PARAMETER_SIZE);
    jint *elems = env->GetIntArrayElements(jint_arr, NULL);

    for (int i = 0; i < PRODUCT_WT_OTP_OUTPUT_PARAMETER_SIZE; i++)
    {
        elems[i] = stOutOTP.agOTP[i];
    }
    env->ReleaseIntArrayElements(jint_arr, elems, 0);
    return jint_arr;
}

JNIEXPORT jintArray JNIGetWTOTPHeader(JNIEnv* env, jobject thiz)
{
    jintArray jint_arr = env->NewIntArray(PRODUCT_WT_OTP_OUTPUT_HEADER_SIZE);
    jint *elems = env->GetIntArrayElements(jint_arr, NULL);

    for (int i = 0; i < PRODUCT_WT_OTP_OUTPUT_HEADER_SIZE; i++)
    {
        elems[i] = stOutOTP.agHeader[i];
    }
    env->ReleaseIntArrayElements(jint_arr, elems, 0);
    return jint_arr;
}



static const char *hardWareClassPathName =
        "com/sprd/cameracalibration/itemstest/camera/NativeCameraCalibration";


static JNINativeMethod getMethods[] = {
        {"native_WTCameraCalibration",
        "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;IIIIII)I",
        (void*) JNIWTCameraCalibration},
        {"native_getWTCameraCalibrationOTP",
        "()[I",
        (void*) JNIGetWTCameraCalibrationOTP},
        {"native_getWTOTPHeader",
        "()[I",
        (void*) JNIGetWTOTPHeader}
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





