#ifndef _CAMERACALI_API_H_
#define _CAMERACALI_API_H_

#include <string>

#ifdef __cplusplus
extern "C"
{
#endif

#ifndef NDK
#ifdef DUALCAMERACALIBRATION_EXPORTS
#define CAMERACALI_API  __declspec(dllexport)
#else
#define CAMERACALI_API  __declspec(dllimport)
#endif
#else
#define CAMERACALI_API __attribute__((visibility("default")))
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


#define INPUT_PARAMETER_SIZE  31
#define PRODUCT_OTP_OUTPUT_PARAMETER_SIZE 63
#define TOTAL_NUM_ERROR  25
#define DEFAULT_SALE_AFT_CALIBRATION
#define DEFAULT_OTP_DATA_HEAD 16
#define DEFAULT_CAPTURE_COUNT 3

typedef struct _INPUT_PICTURE_T_
{
    int LeftImage_nWidth;
    int LeftImage_nHeight;
    int RightImage_nWidth;
    int RightImage_nHeight;
}INPUT_PICTURE_T,*INPUT_PICTURE_PTR;

typedef struct _CFA_DATA_T_
{
    int nWidth;
    int nHeight;
    int BlackLevel;
    int BayerPattern;
    int DataBits;
}CFA_DATA_T;

typedef struct _INPUT_PARAM_DATA_T_
{
    float agParam[INPUT_PARAMETER_SIZE];
    int LeftImage_nWidth;
    int LeftImage_nHeight;
    int RightImage_nWidth;
    int RightImage_nHeight;
    int Calibration_nWidth;
    int Calibration_nHeight;

    CFA_DATA_T LeftImageCFA;
    CFA_DATA_T RightImageCFA;

    int nCount;
    _INPUT_PARAM_DATA_T_()
    {
        memset(this,0, sizeof(_INPUT_PARAM_DATA_T_));
        nCount = INPUT_PARAMETER_SIZE;
    }

}INPUT_PARAM_DATA_T,*PINPUTPARAM_DATA_PTR;

typedef struct _OTP_DATA_T_
{
    int agOTP[PRODUCT_OTP_OUTPUT_PARAMETER_SIZE];
    int nCount;
    _OTP_DATA_T_()
    {
        memset(this,0, sizeof(_OTP_DATA_T_));
        nCount = PRODUCT_OTP_OUTPUT_PARAMETER_SIZE;
    }

}OTP_DATA_T,*POTP_DATA_PTR;

typedef struct _OTP_DATA_EXT_T_
{
    int otp_data_head[4];
    OTP_DATA_T pOutOTP[20];
    _OTP_DATA_EXT_T_()
    {
        memset(this,0, sizeof(_OTP_DATA_EXT_T_));
    }
}OTP_DATA_EXT_T,*POTP_DATA_EXT_PTR;

CAMERACALI_API int  Calibration_Verification(const unsigned char* pLeftImage,const unsigned char* pRightImage,INPUT_PARAM_DATA_T stInputParam, OTP_DATA_T *pOutOTP);
CAMERACALI_API int  Calibration_VerificationRaw(const unsigned short* pLeftImage,const unsigned short* pRightImage,INPUT_PARAM_DATA_T stInputParam, OTP_DATA_T *pOutOTP);
CAMERACALI_API int  Calibration_VerificationYUV(const char* filename_left,const char* filename_right, INPUT_PARAM_DATA_T stInputParam,  OTP_DATA_T *pOutOTP);

CAMERACALI_API int  Calibration_SalesAfterYUV(const char* capture_path,const char* otpdata_path, const char* sale_aft_cali_path, const char* stInputParam_path,INPUT_PICTURE_T* pPicture,OTP_DATA_EXT_T *pOutOTPEXT);
CAMERACALI_API int  Calibration_SalesAfterV2YUV(const char* capture_path,const char* stInputParam_path, INPUT_PICTURE_T* pPicture,OTP_DATA_EXT_T *pOutOTPEXT);

CAMERACALI_API void load_parameter(const char *filename,float *params_input, int size);
CAMERACALI_API void ReadMe(void);
CAMERACALI_API void GetCalibLibVersion(char* VersionInfo);
CAMERACALI_API void GetStereoInfo(char* StereoInfo);
CAMERACALI_API char GetModuleLocationInfo(void);
CAMERACALI_API void GetOtpVersion(int* otpVersion);


#ifdef __cplusplus
}
#endif

#endif //_CAMERACALI_API_H_
