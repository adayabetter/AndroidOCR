package com.example.androidocr;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.os.Bundle;

import com.example.androidocr.whiteboard.WhiteBoardFragment;

public class CanvasDemoActivity extends Activity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        FragmentTransaction ts = getFragmentManager().beginTransaction();
        ts.add(R.id.fl_main, new WhiteBoardFragment(), "wb").commitAllowingStateLoss();
    }
}
