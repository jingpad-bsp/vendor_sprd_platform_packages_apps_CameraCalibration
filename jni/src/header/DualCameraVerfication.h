#ifndef _DUALCAMERAVERIFICATION_H_
#define _DUALCAMERAVERIFICATION_H_

#ifndef __linux__
#define EXPORT_INTERFACE
#else
#define EXPORT_INTERFACE __attribute__((visibility("default")))
#endif

#define IMAGE_RGB_FORMAT  1
#define IMAGE_NV21_FORMAT 2

#include <string.h>
#include <string>

using namespace std;

typedef struct tagDualCameraVerificationInputConfig
{
	//input arguments
	std::string filename_left;
	string filename_right;
	string filename_otp;
	//std::string arrange_dir;
	string otpMapVer;
	string moduleCombination;
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

//#ifdef __cplusplus
extern "C" {
//#endif

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

//New version
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

EXPORT_INTERFACE int DualCameraVerfication(const unsigned char* image_left, const unsigned char* image_right, CalibParam* param, DualCameraVerficationConfig* dualCameraVerficationConfig);

//rio.li, 171010
EXPORT_INTERFACE double GetDualcameraVerificationRMS(double rmsSrc);
//#ifdef __cplusplus

EXPORT_INTERFACE int DualCameraVerificationNV21(const unsigned char* image_left, const unsigned char* image_right, CalibParam* param, DualCameraVerificationConfig* dualCameraVerificationConfig);

}
//#endif

#endif
