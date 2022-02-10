#ifndef _READ_CALIB_PARAM_H_
#define _READ_CALIB_PARAM_H_

#ifdef __cplusplus
extern "C"
{
#endif

#include "CalibParam.h"
#include "typedef.h"

enum dualCamCombination
{
    DUALCAMCOMBINATION_8M_2M,
    DUALCAMCOMBINATION_13M_5M,
    DUALCAMCOMBINATION_13M_2M,
    DUALCAMCOMBINATION_16M_5M,
    DUALCAMCOMBINATION_16M_8M
};

enum dualCamOtpMapVer
{
    DUALCAMOTPMAPVERSION_V0_4,//V0.4
    DUALCAMOTPMAPVERSION_V0_5,//V0.5
    DUALCAMOTPMAPVERSION_V1_0//v1.0
};

BOOL read_calibration_param(int8* calibration_file_name, CalibParam* param);
BOOL read_calibration_param_int(int *OTP_params_output, CalibParam* param);
BOOL read_bin_calibration_param(const char* otp_original, CalibParam* param);
BOOL read_txt_module_arrange_dir(int8* module_arrange, int* dir);
BOOL read_bin_module_arrange_dir(const char* otp_original,int* dir);
BOOL readModuleArrangeDirFromBinDualCamOtp(const char* otp_original, int* dir, int otpMapVer, int moduleCombinationFlag);
BOOL readCalibrationParamFromDiffOtpMapVer(const char* otp_original, CalibParam* param, int otpMapVer);

BOOL readModuleLocationDirFromVcmOtpBin(const char* otp_original, int* dir);
BOOL readCaliCountDirFromVcmOtpBin(const char* otp_original, int* count);
BOOL readCalibrationParamFromVcmOtpBin(const char* otp_original, CalibParam* param, int count);
BOOL  get_calibration_file_size(const char* otp_file, long* size);
#ifdef __cplusplus
}
#endif

#endif
