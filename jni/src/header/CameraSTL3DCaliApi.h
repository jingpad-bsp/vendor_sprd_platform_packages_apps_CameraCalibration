#ifndef _CAMERACALI_API_H_
#define _CAMERACALI_API_H_

#ifdef __cplusplus
extern "C"
{
#endif
	
#ifndef NDK
#ifdef MONOCAMERAVERIFICATION_EXPORTS
#define CAMERACALI_API _declspec(dllexport)
#else
#define CAMERACALI_API _declspec(dllimport)
#endif
#else
#define CAMERACALI_API __attribute__((visibility("default")))
#endif
/*	
#ifdef DUALCAMERACALIBRATION_EXPORTS
#define CAMERACALI_API  extern "C" __declspec(dllexport)
#else
#define CAMERACALI_API  extern "C" __declspec(dllimport)
#endif
*/
#include<string.h>
/* Library Errors */
enum
{
	UNLOACKKEY_OK = 0,
	UNLOACKKEY_ERRPARAM,
	UNLOACKKEY_ERRGEN,
	
/* insert here new errors */
	UNLOACKKEY_ERRMAX
};
//#define INPUT_PARAMETER_SIZE  30
#define INPUT_PARAMETER_SIZE  32
#define PRODUCT_OTP_OUTPUT_PARAMETER_SIZE 113//72 = 57+9+6//63 .....//118-5=113
#define TOTAL_NUM_ERROR  27  
#define DEFAULT_OTP_DATA_HEAD_SIZE 16
#define DEFAULT_CALIBRATION_COUNT 20

typedef struct _CFA_DATA_T_
{
	int nWidth;
	int nHeight;
	int BlackLevel;
	int BayerPattern;
	int DataBits;
}CFA_DATA_T;

typedef struct _INPUT_PICTURE_T_
{
	int LeftImage_nWidth;
	int LeftImage_nHeight;
	int RightImage_nWidth;
	int RightImage_nHeight;
	int RgbImage_nWidth;
	int RgbImage_nHeight;
	int IrIrCalibration_nWidth;
	int IrIrCalibration_nHeight;
	int IrRgbCalibration_nWidth;
	int IrRgbCalibration_nHeight;
	
	CFA_DATA_T LeftImageCFA;
	CFA_DATA_T RightImageCFA;

	_INPUT_PICTURE_T_()
	{
		memset(this,0, sizeof(_INPUT_PICTURE_T_));
	}
}INPUT_PICTURE_T,*INPUT_PICTURE_PTR;

typedef struct _INPUT_PARAM_DATA_T_
{
	float agParam[INPUT_PARAMETER_SIZE];
	int LeftImage_nWidth;
	int LeftImage_nHeight;
	int RightImage_nWidth;
	int RightImage_nHeight;
	int RgbImage_nWidth;
	int RgbImage_nHeight;
	int IrIrCalibration_nWidth;
	int IrIrCalibration_nHeight;
	int IrRgbCalibration_nWidth;
	int IrRgbCalibration_nHeight;
	CFA_DATA_T LeftImageCFA;
	CFA_DATA_T RightImageCFA;

	int vcm;
	_INPUT_PARAM_DATA_T_()
	{
		memset(this,0, sizeof(_INPUT_PARAM_DATA_T_));
		vcm = INPUT_PARAMETER_SIZE;
	}

}INPUT_PARAM_DATA_T,*PINPUTPARAM_DATA_PTR;

typedef struct _OTP_DATA_T_
{
	int agOTP[PRODUCT_OTP_OUTPUT_PARAMETER_SIZE];
	int vcm;
	_OTP_DATA_T_()
	{
		memset(this,0, sizeof(_OTP_DATA_T_));
		vcm = PRODUCT_OTP_OUTPUT_PARAMETER_SIZE;
	}

}OTP_DATA_T,*POTP_DATA_PTR;

typedef struct _OTP_DATA_EXT_T_
{
    int otp_data_head[DEFAULT_OTP_DATA_HEAD_SIZE/4];
    OTP_DATA_T pOutOTP[DEFAULT_CALIBRATION_COUNT];
}OTP_DATA_EXT_T,*POTP_DATA_EXT_PTR;

CAMERACALI_API int  Calibration_Verification(const unsigned char* pLeftImage,const unsigned char* pRightImage,INPUT_PARAM_DATA_T stInputParam, OTP_DATA_T *pOutOTP);
CAMERACALI_API int  Calibration_VerificationRaw(const unsigned short* pLeftImage,const unsigned short* pRightImage,INPUT_PARAM_DATA_T stInputParam, OTP_DATA_T *pOutOTP);
CAMERACALI_API int  Calibration_VerificationSTL3D(const char* capture_left_path,const char* capture_right_path,const char* capture_yuv_path,const char* stInputParam_path,const int* cali_count_vcm,INPUT_PICTURE_T* pPicture,OTP_DATA_EXT_T *pOutOTPEXT,float* cx,float* cy);
CAMERACALI_API void load_parameter(char *filename,float *params_input, int size);
CAMERACALI_API void ReadMe(void);
CAMERACALI_API void GetCalibLibVersion(char* VersionInfo);
CAMERACALI_API void GetStereoInfo(char* StereoInfo);

#ifdef __cplusplus
}
#endif
#endif //_CAMERACALI_API_H_