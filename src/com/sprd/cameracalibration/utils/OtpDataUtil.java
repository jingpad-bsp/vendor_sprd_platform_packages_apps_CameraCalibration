package com.sprd.cameracalibration.utils;

import android.util.Log;

/**
 * Created by SPREADTRUM\emily.miao on 19-11-1.
 */

public class OtpDataUtil {

    private static final String TAG = "OtpDataUtil";


    public static final int BUFFER_SIZE = 16 * 1024;
    public static final int OTP_CONTROL_SIZE = 30;
    public static final int OTP_DATA_LEN_SIZE = 2;
    public static final int HEAD_DATA_SIZE = 16;

    public static final int CONTROL_INFO = 20;


    public static final int OTP_DATA_SIZE = BUFFER_SIZE - OTP_CONTROL_SIZE - OTP_DATA_LEN_SIZE - HEAD_DATA_SIZE;

    public static final String OTPD_WRITE_DATA = "Otp Write Data";
    public static final String OTPD_READ_DATA = "Otp Read Data";
    public static final String OTPD_READ_RSP = "Otp Read Rsp";
    public static final String OTPD_MSG_OK = "Otp Data Ok";
    public static final String OTPD_MSG_FAILED = "Otp Data Failed";

    public static final String OTPD_INIT_GOLDEN_DATA = "Otp Init Golden Data";

    public static final int HEAD_DATA_INDEX = OTP_CONTROL_SIZE + OTP_DATA_LEN_SIZE;
    public static final int OTP_DATA_INDEX = OTP_CONTROL_SIZE + OTP_DATA_LEN_SIZE + HEAD_DATA_SIZE;

    public static final int OTP_DATA_TYPE_INDEX = CONTROL_INFO;

    private byte[] mOtpdBuffer = null;

    private byte[] mHeadData = null;

    public OtpDataUtil() {
        mOtpdBuffer = new byte[BUFFER_SIZE];
        mHeadData = new byte[HEAD_DATA_SIZE];
    }

    public void writeInt(byte[] dataArray, int pos, int inpuInt) {
        Log.d(TAG, "writeInt inpuInt=" + inpuInt);
        dataArray[pos + 3] = (byte) (inpuInt >> 24 & 0xff);
        dataArray[pos + 2] = (byte) (inpuInt >> 16 & 0xff);
        dataArray[pos + 1] = (byte) (inpuInt >> 8 & 0xff);
        dataArray[pos] = (byte) (inpuInt & 0xff);
    }

    public int readInt(byte[] dataArray, int pos) {
        int b0 = dataArray[pos] & 0xFF;
        int b1 = dataArray[pos + 1] & 0xFF;
        int b2 = dataArray[pos + 2] & 0xFF;
        int b3 = dataArray[pos + 3] & 0xFF;
        return b0 | (b1 << 8) | (b2 << 16) | (b3 << 24);
    }

    public byte[] parseOtpData(int[] dataArray) {
        byte[] temp = new byte[OTP_DATA_SIZE];
        int dataPos = 0;
        Log.d(TAG, "parseOtpData dataArray = " + dataArray.length);
        int pos = 0;
        for (int data : dataArray) {
            Log.d(TAG, "parseOtpData data = " + data);
            writeInt(temp, pos + 4, data);
        }
        Log.d(TAG, "parseOtpData dataPos = " + dataPos);
        return temp;
    }

    public byte[] intToBytes(int value) {
        byte[] src = new byte[4];
        src[3] = (byte) ((value >> 24) & 0xFF);
        src[2] = (byte) ((value >> 16) & 0xFF);
        src[1] = (byte) ((value >> 8) & 0xFF);
        src[0] = (byte) (value & 0xFF);
        return src;
    }

    public static int bytesToInt(byte[] src, int offset) {
        int value;
        value = (int) ((src[offset] & 0xFF) | ((src[offset + 1] & 0xFF) << 8)
                | ((src[offset + 2] & 0xFF) << 16) | ((src[offset + 3] & 0xFF) << 24));
        return value;
    }

    public void setSpwHeadData(int[] headData) {
        //Write otp data header
        int pos = HEAD_DATA_INDEX;
        Log.d(TAG, "parseOtpData header length = " + headData.length);
        for (int data : headData) {
            Log.d(TAG, "parseOtpData header = " + data + ",pos=" + pos);
            writeInt(mOtpdBuffer, pos, data);
            pos += 4;
        }
    }

    public void setHeadData(int otpVersion,int modulueLocation,int mcaptureCount) {
        Log.d(TAG, "setHeadData otpVersion="+otpVersion+",modulueLocation="+modulueLocation+",mcaptureCount="+mcaptureCount);
        try {
            //1.set otp version
            writeInt(mHeadData, 0, otpVersion);
            for(int i = 0;i< 4;i++){
                String hexString = Integer.toHexString(mHeadData[i]);
                Log.d(TAG, "setHeadData hexString="+hexString +",i="+i);
            }
            //2.set direction
            byte direction = (byte) (modulueLocation & 0xFF);
            mHeadData[4] = direction;
            String hexString = Integer.toHexString(mHeadData[4]);
            Log.d(TAG, "setHeadData direction hexString="+hexString);
            //3.set capture count
            byte captureCount = (byte) (mcaptureCount & 0xFF);
            mHeadData[5] = captureCount;
            String hexString2 = Integer.toHexString(mHeadData[5]);
            Log.d(TAG, "setHeadData captureCount hexString2="+hexString2);
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public String setSpwOtpData(int[] dataArray){
        String result = "";
        // Write socket control buff
        writeOtpByteArray(OTPD_WRITE_DATA.getBytes(), 0, 0);
        mOtpdBuffer[OTP_DATA_TYPE_INDEX] = (byte)0x03;

        //Write otp data buff
        int pos = OTP_DATA_INDEX;
        int dataLen = HEAD_DATA_SIZE;
        Log.d(TAG, "parseOtpData data length = " + dataArray.length);
        for (int data : dataArray) {
            Log.d(TAG, "parseOtpData data = " + data + ",pos=" + pos);
            writeInt(mOtpdBuffer, pos, data);
            pos += 4;
            dataLen += 4;
        }

        //write otp data length
        byte[] otp_data_len = new byte[2];
        otp_data_len[0] = (byte) (dataLen & 0xFF);
        otp_data_len[1] = (byte) ((dataLen >> 8) & 0xFF);
        Log.d(TAG, "\n writeOtpData dataLen=" + dataLen);
        writeOtpByteArray(otp_data_len, 0, OTP_CONTROL_SIZE);

        Log.d(TAG, "writeOtpData result=" + result);
        return result;
    }


    public String setOtpData(int[] dataArray) {
        String result = "";
        // 1.Write control buff
        writeOtpByteArray(OTPD_WRITE_DATA.getBytes(), 0, 0);
        mOtpdBuffer[OTP_DATA_TYPE_INDEX] = (byte)0x01;
        // 2.Write head data buff
        writeOtpByteArray(mHeadData, 0, HEAD_DATA_INDEX);
        // 3.Write otp data buff
        // writeOtpByteArray(data, 0, OTP_CONTROL_SIZE);
        int pos = OTP_DATA_INDEX;
        int dataLen = 0;
        Log.d(TAG, "parseOtpData data length = " + dataArray.length);
        for (int data : dataArray) {
            Log.d(TAG, "parseOtpData data = " + data + ",pos=" + pos);
            writeInt(mOtpdBuffer, pos, data);
            pos += 4;
            dataLen += 4;
        }
        dataLen += HEAD_DATA_SIZE;

        //4.write otp data length
        byte[] otp_data_len = new byte[2];
        otp_data_len[0] = (byte) (dataLen & 0xFF);
        otp_data_len[1] = (byte) ((dataLen >> 8) & 0xFF);
        Log.d(TAG, "\n writeOtpData dataLen=" + dataLen);
        writeOtpByteArray(otp_data_len, 0, OTP_CONTROL_SIZE);

        Log.d(TAG, "writeOtpData result=" + result);
        return result;
    }

    public String setOtpDataFull(int[] dataArrayFull) {
        String result = "";
        // 1.Write socket control buff
        writeOtpByteArray(OTPD_WRITE_DATA.getBytes(), 0, 0);
        //write type
        mOtpdBuffer[OTP_DATA_TYPE_INDEX] = (byte)0x01;
        // 3.Write otp data buff
        int pos = HEAD_DATA_INDEX;
        int dataLen = 0;
        Log.d(TAG, "parseOtpData data length = " + dataArrayFull.length);
        for (int data : dataArrayFull) {
            Log.d(TAG, "parseOtpData data = " + data + ",pos=" + pos);
            writeInt(mOtpdBuffer, pos, data);
            pos += 4;
            dataLen += 4;
        }
        //4.write otp data length
        byte[] otp_data_len = new byte[2];
        otp_data_len[0] = (byte) (dataLen & 0xFF);
        otp_data_len[1] = (byte) ((dataLen >> 8) & 0xFF);
        Log.d(TAG, "\n writeOtpData dataLen=" + dataLen);
        writeOtpByteArray(otp_data_len, 0, OTP_CONTROL_SIZE);

        Log.d(TAG, "writeOtpDataFull result=" + result);
        return result;
    }

    public String writeWTOtpAndHeaderData(int[] otpData,int[] headData,int otpFlag){
        String result = "";
        //send header data
        int headpos = HEAD_DATA_INDEX;
        Log.d(TAG, "parseHeadData header length = " + headData.length);
        for (int data : headData) {
            Log.d(TAG, "parseHeadData header = " + data + ",pos=" + headpos);
            writeInt(mOtpdBuffer, headpos, data);
            headpos += 4;
        }

        // send otp data
        // Write socket control buff
        writeOtpByteArray(OTPD_WRITE_DATA.getBytes(), 0, 0);
        mOtpdBuffer[OTP_DATA_TYPE_INDEX] = (byte)otpFlag;

        //Write otp data buff
        int otppos = OTP_DATA_INDEX;
        int dataLen = HEAD_DATA_SIZE;
        Log.d(TAG, "parseOtpData data length = " + otpData.length);
        for (int data : otpData) {
            Log.d(TAG, "parseOtpData data = " + data + ",pos=" + otppos);
            writeInt(mOtpdBuffer, otppos, data);
            otppos += 4;
            dataLen += 4;
        }

        //write otp data length
        byte[] otp_data_len = new byte[2];
        otp_data_len[0] = (byte) (dataLen & 0xFF);
        otp_data_len[1] = (byte) ((dataLen >> 8) & 0xFF);
        Log.d(TAG, "\n writeOtpData dataLen=" + dataLen);
        writeOtpByteArray(otp_data_len, 0, OTP_CONTROL_SIZE);

        Log.d(TAG, "writeOtpData result=" + result);
        return result;
    }


    public void writeSTL3DOtpAndHeaderData(int[] otpData,int[] headData){

        //send header data
        int headpos = HEAD_DATA_INDEX;
        Log.d(TAG, "parseHeadData header length = " + headData.length);
        for (int data : headData) {
            Log.d(TAG, "parseHeadData header = " + data + ",pos=" + headpos);
            writeInt(mOtpdBuffer, headpos, data);
            headpos += 4;
        }

        // send otp data
        // Write socket control buff
        writeOtpByteArray(OTPD_WRITE_DATA.getBytes(), 0, 0);
        mOtpdBuffer[OTP_DATA_TYPE_INDEX] = (byte)0x04;

        //Write otp data buff
        int otppos = OTP_DATA_INDEX;
        int dataLen = HEAD_DATA_SIZE;
        Log.d(TAG, "parseOtpData data length = " + otpData.length);
        for (int data : otpData) {
            Log.d(TAG, "parseOtpData data = " + data + ",pos=" + otppos);
            writeInt(mOtpdBuffer, otppos, data);
            otppos += 4;
            dataLen += 4;
        }

        //write otp data length
        byte[] otp_data_len = new byte[2];
        otp_data_len[0] = (byte) (dataLen & 0xFF);
        otp_data_len[1] = (byte) ((dataLen >> 8) & 0xFF);
        Log.d(TAG, "\n writeOtpData dataLen=" + dataLen);
        writeOtpByteArray(otp_data_len, 0, OTP_CONTROL_SIZE);
    }

    public void writeOtpByteArray(byte[] src, int srcPos, int dstPos) {
        System.arraycopy(src, srcPos, mOtpdBuffer, dstPos, src.length);
    }

    public byte[] getFinalOtpDataByteArray(){
        return mOtpdBuffer;
    }

    public byte[] getGoldenOtpByteArray() {
        byte[] otpSocketBuff = new byte[OTPD_INIT_GOLDEN_DATA.getBytes().length];
        System.arraycopy(OTPD_INIT_GOLDEN_DATA.getBytes(), 0, otpSocketBuff, 0, OTPD_INIT_GOLDEN_DATA.getBytes().length);
        return otpSocketBuff;
    }
}
