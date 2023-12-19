package com.example.androidocr.whiteboard.utils;

import android.app.Activity;
import android.view.View;

public class ViewUtils {

    public static <T extends View> T findViewById(Activity activity, int id){
        return (T) activity.findViewById(id);
    }

    public static <T extends View> T findViewById(View view, int id){
        return (T) view.findViewById(id);
    }

}

