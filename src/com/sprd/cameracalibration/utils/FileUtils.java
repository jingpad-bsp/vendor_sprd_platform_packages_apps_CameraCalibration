package com.sprd.cameracalibration.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import android.util.Log;

public class FileUtils {

    private static final String TAG = "FileUtils";

    public static boolean copyFile(String srcPath, String destDir) {
        boolean flag = false;

        File srcFile = new File(srcPath);
        if (!srcFile.exists()) {
            Log.d(TAG, "src file not exists ");
            return false;
        }
        String fileName = srcPath
                .substring(srcPath.lastIndexOf(File.separator));
        String destPath = destDir + fileName;
        if (destPath.equals(srcPath)) {
            Log.d(TAG, "src file and dest Dir in the same Dir");
            return false;
        }
        File destFile = new File(destPath);
        File destFileDir = new File(destDir);
        destFileDir.mkdirs();
        try (FileInputStream fis = new FileInputStream(srcPath);
             FileOutputStream fos = new FileOutputStream(destFile)){
            byte[] buf = new byte[1024];
            int c;
            while ((c = fis.read(buf)) != -1) {
                fos.write(buf, 0, c);
            }
            //fis.close();
            //fos.close();

            flag = true;
        } catch (IOException e) {
            Log.d(TAG, e.toString());
            e.printStackTrace();
            return false;
        }

        if (flag) {
            Log.d(TAG, "copy success");
        }

        return flag;
    }

    public static boolean deleteFile(File file) {
        return file.delete();
    }

    public static int getIntFromFile(String filename) {
        File file = new File(filename);
        InputStream fIn = null;
        try {
            fIn = new FileInputStream(file);
            InputStreamReader isr = new InputStreamReader(fIn,
                    Charset.defaultCharset());
            char[] inputBuffer = new char[1024];
            int q = -1;

            q = isr.read(inputBuffer);
            isr.close();
            fIn.close();

            if (q > 0)
                return Integer.parseInt(String.valueOf(inputBuffer, 0, q)
                        .trim());
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
        return -1;
    }

    public static void writeFile(String filename, String content) {
        try (FileOutputStream fos = new FileOutputStream(filename)){
            byte[] bytes = content.getBytes();
            fos.write(bytes);
            fos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void deleteDir(final String pPath) {
        File dir = new File(pPath);
        deleteDirWihtFile(dir);
    }

    public static void deleteDirWihtFile(File dir) {
        if (dir == null || !dir.exists() || !dir.isDirectory())
            return;
        for (File file : dir.listFiles()) {
            if (file.isFile())
                file.delete();
            else if (file.isDirectory())
                deleteDirWihtFile(file);
        }
        dir.delete();
    }

    public static boolean fileIsExists(String path) {
        try {
            File file = new File(path);
            Log.d(TAG, "fileIsExists path=" + path);
            if (!file.exists()) {
                Log.d(TAG, path + " fileIsExists false");
                return false;
            }
        } catch (NullPointerException e) {
            Log.d(TAG, path + " fileIsExists Exception e = " + e);
            return false;
        }
        Log.d(TAG, path + " fileIsExists true");
        return true;
    }

    public static synchronized String readFile(String path) {
        File file = new File(path);
        StringBuffer sBuffer = new StringBuffer();
        try (InputStream fIn = new FileInputStream(file);BufferedReader bReader = new BufferedReader(new InputStreamReader(fIn))){
            String str = bReader.readLine();

            while (str != null) {
                sBuffer.append(str + "\n");
                str = bReader.readLine();
            }
            return sBuffer.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
