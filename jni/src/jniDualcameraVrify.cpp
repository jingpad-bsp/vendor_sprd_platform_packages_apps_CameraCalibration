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

#define LOG_TAG "jniDualcameraVrify"
#include "utils/Log.h"

#include <stdint.h>
#include <jni.h>

#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <malloc.h>
#include <dlfcn.h>
#include "header/CalibParam.h"
//#include "header/DualCameraVerfication.h"
#include "header/DualCameraVerification_API.h"
#include "header/ImageFormat_Conversion.h"
#include "header/ReadCalibParam.h"
#include "header/typedef.h"

#include <time.h>
#include <iostream>

#define ROOT_MAGIC 0x524F4F54 //"ROOT"
#define ROOT_OFFSET 512
#define MAX_COMMAND_BYTES               (8 * 1024)

#define FORCE_USE_VERTICAL 1

void * filehandle = NULL;

#define DEBUG_MODE 0

typedef int (*getResult)(const char*, const char*, CalibParam*,
        DualCameraVerificationConfig*);
getResult getResultMethod;
/*SPRD bug 759782 : Display RMS value*/
typedef double (*getResultRMS)(double rmsSrc);
getResultRMS getResultRMSMethod;
double rms = 0.0;
/*@}*/
#define LIB_CACULATE_PATH "libCameraVerfication.so"
int TestNV21(dualCameraVerificationInputConfig* inputConfig);
int TestNV21_new(dualCameraVerificationInputConfig* inputConfig);
int Check_OtpSize(const char* otp_original);

typedef int (*duDualcameraCali)(const char*, const char*, const char*,
        DualCameraVerificationConfig*);
duDualcameraCali duDualcameraCaliMethod;

int Check_OtpSize(const char* otp_original){
    FILE* fp;
    int size;
    unsigned char dir_read = 0xff;

    fp = fopen(otp_original, "rb");
    if(!fp)
    {
        printf("Check_OtpSize dualCam otp file load failed!\n");
        return -1;
    }
    fseek(fp, 0L, SEEK_END);//end is eof, not the last effective area
    size = ftell(fp);
    ALOGD("Check_OtpSize size: %d", size);
    fclose(fp);
    return size;
}

int TestNV21(dualCameraVerificationInputConfig* inputConfig)
{
    clock_t start, stop;
    double time_spent;

    std::string filename_left = inputConfig->filename_left;
    std::string filename_right = inputConfig->filename_right;
    std::string filename_otp = inputConfig->filename_otp;
    std::string otpMapVer = inputConfig->otpMapVer;
    std::string moduleCombination = inputConfig->moduleCombination;
    int nx = inputConfig->nx;
    int ny = inputConfig->ny;
    double rms_th = inputConfig->rms_th;
    int leftImgWidth = inputConfig->leftImgWidth;
    int leftImgHeight = inputConfig->leftImgHeight;
    int rightImgWidth = inputConfig->rightImgWidth;
    int rightImgHeight = inputConfig->rightImgHeight;
    int calibrationWidth = inputConfig->calibrationWidth;
    int calibrationHeight = inputConfig->calibrationHeight;

    int leftWidth = leftImgWidth;
    int leftHeight = leftImgHeight;
    int rightWidth = rightImgWidth;
    int rightHeight = rightImgHeight;


//  uint8 *YUVImage_left, *YUVImage_right;
//  YUVImage_left = new uint8[leftHeight * leftWidth * 3 / 2];
//  YUVImage_right = new uint8[rightHeight * rightWidth * 3 / 2];

    char *YUVImage_left, *YUVImage_right;
    YUVImage_left = new char[leftHeight * leftWidth * 3 / 2];
    YUVImage_right = new char[rightHeight * rightWidth * 3 / 2];
    const char* left = filename_left.c_str();;
    const char* right = filename_right.c_str();;

    if(!ReadYUV420File(/*filename_left.c_str()*/left, leftWidth, leftHeight, YUVImage_left)){
        ALOGD("ReadYUV420File YUVImage_left fail!");
        if (YUVImage_left != NULL) delete[] YUVImage_left;
        if (YUVImage_right != NULL) delete[] YUVImage_right;
        return -1;
    }
    if(!ReadYUV420File(/*filename_right.c_str()*/right, rightWidth, rightHeight, YUVImage_right)){
        ALOGD("ReadYUV420File YUVImage_right fail!");
        if (YUVImage_left != NULL) delete[] YUVImage_left;
        if (YUVImage_right != NULL) delete[] YUVImage_right;
        return -1;
    }


    CalibParam calibParam;
    DualCameraVerificationConfig dualCameraVerificationConfig;
    dualCameraVerificationConfig.pattern_size_row = nx;
    dualCameraVerificationConfig.pattern_size_col = ny;
    dualCameraVerificationConfig.rms_th = rms_th;

    dualCameraVerificationConfig.leftWidth   = leftWidth;
    dualCameraVerificationConfig.leftHeight  = leftHeight;
    dualCameraVerificationConfig.rightWidth  = rightWidth;
    dualCameraVerificationConfig.rightHeight = rightHeight;

    dualCameraVerificationConfig.image_format = IMAGE_NV21_FORMAT;
    dualCameraVerificationConfig.width_calibration = calibrationWidth;
    dualCameraVerificationConfig.height_calibration = calibrationHeight;

    ////for verification on phone, only bin input support//////////
    //currently only otp map version: v0.5 and v1.0 are supported//
    ////the otp map version is coded into an integer: v0.5 ---> 0//
    ////////////////////////////////////////////////v1.0 ---> 1////

    int otpMapVerFlag = 0xffff;//detecting the otp map version
    if(!otpMapVer.compare(std::string("v0.5")))
        otpMapVerFlag = DUALCAMOTPMAPVERSION_V0_5;//V0.5
    else if(!otpMapVer.compare(std::string("v1.0")))
        otpMapVerFlag = DUALCAMOTPMAPVERSION_V1_0;//V1.0
    else if(!otpMapVer.compare(std::string("v0.4")))
        otpMapVerFlag = DUALCAMOTPMAPVERSION_V0_4;//V0.4
    else
    {
        ALOGD("unknow otp map version!\n");
        if (YUVImage_left != NULL) delete[] YUVImage_left;
        if (YUVImage_right != NULL) delete[] YUVImage_right;
        return -1;
    }

    int moduleCombinationFlag = 0xffff;//detecting module combination format
                                       //currently, your combination should be 8M_2M or 13M_5M
    if(!moduleCombination.compare(std::string("8M_2M")))
        moduleCombinationFlag = DUALCAMCOMBINATION_8M_2M;
    else if(!moduleCombination.compare(std::string("13M_5M")))
        moduleCombinationFlag = DUALCAMCOMBINATION_13M_5M;
    else if(!moduleCombination.compare(std::string("13M_2M")))
        moduleCombinationFlag = DUALCAMCOMBINATION_13M_2M;
    else if(!moduleCombination.compare(std::string("16M_5M")))
        moduleCombinationFlag = DUALCAMCOMBINATION_16M_5M;
    else if(!moduleCombination.compare(std::string("16M_8M")))
        moduleCombinationFlag = DUALCAMCOMBINATION_16M_8M;
    else
    {
        ALOGD("unknown dualCam module combination!\n");
        printf("unknown dualCam module combination!\n");
        if (YUVImage_left != NULL) delete[] YUVImage_left;
        if (YUVImage_right != NULL) delete[] YUVImage_right;
        return -1;
    }

    ALOGD("readModuleArrangeDirFromBinDualCamOtp work! otpMapVer=%s",otpMapVer.c_str());
    int isVerticalStereo = 0xffff;//detecting the dualCam placement:arrange direction
    if(otpMapVer.compare(std::string("v0.4")) != 0 && !readModuleArrangeDirFromBinDualCamOtp(filename_otp.c_str(), &isVerticalStereo, otpMapVerFlag, moduleCombinationFlag))
    {
        ALOGD("readModuleArrangeDirFromBinDualCamOtp fail!\n");
        if (YUVImage_left != NULL) delete[] YUVImage_left;
        if (YUVImage_right != NULL) delete[] YUVImage_right;
        return -1;
    }
    ALOGD("readModuleArrangeDirFromBinDualCamOtp work! isVerticalStereo=%d",isVerticalStereo);
    dualCameraVerificationConfig.isverticalstereo = isVerticalStereo;

    //detecting the otp format: txt or bin
    int LastN = 3;
    std::string txt_format = "txt";
    std::string otp_format = filename_otp.substr(filename_otp.size() - LastN);
    //ALOGD("JNICameraVerifcation  11 otpMapVer=%s,otp_format=%s",otpMapVer.c_str(),otp_format.c_str());
    if(!otp_format.compare(txt_format))
    {
        //ALOGD("JNICameraVerifcation  otpMapVer=%s",otpMapVer.c_str());
        read_calibration_param((int8*)(filename_otp.c_str()), &calibParam);
        ALOGD("If you are debuging on phone, the otp format should be \"bin\", not \"txt\"!\n");
    }
    else
    {
        ALOGD("JNICameraVerifcation  readCalibrationParamFromDiffOtpMapVer");
        if(!readCalibrationParamFromDiffOtpMapVer(filename_otp.c_str(), &calibParam, otpMapVerFlag))
        {
            ALOGD("readCalibrationParamFromDiffOtpMapVer 22 fail!\n");
            if (YUVImage_left != NULL) delete[] YUVImage_left;
            if (YUVImage_right != NULL) delete[] YUVImage_right;
            return -1;
        }
    }

    start = clock();

    filehandle = dlopen(LIB_CACULATE_PATH, RTLD_NOW);
    if (!filehandle) {
        ALOGD("JNICameraVerifcation stderr:%s dlerror:%s\n", stderr, dlerror());
        if (YUVImage_left != NULL) delete[] YUVImage_left;
        if (YUVImage_right != NULL) delete[] YUVImage_right;
        return -1;
    }
    ALOGD("JNICameraVerifcation JNIDualCameraVerfication2");
    getResultMethod = (getResult) dlsym(filehandle,
            "DualCameraVerificationNV21");
    if (!getResultMethod) {
        ALOGD("JNICameraVerifcation dlsym stderr:%s dlerror:%s\n", stderr,
                dlerror());
        if (YUVImage_left != NULL) delete[] YUVImage_left;
        if (YUVImage_right != NULL) delete[] YUVImage_right;
        return -1;
    }
    ALOGD("JNICameraVerifcation DualCameraVerfication getResultMethod done");
    int result = getResultMethod(YUVImage_left, YUVImage_right, &calibParam,
            &dualCameraVerificationConfig);
    ALOGD("JNICameraVerifcation DualCameraVerfication result: %d", result);
    //int result = DualCameraVerificationNV21(YUVImage_left, YUVImage_right, &calibParam, &dualCameraVerificationConfig);
    stop = clock();
    time_spent = (double)(stop - start) / CLOCKS_PER_SEC;
    printf("\nDualCameraVerification time used = %fs\n", time_spent);
    rms = dualCameraVerificationConfig.rms;
    if(result == 0)
        printf("DualCameraVerification success with return value %d,\nrms %.5f is less than rms threshold %.5f.\n", result,
            dualCameraVerificationConfig.rms, dualCameraVerificationConfig.rms_th);
    else if(result == 1)
        printf("DualCameraVerification failed with return value %d, not all corners are detected\n", result);
    else if(result == 2)
        printf("DualCameraVerification failed with return value %d,\nrms %.5f is larger than rms threshold %.5f.\n", result,
            dualCameraVerificationConfig.rms, dualCameraVerificationConfig.rms_th);

    if (YUVImage_left != NULL) delete[] YUVImage_left;
    if (YUVImage_right != NULL) delete[] YUVImage_right;
    return result;
}

int TestNV21_new(dualCameraVerificationInputConfig* inputConfig)
{
    clock_t start, stop;
    double time_spent;

    std::string filename_left = inputConfig->filename_left;
    std::string filename_right = inputConfig->filename_right;
    std::string filename_otp = inputConfig->filename_otp;
    std::string otpMapVer = inputConfig->otpMapVer;
    std::string moduleCombination = inputConfig->moduleCombination;
    int nx = inputConfig->nx;
    int ny = inputConfig->ny;
    double rms_th = inputConfig->rms_th;
    int leftImgWidth = inputConfig->leftImgWidth;
    int leftImgHeight = inputConfig->leftImgHeight;
    int rightImgWidth = inputConfig->rightImgWidth;
    int rightImgHeight = inputConfig->rightImgHeight;
    int calibrationWidth = inputConfig->calibrationWidth;
    int calibrationHeight = inputConfig->calibrationHeight;

    int leftWidth = leftImgWidth;
    int leftHeight = leftImgHeight;
    int rightWidth = rightImgWidth;
    int rightHeight = rightImgHeight;

    const char* left_image_file = filename_left.c_str();
    const char* right_image_file = filename_right.c_str();
    const char* otp_file = filename_otp.c_str();;
    ALOGD("TestNV21_new left_image_file=%s!\n",left_image_file);
    ALOGD("TestNV21_new right_image_file=%s!\n",right_image_file);
    ALOGD("TestNV21_new otp_file=%s!\n",otp_file);


    int moduleCombinationFlag = 0xffff;//detecting module combination format
                                       //currently, your combination should be 8M_2M or 13M_5M
    if(!moduleCombination.compare(std::string("8M_2M")))
        moduleCombinationFlag = DUALCAMCOMBINATION_8M_2M;
    else if(!moduleCombination.compare(std::string("13M_5M")))
        moduleCombinationFlag = DUALCAMCOMBINATION_13M_5M;
    else if(!moduleCombination.compare(std::string("13M_2M")))
        moduleCombinationFlag = DUALCAMCOMBINATION_13M_2M;
    else if(!moduleCombination.compare(std::string("16M_5M")))
        moduleCombinationFlag = DUALCAMCOMBINATION_16M_5M;
    else if(!moduleCombination.compare(std::string("16M_8M")))
        moduleCombinationFlag = DUALCAMCOMBINATION_16M_8M;
    else
    {
        ALOGD("unknown dualCam module combination!\n");
        return -1;
    }

    //Dual camera verification config.
    DualCameraVerificationConfig dualCameraVerificationConfig;
    dualCameraVerificationConfig.pattern_size_row = nx;
    dualCameraVerificationConfig.pattern_size_col = ny;
    dualCameraVerificationConfig.rms_th = rms_th;

    dualCameraVerificationConfig.leftWidth   = leftWidth;
    dualCameraVerificationConfig.leftHeight  = leftHeight;
    dualCameraVerificationConfig.rightWidth  = rightWidth;
    dualCameraVerificationConfig.rightHeight = rightHeight;

    dualCameraVerificationConfig.image_format = IMAGE_NV21_FORMAT;
    dualCameraVerificationConfig.width_calibration = calibrationWidth;
    dualCameraVerificationConfig.height_calibration = calibrationHeight;
    //Set vertical stereo = 0 default.
    dualCameraVerificationConfig.isverticalstereo = 0;
    //Set otp map version
    dualCameraVerificationConfig.otpMapVer = otpMapVer;
    //Set otp moduleCombination
    dualCameraVerificationConfig.moduleCombination = moduleCombinationFlag;

    start = clock();

    filehandle = dlopen(LIB_CACULATE_PATH, RTLD_NOW);
    if (!filehandle) {
        ALOGD("JNICameraVerifcation stderr:%s dlerror:%s\n", stderr, dlerror());
        return -1;
    }
    ALOGD("TestNV21_new JNIDualCameraVerfication2 DualCameraVerificationNV21_New");
    duDualcameraCaliMethod = (duDualcameraCali) dlsym(filehandle,
            "DualCameraVerificationNV21_New");
    if (!duDualcameraCaliMethod) {
        ALOGD("TestNV21_new dlsym stderr:%s dlerror:%s\n", stderr,
                dlerror());
        return -1;
    }
    ALOGD("TestNV21_new DualCameraVerfication getResultMethod done");
    int result = duDualcameraCaliMethod(left_image_file, right_image_file, otp_file,
            &dualCameraVerificationConfig);
    ALOGD("TestNV21_new DualCameraVerfication result: %d", result);

    stop = clock();
    time_spent = (double)(stop - start) / CLOCKS_PER_SEC;
    ALOGD("\n TestNV21_new DualCameraVerification time used = %fs\n", time_spent);
    rms = dualCameraVerificationConfig.rms;
    if(result == 0)
        printf("TestNV21_new DualCameraVerification success with return value %d,\nrms %.5f is less than rms threshold %.5f.\n", result,
            dualCameraVerificationConfig.rms, dualCameraVerificationConfig.rms_th);
    else if(result == 1)
        printf("TestNV21_new DualCameraVerification failed with return value %d, not all corners are detected\n", result);
    else if(result == 2)
        printf("TestNV21_new DualCameraVerification failed with return value %d,\nrms %.5f is larger than rms threshold %.5f.\n", result,
            dualCameraVerificationConfig.rms, dualCameraVerificationConfig.rms_th);

    return result;
}

int TestNV21_vcm(dualCameraVerificationInputConfig* inputConfig,int checkVcm)
{
    clock_t start, stop;
    double time_spent;

    std::string filename_left = inputConfig->filename_left;
    std::string filename_right = inputConfig->filename_right;
    std::string filename_otp = inputConfig->filename_otp;
    std::string otpMapVer = inputConfig->otpMapVer;
    std::string moduleCombination = inputConfig->moduleCombination;
    int nx = inputConfig->nx;
    int ny = inputConfig->ny;
    double rms_th = inputConfig->rms_th;
    int leftImgWidth = inputConfig->leftImgWidth;
    int leftImgHeight = inputConfig->leftImgHeight;
    int rightImgWidth = inputConfig->rightImgWidth;
    int rightImgHeight = inputConfig->rightImgHeight;
    int calibrationWidth = inputConfig->calibrationWidth;
    int calibrationHeight = inputConfig->calibrationHeight;

    int leftWidth = leftImgWidth;
    int leftHeight = leftImgHeight;
    int rightWidth = rightImgWidth;
    int rightHeight = rightImgHeight;


//  uint8 *YUVImage_left, *YUVImage_right;
//  YUVImage_left = new uint8[leftHeight * leftWidth * 3 / 2];
//  YUVImage_right = new uint8[rightHeight * rightWidth * 3 / 2];

    char *YUVImage_left, *YUVImage_right;
    YUVImage_left = new char[leftHeight * leftWidth * 3 / 2];
    YUVImage_right = new char[rightHeight * rightWidth * 3 / 2];
    const char* left = filename_left.c_str();;
    const char* right = filename_right.c_str();;

    if(!ReadYUV420File(/*filename_left.c_str()*/left, leftWidth, leftHeight, YUVImage_left)){
        ALOGD("ReadYUV420File YUVImage_left fail!");
        if (YUVImage_left != NULL) delete[] YUVImage_left;
        if (YUVImage_right != NULL) delete[] YUVImage_right;
        return -1;
    }
    if(!ReadYUV420File(/*filename_right.c_str()*/right, rightWidth, rightHeight, YUVImage_right)){
        ALOGD("ReadYUV420File YUVImage_right fail!");
        if (YUVImage_left != NULL) delete[] YUVImage_left;
        if (YUVImage_right != NULL) delete[] YUVImage_right;
        return -1;
    }


    CalibParam calibParam;
    DualCameraVerificationConfig dualCameraVerificationConfig;

    dualCameraVerificationConfig.pattern_size_row = nx;
    dualCameraVerificationConfig.pattern_size_col = ny;
    dualCameraVerificationConfig.rms_th = rms_th;

    dualCameraVerificationConfig.leftWidth   = leftWidth;
    dualCameraVerificationConfig.leftHeight  = leftHeight;
    dualCameraVerificationConfig.rightWidth  = rightWidth;
    dualCameraVerificationConfig.rightHeight = rightHeight;

    dualCameraVerificationConfig.image_format = IMAGE_NV21_FORMAT;
    dualCameraVerificationConfig.width_calibration = calibrationWidth;
    dualCameraVerificationConfig.height_calibration = calibrationHeight;

    ////for verification on phone, only bin input support//////////
    //currently only otp map version: v0.5 and v1.0 are supported//
    ////the otp map version is coded into an integer: v0.5 ---> 0//
    ////////////////////////////////////////////////v1.0 ---> 1////

    int otpMapVerFlag = 0xffff;//detecting the otp map version
    if(!otpMapVer.compare(std::string("v0.5")))
        otpMapVerFlag = DUALCAMOTPMAPVERSION_V0_5;//V0.5
    else if(!otpMapVer.compare(std::string("v1.0")))
        otpMapVerFlag = DUALCAMOTPMAPVERSION_V1_0;//V1.0
    else if(!otpMapVer.compare(std::string("v0.4")))
        otpMapVerFlag = DUALCAMOTPMAPVERSION_V0_4;//V0.4
    else
    {
        ALOGD("unknow otp map version!\n");
        if (YUVImage_left != NULL) delete[] YUVImage_left;
        if (YUVImage_right != NULL) delete[] YUVImage_right;
        return -1;
    }

    int moduleCombinationFlag = 0xffff;//detecting module combination format
                                       //currently, your combination should be 8M_2M or 13M_5M
    if(!moduleCombination.compare(std::string("8M_2M")))
        moduleCombinationFlag = DUALCAMCOMBINATION_8M_2M;
    else if(!moduleCombination.compare(std::string("13M_5M")))
        moduleCombinationFlag = DUALCAMCOMBINATION_13M_5M;
    else if(!moduleCombination.compare(std::string("13M_2M")))
        moduleCombinationFlag = DUALCAMCOMBINATION_13M_2M;
    else if(!moduleCombination.compare(std::string("16M_5M")))
        moduleCombinationFlag = DUALCAMCOMBINATION_16M_5M;
    else if(!moduleCombination.compare(std::string("16M_8M")))
        moduleCombinationFlag = DUALCAMCOMBINATION_16M_8M;
    else
    {
        ALOGD("unknown dualCam module combination!\n");
        printf("unknown dualCam module combination!\n");
        if (YUVImage_left != NULL) delete[] YUVImage_left;
        if (YUVImage_right != NULL) delete[] YUVImage_right;
        return -1;
    }

     //here to get otp_size
    long otp_size = 0;
    if(!get_calibration_file_size(filename_otp.c_str(), &otp_size)) // can not open otp file
    {
        ALOGD("get_calibration_file_size fail!\n");
        if (YUVImage_left != NULL) delete[] YUVImage_left;
        if (YUVImage_right != NULL) delete[] YUVImage_right;
        return -1;
    }

    filehandle = dlopen(LIB_CACULATE_PATH, RTLD_NOW);
    if (!filehandle) {
        ALOGD("JNICameraVerifcation stderr:%s dlerror:%s\n", stderr, dlerror());
        if (YUVImage_left != NULL) delete[] YUVImage_left;
        if (YUVImage_right != NULL) delete[] YUVImage_right;
        return -1;
    }
    ALOGD("JNICameraVerifcation JNIDualCameraVerfication2");
    getResultMethod = (getResult) dlsym(filehandle,
            "DualCameraVerificationNV21");
    if (!getResultMethod) {
        ALOGD("JNICameraVerifcation dlsym stderr:%s dlerror:%s\n", stderr,
                dlerror());
        if (YUVImage_left != NULL) delete[] YUVImage_left;
        if (YUVImage_right != NULL) delete[] YUVImage_right;
        return -1;
    }

    int result = -1;
    ALOGD("JNICameraVerifcation DualCameraVerfication otp_size: %d", otp_size);
    if((otp_size==456)||(otp_size==256))  // orig dual bin
    {

        int isVerticalStereo = 0xffff;//detecting the dualCam placement:arrange direction
        if(!readModuleArrangeDirFromBinDualCamOtp(filename_otp.c_str(), &isVerticalStereo, otpMapVerFlag, moduleCombinationFlag))
        {
            ALOGD("readModuleArrangeDirFromBinDualCamOtp fail!\n");
            if (YUVImage_left != NULL) delete[] YUVImage_left;
            if (YUVImage_right != NULL) delete[] YUVImage_right;
            return -1;
        }

        dualCameraVerificationConfig.isverticalstereo = isVerticalStereo;

        //detecting the otp format: txt or bin
        int LastN = 3;
        std::string txt_format = "txt";
        std::string otp_format = filename_otp.substr(filename_otp.size() - LastN);
        if(!otp_format.compare(txt_format))
        {
            read_calibration_param((int8*)(filename_otp.c_str()), &calibParam);
            ALOGD("If you are debuging on phone, the otp format should be \"bin\", not \"txt\"!\n");
        }
        else
        {
            if(!readCalibrationParamFromDiffOtpMapVer(filename_otp.c_str(), &calibParam, otpMapVerFlag))
            {
                ALOGD("JNICameraVerifcation DualCameraVerfication readCalibrationParamFromDiffOtpMapVer fail!");
                if (YUVImage_left != NULL) delete[] YUVImage_left;
                if (YUVImage_right != NULL) delete[] YUVImage_right;
                return -1;
            }
        }

        start = clock();
        int result = getResultMethod(YUVImage_left, YUVImage_right, &calibParam, &dualCameraVerificationConfig);
        stop = clock();
        time_spent = (double)(stop - start) / CLOCKS_PER_SEC;
        ALOGD("\nDualCameraVerification time used = %fs\n", time_spent);
        rms = dualCameraVerificationConfig.rms;
        ALOGD("JNICameraVerifcation DualCameraVerfication vcm rms: %d", rms);
        ALOGD("JNICameraVerifcation DualCameraVerfication vcm result: %d", result);
        if(result == 0)
            ALOGD("DualCameraVerification success with return value %d,\nrms %.5f is less than rms threshold %.5f.\n", result,
                dualCameraVerificationConfig.rms, dualCameraVerificationConfig.rms_th);
        else if(result == 1)
            ALOGD("DualCameraVerification failed with return value %d, not all corners are detected\n", result);
        else if(result == 2)
            ALOGD("DualCameraVerification failed with return value %d,\nrms %.5f is larger than rms threshold %.5f.\n", result,
                dualCameraVerificationConfig.rms, dualCameraVerificationConfig.rms_th);

        if (YUVImage_left != NULL) delete[] YUVImage_left;
        if (YUVImage_right != NULL) delete[] YUVImage_right;
        return result;
    }
    else if(((otp_size-16)%256) == 0)//n vcm calibrate
    {


        int isVerticalStereo = 0xffff;//detecting the dualCam placement:arrange direction
        if(!readModuleLocationDirFromVcmOtpBin(filename_otp.c_str(), &isVerticalStereo))
        {
            ALOGD("JNICameraVerifcation DualCameraVerfication readModuleLocationDirFromVcmOtpBin fail!");
            if (YUVImage_left != NULL) delete[] YUVImage_left;
            if (YUVImage_right != NULL) delete[] YUVImage_right;
            return -1;
        }

        dualCameraVerificationConfig.isverticalstereo = isVerticalStereo;

        int caliCount = 0x00;
        if(!readCaliCountDirFromVcmOtpBin(filename_otp.c_str(), &caliCount))
        {
            ALOGD("JNICameraVerifcation DualCameraVerfication readCaliCountDirFromVcmOtpBin fail!");
            if (YUVImage_left != NULL) delete[] YUVImage_left;
            if (YUVImage_right != NULL) delete[] YUVImage_right;
            return -1;
        }
        int count = 0;
        ALOGD("JNICameraVerifcation DualCameraVerfication vcm caliCount: %d", caliCount);
        while(( count++) < caliCount)
        {
            ALOGD("Verification begin at count=%d,checkVcm=%d\n",count,checkVcm);
            if(checkVcm){
                char buff[64] = {0};
                snprintf(buff, sizeof(buff), "%s%d","vcm",count-1);
                ALOGD("Verification begin at buff=%s\n",buff);
                if(filename_left.find(buff) == std::string::npos){
                    continue;
                }
            }
            ALOGD("Verification begin at 111 count=%d\n",count);
            readCalibrationParamFromVcmOtpBin(filename_otp.c_str(),&calibParam, count);

            start = clock();
            result = getResultMethod(YUVImage_left, YUVImage_right, &calibParam, &dualCameraVerificationConfig);
            stop = clock();

            rms = dualCameraVerificationConfig.rms;
            ALOGD("JNICameraVerifcation DualCameraVerfication vcm rms: %.5f", rms);
            time_spent = (double)(stop - start) / CLOCKS_PER_SEC;
            ALOGD("\nDualCameraVerification time used = %fs\n", time_spent);
            if(result == 0)
            {
                ALOGD("Verification Success at vcm%d\n",count);
                ALOGD("DualCameraVerification success with return value %d,\nrms %.5f is less than rms threshold %.5f.\n", result,
                    dualCameraVerificationConfig.rms, dualCameraVerificationConfig.rms_th);
                if(checkVcm){
                    ALOGD("Verification checkVcm Success!\n");
                    if (YUVImage_left != NULL) delete[] YUVImage_left;
                    if (YUVImage_right != NULL) delete[] YUVImage_right;
                    return result;
                }
                if(count == caliCount){
                    ALOGD("Verification ALL Success!\n");
                    if (YUVImage_left != NULL) delete[] YUVImage_left;
                    if (YUVImage_right != NULL) delete[] YUVImage_right;
                    return result;
                }
            }
            else if(result == 1)
            {
                ALOGD("Verification Failed at vcm%d\n",count);
                ALOGD("DualCameraVerification failed with return value %d, not all corners are detected\n", result);
                if (YUVImage_left != NULL) delete[] YUVImage_left;
                if (YUVImage_right != NULL) delete[] YUVImage_right;
                return result;
            }
            else if(result == 2)
            {
                ALOGD("Verification Failed at vcm%d\n",count);
                ALOGD("DualCameraVerification failed with return value %d,\nrms %.5f is larger than rms threshold %.5f.\n", result,
                    dualCameraVerificationConfig.rms, dualCameraVerificationConfig.rms_th);
                if (YUVImage_left != NULL) delete[] YUVImage_left;
                if (YUVImage_right != NULL) delete[] YUVImage_right;
                return result;
            }
            ALOGD("JNICameraVerifcation DualCameraVerfication vcm2 result: %d", result);
        }
    }
    else
    {
        ALOGD("unknown dualCam otp bin !\n");
    }


    delete[] YUVImage_left;
    delete[] YUVImage_right;

    ALOGD("JNICameraVerifcation DualCameraVerfication vcm3 fail!");
    return result;
}

static jint JNIDualCameraVerfication(JNIEnv* env, jobject thiz,
        jstring image_left, jstring image_right, jstring otpfile) {
    const char *filename_left = env->GetStringUTFChars(image_left, 0);
    const char *filename_right = env->GetStringUTFChars(image_right, 0);
    const char *filename_otp = env->GetStringUTFChars(otpfile, 0);
    int result = -1;
    char *error;
    ALOGD("JNICameraVerifcation JNIDualCameraVerfication");
    int otpSize = Check_OtpSize(filename_otp);
    //CalibParam calibParam;
//  DualCameraVerficationConfig dualCameraVerficationConfig = { 11, 7, 1.5,
//          width, height, IMAGE_NV21_FORMAT, 800, 600, 0.0 };
    dualCameraVerificationInputConfig inputConfig;
    inputConfig.filename_left = filename_left;
    inputConfig.filename_right = filename_right;
    inputConfig.filename_otp = filename_otp;
    inputConfig.otpMapVer = "v0.5";
    if(otpSize == 256){
        inputConfig.otpMapVer = "v1.0";
    }else if(otpSize == 228){
        inputConfig.otpMapVer = "v0.4";
    }else if(otpSize == 456){
        inputConfig.otpMapVer = "v1.0";
    }
    inputConfig.moduleCombination = "16M_8M";
    inputConfig.nx = 11;
    inputConfig.ny = 7;
    inputConfig.rms_th = 1.5;
    inputConfig.leftImgWidth = 1600;
    inputConfig.leftImgHeight = 1200;
    inputConfig.rightImgWidth = 1600;
    inputConfig.rightImgHeight = 1200;
    inputConfig.calibrationWidth = 800;
    inputConfig.calibrationHeight = 600;

    //result = TestNV21(&inputConfig);
    result = TestNV21_vcm(&inputConfig,0);
    ALOGD("JNICameraVerifcation JNIDualCameraVerfication 11 result:%d\n", result);
    return result;
}

static jint JNIDualCameraVerficationVCM(JNIEnv* env, jobject thiz,
        jstring image_left, jstring image_right, jstring otpfile) {
    const char *filename_left = env->GetStringUTFChars(image_left, 0);
    const char *filename_right = env->GetStringUTFChars(image_right, 0);
    const char *filename_otp = env->GetStringUTFChars(otpfile, 0);
    int result = -1;
    char *error;
    ALOGD("JNICameraVerifcation JNIDualCameraVerfication");
    int otpSize = Check_OtpSize(filename_otp);
    //CalibParam calibParam;
//  DualCameraVerficationConfig dualCameraVerficationConfig = { 11, 7, 1.5,
//          width, height, IMAGE_NV21_FORMAT, 800, 600, 0.0 };
    dualCameraVerificationInputConfig inputConfig;
    inputConfig.filename_left = filename_left;
    inputConfig.filename_right = filename_right;
    inputConfig.filename_otp = filename_otp;
    inputConfig.otpMapVer = "v0.5";
    if(otpSize == 256){
        inputConfig.otpMapVer = "v1.0";
    }else if(otpSize == 228){
        inputConfig.otpMapVer = "v0.4";
    }else if(otpSize == 456){
        inputConfig.otpMapVer = "v1.0";
    }
    inputConfig.moduleCombination = "16M_8M";
    inputConfig.nx = 11;
    inputConfig.ny = 7;
    inputConfig.rms_th = 0.6;
    inputConfig.leftImgWidth = 1600;
    inputConfig.leftImgHeight = 1200;
    inputConfig.rightImgWidth = 1600;
    inputConfig.rightImgHeight = 1200;
    inputConfig.calibrationWidth = 800;
    inputConfig.calibrationHeight = 600;

    //result = TestNV21(&inputConfig);
    result = TestNV21_vcm(&inputConfig,1);
    ALOGD("JNICameraVerifcation JNIDualCameraVerfication 11 result:%d\n", result);
    return result;
}

/*SPRD bug 759782 : Display RMS value*/
static jdouble JNIGetCameraVerficationRMS(JNIEnv* env, jobject thiz) {
    return rms;
}
/*@}*/

static const char *hardWareClassPathName =
        "com/sprd/cameracalibration/itemstest/camera/NativeCameraCalibration";

static JNINativeMethod getMethods[] = { { "native_dualCameraVerfication",
        "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)I",
        (void*) JNIDualCameraVerfication },
        { "native_getCameraVerficationRMS",
        "()D", (void*) JNIGetCameraVerficationRMS },
        { "native_dualCameraVerfication_vcm",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)I",
                (void*) JNIDualCameraVerficationVCM },
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
