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

#define LOG_TAG "jniDualcameraCalibration"
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
#include "header/CameraCaliApi.h"
#include "header/bmp_io.h"

#define DEBUG 0

OTP_DATA_T  stOutOTP[7];
char StereoInfo[256];
char modulueLocation;
int otpVersion = 0x000000;

int mIsAfterSales = 0;
OTP_DATA_EXT_T stOutOTPEXT;
#define AFT_CALI_FILE "/system/etc/otpdata/sell_aft_cali.txt"
#define INPUT_PARM_FILE "/system/etc/otpdata/sale_after_input_parameters_values.txt"

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

void int2bytes(int i, BYTE* bytes, int size) {
    // byte[] bytes = new byte[4];
    memset(bytes, 0, sizeof(BYTE) * size);
    bytes[0] = (BYTE)(0xff & i);
    bytes[1] = (BYTE)((0xff00 & i) >> 8);
    bytes[2] = (BYTE)((0xff0000 & i) >> 16);
    bytes[3] = (BYTE)((0xff000000 & i) >> 24);
}

int bytes2int(BYTE* bytes, int size) {
    int iRetVal = bytes[0] & 0xFF;
    iRetVal |= ((bytes[1] << 8) & 0xFF00);
    iRetVal |= ((bytes[2] << 16) & 0xFF0000);
    iRetVal |= ((bytes[3] << 24) & 0xFF000000);
    return iRetVal;
}


JNIEXPORT jintArray JNIGetDualCameraCalibrationOTP(JNIEnv* env, jobject thiz)
{
    jintArray jint_arr = env->NewIntArray(PRODUCT_OTP_OUTPUT_PARAMETER_SIZE);
    jint *elems = env->GetIntArrayElements(jint_arr, NULL);

    for (int i = 0; i < PRODUCT_OTP_OUTPUT_PARAMETER_SIZE; i++)
    {
        elems[i] = stOutOTP[0].agOTP[i];
    }
    env->ReleaseIntArrayElements(jint_arr, elems, 0);
    return jint_arr;
}

JNIEXPORT jintArray JNIGetDualCameraCalibrationOTP_async(JNIEnv* env, jobject thiz, jint vcmIndex)
{
    jintArray jint_arr = env->NewIntArray(PRODUCT_OTP_OUTPUT_PARAMETER_SIZE);
    jint *elems = env->GetIntArrayElements(jint_arr, NULL);
    const int index = vcmIndex;

    for (int i = 0; i < PRODUCT_OTP_OUTPUT_PARAMETER_SIZE; i++)
    {
        elems[i] = stOutOTP[index].agOTP[i];
    }
    env->ReleaseIntArrayElements(jint_arr, elems, 0);
    return jint_arr;
}

JNIEXPORT jintArray JNIGetDualCameraCalibrationOTP_afterSales(JNIEnv* env, jobject thiz)
{
    ALOGD("JNIGetDualCameraCalibrationOTP_afterSales\n");
    int otpDataLen = 0;
    int otp_data_head_count = sizeof(stOutOTPEXT.otp_data_head)/sizeof(stOutOTPEXT.otp_data_head[0]);
    ALOGD("JNIGetDualCameraCalibrationOTP otp_data_head_count =%d \n",otp_data_head_count);
    BYTE otp_head_byte[otp_data_head_count * 4];
    int index = 0;
    for(int i=0;i<otp_data_head_count;i++){
        BYTE temp[4];
        int2bytes(stOutOTPEXT.otp_data_head[i],temp,4);
        for(int j=0;j<4;j++){
            otp_head_byte[index++] = temp[j];
        }
    }
    int otp_data_count = otp_head_byte[5] & 0xFF;
    ALOGD("JNIGetDualCameraCalibrationOTP otp_data_count =%d \n",otp_data_count);

    otpDataLen = otp_data_count * 64 + otp_data_head_count;
    ALOGD("JNIGetDualCameraCalibrationOTP_afterSales otpDataLen =%d \n",otpDataLen);
    if(otpDataLen <= 0){
        return NULL;
    }

    jintArray jint_arr = env->NewIntArray(otpDataLen);
    jint *elems = env->GetIntArrayElements(jint_arr, NULL);
    //set otp head
    index = 0;
    for(int i=0;i<otp_data_head_count;i++){
        elems[index++] = stOutOTPEXT.otp_data_head[i];
        ALOGD("JNIGetDualCameraCalibrationOTP_afterSales elems[i] =%d,i=%d,index=%d \n",elems[i],i,index);
    }
    //set otp data
    for(int j=0;j<otp_data_count;j++){
        //OTP_DATA_T  stOutOTP = stOutOTPEXT.pOutOTP[j];
        for (int i = 0; i < 64; i++)
        {
            elems[index++] = stOutOTPEXT.pOutOTP[j].agOTP[i];
            ALOGD("JNIGetDualCameraCalibrationOTP_afterSales elems[i] =%d,i=%d,index=%d \n",elems[i],i,index);
        }
    }

    env->ReleaseIntArrayElements(jint_arr, elems, 0);
    return jint_arr;
}

JNIEXPORT jstring JNIGetStereoInfo(JNIEnv* env, jobject thiz)
{

    jstring stereoInfo = env->NewStringUTF(StereoInfo);
    ALOGD("JNIGetStereoInfo StereoInfo: %s\n", StereoInfo);
    return stereoInfo;
}

JNIEXPORT jint JNIGetModulueLocation(JNIEnv* env, jobject thiz)
{
    return modulueLocation;
}

JNIEXPORT jint JNIGetOtpVersion(JNIEnv* env, jobject thiz)
{
    return otpVersion;
}

static jint  JNIDualCameraCalibrationYUV(JNIEnv* env, jobject thiz,
        jstring image_left, jstring image_right, jstring calibration_version) {
         const  char *filename_left = env->GetStringUTFChars(image_left, 0);
            const  char *filename_right = env->GetStringUTFChars(image_right, 0);
            const  char *c_version = env->GetStringUTFChars(calibration_version, 0);
            int index = 0;

            //demo
            clock_t start, stop;
            double time_spent;
            INPUT_PARAM_DATA_T stInputPara;
            char OTP_SaveFilename[500];
            char InputParams_Filename[500];

            FILE *fp_w;

            std::string Main_Filename = filename_left;
            std::string Sub_Filename = filename_right;
        //    stInputPara.LeftImage_nWidth = 1600;
        //    stInputPara.LeftImage_nHeight = 1200;
        //    stInputPara.RightImage_nWidth = 1600;
        //    stInputPara.RightImage_nHeight = 1200;
        //
        //
        //    stInputPara.Calibration_nWidth  = 800;
        //    stInputPara.Calibration_nHeight = 600;
            stInputPara.LeftImage_nWidth = 4000;
            stInputPara.LeftImage_nHeight = 3000;
            stInputPara.RightImage_nWidth = 800;
            stInputPara.RightImage_nHeight = 600;

            stInputPara.Calibration_nWidth  = 800;
            stInputPara.Calibration_nHeight = 600;

            ALOGD("JNIDualCameraCalibrationYUV filename_left: %s\n", filename_left);
            ALOGD("JNIDualCameraCalibrationYUV filename_right: %s\n", filename_right);
            if(strcmp(c_version,"3") == 0) {
             //Load V1 Input Parameters
                if(access("/system/etc/otpdata/input_parameters_values_v1.txt", F_OK) == 0){
                    ALOGD("JNIDualCameraCalibrationYUV input_parameters_values_v1!");
                    strcpy(InputParams_Filename,"/system/etc/otpdata/input_parameters_values_v1.txt");
                }
            }else if(strcmp(c_version,"4") == 0){
             //Load V2 Input Parameters
                if(access("/system/etc/otpdata/input_parameters_values_v2.txt", F_OK) == 0){
                    ALOGD("JNIDualCameraCalibrationYUV input_parameters_values_v2!");
                    strcpy(InputParams_Filename,"/system/etc/otpdata/input_parameters_values_v2.txt");
                }
            }else if (strcmp(c_version,"0") == 0) {
                if(access("/system/etc/otpdata/input_parameters_values.txt", F_OK) == 0){
                        ALOGD("JNIDualCameraCalibrationYUV input_parameters_values!");
                        strcpy(InputParams_Filename,"/system/etc/otpdata/input_parameters_values.txt");
                    }
            }
            load_parameter(InputParams_Filename, (float *)&stInputPara.agParam[0], INPUT_PARAMETER_SIZE);
            ALOGD("stInputPara.agParam[0] %f\n", stInputPara.agParam[0]);
            ALOGD("stInputPara.agParam[1] %f\n", stInputPara.agParam[1]);
            ALOGD("stInputPara.agParam[29] %f\n", stInputPara.agParam[29]);
            ALOGD("stInputPara.agParam[30] %f\n", stInputPara.agParam[30]);

            char versionInfo[256];
            GetCalibLibVersion(versionInfo);
            ALOGD("Calibration library version: %s\n", versionInfo);
            ReadMe();


            start = clock();
            ALOGD("Calibration_Verification begin!\n");
            int result = -1;
            if(DEBUG){
                ALOGD("Calibration_Verification begin DEBUG!\n");
                const  char *filename_left_test = "/storage/emulated/0/cali/test_left_cali.yuv";
                const  char *filename_right_test = "/storage/emulated/0/cali/test_right_cali.yuv";
                stInputPara.LeftImage_nWidth  = 1600;
                stInputPara.LeftImage_nHeight  = 1200;
                stInputPara.RightImage_nWidth  = 1600;
                stInputPara.RightImage_nHeight  = 1200;
                result = Calibration_VerificationYUV(filename_left_test, filename_right_test, stInputPara, &stOutOTP[index]);
            }else{
                result = Calibration_VerificationYUV(filename_left, filename_right, stInputPara, &stOutOTP[index]);
            }
            stop = clock();
            time_spent = (double)(stop - start) / CLOCKS_PER_SEC;
            ALOGD("Calibration_Verification time used = %fs\n",time_spent);

            //save angles
            GetStereoInfo(StereoInfo);
            //printf("Stereo information: %s\n", StereoInfo);
            ALOGD("Stereo information: %s\n", StereoInfo);
            fp_w = fopen("/storage/emulated/0/cali/relative_RT.txt","wt");
            fprintf(fp_w,"%s\n",StereoInfo);
            fclose(fp_w);

            // get module location info
            modulueLocation = GetModuleLocationInfo();
            ALOGD("modulueLocation: %d\n", modulueLocation);

            GetOtpVersion(&otpVersion);

            //
            ALOGD("Calibration result =%d \n",result);
            if (result == 0)
            {
                ALOGD("Calibration Success \n");
                for (int i = 0; i < PRODUCT_OTP_OUTPUT_PARAMETER_SIZE; i ++)
                {
                    ALOGD("stOutOTP Index %d: %d \n",i,stOutOTP[index].agOTP[i]);
                }
            }
            else
            {
                for (int i = 0; i < ERRORLISTNUMBER; i ++)
                {
                    if (result & (1 << i))
                        ALOGD("Error Index %d: %s \n",i,CalibrationErrorList[i]);
                }
            }
            return result;
        }


static jint  JNIDualCameraCalibrationYUV_async(JNIEnv* env, jobject thiz,
        jstring image_left, jstring image_right, jstring calibration_version,jint vcmIndex,jint leftImage_w,jint leftImage_h,jint rightImage_w,jint rightImage_h) {
    const  char *filename_left = env->GetStringUTFChars(image_left, 0);
    const  char *filename_right = env->GetStringUTFChars(image_right, 0);
    const  char *c_version = env->GetStringUTFChars(calibration_version, 0);
    int index = vcmIndex;

    //demo
    clock_t start, stop;
    double time_spent;
    INPUT_PARAM_DATA_T stInputPara;
    char OTP_SaveFilename[500];
    char InputParams_Filename[500];

    FILE *fp_w;

    std::string Main_Filename = filename_left;
    std::string Sub_Filename = filename_right;
//    stInputPara.LeftImage_nWidth = 1600;
//    stInputPara.LeftImage_nHeight = 1200;
//    stInputPara.RightImage_nWidth = 1600;
//    stInputPara.RightImage_nHeight = 1200;
//
//
//    stInputPara.Calibration_nWidth  = 800;
//    stInputPara.Calibration_nHeight = 600;
    ALOGD("JNIDualCameraCalibrationYUV leftImage_w: %d", leftImage_w);
    ALOGD("JNIDualCameraCalibrationYUV leftImage_h: %d", leftImage_h);
    stInputPara.LeftImage_nWidth = leftImage_w;
    stInputPara.LeftImage_nHeight = leftImage_h;
    stInputPara.RightImage_nWidth = rightImage_w;
    stInputPara.RightImage_nHeight = rightImage_h;

    stInputPara.Calibration_nWidth  = 800;
    stInputPara.Calibration_nHeight = 600;

    ALOGD("JNIDualCameraCalibrationYUV filename_left: %s\n", filename_left);
    ALOGD("JNIDualCameraCalibrationYUV filename_right: %s\n", filename_right);
    if(strcmp(c_version,"3") == 0) {
     //Load V1 Input Parameters
        if(access("/system/etc/otpdata/input_parameters_values_v1.txt", F_OK) == 0){
            ALOGD("JNIDualCameraCalibrationYUV input_parameters_values_v1!");
            strcpy(InputParams_Filename,"/system/etc/otpdata/input_parameters_values_v1.txt");
        }
    }else if(strcmp(c_version,"4") == 0){
     //Load V2 Input Parameters
        if(access("/system/etc/otpdata/input_parameters_values_v2.txt", F_OK) == 0){
            ALOGD("JNIDualCameraCalibrationYUV input_parameters_values_v2!");
            strcpy(InputParams_Filename,"/system/etc/otpdata/input_parameters_values_v2.txt");
        }
    }else if (strcmp(c_version,"0") == 0) {
        if(access("/system/etc/otpdata/input_parameters_values.txt", F_OK) == 0){
                ALOGD("JNIDualCameraCalibrationYUV input_parameters_values!");
                strcpy(InputParams_Filename,"/system/etc/otpdata/input_parameters_values.txt");
            }
    }
    load_parameter(InputParams_Filename, (float *)&stInputPara.agParam[0], INPUT_PARAMETER_SIZE);
    ALOGD("stInputPara.agParam[0] %f\n", stInputPara.agParam[0]);
    ALOGD("stInputPara.agParam[1] %f\n", stInputPara.agParam[1]);
    ALOGD("stInputPara.agParam[29] %f\n", stInputPara.agParam[29]);
    ALOGD("stInputPara.agParam[30] %f\n", stInputPara.agParam[30]);

    char versionInfo[256];
    GetCalibLibVersion(versionInfo);
    ALOGD("Calibration library version: %s\n", versionInfo);
    ReadMe();


    start = clock();
    ALOGD("Calibration_Verification begin!\n");
    int result = -1;
    if(DEBUG){
        ALOGD("Calibration_Verification begin DEBUG!\n");
        const  char *filename_left_test = "/storage/emulated/0/cali/test_left_cali.yuv";
        const  char *filename_right_test = "/storage/emulated/0/cali/test_right_cali.yuv";
        stInputPara.LeftImage_nWidth  = 1600;
        stInputPara.LeftImage_nHeight  = 1200;
        stInputPara.RightImage_nWidth  = 1600;
        stInputPara.RightImage_nHeight  = 1200;
        result = Calibration_VerificationYUV(filename_left_test, filename_right_test, stInputPara, &stOutOTP[index]);
    }else{
        result = Calibration_VerificationYUV(filename_left, filename_right, stInputPara, &stOutOTP[index]);
    }
    stop = clock();
    time_spent = (double)(stop - start) / CLOCKS_PER_SEC;
    ALOGD("Calibration_Verification time used = %fs\n",time_spent);

    //save angles
    GetStereoInfo(StereoInfo);
    //printf("Stereo information: %s\n", StereoInfo);
    ALOGD("Stereo information: %s\n", StereoInfo);
    fp_w = fopen("/storage/emulated/0/cali/relative_RT.txt","wt");
    fprintf(fp_w,"%s\n",StereoInfo);
    fclose(fp_w);

    // get module location info
    modulueLocation = GetModuleLocationInfo();
    ALOGD("modulueLocation: %d\n", modulueLocation);

    GetOtpVersion(&otpVersion);

    //
    ALOGD("Calibration result =%d \n",result);
    if (result == 0)
    {
        ALOGD("Calibration Success \n");
        for (int i = 0; i < PRODUCT_OTP_OUTPUT_PARAMETER_SIZE; i ++)
        {
            ALOGD("stOutOTP Index %d: %d \n",i,stOutOTP[index].agOTP[i]);
        }
    }
    else
    {
        for (int i = 0; i < ERRORLISTNUMBER; i ++)
        {
            if (result & (1 << i))
                ALOGD("Error Index %d: %s \n",i,CalibrationErrorList[i]);
        }
    }
    return result;
}



static jint JNIDualCameraCalibrationYUV_afterSales(JNIEnv* env, jobject thiz,jstring image_dir,jstring otpDataPath,jint leftImage_w,jint leftImage_h,jint rightImage_w,jint rightImage_h,
        jint isAftersales) {
    const  char *image_dir_path = env->GetStringUTFChars(image_dir, 0);
    const  char *otp_data_path = env->GetStringUTFChars(otpDataPath, 0);

    const int leftImage_width = leftImage_w;
    const int leftImage_height = leftImage_h;
    const int rightImage_width = rightImage_w;
    const int rightImage_height = rightImage_h;

    const int is_after_sales = isAftersales;
    ALOGD("JNIDualCameraCalibrationYUV_afterSales leftImage_size: %dx%d\n", leftImage_width,leftImage_height);
    ALOGD("JNIDualCameraCalibrationYUV_afterSales rightImage_size: %dx%d\n", rightImage_width,rightImage_height);
    ALOGD("JNIDualCameraCalibrationYUV_afterSales is_after_sales: %d\n", is_after_sales);
    mIsAfterSales = is_after_sales;
    //demo
    clock_t start, stop;
    double time_spent;
    INPUT_PICTURE_T *pPicture = (INPUT_PICTURE_T *)malloc(sizeof(INPUT_PICTURE_T));


    pPicture->LeftImage_nWidth = leftImage_width;
    pPicture->LeftImage_nHeight = leftImage_height;
    pPicture->RightImage_nWidth = rightImage_width;
    pPicture->RightImage_nHeight = rightImage_height;

    ALOGD("JNIDualCameraCalibrationYUV_afterSales otp_data_path: %s\n", otp_data_path);
    ALOGD("JNIDualCameraCalibrationYUV_afterSales image_dir_path: %s\n", image_dir_path);

    start = clock();
    ALOGD("JNIDualCameraCalibrationYUV_afterSales Calibration_Verification begin!\n");
    int result = -1;

    ALOGD("JNIDualCameraCalibrationYUV_afterSales AFT_CALI_FILE: %s\n", AFT_CALI_FILE);
    ALOGD("JNIDualCameraCalibrationYUV_afterSales INPUT_PARM_FILE: %s\n", INPUT_PARM_FILE);
    result = Calibration_SalesAfterYUV(image_dir_path, otp_data_path, AFT_CALI_FILE, INPUT_PARM_FILE, pPicture, &stOutOTPEXT);

    stop = clock();
    time_spent = (double)(stop - start) / CLOCKS_PER_SEC;
    ALOGD("JNIDualCameraCalibrationYUV_afterSales time used = %fs\n",time_spent);

    //
    ALOGD("JNIDualCameraCalibrationYUV_afterSales Calibration result =%d \n",result);
    if (result == 0)
    {
        ALOGD("JNIDualCameraCalibrationYUV_afterSales Calibration Success \n");

    }
    else
    {
        for (int i = 0; i < ERRORLISTNUMBER; i ++)
        {
            if (result & (1 << i))
                ALOGD("Error Index %d: %s \n",i,CalibrationErrorList[i]);
        }
    }


    free(pPicture);
    return result;
}

static jint JNIDualCameraCalibrationYUV_afterSales_v2(JNIEnv* env, jobject thiz,jstring image_dir,jstring otpDataPath,jint leftImage_w,jint leftImage_h,jint rightImage_w,jint rightImage_h) {
    const  char *image_dir_path = env->GetStringUTFChars(image_dir, 0);
    const  char *otp_data_path = env->GetStringUTFChars(otpDataPath, 0);

    const int leftImage_width = leftImage_w;
    const int leftImage_height = leftImage_h;
    const int rightImage_width = rightImage_w;
    const int rightImage_height = rightImage_h;

    ALOGD("JNIDualCameraCalibrationYUV_afterSales_v2 leftImage_size: %dx%d\n", leftImage_width,leftImage_height);
    ALOGD("JNIDualCameraCalibrationYUV_afterSales_v2 rightImage_size: %dx%d\n", rightImage_width,rightImage_height);

    //demo
    clock_t start, stop;
    double time_spent;
    INPUT_PICTURE_T *pPicture = (INPUT_PICTURE_T *)malloc(sizeof(INPUT_PICTURE_T));


    pPicture->LeftImage_nWidth = leftImage_width;
    pPicture->LeftImage_nHeight = leftImage_height;
    pPicture->RightImage_nWidth = rightImage_width;
    pPicture->RightImage_nHeight = rightImage_height;

    ALOGD("JNIDualCameraCalibrationYUV_afterSales_v2 otp_data_path: %s\n", otp_data_path);
    ALOGD("JNIDualCameraCalibrationYUV_afterSales_v2 image_dir_path: %s\n", image_dir_path);

    start = clock();
    ALOGD("JNIDualCameraCalibrationYUV_afterSales_v2 Calibration_Verification begin!\n");
    int result = Calibration_SalesAfterV2YUV(image_dir_path, INPUT_PARM_FILE, pPicture, &stOutOTPEXT);

    ALOGD("JNIDualCameraCalibrationYUV_afterSales_v2 AFT_CALI_FILE: %s\n", AFT_CALI_FILE);
    ALOGD("JNIDualCameraCalibrationYUV_afterSales_v2 INPUT_PARM_FILE: %s\n", INPUT_PARM_FILE);

    stop = clock();
    time_spent = (double)(stop - start) / CLOCKS_PER_SEC;
    ALOGD("JNIDualCameraCalibrationYUV_afterSales_v2 time used = %fs\n",time_spent);

    //
    ALOGD("JNIDualCameraCalibrationYUV_afterSales_v2 Calibration result =%d \n",result);
    if (result == 0)
    {
        ALOGD("JNIDualCameraCalibrationYUV_afterSales_v2 Calibration Success \n");

    }
    else
    {
        for (int i = 0; i < ERRORLISTNUMBER; i ++)
        {
            if (result & (1 << i))
                ALOGE("Error Index %d: %s \n",i,CalibrationErrorList[i]);
        }
    }


    free(pPicture);
    return result;
}

static const char *hardWareClassPathName =
        "com/sprd/cameracalibration/itemstest/camera/NativeCameraCalibration";

static JNINativeMethod getMethods[] = {
        { "native_dualCameraCalibrationYUV",
        "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)I",
        (void*) JNIDualCameraCalibrationYUV },
        { "native_dualCameraCalibrationYUV",
        "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;IIIII)I",
        (void*) JNIDualCameraCalibrationYUV_async},
        { "native_getdualCameraCalibrationOTP",
        "()[I",
        (void*) JNIGetDualCameraCalibrationOTP },
        { "native_getdualCameraCalibrationOTP",
        "(I)[I",
        (void*) JNIGetDualCameraCalibrationOTP_async },
        { "native_getStereoInfo",
        "()Ljava/lang/String;",
        (void*) JNIGetStereoInfo },
        { "native_getModulueLocation",
        "()I",
        (void*) JNIGetModulueLocation },
        { "native_getOtpVersion",
        "()I",
        (void*) JNIGetOtpVersion },
        { "native_dualCameraCalibrationYUV_afterSales",
        "(Ljava/lang/String;Ljava/lang/String;IIIII)I",
        (void*) JNIDualCameraCalibrationYUV_afterSales },
        { "native_getdualCameraCalibrationOTP_afterSales",
        "()[I",
        (void*) JNIGetDualCameraCalibrationOTP_afterSales },
        { "native_dualCameraCalibrationYUV_afterSales_v2",
        "(Ljava/lang/String;Ljava/lang/String;IIII)I",
        (void*) JNIDualCameraCalibrationYUV_afterSales_v2 }
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
