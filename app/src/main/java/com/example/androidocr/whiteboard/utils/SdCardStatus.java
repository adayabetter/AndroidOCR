package com.example.androidocr.whiteboard.utils;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.os.storage.StorageManager;
import android.text.TextUtils;
import android.util.Log;

/**
 * Created by gpy on 2015/10/20.
 */
public class SdCardStatus {
    private static String CACHE_FOLDER_NAME = "WMBWhiteBoard";
    private static String NONE_SD_CARD_PROMPT = "您的手机中sd卡不存在";

    public static void init(String cacheFolderName) {
        CACHE_FOLDER_NAME = cacheFolderName;
    }
    public static String getDefaulstCacheDirInSdCard(Context context) throws IllegalStateException {
//        String sdCardPath = null;
//        sdCardPath = getSDPath();
//        if (null == sdCardPath) {
//            throw new IllegalStateException(NONE_SD_CARD_PROMPT);
//        }
        if (context == null) {
            return "";
        }
        File cacheDir = context.getDir("images", Context.MODE_PRIVATE);
        if (cacheDir != null && cacheDir.exists()) {
            return cacheDir.getAbsolutePath();
        }
        return "";
    }

    /**
     * when not exist sd card,return null.
     *
     * @return
     */
    public static String getSDPath() {
        boolean sdCardExist = Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED);
        if (sdCardExist) {
            return Environment.getExternalStorageDirectory().getAbsolutePath();
        } else {
            return null;
        }
    }
    public static int getReflactField(String className,String fieldName){
        int result = -1;
        try {
            Class<?> clz = Class.forName(className);
            Field field = clz.getField(fieldName);
            field.setAccessible(true);
            result = field.getInt(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
    int title_id = getReflactField("com.android.internal.R$id", "title");
    int icon_id = getReflactField("com.android.internal.R$id", "icon");

//    public static StorageVolume[] getVolumeList(StorageManager storageManager){
//      try {
//          Class clz = StorageManager.class;
//          Method getVolumeList = clz.getMethod("getVolumeList", null);
//          StorageVolume[] result = (StorageVolume[]) getVolumeList.invoke(storageManager, null);
//          return result;
//      } catch (Exception e) {
//          e.printStackTrace();
//      }
//      return null;
//    }

    public static void shutDown(){
        try {
            Class<?> clz = Class.forName("android.os.ServiceManager");
            Log.e("wmb", "--shutDown 111");
            Method getService = clz.getMethod("getService", String.class);
            Log.e("wmb", "--shutDown 222");
            Object powerService = getService.invoke(null, Context.POWER_SERVICE);
            Log.e("wmb", "--shutDown 333");
            Class<?> cStub =  Class.forName("android.os.IPowerManager$Stub");
            Log.e("wmb", "--shutDown 444");
            Method asInterface = cStub.getMethod("asInterface", IBinder.class);
            Log.e("wmb", "--shutDown 555");
            Object IPowerManager = asInterface.invoke(null, powerService);
            Log.e("wmb", "--shutDown 666");
            Method shutDown = IPowerManager.getClass().getMethod("shutdown", boolean.class, boolean.class);
            Log.e("wmb", "--shutDown 777");
            shutDown.invoke(IPowerManager, false,true);
            Log.e("wmb", "--shutDown 888");
        } catch (Exception e) {
            Log.e("wmb", "--shutDown has an exception");
            e.printStackTrace();
        }
    }

    public static void reboot(){
        try {
            Class<?> clz = Class.forName("android.os.ServiceManager");
            Log.e("wmb", "--shutDown 111");
            Method getService = clz.getMethod("getService", String.class);
            Log.e("wmb", "--shutDown 222");
            Object powerService = getService.invoke(null, Context.POWER_SERVICE);
            Log.e("wmb", "--shutDown 333");
            Class<?> cStub =  Class.forName("android.os.IPowerManager$Stub");
            Log.e("wmb", "--shutDown 444");
            Method asInterface = cStub.getMethod("asInterface", IBinder.class);
            Log.e("wmb", "--shutDown 555");
            Object IPowerManager = asInterface.invoke(null, powerService);
            Log.e("wmb", "--shutDown 666");
            Method reboot = IPowerManager.getClass().getMethod("reboot", boolean.class,String.class, boolean.class);
            Log.e("wmb", "--shutDown 777");
            reboot.invoke(IPowerManager, false,"wmb test",true);
            Log.e("wmb", "--shutDown 888");
        } catch (Exception e) {
            Log.e("wmb", "--shutDown has an exception");
            e.printStackTrace();
        }
    }

    public static String getVolumeState(StorageManager storageManager, String path){
        String result = "";
        if(null == storageManager || TextUtils.isEmpty(path)){
            return result;
        }
        try {
            Class clz = StorageManager.class;
            Method getVolumeList = clz.getMethod("getVolumeState", String.class);
            result = (String) getVolumeList.invoke(storageManager, path);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}

