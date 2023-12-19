package com.example.androidocr.whiteboard.utils;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

public class CommonUtils {

    public static Context mContext;
    public static final String TAG_WHITEBOARD = "--whiteboard";

    public static void init(Context context){
        mContext = context;
    }
    public static void showToast(String text, int time){
        if(null != mContext){
            Toast.makeText(mContext, text, time).show();
        }
        else{
            Log.e(TAG_WHITEBOARD, "--showToast--mcontext is null");
        }

    }
    public static void destory() {
        mContext = null;
    }

}
