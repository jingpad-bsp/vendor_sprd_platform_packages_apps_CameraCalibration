#include <stdio.h>
#include "header/CalibParam.h"
#include "header/typedef.h"
#include "header/ReadCalibParam.h"
#include "utils/Log.h"
#define OTP_FILE_ITEM_COUNT 256//currently, in otpMapVer v0.5 and v1.0, 256Bytes contained
#define OTP_FILE_ITEM_COUNT2 456
void dump_data(CalibParam* param){
        int32 i,j,offset=0;
        int32 PrecisionBits = 19;

        ALOGD("JNICameraVerifcation read_calibration_param_int param->cx_left: %d", param->cx_left);
        ALOGD("JNICameraVerifcation read_calibration_param_int param->cy_left: %d", param->cy_left);
        ALOGD("JNICameraVerifcation read_calibration_param_int param->fx_left: %d", param->fx_left);
        ALOGD("JNICameraVerifcation read_calibration_param_int param->fy_left: %d", param->fy_left);
        ALOGD("JNICameraVerifcation read_calibration_param_int param->k1_left: %d", param->k1_left);
        ALOGD("JNICameraVerifcation read_calibration_param_int param->k2_left: %d", param->k2_left);
        ALOGD("JNICameraVerifcation read_calibration_param_int param->k3_left: %d", param->k3_left);
        for (i = 0; i < 3; i ++)
        {
            for (j = 0; j < 3; j ++)
            {
                ALOGD("JNICameraVerifcation read_calibration_param_int param->R_left[i][j]: %d", param->R_left[i][j]);
            }
        }
        for (i = 0; i < 3; i ++)
        {
            for (j = 0; j < 3; j ++)
            {
                ALOGD("JNICameraVerifcation read_calibration_param_int param->P_left[i][j]: %d", param->P_left[i][j]);
            }
        }
        ALOGD("JNICameraVerifcation read_calibration_param_int param->cx_right: %d", param->cx_right);
        ALOGD("JNICameraVerifcation read_calibration_param_int param->cy_right: %d", param->cy_right);
        ALOGD("JNICameraVerifcation read_calibration_param_int param->fx_right: %d", param->fx_right);
        ALOGD("JNICameraVerifcation read_calibration_param_int param->fy_right: %d", param->fy_right);
        ALOGD("JNICameraVerifcation read_calibration_param_int param->k1_right: %d", param->k1_right);
        ALOGD("JNICameraVerifcation read_calibration_param_int param->k2_right: %d", param->k2_right);
        ALOGD("JNICameraVerifcation read_calibration_param_int param->k3_right: %d", param->k3_right);

        for (i = 0; i < 3; i ++)
        {
            for (j = 0; j < 3; j ++)
            {
                ALOGD("JNICameraVerifcation read_calibration_param_int param->R_right[i][j]: %d", param->R_right[i][j]);
            }
        }
        for (i = 0; i < 3; i ++)
        {
            for (j = 0; j < 3; j ++)
            {
                ALOGD("JNICameraVerifcation read_calibration_param_int param->P_right[i][j]: %d", param->P_right[i][j]);
            }
        }

        ALOGD("JNICameraVerifcation read_calibration_param_int param->disparity_min: %d", param->disparity_min);
        ALOGD("JNICameraVerifcation read_calibration_param_int param->disparity_max: %d", param->disparity_max);
        ALOGD("JNICameraVerifcation read_calibration_param_int param->d_52: %d", param->d_52);
        ALOGD("JNICameraVerifcation read_calibration_param_int param->d_53: %d", param->d_53);
        ALOGD("JNICameraVerifcation read_calibration_param_int param->d_54: %d", param->d_54);
        ALOGD("JNICameraVerifcation read_calibration_param_int param->d_55: %d", param->d_55);
        ALOGD("JNICameraVerifcation read_calibration_param_int param->d_56: %d", param->d_56);
        ALOGD("JNICameraVerifcation read_calibration_param_int param->d_57: %d", param->d_57);
}

BOOL read_calibration_param(int8* calibration_file_name, CalibParam* param)
{
    FILE* pf;
    int32 i,j;
    int32 param_int;
    int32 PrecisionBits = 19;
    pf = fopen(calibration_file_name, "rt");
    if(NULL == pf) {
        printf("Cann't open calibration file: %s\n", calibration_file_name);
        return FALSE;
    }

    fscanf(pf, "%d\n", &param_int);
    param->cx_left = (double)param_int/(1<<PrecisionBits);
    fscanf(pf, "%d\n", &param_int);
    param->cy_left = (double)param_int/(1<<PrecisionBits);
    fscanf(pf, "%d\n", &param_int);
    param->fx_left = (double)param_int/(1<<PrecisionBits);
    fscanf(pf, "%d\n", &param_int);
    param->fy_left = (double)param_int/(1<<PrecisionBits);
    fscanf(pf, "%d\n", &param_int);
    param->k1_left = (double)param_int/(1<<PrecisionBits);
    fscanf(pf, "%d\n", &param_int);
    param->k2_left = (double)param_int/(1<<PrecisionBits);
    fscanf(pf, "%d\n", &param_int);
    param->k3_left = (double)param_int/(1<<PrecisionBits);
    for (i = 0; i < 3; i ++)
    {
        for (j = 0; j < 3; j ++)
        {
            fscanf(pf, "%d\n", &param_int);
            param->R_left[i][j] = (double)param_int/(1<<PrecisionBits);
        }
    }
    for (i = 0; i < 3; i ++)
    {
        for (j = 0; j < 3; j ++)
        {
            fscanf(pf, "%d\n", &param_int);
            param->P_left[i][j] = (double)param_int/(1<<PrecisionBits);
        }
    }

    fscanf(pf, "%d\n", &param_int);
    param->cx_right = (double)param_int/(1<<PrecisionBits);
    fscanf(pf, "%d\n", &param_int);
    param->cy_right = (double)param_int/(1<<PrecisionBits);
    fscanf(pf, "%d\n", &param_int);
    param->fx_right = (double)param_int/(1<<PrecisionBits);
    fscanf(pf, "%d\n", &param_int);
    param->fy_right = (double)param_int/(1<<PrecisionBits);
    fscanf(pf, "%d\n", &param_int);
    param->k1_right = (double)param_int/(1<<PrecisionBits);
    fscanf(pf, "%d\n", &param_int);
    param->k2_right = (double)param_int/(1<<PrecisionBits);
    fscanf(pf, "%d\n", &param_int);
    param->k3_right = (double)param_int/(1<<PrecisionBits);
    for (i = 0; i < 3; i ++)
    {
        for (j = 0; j < 3; j ++)
        {
            fscanf(pf, "%d\n", &param_int);
            param->R_right[i][j] = (double)param_int/(1<<PrecisionBits);
        }
    }
    for (i = 0; i < 3; i ++)
    {
        for (j = 0; j < 3; j ++)
        {
            fscanf(pf, "%d\n", &param_int);
            param->P_right[i][j] = (double)param_int/(1<<PrecisionBits);
        }
    }

    fscanf(pf, "%d\n", &param_int);
    param->disparity_min = (param_int>>PrecisionBits);
    fscanf(pf, "%d\n", &param_int);
    param->disparity_max = (param_int>>PrecisionBits);

    fscanf(pf, "%d\n", &param_int);
    param->d_52 = (double)param_int/(1<<PrecisionBits);
    fscanf(pf, "%d\n", &param_int);
    param->d_53 = (double)param_int/(1<<PrecisionBits);
    fscanf(pf, "%d\n", &param_int);
    param->d_54 = (double)param_int/(1<<PrecisionBits);
    fscanf(pf, "%d\n", &param_int);
    param->d_55 = (double)param_int/(1<<PrecisionBits);
    fscanf(pf, "%d\n", &param_int);
    param->d_56 = (double)param_int/(1<<PrecisionBits);
    fscanf(pf, "%d\n", &param_int);
    param->d_57 = (double)param_int/(1<<PrecisionBits);

    fclose(pf);
    dump_data(param);
    return TRUE;
}

BOOL read_calibration_param_int(int32 *OTP_params_output, CalibParam* param)
{
    int32 i,j,offset=0;
    int32 PrecisionBits = 19;

    param->cx_left = (double)OTP_params_output[offset++]/(1<<PrecisionBits);
    param->cy_left = (double)OTP_params_output[offset++]/(1<<PrecisionBits);
    param->fx_left = (double)OTP_params_output[offset++]/(1<<PrecisionBits);
    param->fy_left = (double)OTP_params_output[offset++]/(1<<PrecisionBits);
    param->k1_left = (double)OTP_params_output[offset++]/(1<<PrecisionBits);
    param->k2_left = (double)OTP_params_output[offset++]/(1<<PrecisionBits);
    param->k3_left = (double)OTP_params_output[offset++]/(1<<PrecisionBits);

    for (i = 0; i < 3; i ++)
    {
        for (j = 0; j < 3; j ++)
        {
            param->R_left[i][j] = (double)OTP_params_output[offset++]/(1<<PrecisionBits);
        }
    }

    for (i = 0; i < 3; i ++)
    {
        for (j = 0; j < 3; j ++)
        {
            param->P_left[i][j] = (double)OTP_params_output[offset++]/(1<<PrecisionBits);
        }
    }

    param->cx_right = (double)OTP_params_output[offset++]/(1<<PrecisionBits);
    param->cy_right = (double)OTP_params_output[offset++]/(1<<PrecisionBits);
    param->fx_right = (double)OTP_params_output[offset++]/(1<<PrecisionBits);
    param->fy_right = (double)OTP_params_output[offset++]/(1<<PrecisionBits);
    param->k1_right = (double)OTP_params_output[offset++]/(1<<PrecisionBits);
    param->k2_right = (double)OTP_params_output[offset++]/(1<<PrecisionBits);
    param->k3_right = (double)OTP_params_output[offset++]/(1<<PrecisionBits);

    for (i = 0; i < 3; i ++)
    {
        for (j = 0; j < 3; j ++)
        {
            param->R_right[i][j] = (double)OTP_params_output[offset++]/(1<<PrecisionBits);
        }
    }
    for (i = 0; i < 3; i ++)
    {
        for (j = 0; j < 3; j ++)
        {
            param->P_right[i][j] = (double)OTP_params_output[offset++]/(1<<PrecisionBits);
        }
    }

//  param->disparity_min = (OTP_params_output[offset++]>>PrecisionBits);
//  param->disparity_max = (OTP_params_output[offset++]>>PrecisionBits);
//
//  param->d_52 = OTP_params_output[offset++];
//  param->d_53 = OTP_params_output[offset++];
//  param->d_54 = OTP_params_output[offset++];
//  param->d_55 = OTP_params_output[offset++];
//  param->d_56 = OTP_params_output[offset++];
//  param->d_57 = OTP_params_output[offset++];

    param->disparity_min = (OTP_params_output[offset++]>>PrecisionBits);
    param->disparity_max = (OTP_params_output[offset++]>>PrecisionBits);

    param->d_52 = (double)OTP_params_output[offset++]/(1<<PrecisionBits);
    param->d_53 = (double)OTP_params_output[offset++]/(1<<PrecisionBits);
    param->d_54 = (double)OTP_params_output[offset++]/(1<<PrecisionBits);
    param->d_55 = (double)OTP_params_output[offset++]/(1<<PrecisionBits);
    param->d_56 = (double)OTP_params_output[offset++]/(1<<PrecisionBits);
    param->d_57 = (double)OTP_params_output[offset++]/(1<<PrecisionBits);

    dump_data(param);
    return TRUE;
}

BOOL readCalibrationParamFromDiffOtpMapVer(const char* otp_original, CalibParam* param, int otpMapVer)
{
    int OTP_Data[57];
    unsigned char dualCamVer = 0xff;
    FILE* fp = fopen(otp_original, "rb");
    if(!fp)
    {
        printf("dualCam otp load failed!\n");
        return FALSE;
    }
    if(otpMapVer == DUALCAMOTPMAPVERSION_V1_0)
    {
        fread(&dualCamVer, 1, 1, fp);
        if((dualCamVer != 0x02) && (dualCamVer != 0x04))
        {
            fclose(fp);
            printf("dualCam version should be 0x02, pls check your otpMapVer!\n");
            return FALSE;
        }
        fseek(fp, 1L, 0L);
    }
    fread(OTP_Data, sizeof(int), 57, fp);
    fclose(fp);

    read_calibration_param_int(OTP_Data, param);

    return TRUE;
}

BOOL read_bin_calibration_param(const char* otp_original, CalibParam* param)
{
    int OTP_Data[57];

    FILE *fid = fopen(otp_original, "rb");
    if(!fid)
    {
        printf("otp.txt load failed!\n");
        return FALSE;
    }
    fread(OTP_Data, sizeof(int), 57, fid);
    fclose(fid);

    read_calibration_param_int(OTP_Data, param);

    return TRUE;
}

BOOL read_txt_module_arrange_dir(int8* module_arrange, int* dir)
{
    FILE* pf = fopen(module_arrange, "rt");
    if(NULL == pf) {
        printf("Cann't open: %s\n", module_arrange);
        return FALSE;
    }

    fscanf(pf, "%d\n", dir);

    fclose(pf);

    return TRUE;
}

BOOL read_bin_module_arrange_dir(const char* module_arrange, int* dir)
{
    FILE *fid = fopen(module_arrange, "rb");
    if(!fid)
    {
        printf("module_arrange.bin load failed!\n");
        return FALSE;
    }
    if(fread(dir, sizeof(int), 1, fid) != 1)
    {
        printf("fread failed!");
    }
    fclose(fid);

    return TRUE;
}

BOOL readModuleArrangeDirFromBinDualCamOtp(const char* otp_original, int* dir, int otpMapVer, int moduleCombinationFlag)
{
    FILE* fp;
    int size;
    unsigned char dir_read = 0xff;
    ALOGD("readModuleArrangeDirFromBinDualCamOtp 1!\n");
    fp = fopen(otp_original, "rb");
    if(!fp)
    {
        ALOGD("dualCam otp file load failed!\n");
        printf("dualCam otp file load failed!\n");
        return FALSE;
    }
    fseek(fp, 0L, SEEK_END);//end is eof, not the last effective area
    size = ftell(fp);
    ALOGD("readModuleArrangeDirFromBinDualCamOtp 2 size=%d",size);
    if( (size != OTP_FILE_ITEM_COUNT)  &&  (size !=  OTP_FILE_ITEM_COUNT2))
    {
        ALOGD("dualCam otp file version error, make sure your otp version is v0.5 or v1.0!\n");
        printf("dualCam otp file version error, make sure your otp version is v0.5 or v1.0!\n");
        fclose(fp);
        return FALSE;
    }

    if(otpMapVer == DUALCAMOTPMAPVERSION_V0_5)//V0.5
    {
        ALOGD("readModuleArrangeDirFromBinDualCamOtp 3 otpMapVer=%d",otpMapVer);
        //currently, in v0.5 only DUALCAMCOMBINATION_8M_2M is defined
        fseek(fp, -26L, SEEK_END);
        ALOGD("readModuleArrangeDirFromBinDualCamOtp 4 otpMapVer=%d",otpMapVer);
        if(fread(&dir_read, 1, 1, fp) != 1)
        {
            printf("fread failed!");
        }
        //ALOGD("readModuleArrangeDirFromBinDualCamOtp 244 dir_read=%s",dir_read);
    }
    else if(otpMapVer == DUALCAMOTPMAPVERSION_V1_0)
    {
        switch(moduleCombinationFlag)
        {
            case DUALCAMCOMBINATION_8M_2M://not defined yet
                break;
            case DUALCAMCOMBINATION_13M_5M:
                fseek(fp, -1L, SEEK_END);
                if(fread(&dir_read, 1, 1, fp) != 1)
                {
                    printf("fread error!");
                }
                break;
            case DUALCAMCOMBINATION_13M_2M://no arrange direction info
                break;
            case DUALCAMCOMBINATION_16M_5M:
                fseek(fp, -1L, SEEK_END);
                if(fread(&dir_read, 1, 1, fp) != 1)
                {
                    printf("fread error!");
                }
                break;
            case DUALCAMCOMBINATION_16M_8M:
                fseek(fp, -1L, SEEK_END);
                if(fread(&dir_read, 1, 1, fp) != 1)
                {
                    printf("fread error!");
                }
                break;
            default:
                ALOGD("unknown dualCam combination!\n");
                printf("unknown dualCam combination!\n");
                break;
        }
    }
    fclose(fp);

    //ALOGD("dualCam combination dir_read=%s",dir_read);
    //transform
    if(dir_read == 0x01 || dir_read ==0x02)
        *dir = 1;
    else if(dir_read == 0x3)
        *dir = 0;
    else
    {
        printf("unknown dualCam arrange direction! please check your otp or module combination.\n");
        return FALSE;
    }
    return TRUE;
}

BOOL readModuleLocationDirFromVcmOtpBin(const char* otp_original, int* dir)
{
    FILE* fp;
    int size;
    unsigned char dir_read = 0xff;

    fp = fopen(otp_original, "rb");
    if(!fp)
    {
        printf("dualCam otp file load failed!\n");
        return FALSE;
    }

    fseek(fp, 4, SEEK_SET);
    fread(&dir_read, 1, 1, fp);

    fclose(fp);

    //transform
    if(dir_read == 0x01 || dir_read ==0x02)
        *dir = 1;
    else if(dir_read == 0x3)
        *dir = 0;
    else
    {
        printf("unknown dualCam arrange direction! please check your otp or module combination.\n");
        return FALSE;
    }
    return TRUE;
}

BOOL readCaliCountDirFromVcmOtpBin(const char* otp_original, int* count)
{
    FILE* fp;
    int size;
    unsigned char count_read = 0x00;

    fp = fopen(otp_original, "rb");
    if(!fp)
    {
        ALOGD("dualCam otp file load failed!\n");
        return FALSE;
    }

    fseek(fp, 5, SEEK_SET);
    fread(&count_read, 1, 1, fp);

    fclose(fp);
    ALOGD("readCaliCountDirFromVcmOtpBin count_read %d.\n", count_read);
    //transform
    if(count_read > 0)
        *count = count_read;
    else
    {
        ALOGD(" Error calibration count in otp bin !.\n");
        return FALSE;
    }
    return TRUE;
}
BOOL readCalibrationParamFromVcmOtpBin(const char* otp_original, CalibParam* param,int count)
{
    int OTP_Data[57];
    unsigned char dualCamVer = 0xff;
    FILE* fp = fopen(otp_original, "rb");
    if(!fp)
    {
        printf("phone dualCam otp load failed!\n");
        return FALSE;
    }

#if 0
    if(count ==1)
    {
        fseek(fp, 16, SEEK_SET);
        fread(OTP_Data, sizeof(int), 57, fp);
    }
    else  if(count ==2)
    {
        fseek(fp, 272, SEEK_SET);
        fread(OTP_Data, sizeof(int), 57, fp);
    }
    else  if(count ==3)
    {
        fseek(fp, 528, SEEK_SET);
        fread(OTP_Data, sizeof(int), 57, fp);
    }
    else  if(count ==4)
    {
        fseek(fp, 784, SEEK_SET);
        fread(OTP_Data, sizeof(int), 57, fp);
    }
    else  if(count ==5)
    {
        fseek(fp, 1040, SEEK_SET);
        fread(OTP_Data, sizeof(int), 57, fp);
    }
    else  if(count ==6)
    {
        fseek(fp, 1296, SEEK_SET);
        fread(OTP_Data, sizeof(int), 57, fp);
    }
    else  if(count ==7)
    {
        fseek(fp, 1552, SEEK_SET);
        fread(OTP_Data, sizeof(int), 57, fp);
    }
#else
    //n time
    int offset = 16+256*(count-1);
    fseek(fp, offset, SEEK_SET);
    if(fread(OTP_Data, sizeof(int), 57, fp) != 57)
    {
        printf("fread failed!");
    }
#endif

    fclose(fp);

    read_calibration_param_int(OTP_Data, param);

    return TRUE;
}

BOOL  get_calibration_file_size(const char* otp_file, long* size)
{
    FILE* fp;
    long sz;

    fp = fopen(otp_file, "r");
    if(fp == NULL) {
        printf("open file error:");
        return FALSE;
    }
    fseek(fp, 0, SEEK_END);
    sz = ftell(fp);
    fclose(fp);

    *size = sz;

   return TRUE;
}
