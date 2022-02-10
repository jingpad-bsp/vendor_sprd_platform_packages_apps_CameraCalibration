#ifndef _WTCAMERAVERIFICATION_API_H_
#define _WTCAMERAVERIFICATION_API_H_

#ifdef __cplusplus
extern "C"
{
#endif

#ifndef NDK
#ifdef WTCAMERAVERIFICATION_EXPORTS
#define CAMERAVERIFY_EXPORTS _declspec(dllexport)
#else
#define CAMERAVERIFY_EXPORTS _declspec(dllimport)
#endif
#else
#define CAMERAVERIFY_EXPORTS __attribute__((visibility("default")))
#endif

#define IMAGE_RGB_FORMAT 1
#define IMAGE_NV21_FORMAT 2


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

	CFA_DATA_T LeftImageCFA;
	CFA_DATA_T RightImageCFA;

}INPUT_PARAM_DATA_VERIFICATION_T,*PINPUTPARAM_DATA_VERIFICATION_PTR;

typedef struct tagWTCameraVerificationConfig {
	int pattern_size_row;
	int pattern_size_col;
	
	double shiftx_th;
	double shifty_th;


	int  width;
	int  height;


	int leftWidth;
	int leftHeight;
	int rightWidth;
	int rightHeight;

	int image_format;
	
	int width_calibration;
	int height_calibration;

	double shiftx;
	double shifty;

	INPUT_PARAM_DATA_VERIFICATION_T stInputPara;
}WTCameraVerificationConfig;

CAMERAVERIFY_EXPORTS int WTCameraVerificationRaw(const unsigned short* pLeftImage, const unsigned short* pRightImage,  const char* otp_path, WTCameraVerificationConfig* wtCameraVerificationConfig);
CAMERAVERIFY_EXPORTS void GetWTCameraVerificationVersion(char* VersionInfo);

#ifdef __cplusplus
}
#endif
#endif