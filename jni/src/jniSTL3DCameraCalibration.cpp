//
// Created by SPREADTRUM\emily.miao on 19-8-15.
//

#define LOG_TAG "jniSTL3DCameraCalibration"
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
#include "header/CameraSTL3DCaliApi.h"
#include "header/bmp_io.h"

OTP_DATA_EXT_T stOutOTPExt;
float cx,cy;
int vcm;

#define ERRORLISTNUMBER 26
char CalibrationErrorList[ERRORLISTNUMBER][100]=
{
	{"Not all corners have been detected"},
	{"points re-projection is larger than threshold"},
    {"points/epipolar line dot product error is larger than threshold"},
    {"rectifying x angle of left camera is larger than threshold"},
    {"rectifying y angle of left camera is larger than threshold"},
    {"rectifying z angle of left camera is larger than threshold"},
    {"rectifying x angle of right camera is larger than threshold"},
    {"rectifying y angle of right camera is larger than threshold"},
    {"rectifying z angle of right camera is larger than threshold"},
    {"relative left/right  x angle is larger than threshold"},
    {"relative left/right  y angle is larger than threshold"},
    {"relative left/right  z angle is larger than threshold"},
    {"center x shift for left camera (before rectification) is larger than threshold"},
    {"center y shift for left camera (before rectification) is larger than threshold"},
    {"center x shift for right camera (before rectification) is larger than threshold"},
    {"center y shift for right camera (before rectification) is larger than threshold"},
    {"center x shift for left camera (after rectification) is larger than threshold"},
    {"center y shift for left camera (after rectification) is larger than threshold"},
    {"center x shift for right camera (after rectification) is larger than threshold"},
    {"center y shift for right camera (after rectification) is larger than threshold"},
	{"left camera lens Distortion coefficient is larger than threshold"},
	{"right camera lens Distortion coefficient is larger than threshold"},
	{"left and right camera  interval is larger than threshold"},
	{"cannot get vaid depth range"},
	{"sensor alignment(vertcial or horizontal) is wrong "},
	{"FOV is smaller than threshold"}
};


static jint JNISTL3DCameraCalibration(JNIEnv* env, jobject thiz,jstring irleft,jstring irright,jstring yuv){
    const  char *ir_left = env->GetStringUTFChars(irleft, 0);
    const  char *ir_rigth = env->GetStringUTFChars(irright, 0);
    const  char *yuv_path = env->GetStringUTFChars(yuv, 0);
    int cali_count[4] = {1,300,200,100};
    INPUT_PICTURE_T pPictur;

    clock_t start, stop;
    double time_spent;

    char OTP_SaveFilename[500];
    FILE *fp_w;

    char versionInfo[256];
    GetCalibLibVersion(versionInfo);
    ALOGD("Calibration library version: %s\n", versionInfo);
    ALOGD("ir_left_path: %s\n", ir_left);
    ALOGD("ir_rigth_path: %s\n", ir_rigth);
    ALOGD("yuv_path: %s\n", yuv_path);

    ReadMe();

    start = clock();

    pPictur.LeftImage_nWidth = 640;
    pPictur.LeftImage_nHeight = 480;
    pPictur.RightImage_nWidth = 640;
    pPictur.RightImage_nHeight= 480;
    pPictur.RgbImage_nWidth = 1440;
    pPictur.RgbImage_nHeight= 1080;

    pPictur.IrIrCalibration_nWidth  = 640;
    pPictur.IrIrCalibration_nHeight = 480;
    pPictur.IrRgbCalibration_nWidth = 640;
    pPictur.IrRgbCalibration_nHeight = 480;

    //Pattern 0:RGGB 1: GRBG 2:GBRG 3: BGGR
    pPictur.LeftImageCFA.nWidth  = 640; //3264; //1600; //4224; //4224;//800;
    pPictur.LeftImageCFA.nHeight = 480; //2448; //1200; //3136; //3136;//600;
    pPictur.LeftImageCFA.BlackLevel = 64;
    pPictur.LeftImageCFA.DataBits = 10; //8; //10;
    pPictur.LeftImageCFA.BayerPattern = 2;

    pPictur.RightImageCFA.nWidth  = 640; //1600; //4160; //2592;//800;
    pPictur.RightImageCFA.nHeight = 480; //1200; //3120; //1944;//600;
    pPictur.RightImageCFA.BlackLevel = 64;
    pPictur.RightImageCFA.DataBits = 10; //8; //10;
    pPictur.RightImageCFA.BayerPattern = 3;



    INPUT_PARAM_DATA_T stInputPara;

    int error = Calibration_VerificationSTL3D(ir_left,ir_rigth,yuv_path,"/system/etc/otpdata/input_parameters_stl3d.txt",cali_count,&pPictur,&stOutOTPExt,&cx,&cy);
    stop = clock();
    time_spent = (double)(stop - start) / CLOCKS_PER_SEC;
    ALOGD("Calibration_Verification time used = %fs\n",time_spent);


    char StereoInfo[256];
    GetStereoInfo(StereoInfo);
    ALOGD("Stereo information: %s\n", StereoInfo);

    ALOGD("STL3D Calibration result =%d \n",error);

    if (error == 0)
    {
        ALOGD("Calibration Success \n");
        // print otp data
        for (int i = 0; i < PRODUCT_OTP_OUTPUT_PARAMETER_SIZE; i ++)
        {
            ALOGD("%d %d \n",i,stOutOTPExt.pOutOTP[0].agOTP[i]);
        }

        // otp data Save into a File
        fp_w = fopen("/storage/emulated/0/cali/stl3d_otp.txt","w");

        for (int i = 0; i < 4; i ++)
        {
            fprintf(fp_w,"%d\n",stOutOTPExt.otp_data_head[i]); // save otp head
        }

        for (int i = 0; i < PRODUCT_OTP_OUTPUT_PARAMETER_SIZE; i ++)
        {
            fprintf(fp_w,"%d\n",stOutOTPExt.pOutOTP[0].agOTP[i]); //save otp
        }

        fprintf(fp_w,"%d\n",stOutOTPExt.pOutOTP[0].vcm); //save vcm
        vcm = stOutOTPExt.pOutOTP[0].vcm;
        fclose(fp_w);
    }
    else
    {
        ALOGD("Calibration Fail \n");
        for (int i = 0; i < ERRORLISTNUMBER; i ++)
        {
            if (error & (1 << i))
            ALOGD("Error Index %d: %s \n",i,CalibrationErrorList[i]);
        }
    }
    return error;
}

JNIEXPORT jintArray JNIGetSTL3DCameraCalibrationOTP(JNIEnv* env, jobject thiz)
{
    jintArray jint_arr = env->NewIntArray(PRODUCT_OTP_OUTPUT_PARAMETER_SIZE + 1);
    jint *elems = env->GetIntArrayElements(jint_arr, NULL);

    for (int i = 0; i < PRODUCT_OTP_OUTPUT_PARAMETER_SIZE; i++)
    {
        elems[i] = stOutOTPExt.pOutOTP[0].agOTP[i];
    }
    elems[PRODUCT_OTP_OUTPUT_PARAMETER_SIZE] = vcm;
    env->ReleaseIntArrayElements(jint_arr, elems, 0);
    return jint_arr;
}


JNIEXPORT jintArray JNIGetSTL3DOTPHeader(JNIEnv* env, jobject thiz)
{
    jintArray jint_arr = env->NewIntArray(4);
    jint *elems = env->GetIntArrayElements(jint_arr, NULL);

    for (int i = 0; i < 4; i++)
    {
        elems[i] = stOutOTPExt.otp_data_head[i];
    }
    env->ReleaseIntArrayElements(jint_arr, elems, 0);
    return jint_arr;
}

JNIEXPORT jfloatArray JNIGetSTL3DCoordinate(JNIEnv* env, jobject thiz)
{
    jfloatArray jfloat_arr = env->NewFloatArray(2);
    jfloat *elems = env->GetFloatArrayElements(jfloat_arr, NULL);
    elems[0] = cx;
    elems[1] = cy;
    env->ReleaseFloatArrayElements(jfloat_arr, elems, 0);
    return jfloat_arr;
}

static const char *hardWareClassPathName =
        "com/sprd/cameracalibration/itemstest/camera/NativeCameraCalibration";


static JNINativeMethod getMethods[] = {
        {"native_STL3DCameraCalibration",
        "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)I",
        (void*) JNISTL3DCameraCalibration},
        {"native_getSTL3DCameraCalibrationOTP",
        "()[I",
        (void*) JNIGetSTL3DCameraCalibrationOTP},
        {"native_getSTL3DOTPHeader",
        "()[I",
        (void*) JNIGetSTL3DOTPHeader},
        {"native_getSTL3DCoordinate",
        "()[F",
        (void*) JNIGetSTL3DCoordinate}
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