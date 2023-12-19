package com.example.androidocr.whiteboard.bean;

import android.graphics.Bitmap;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * sketch data
 */
public class SketchData {
    public List<PhotoRecord> photoRecordList;
    public List<StrokeRecord> strokeRecordList;
    public List<StrokeRecord> strokeRedoList;
    public Bitmap thumbnailBM;//缩略图文件
    public Bitmap backgroundBM;

    public SketchData() {
        strokeRecordList = new ArrayList<>();
        photoRecordList = new ArrayList<>();
        strokeRedoList = new ArrayList<>();
        backgroundBM = null;
        thumbnailBM = null;
    }

}

