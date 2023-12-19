package com.example.androidocr.whiteboard.bean;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.text.TextPaint;
/**
 * photo record
 * @author WMB
 *
 */
public class PhotoRecord {

    public Bitmap bitmap;//图形
    public Matrix matrix;//图形
    public RectF photoRectSrc = new RectF();
    public float scaleMax = 3;

}
