#ifndef _CALIBRATION_PARAM_H
#define _CALIBRATION_PARAM_H

#ifdef __cplusplus
extern "C" {
#endif


typedef struct
{
	//Start from 0
	double cx_left;								//optical center in x direction of left camera
	double cy_left;								//optical center in y direction of left camera
	double fx_left;								//focal length in x direction of left camera
	double fy_left;								//focal length in y direction of left camera
	double k1_left, k2_left, k3_left;			//coefficients of distortion model of left camera
	double R_left[3][3];						//R matrix of left camera
	double P_left[3][3];						//P matrix of left camera

	//Start from 25
	double cx_right;							//optical center in x direction of right camera
	double cy_right;							//optical center in y direction of right camera
	double fx_right;							//focal length in x direction of right camera
	double fy_right;							//focal length in y direction of right camera
	double k1_right, k2_right, k3_right;		//coefficients of distortion model of right camera
	double R_right[3][3];						//R matrix of right camera
	double P_right[3][3];						//P matrix of right camera

	//Start from 50
	int disparity_min;						//minimum of disparity 
	int disparity_max;						//maximum of disparity

	double d_52;
	double d_53;
	double d_54;
	double d_55;
	double d_56;
	double d_57;
} CalibParam;

#ifdef __cplusplus
}
#endif

#endif