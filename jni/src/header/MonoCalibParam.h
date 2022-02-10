#ifndef _MONO_CALIBRATION_PARAM_H
#define _MONO_CALIBRATION_PARAM_H 

typedef struct 
{
	//start from 0
	double cx;
	double cy;
	double fx;
	double fy;
	double k1, k2, k3;
	double p1, p2;
} MonoCalibParam;


#endif 