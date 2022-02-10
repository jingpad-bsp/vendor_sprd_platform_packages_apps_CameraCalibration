#ifndef _DUALCAMERAVERIFICATION_API_H_
#define _DUALCAMERAVERIFICATION_API_H_
#include <string>
#include <string.h>
#ifdef __cplusplus
extern "C"
{
#endif

#ifndef __linux__
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

using namespace std;

typedef struct tagDualCameraVerificationInputConfig
{
	//input arguments
	std::string filename_left;
	std::string filename_right;
	std::string filename_otp;
	//std::string arrange_dir;
	std::string otpMapVer;
	std::string moduleCombination;
	int nx;
	int ny;
	double rms_th;

	int leftImgWidth;
	int leftImgHeight;
	int rightImgWidth;
	int rightImgHeight;

	int calibrationWidth;
	int calibrationHeight;
}dualCameraVerificationInputConfig;

typedef struct tagDualCameraVerficationConfig {
	int pattern_size_row;  //number of corners in row
	int pattern_size_col;  //number of corners in column
	double rms_th;         //threshold of rms. used to determine whether verification passes or not

	int width;			//widht of input image
	int height;			//height of input image
	int image_format;	//image format, IMAGE_RGB_FORMAT or IMAGE_NV21_FORMAT
	int width_calibration;			//width_calibration of input image to resize
	int height_calibration;			//height_calibration of input image to resize
	//rio.li, 171010
	double rms; //calculated reprojection rms
} DualCameraVerficationConfig;



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

	int image_format;
	
	int width_calibration;
	int height_calibration;

	double rms;
	double disparity;

	double left_disparity;
	double right_disparity;
		
	bool isverticalstereo;

	std::string otpMapVer;
	std::string moduleCombination;

	INPUT_PARAM_DATA_VERIFICATION_T stInputPara;
}DualCameraVerificationConfig;

//CAMERAVERIFY_EXPORTS int DualCameraVerificationRaw(const unsigned short* pLeftImage, const unsigned short* pRightImage, CalibParam* param, DualCameraVerificationConfig* dualCameraVerificationConfig);
CAMERAVERIFY_EXPORTS int DualCameraVerificationNV21_New(const std::string image_left, const std::string image_right, const std::string otp_file, DualCameraVerificationConfig* dualCameraVerificationConfig);
CAMERAVERIFY_EXPORTS void GetDualCameraVerificationVersion(char* VersionInfo);

CAMERAVERIFY_EXPORTS int DualCameraVerfication(const unsigned char* image_left, const unsigned char* image_right, CalibParam* param, DualCameraVerficationConfig* dualCameraVerficationConfig);

//rio.li, 171010
CAMERAVERIFY_EXPORTS double GetDualcameraVerificationRMS(double rmsSrc);
//#ifdef __cplusplus

CAMERAVERIFY_EXPORTS int DualCameraVerificationNV21(const unsigned char* image_left, const unsigned char* image_right, CalibParam* param, DualCameraVerificationConfig* dualCameraVerificationConfig);
#ifdef __cplusplus
}
#endif
#endif
