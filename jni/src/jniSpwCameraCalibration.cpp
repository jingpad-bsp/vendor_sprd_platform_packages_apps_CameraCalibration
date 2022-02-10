//
// Created by SPREADTRUM\emily.miao on 19-5-24.
//

#define LOG_TAG "jniSpwCameraCalibration"
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
#include "header/CameraSpwCaliApi.h"
#include "header/bmp_io.h"

#define DEBUG 0

SPW_OTP_DATA_T stOutOTP;

#define ERRORLISTNUMBER 6
char CalibrationErrorList[ERRORLISTNUMBER][100]=
{
	{"Not all corners have been detected"},
	{"points re-projection is larger than threshold"},
    {"lens Distortion coefficient is larger than threshold"},
    {"center x shift for  camera (before rectification) is larger than threshold"},
    {"center y shift for  camera (before rectification) is larger than threshold"},
	{"FOV is smaller than threshold"}
};

static jint JNISpwCameraCalibrationYUV(JNIEnv* env, jobject thiz,jstring image_file,int imageWidth,int imageHeight){
    const  char *filename = env->GetStringUTFChars(image_file, 0);
    clock_t start, stop;
    double time_spent;
    INPUT_SPW_PARAM_DATA_T stInputPara;
	char OTP_SaveFilename[500];
	char InputParams_Filename[500];


    //Load Input Parameters
    strcpy(InputParams_Filename,"/system/etc/otpdata/spw_input_parameters_values.txt");
    SPW_load_parameter(InputParams_Filename, (float *)&stInputPara.agParam[0], INPUT_SPW_PARAMETER_SIZE);

    stInputPara.Image_nWidth = imageWidth;
    stInputPara.Image_nHeight = imageHeight;
    stInputPara.Calibration_nWidth  = stInputPara.agParam[5];//800
    stInputPara.Calibration_nHeight = stInputPara.agParam[6];//600

    char versionInfo[256];
    SPW_GetCalibLibVersion(versionInfo);
    ALOGD("Calibration library version: %s\n", versionInfo);
    SPW_ReadMe();

    start = clock();
    int result = SPW_Calibration_VerificationYUV(filename, stInputPara,&stOutOTP, 0);
    stop = clock();

    time_spent = (double)(stop - start) / CLOCKS_PER_SEC;

        if (result == 0)
        {
            ALOGD("Calibration Success \n");

            for (int i = 0; i < PRODUCT_SPW_OTP_OUTPUT_HEADER_SIZE; i ++)
            {
                ALOGD("%d %d\n",i+1,stOutOTP.agHeader[i]);

            }
            for (int i = 0; i < PRODUCT_SPW_OTP_OUTPUT_PARAMETER_SIZE; i ++)
            {
                ALOGD("%d %d\n",i+1+PRODUCT_SPW_OTP_OUTPUT_HEADER_SIZE,stOutOTP.agOTP[i]);

            }

        }

    return result;
}

JNIEXPORT jintArray JNIGetSpwCameraCalibrationOTP(JNIEnv* env, jobject thiz)
{
    jintArray jint_arr = env->NewIntArray(PRODUCT_SPW_OTP_OUTPUT_PARAMETER_SIZE);
    jint *elems = env->GetIntArrayElements(jint_arr, NULL);

    for (int i = 0; i < PRODUCT_SPW_OTP_OUTPUT_PARAMETER_SIZE; i++)
    {
        elems[i] = stOutOTP.agOTP[i];
    }
    env->ReleaseIntArrayElements(jint_arr, elems, 0);
    return jint_arr;
}

JNIEXPORT jintArray JNIGetSpwOTPHeader(JNIEnv* env, jobject thiz)
{
    jintArray jint_arr = env->NewIntArray(PRODUCT_SPW_OTP_OUTPUT_HEADER_SIZE);
    jint *elems = env->GetIntArrayElements(jint_arr, NULL);

    for (int i = 0; i < PRODUCT_SPW_OTP_OUTPUT_HEADER_SIZE; i++)
    {
        elems[i] = stOutOTP.agHeader[i];
    }
    env->ReleaseIntArrayElements(jint_arr, elems, 0);
    return jint_arr;
}


static const char *hardWareClassPathName =
        "com/sprd/cameracalibration/itemstest/camera/NativeCameraCalibration";


static JNINativeMethod getMethods[] = {
        {"native_SpwCameraCalibrationYUV",
        "(Ljava/lang/String;II)I",
        (void*) JNISpwCameraCalibrationYUV},
        {"native_getSpwCameraCalibrationOTP",
        "()[I",
        (void*) JNIGetSpwCameraCalibrationOTP},
        {"native_getSpwOTPHeader",
        "()[I",
        (void*) JNIGetSpwOTPHeader}
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
