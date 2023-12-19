package com.example.androidocr;


import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

public class FileManager {
    String TAG = "FILE";
    Context context = null;

    public FileManager(Context context)
    {
        this.context = context;
    }

    private File getFilePtr(String outName, String subFolder) throws IOException {
        //找到目录
        File filesDir = context.getFilesDir();
        if (!filesDir.exists()) {
            filesDir.mkdirs();
        }
        //创建专属目录
        File outFileFolder = new File(filesDir.getAbsolutePath()+"/target/"+subFolder);
        if(!outFileFolder.exists()) {
            outFileFolder.mkdirs();
        }
        //创建输出文件
        File outFile=new File(outFileFolder,outName);
        String outFilename = outFile.getAbsolutePath();
        Log.i(TAG, "outFile is " + outFilename);
        if (!outFile.exists()) {
            boolean res = outFile.createNewFile();
            if (!res) {
                Log.e(TAG, "outFile not exist!(" + outFilename + ")");
                return null;
            }
        }
        return outFile;
    }
    private int copyData(File outFile, InputStream is){
        try {
            FileOutputStream fos = new FileOutputStream(outFile);
            //分段读取文件，并写出到输出文件，完成拷贝操作。
            byte[] buffer = new byte[1024];
            int byteCount;
            while ((byteCount = is.read(buffer)) != -1) {
                fos.write(buffer, 0, byteCount);
            }
            fos.flush();
            is.close();
            fos.close();
            return 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public String getFilePathAfterCopy(Uri uri, String outName, String subFolder, boolean ifReturnParent){
        try {
            File outFile=getFilePtr(outName,subFolder);
            //创建输入文件流
            InputStream is= context.getContentResolver().openInputStream(uri);
            if(0!=copyData(outFile,is)) {
                return null;
            }
            //返回路径
            if(ifReturnParent) {
                return  outFile.getParent();
            } else {
                return outFile.getPath();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    public String getFilePathAfterCopy(int resId,String outName,String subFolder,boolean ifReturnParent) {
        try {
            //找到目录
            File outFile=getFilePtr(outName,subFolder);
            //创建输入文件流
            InputStream is = context.getResources().openRawResource(resId);
            if(0!=copyData(outFile,is)) {
                return null;
            }
            //返回路径
            if(ifReturnParent) {
                return  outFile.getParent();
            } else {
                return outFile.getPath();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    public String byteToString(byte[] data) {
        int index = data.length;
        for (int i = 0; i < data.length; i++) {
            if (data[i] == 0) {
                index = i;
                break;
            }
        }
        byte[] temp = new byte[index];
        Arrays.fill(temp, (byte) 0);
        System.arraycopy(data, 0, temp, 0, index);
        String str;
        try {
            str = new String(temp, "ISO-8859-1");//ISO-8859-1//GBK
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return "";
        }
        return str;
    }

}