#ifndef _WTCAMERAVERIFICATIONNV21_H_
#define _WTCAMERAVERIFICATIONNV21_H_
#include "WTCameraVerification_API.h"
#include "CalibParam.h"

#ifdef __cplusplus
extern "C"
{
#endif

typedef struct tagWTCameraVerificationInputConfig
{
	std::string filename_left;
	std::string filename_right;
	std::string filename_otp;
	std::string otpMapVer;
	std::string moduleCombination;
	int nx;
	int ny;

	int leftImgWidth;
	int leftImgHeight;
	int rightImgWidth;
	int rightImgHeight;

	int calibrationWidth;
	int calibrationHeight;
}wtCameraVerificationInputConfig;

//bool DetectCorners(const cv::Mat& GrayImage_left, const cv::Mat& GrayImage_right, int pattern_size_x, int pattern_size_y, std::vector<cv::Point2f>& corners_left, std::vector<cv::Point2f>& corners_right);
void GetWTCameraVerificationVersion(char* VersionInfo);

CAMERAVERIFY_EXPORTS int WTCameraVerificationNV21(const unsigned char* image_left, const unsigned char* image_right, CalibParam* param, WTCameraVerificationConfig* wtCameraVerificationConfig);
CAMERAVERIFY_EXPORTS int WTCameraVerificationYUV(const char* filename_left, const char* filename_right, const char* filename_otp, WTCameraVerificationConfig* wtCameraVerificationConfig);

#ifdef __cplusplus
}
#endif

#endif 