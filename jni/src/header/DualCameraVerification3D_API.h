#ifndef _DUALCAMERAVERIFICATION3D_API_H_
#define _DUALCAMERAVERIFICATION3D_API_H_

#ifdef __cplusplus
extern "C"
{
#endif

#ifndef NDK
#ifdef DUALCAMERAVERIFICATION_EXPORTS
#define CAMERAVERIFY_EXPORTS _declspec(dllexport)
#else
#define CAMERAVERIFY_EXPORTS _declspec(dllexport)
#endif
#else
#define CAMERAVERIFY_EXPORTS __attribute__((visibility("default")))
#endif

#define IMAGE_RGB_FORMAT 1
#define IMAGE_NV21_FORMAT 2

#include "CalibParam3d.h"

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
	int LeftImage_nWidth;
	int LeftImage_nHeight;
	int RightImage_nWidth;
	int RightImage_nHeight;
	int YuvImage_nWidth;
	int YuvImage_nHeight;

	CFA_DATA_T LeftImageCFA;
	CFA_DATA_T RightImageCFA;
	CFA_DATA_T YuvImageCFA;

}INPUT_PARAM_DATA_VERIFICATION_T,*PINPUTPARAM_DATA_VERIFICATION_PTR;

typedef struct tagDualCameraVerificationConfig {
	int pattern_size_row;
	int pattern_size_col;
	double rms_th;
	double disparity_th;
	
	int width;
	int height;
	//add left and right size, to deal with separately
	int leftWidth;
	int leftHeight;
	int rightWidth;
	int rightHeight;
	int yuvWidth;
	int yuvHeight;

	int image_format;
	
	int width_calibration;
	int height_calibration;

	double rms;
	double disparity;
	double rms_y;
	double rms_x;

	double left_disparity;
	double right_disparity;
		
	bool isverticalstereo;

	INPUT_PARAM_DATA_VERIFICATION_T stInputPara;
}DualCameraVerificationConfig;

CAMERAVERIFY_EXPORTS int DualCameraVerificationRaw(const unsigned short* pLeftImage, const unsigned short* pRightImage, CalibParam* param, DualCameraVerificationConfig* dualCameraVerificationConfig);
CAMERAVERIFY_EXPORTS int DualCameraVerificationRawExt(const char* capture_left_path,const char* capture_right_path,const char* capture_yuv_path,const char* otp_path, DualCameraVerificationConfig* dualCameraVerificationConfig);
CAMERAVERIFY_EXPORTS void GetDualCameraVerificationVersion(char* VersionInfo);

#ifdef __cplusplus
}
#endif
#endif