package com.example.androidocr.whiteboard;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class DrawView extends View {

    float currentX, currentY;
    Paint p = new Paint();
    Bitmap bp;

    public DrawView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public DrawView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DrawView(Context context) {
        super(context);
        bp = Bitmap.createBitmap(2000, 1000, Config.ARGB_4444);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Log.e("wmb", "--onDraw--canvas:"+canvas.toString());
        p.setColor(Color.RED);
        canvas.drawCircle(currentX, currentY, 30, p);
        canvas.saveLayer(0, 0, 2000, 1000, p, Canvas.ALL_SAVE_FLAG);
//      p.setColor(Color.GREEN);
        bp = Bitmap.createBitmap(2000, 1000, Config.ARGB_4444);
//      Canvas tmpcs = new Canvas(bp);
//      p.setStyle(Style.STROKE);
//      tmpcs.drawRect(new Rect(2, 2, 2000, 900), p);
//        tmpcs.drawColor(Color.GRAY);
//      tmpcs.drawCircle(currentX, currentY, 30, p);
//      canvas.drawBitmap(bp, 0, 0, null);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        currentX = event.getX();
        currentY = event.getY();
        Log.e("wmb", "--onTouchEvent--currentX:"+currentX+"--currentY:"+currentY);
        this.invalidate();
        return true;
    }
}

