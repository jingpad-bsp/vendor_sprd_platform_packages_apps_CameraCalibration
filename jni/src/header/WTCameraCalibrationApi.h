#ifndef _WTCAMERACALIBRATION_API_H_
#define _WTCAMERACALIBRATION_API_H_

#pragma once

#include "string.h"

#ifdef __cplusplus
extern "C"
{
#endif

#ifndef NDK
#ifdef WTCAMERACALIBRATIONLIB_EXPORTS
#define WTCAMERACALIBRATIONLIB_API __declspec(dllexport)
#else
#define WTCAMERACALIBRATIONLIB_API __declspec(dllimport)
#endif
#else 
#define WTCAMERACALIBRATIONLIB_API __attribute__((visibility("default")))
#endif 

/* Library Errors */
enum
{
	UNLOACKKEY_OK = 0,
	UNLOACKKEY_ERRPARAM,
	UNLOACKKEY_ERRGEN,
	
/* insert here new errors */
	UNLOACKKEY_ERRMAX
};

#define  USE_OTP_HEADER 

#define INPUT_WT_PARAMETER_SIZE  32


#ifdef USE_OTP_HEADER
#define PRODUCT_WT_OTP_OUTPUT_HEADER_SIZE 4
#endif 
#define PRODUCT_WT_OTP_OUTPUT_PARAMETER_SIZE 114


#define WT_TOTAL_NUM_ERROR  25 

typedef struct _CFA_DATA_T_
{
	int nWidth;
	int nHeight;
	int BlackLevel;
	int BayerPattern;
	int DataBits;
}CFA_DATA_T;

typedef struct _INPUT_WT_PARAM_DATA_T_
{
	float agParam[INPUT_WT_PARAMETER_SIZE];
	int LeftImage_nWidth;
	int LeftImage_nHeight;
	int RightImage_nWidth;
	int RightImage_nHeight;
	int Calibration_nWidth;
	int Calibration_nHeight;

	CFA_DATA_T LeftImageCFA;
	CFA_DATA_T RightImageCFA;

	int nCount;
	_INPUT_WT_PARAM_DATA_T_()
	{
		memset(this,0, sizeof(_INPUT_WT_PARAM_DATA_T_));
		nCount = INPUT_WT_PARAMETER_SIZE;
	}

}INPUT_WT_PARAM_DATA_T,*PINPUTWTPARAM_DATA_PTR;

typedef struct _WT_OTP_DATA_T_
{
#ifdef USE_OTP_HEADER
	int agHeader[PRODUCT_WT_OTP_OUTPUT_HEADER_SIZE];
#endif 
	int agOTP[PRODUCT_WT_OTP_OUTPUT_PARAMETER_SIZE];
	int nCount;
	_WT_OTP_DATA_T_()
	{
		memset(this,0, sizeof(_WT_OTP_DATA_T_));
		nCount = PRODUCT_WT_OTP_OUTPUT_PARAMETER_SIZE;
	}

}WT_OTP_DATA_T,*PWTOTP_DATA_PTR;


WTCAMERACALIBRATIONLIB_API int  WT_Calibration_VerificationRaw(const unsigned short* pLeftImage, const unsigned short* pRightImage, INPUT_WT_PARAM_DATA_T stInputParam, WT_OTP_DATA_T *pOutOTP, int wVCM, int tVCM);
WTCAMERACALIBRATIONLIB_API int  WT_Calibration_VerificationYUV(const char* filename_left, const char* filename_right,INPUT_WT_PARAM_DATA_T stInputParam, WT_OTP_DATA_T *pOutOTP, int wVCM, int tVCM);
WTCAMERACALIBRATIONLIB_API void WT_load_parameter(char *filename,float *params_input, int size);
WTCAMERACALIBRATIONLIB_API void WT_ReadMe(void);
WTCAMERACALIBRATIONLIB_API void WT_GetCalibLibVersion(char* VersionInfo);

#ifdef __cplusplus
}
#endif

#endif //_WTCAMERACALIBRATION_API_H_