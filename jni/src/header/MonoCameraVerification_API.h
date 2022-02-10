#ifndef _MONOCAMERAVERIFICATION_API_H_
#define _MONOCAMERAVERIFICATION_API_H_

#ifdef __cplusplus
extern "C"
{
#endif

#ifndef NDK
#ifdef MONOCAMERAVERIFICATION_EXPORTS
#define CAMERAVERIFY_EXPORTS _declspec(dllexport)
#else
#define CAMERAVERIFY_EXPORTS _declspec(dllimport)
#endif
#else
#define CAMERAVERIFY_EXPORTS __attribute__((visibility("default")))
#endif

#define IMAGE_RGB_FORMAT 1
#define IMAGE_NV21_FORMAT 2

#include <string.h>
#include "MonoCalibParam.h"



#define ERR_NO_ERROR                   (0)
#define ERR_RMS_TOO_LARGE              (-1)
#define ERR_NOT_ALL_CONERS_FOUND       (-2)
#define ERR_READ_YUV_FAIL              (-3)
#define ERR_MAP_VERSION_NOT_SUPPORT    (-4)
#define ERR_MODULE_COMBIN_NOT_SUPPORT  (-5)
#define ERR_OTP_BIN_SIZE_READ_FIAL     (-6)
#define ERR_MODULE_DIR_READ_FIAL       (-7) 
#define ERR_OTP_BIN_DATA_READ_FIAL     (-8)
#define ERR_OTP_BIN_CALI_NUM_READ_FIAL (-9)
#define ERR_OTP_BIN_SIZE_NOT_SUPPORT   (-10)
#define ERR_READ_LEFT_IMAGE_NAME       (-11)
#define ERR_FAILED_CALLOC_BUFFER       (-12)


typedef struct tagMonoCameraVerificationInputConfig
{
	//input arguments
	std::string filename_img;
	std::string filename_otp;
	//std::string arrange_dir;
	std::string otpMapVer;
	std::string module;
	int nx;
	int ny;
	int square_size;
	double rms_th;
	
	int ImgWidth;
	int ImgHeight;
	
	int calibrationWidth;
	int calibrationHeight;
}monoCameraVerificationInputConfig;

typedef struct _CFA_DATA_T_
{
	int nWidth;
	int nHeight;
	int BlackLevel;
	int BayerPattern;
	int DataBits;
}CFA_DATA_T;

typedef struct _INPUT_PARAM_DATA_VERIFICATION_T_
{
	int Image_nWidth;
	int Image_nHeight;

	CFA_DATA_T ImageCFA;

}INPUT_PARAM_DATA_VERIFICATION_T,*PINPUTPARAM_DATA_VERIFICATION_PTR;

typedef struct tagMonoCameraVerificationConfig {
	int pattern_size_row;
	int pattern_size_col;
	int square_size;
	double rms_th;



	int  Width;
	int  Height;


	int image_format;
	
	int width_calibration;
	int height_calibration;

	double rms;
	
	const char* otpMapVer;
	const char* module;

	INPUT_PARAM_DATA_VERIFICATION_T stInputPara;
}MonoCameraVerificationConfig;

CAMERAVERIFY_EXPORTS int MonoCameraVerificationRaw(const unsigned short* pImage,  MonoCalibParam* param, MonoCameraVerificationConfig* monoCameraVerificationConfig);
CAMERAVERIFY_EXPORTS void GetMonoCameraVerificationVersion(char* VersionInfo);
CAMERAVERIFY_EXPORTS int MonoCameraVerificationNV21_New(const char* image_file, const char* otp_file,  MonoCameraVerificationConfig* monoCameraVerificationConfig);

#ifdef __cplusplus
}
#endif
#endif