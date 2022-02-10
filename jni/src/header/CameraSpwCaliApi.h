#ifndef _CAMERASPWCALI_API_H_
#define _CAMERASPWCALI_API_H_

#include <string.h>

#ifndef NDK
#ifdef SPWMONOCAMERACALIBRATION_EXPORTS
#define SPWCAMERACALI_API  extern "C" __declspec(dllexport)
#else
#define SPWCAMERACALI_API  extern "C" __declspec(dllimport)
#endif
#else 
#define SPWCAMERACALI_API  extern "C"  __attribute__((visibility("default")))
#endif 


#define ERR_NO_ERROR             (0)
#define ERR_FAILED_CALLOC_BUFFER (-1)
#define ERR_READ_YUV_FAIL        (-2)

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

#define INPUT_SPW_PARAMETER_SIZE  21

#ifdef USE_OTP_HEADER
#define PRODUCT_SPW_OTP_OUTPUT_HEADER_SIZE 4
#endif 
#define PRODUCT_SPW_OTP_OUTPUT_PARAMETER_SIZE 114


#define TOTAL_NUM_ERROR  6  
#define OUTPUT_PARAMETER_SIZE 114
#define ERROR_SIZE 6
//#define OTP_OUTPUT_PARAMETER_SIZE 39

typedef struct _CFA_DATA_T_
{
	int nWidth;
	int nHeight;
	int BlackLevel;
	int BayerPattern;
	int DataBits;
}CFA_DATA_T;

typedef struct _INPUT_SPW_PARAM_DATA_T_
{
	float agParam[INPUT_SPW_PARAMETER_SIZE];
	int Image_nWidth;
	int Image_nHeight;
	int Calibration_nWidth;
	int Calibration_nHeight;

	CFA_DATA_T ImageCFA;

	int nCount;
	_INPUT_SPW_PARAM_DATA_T_()
	{
		memset(this,0, sizeof(_INPUT_SPW_PARAM_DATA_T_));
		nCount = INPUT_SPW_PARAMETER_SIZE;
	}

}INPUT_SPW_PARAM_DATA_T,*PINPUTPARAM_DATA_PTR;

typedef struct _SPW_OTP_DATA_T_
{
#ifdef USE_OTP_HEADER
	int agHeader[PRODUCT_SPW_OTP_OUTPUT_HEADER_SIZE];
#endif 
	int agOTP[PRODUCT_SPW_OTP_OUTPUT_PARAMETER_SIZE];
	int nCount;
	_SPW_OTP_DATA_T_()
	{
		memset(this,0, sizeof(_SPW_OTP_DATA_T_));
		nCount = PRODUCT_SPW_OTP_OUTPUT_PARAMETER_SIZE;
	}

}SPW_OTP_DATA_T,*POTP_DATA_PTR;

SPWCAMERACALI_API int  SPW_Calibration_Verification(const unsigned char* pImage,INPUT_SPW_PARAM_DATA_T stInputParam, SPW_OTP_DATA_T *pOutOTP);
SPWCAMERACALI_API int  SPW_Calibration_VerificationRaw(const unsigned short* pImage, INPUT_SPW_PARAM_DATA_T stInputParam, SPW_OTP_DATA_T *pOutOTP, int vcm);
SPWCAMERACALI_API int  SPW_Calibration_VerificationYUV(const char* filename, INPUT_SPW_PARAM_DATA_T stInputParam,   SPW_OTP_DATA_T *pOutOTP, int vcm);
SPWCAMERACALI_API void SPW_load_parameter(char *filename,float *params_input, int size);
SPWCAMERACALI_API void SPW_ReadMe(void);
SPWCAMERACALI_API void SPW_GetCalibLibVersion(char* VersionInfo);
///*SPWCAMERACALI_API*/ void GetStereoInfo(char* StereoInfo);

#endif //_CAMERACALI_API_H_