package com.example.androidocr;


import static android.graphics.Canvas.ALL_SAVE_FLAG;
import static com.example.androidocr.whiteboard.bean.StrokeRecord.STROKE_TYPE_CIRCLE;
import static com.example.androidocr.whiteboard.bean.StrokeRecord.STROKE_TYPE_DRAW;
import static com.example.androidocr.whiteboard.bean.StrokeRecord.STROKE_TYPE_ERASER;
import static com.example.androidocr.whiteboard.bean.StrokeRecord.STROKE_TYPE_LINE;
import static com.example.androidocr.whiteboard.bean.StrokeRecord.STROKE_TYPE_RECTANGLE;
import static com.example.androidocr.whiteboard.bean.StrokeRecord.STROKE_TYPE_TEXT;

import java.io.File;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Environment;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.androidocr.whiteboard.bean.PhotoRecord;
import com.example.androidocr.whiteboard.bean.SketchData;
import com.example.androidocr.whiteboard.bean.StrokeRecord;
import com.example.androidocr.whiteboard.utils.BitmapUtils;
import com.example.androidocr.whiteboard.utils.ScreenUtils;

/**
 * 画板整体布局
 *
 */
public class SketchView extends View implements OnTouchListener/*,Runnable,SurfaceHolder.Callback*/ {

    final String TAG = getClass().getSimpleName();

    public interface TextWindowCallback {
        void onText(View view, StrokeRecord record);
    }


    public void setTextWindowCallback(TextWindowCallback textWindowCallback) {
        this.textWindowCallback = textWindowCallback;
    }

    private TextWindowCallback textWindowCallback;
    private static final float TOUCH_TOLERANCE = 4;

    public static final int EDIT_STROKE = 1;
    public static final int EDIT_PHOTO = 2;

    private static final int ACTION_NONE = 0;
    private static final int ACTION_DRAG = 1;
    private static final int ACTION_SCALE = 2;
    private static final int ACTION_ROTATE = 3;

    public static final int DEFAULT_STROKE_SIZE = 7;
    public static final int DEFAULT_STROKE_ALPHA = 100;
    public static final int DEFAULT_ERASER_SIZE = 50;


    private float strokeSize = DEFAULT_STROKE_SIZE;
    private int strokeRealColor = Color.BLACK;//画笔实际颜色
    private int strokeColor = Color.BLACK;//画笔颜色
    private int strokeAlpha = 255;//画笔透明度
    private float eraserSize = DEFAULT_ERASER_SIZE;

    Paint boardPaint;

//    Bitmap mirrorMarkBM = BitmapFactory.decodeResource(getResources(), R.drawable.mark_copy);
//    Bitmap deleteMarkBM = BitmapFactory.decodeResource(getResources(), R.drawable.mark_delete);
//    Bitmap rotateMarkBM = BitmapFactory.decodeResource(getResources(), R.drawable.mark_rotate);
//    Bitmap resetMarkBM = BitmapFactory.decodeResource(getResources(), R.drawable.mark_reset);
    //    Bitmap rotateMarkBM = BitmapFactory.decodeResource(getResources(), R.drawable.test);
//    RectF markerCopyRect = new RectF(0, 0, mirrorMarkBM.getWidth(), mirrorMarkBM.getHeight());//镜像标记边界
//    RectF markerDeleteRect = new RectF(0, 0, deleteMarkBM.getWidth(), deleteMarkBM.getHeight());//删除标记边界
//    RectF markerRotateRect = new RectF(0, 0, rotateMarkBM.getWidth(), rotateMarkBM.getHeight());//旋转标记边界
//    RectF markerResetRect = new RectF(0, 0, resetMarkBM.getWidth(), resetMarkBM.getHeight());//旋转标记边界

    private Path strokePath;
    private Paint strokePaint;
    //    private Paint erasePaint;
    private float downX, downY, preX, preY, curX, curY;
    private int mWidth, mHeight;
    private Bitmap   mEraseMaskBitmap = null;// 橡皮擦
    private Canvas mEraseCanvas;

    SketchData curSketchData = new SketchData();

    private Context mContext;
    Rect backgroundSrcRect = new Rect();
    Rect backgroundDstRect = new Rect();
    StrokeRecord curStrokeRecord;
    PhotoRecord curPhotoRecord;

    int actionMode;

    private int editMode = EDIT_STROKE;
    private static float SCALE_MAX = 4.0f;
    private static float SCALE_MIN = 0.2f;
    private static float SCALE_MIN_LEN;
    private boolean isFirstIn = true;
    private int mScreenWidth = 720,mScreenHeight = 1134;

    float simpleScale = 0.5f;//图片载入的缩放倍数
    /**
     * 缩放手势
     */
    private ScaleGestureDetector mScaleGestureDetector = null;
    public void setStrokeType(int strokeType) {
        this.strokeType = strokeType;
    }

    public int getStrokeType() {
        return strokeType;
    }

    private int strokeType = STROKE_TYPE_DRAW;

    private OnDrawChangedListener onDrawChangedListener;

    public void setSketchData(SketchData sketchData) {
        this.curSketchData = sketchData;
        curPhotoRecord = null;
    }

    public void updateSketchData(SketchData sketchData) {
        if (curSketchData != null)
            curSketchData.thumbnailBM = getThumbnailResultBitmap();//更新数据前先保存上一份数据的缩略图
        setSketchData(sketchData);
    }



    public SketchView(Context context, AttributeSet attr) {
        super(context, attr);
        this.mContext = context;
        setFocusable(true);
        initParams(context);
        if (isFocusable()) {
            this.setOnTouchListener(this);
            mScaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.OnScaleGestureListener() {
                @Override
                public boolean onScale(ScaleGestureDetector detector) {
                    onScaleAction(detector);
                    return true;
                }


                @Override
                public boolean onScaleBegin(ScaleGestureDetector detector) {
                    return true;
                }

                @Override
                public void onScaleEnd(ScaleGestureDetector detector) {

                }
            });
        }
        invalidate();
    }

    //初始化
    private void initBmpMask(Canvas canvas) {
        mScreenWidth = getWidth();
        mScreenHeight = getHeight();
        Log.e("iwmb", "--mScreenWidth:"+mScreenWidth+"--mScreenHeight:"+mScreenHeight);

    }

    private void initParams(Context context) {

        strokePaint = new Paint();
        strokePaint.setAntiAlias(true);
        strokePaint.setDither(true);
        strokePaint.setColor(strokeRealColor);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeJoin(Paint.Join.ROUND);
        strokePaint.setStrokeCap(Paint.Cap.ROUND);
        strokePaint.setStrokeWidth(strokeSize);

//        erasePaint = new Paint();
//        erasePaint.setAntiAlias(true);
//        erasePaint.setDither(true);
//        erasePaint.setColor(0xFF000000);
//        erasePaint.setStyle(Paint.Style.STROKE);
//        erasePaint.setStrokeJoin(Paint.Join.ROUND);
//        erasePaint.setStrokeCap(Paint.Cap.ROUND);
//        erasePaint.setStrokeWidth(200);
//        erasePaint.setXfermode(new PorterDuffXfermode(Mode.DST_OUT));

        boardPaint = new Paint();
        boardPaint.setColor(Color.GREEN);
        boardPaint.setStrokeWidth(ScreenUtils.dip2px(mContext, 0.8f));
        boardPaint.setStyle(Paint.Style.STROKE);

        SCALE_MIN_LEN = ScreenUtils.dip2px(context, 20);
    }


    public void setStrokeAlpha(int mAlpha) {
        this.strokeAlpha = mAlpha;
        calculColor();
        strokePaint.setStrokeWidth(strokeSize);
    }


    public void setStrokeColor(int color) {
        strokeColor = color;
        calculColor();
        strokePaint.setColor(strokeRealColor);
    }


    private void calculColor() {
        strokeRealColor = Color.argb(strokeAlpha, Color.red(strokeColor), Color.green(strokeColor), Color.blue(strokeColor));
    }



    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mWidth = MeasureSpec.getSize(widthMeasureSpec);
        mHeight = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(mWidth, mHeight);
    }


    @Override
    public boolean onTouch(View arg0, MotionEvent event) {
        curX = event.getX();
        curY = event.getY();

        switch (event.getAction() & MotionEvent.ACTION_MASK) {

            case MotionEvent.ACTION_POINTER_DOWN:
                float downDistance = spacing(event);
                Log.e("wmb", "--onTouch--"+event.getAction()+"--space:"+downDistance);
                if (actionMode == ACTION_DRAG && downDistance > 10)//防止误触
                    actionMode = ACTION_SCALE;
                break;
            case MotionEvent.ACTION_DOWN:
                Log.e("wmb", "--onTouch--action down");
                touch_down(event);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                Log.e("wmb", "--onTouch--action move");
                touch_move(event);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                Log.e("wmb", "--onTouch--action up");
                touch_up();
                invalidate();
                break;
        }
        preX = curX;
        preY = curY;
        return true;
    }

    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if(isFirstIn){
            initBmpMask(canvas);
            Log.e("iwmb", "--drawRecord isFristIN");
            isFirstIn = false;
        }
        drawBackground(canvas);
        drawRecord(canvas);
//        if (onDrawChangedListener != null)
//            onDrawChangedListener.onDrawChanged();
    }

    private void drawBackground(Canvas canvas) {
        if (curSketchData.backgroundBM != null) {
            //缩放图片，使之能够在屏幕中完整显示（大图压缩、小图放大）
            Matrix matrix = new Matrix();
            float wScale = (float) canvas.getWidth() / curSketchData.backgroundBM.getWidth();
            float hScale = (float) canvas.getHeight() / curSketchData.backgroundBM.getHeight();
            matrix.postScale(wScale, hScale);
            canvas.drawBitmap(curSketchData.backgroundBM, matrix, null);
            Log.e("iwmb", "--canvas.getWidth():"+canvas.getWidth()+"--canvas.getHeight():"+canvas.getHeight());
            Log.d(TAG, "drawBackground:src= " + backgroundSrcRect.toString() + ";dst=" + backgroundDstRect.toString());
        } else {
            canvas.drawColor(Color.WHITE);
        }

    }

    private void drawRecord(Canvas canvas) {
        drawRecord(canvas, true);
    }

    private void drawRecord(Canvas canvas, boolean isDrawBoard) {

        mEraseMaskBitmap = Bitmap.createBitmap(mScreenWidth, mScreenHeight, Bitmap.Config.ARGB_8888);

        mEraseCanvas = new Canvas(mEraseMaskBitmap);

        mEraseCanvas.drawColor(Color.TRANSPARENT);

        if (curSketchData != null) {
            for (PhotoRecord record : curSketchData.photoRecordList) {
                if (record != null)
                    canvas.drawBitmap(record.bitmap, record.matrix, null);
            }
            if (isDrawBoard && editMode == EDIT_PHOTO && curPhotoRecord != null) {
                SCALE_MAX = curPhotoRecord.scaleMax;
                float[] photoCorners = calculateCorners(curPhotoRecord);//计算图片四个角点和中心点
                drawBoard(canvas, photoCorners);//绘制图形边线
//                drawMarks(canvas, photoCorners);//绘制边角图片
            }

            for (StrokeRecord record : curSketchData.strokeRecordList) {
                int type = record.type;
                if (type == STROKE_TYPE_ERASER){ // 橡皮擦
                    mEraseCanvas.drawPath(record.path, record.paint);

                } else if (type == STROKE_TYPE_DRAW || type == STROKE_TYPE_LINE) {
                    Log.e("iwmb", "--drawRecord:--path:"+record.path.toString()+"--type:"+type);
//                    canvas.drawPath(record.path, record.paint);
                    mEraseCanvas.drawPath(record.path, record.paint);
                } else if (type == STROKE_TYPE_CIRCLE) {
                    Log.e("iwmb", "--drawcircle:--path:"+record.rect.toString()+"--type:"+type);
//                    canvas.drawOval(record.rect, record.paint);
                    mEraseCanvas.drawOval(record.rect, record.paint);
                } else if (type == STROKE_TYPE_RECTANGLE) {
//                    canvas.drawRect(record.rect, record.paint);
                    mEraseCanvas.drawRect(record.rect, record.paint);
                } else if (type == STROKE_TYPE_TEXT) {
                    if (record.text != null) {
                        StaticLayout layout = new StaticLayout(record.text, record.textPaint, record.textWidth, Layout.Alignment.ALIGN_NORMAL, 1.0F, 0.0F, true);
//                        canvas.translate(record.textOffX, record.textOffY);
//                        layout.draw(canvas);
//                        canvas.translate(-record.textOffX, -record.textOffY);
                        mEraseCanvas.translate(record.textOffX, record.textOffY);
                        layout.draw(mEraseCanvas);
                        mEraseCanvas.translate(-record.textOffX, -record.textOffY);
                    }
                }

            }
            canvas.drawBitmap(mEraseMaskBitmap, 0, 0, null);
        }

    }

    //绘制图像边线（由于图形旋转或不一定是矩形，所以用Path绘制边线）
    private void drawBoard(Canvas canvas, float[] photoCorners) {
        Path photoBorderPath = new Path();
        photoBorderPath.moveTo(photoCorners[0], photoCorners[1]);
        photoBorderPath.lineTo(photoCorners[2], photoCorners[3]);
        photoBorderPath.lineTo(photoCorners[4], photoCorners[5]);
        photoBorderPath.lineTo(photoCorners[6], photoCorners[7]);
        photoBorderPath.lineTo(photoCorners[0], photoCorners[1]);
        canvas.drawPath(photoBorderPath, boardPaint);
    }

    //绘制边角操作图标
//    private void drawMarks(Canvas canvas, float[] photoCorners) {
//        float x;
//        float y;
//        x = photoCorners[0] - markerCopyRect.width() / 2;
//        y = photoCorners[1] - markerCopyRect.height() / 2;
//        markerCopyRect.offsetTo(x, y);
//        canvas.drawBitmap(mirrorMarkBM, x, y, null);
//
//        x = photoCorners[2] - markerDeleteRect.width() / 2;
//        y = photoCorners[3] - markerDeleteRect.height() / 2;
//        markerDeleteRect.offsetTo(x, y);
//        canvas.drawBitmap(deleteMarkBM, x, y, null);
//
//        x = photoCorners[4] - markerRotateRect.width() / 2;
//        y = photoCorners[5] - markerRotateRect.height() / 2;
//        markerRotateRect.offsetTo(x, y);
//        canvas.drawBitmap(rotateMarkBM, x, y, null);
//
//        x = photoCorners[6] - markerResetRect.width() / 2;
//        y = photoCorners[7] - markerResetRect.height() / 2;
//        markerResetRect.offsetTo(x, y);
//        canvas.drawBitmap(resetMarkBM, x, y, null);
//    }

    private float[] calculateCorners(PhotoRecord record) {
        float[] photoCornersSrc = new float[10];//0,1代表左上角点XY，2,3代表右上角点XY，4,5代表右下角点XY，6,7代表左下角点XY，8,9代表中心点XY
        float[] photoCorners = new float[10];//0,1代表左上角点XY，2,3代表右上角点XY，4,5代表右下角点XY，6,7代表左下角点XY，8,9代表中心点XY
        RectF rectF = record.photoRectSrc;
        photoCornersSrc[0] = rectF.left;
        photoCornersSrc[1] = rectF.top;
        photoCornersSrc[2] = rectF.right;
        photoCornersSrc[3] = rectF.top;
        photoCornersSrc[4] = rectF.right;
        photoCornersSrc[5] = rectF.bottom;
        photoCornersSrc[6] = rectF.left;
        photoCornersSrc[7] = rectF.bottom;
        photoCornersSrc[8] = rectF.centerX();
        photoCornersSrc[9] = rectF.centerY();
        curPhotoRecord.matrix.mapPoints(photoCorners, photoCornersSrc);
        return photoCorners;
    }

    private float getMaxScale(RectF photoSrc) {
        return Math.max(getWidth(), getHeight()) / Math.max(photoSrc.width(), photoSrc.height());
//        SCALE_MIN = SCALE_MAX / 5;
    }

    public void addStrokeRecord(StrokeRecord record) {
        curSketchData.strokeRecordList.add(record);
        invalidate();
    }

    private void touch_down(MotionEvent event) {
        downX = event.getX();
        downY = event.getY();
        if (editMode == EDIT_STROKE) {
            curSketchData.strokeRedoList.clear();
            curStrokeRecord = new StrokeRecord(strokeType);
            if (strokeType == STROKE_TYPE_ERASER) {
                strokePath = new Path();
                strokePath.moveTo(downX, downY);
                curStrokeRecord.paint = new Paint(strokePaint); // Clones the mPaint object
                curStrokeRecord.paint.setXfermode(new PorterDuffXfermode(Mode.DST_OUT));
                curStrokeRecord.paint.setStrokeWidth(eraserSize);
                curStrokeRecord.path = strokePath;
            } else if (strokeType == STROKE_TYPE_DRAW || strokeType == STROKE_TYPE_LINE) {
                strokePath = new Path();
                strokePath.moveTo(downX, downY);
                curStrokeRecord.path = strokePath;
                strokePaint.setColor(strokeRealColor);
                strokePaint.setStrokeWidth(strokeSize);
                curStrokeRecord.paint = new Paint(strokePaint); // Clones the mPaint object
            } else if (strokeType == STROKE_TYPE_CIRCLE || strokeType == STROKE_TYPE_RECTANGLE) {
                RectF rect = new RectF(downX, downY, downX, downY);
                curStrokeRecord.rect = rect;
                strokePaint.setColor(strokeRealColor);
                strokePaint.setStrokeWidth(strokeSize);
                curStrokeRecord.paint = new Paint(strokePaint); // Clones the mPaint object
            } else if (strokeType == STROKE_TYPE_TEXT) {
                curStrokeRecord.textOffX = (int) downX;
                curStrokeRecord.textOffY = (int) downY;
                TextPaint tp = new TextPaint();
                tp.setColor(strokeRealColor);
                curStrokeRecord.textPaint = tp; // Clones the mPaint object
                textWindowCallback.onText(this, curStrokeRecord);
                return;
            }
            curSketchData.strokeRecordList.add(curStrokeRecord);
        } else if (editMode == EDIT_PHOTO) {
            float[] downPoint = new float[]{downX, downY};
//            if (isInMarkRect(downPoint)) {// 先判操作标记区域
//                return;
//            }
            if (isInPhotoRect(curPhotoRecord, downPoint)) {//再判断是否点击了当前图片
                actionMode = ACTION_DRAG;
                return;
            }
            selectPhoto(downPoint);//最后判断是否点击了其他图片
        }
    }

    //judge click which photo，then can edit the photo
    private void selectPhoto(float[] downPoint) {
        PhotoRecord clickRecord = null;
        for (int i = curSketchData.photoRecordList.size() - 1; i >= 0; i--) {
            PhotoRecord record = curSketchData.photoRecordList.get(i);
            if (isInPhotoRect(record, downPoint)) {
                clickRecord = record;
                break;
            }
        }
        if (clickRecord != null) {
            setCurPhotoRecord(clickRecord);
            actionMode = ACTION_DRAG;
        } else {
            actionMode = ACTION_NONE;
        }
    }

//    private boolean isInMarkRect(float[] downPoint) {
//        if (markerRotateRect.contains(downPoint[0], (int) downPoint[1])) {//判断是否在区域内
//            actionMode = ACTION_ROTATE;
//            return true;
//        }
//        if (markerDeleteRect.contains(downPoint[0], (int) downPoint[1])) {//判断是否在区域内
//            curSketchData.photoRecordList.remove(curPhotoRecord);
//            setCurPhotoRecord(null);
//            actionMode = ACTION_NONE;
//            return true;
//        }
//        if (markerCopyRect.contains(downPoint[0], (int) downPoint[1])) {//判断是否在区域内
//            PhotoRecord newRecord = initPhotoRecord(curPhotoRecord.bitmap);
//            newRecord.matrix = new Matrix(curPhotoRecord.matrix);
//            newRecord.matrix.postTranslate(ScreenUtils.dip2px(mContext, 20), ScreenUtils.dip2px(mContext, 20));//偏移小段距离以分辨新复制的图片
//            setCurPhotoRecord(newRecord);
//            actionMode = ACTION_NONE;
//            return true;
//        }
//        if (markerResetRect.contains(downPoint[0], (int) downPoint[1])) {//判断是否在区域内
//            curPhotoRecord.matrix.reset();
//            curPhotoRecord.matrix.setTranslate(getWidth() / 2 - curPhotoRecord.photoRectSrc.width() / 2,
//                    getHeight() / 2 - curPhotoRecord.photoRectSrc.height() / 2);
//            actionMode = ACTION_NONE;
//            return true;
//        }
//        return false;
//    }

    private boolean isInPhotoRect(PhotoRecord record, float[] downPoint) {
        if (record != null) {
            float[] invertPoint = new float[2];
            Matrix invertMatrix = new Matrix();
            record.matrix.invert(invertMatrix);
            invertMatrix.mapPoints(invertPoint, downPoint);
            return record.photoRectSrc.contains(invertPoint[0], invertPoint[1]);
        }
        return false;
    }


    private void touch_move(MotionEvent event) {
        if (editMode == EDIT_STROKE) {
            if (strokeType == STROKE_TYPE_ERASER) {
                strokePath.quadTo(preX, preY, (curX + preX) / 2, (curY + preY) / 2);
//              curStrokeRecord.path.quadTo(preX, preY, (curX + preX) / 2, (curY + preY) / 2);
            } else if (strokeType == STROKE_TYPE_DRAW) {
                strokePath.quadTo(preX, preY, (curX + preX) / 2, (curY + preY) / 2);
//              curStrokeRecord.path.quadTo(preX, preY, (curX + preX) / 2, (curY + preY) / 2);
            } else if (strokeType == STROKE_TYPE_LINE) {
                strokePath.reset();
                strokePath.moveTo(downX, downY);
                strokePath.lineTo(curX, curY);
//              curStrokeRecord.path.reset();
//              curStrokeRecord.path.moveTo(downX, downY);
//              curStrokeRecord.path.lineTo(curX, curY);
            } else if (strokeType == STROKE_TYPE_CIRCLE || strokeType == STROKE_TYPE_RECTANGLE) {
                curStrokeRecord.rect.set(downX < curX ? downX : curX, downY < curY ? downY : curY, downX > curX ? downX : curX, downY > curY ? downY : curY);
            } else if (strokeType == STROKE_TYPE_TEXT) {

            }
        } else if (editMode == EDIT_PHOTO && curPhotoRecord != null) {
            if (actionMode == ACTION_DRAG) {
                onDragAction(curX - preX, curY - preY);
            } else if (actionMode == ACTION_ROTATE) {
                onRotateAction(curPhotoRecord);
            } else if (actionMode == ACTION_SCALE) {
                mScaleGestureDetector.onTouchEvent(event);
            }
        }
        preX = curX;
        preY = curY;
    }

    private void onScaleAction(ScaleGestureDetector detector) {
        float[] photoCorners = calculateCorners(curPhotoRecord);
        //目前图片对角线长度
        float len = (float) Math.sqrt(Math.pow(photoCorners[0] - photoCorners[4], 2) + Math.pow(photoCorners[1] - photoCorners[5], 2));
        double photoLen = Math.sqrt(Math.pow(curPhotoRecord.photoRectSrc.width(), 2) + Math.pow(curPhotoRecord.photoRectSrc.height(), 2));
        float scaleFactor = detector.getScaleFactor();
        //设置Matrix缩放参数
        if ((scaleFactor < 1 && len >= photoLen * SCALE_MIN && len >= SCALE_MIN_LEN) || (scaleFactor > 1 && len <= photoLen * SCALE_MAX)) {
            Log.e(scaleFactor + "", scaleFactor + "");
            curPhotoRecord.matrix.postScale(scaleFactor, scaleFactor, photoCorners[8], photoCorners[9]);
        }
    }

    private void onRotateAction(PhotoRecord record) {
        float[] corners = calculateCorners(record);
        //放大
        //目前触摸点与图片显示中心距离
        float a = (float) Math.sqrt(Math.pow(curX - corners[8], 2) + Math.pow(curY - corners[9], 2));
        //目前上次旋转图标与图片显示中心距离
        float b = (float) Math.sqrt(Math.pow(corners[4] - corners[0], 2) + Math.pow(corners[5] - corners[1], 2)) / 2;

        //设置Matrix缩放参数
        double photoLen = Math.sqrt(Math.pow(record.photoRectSrc.width(), 2) + Math.pow(record.photoRectSrc.height(), 2));
        if (a >= photoLen / 2 * SCALE_MIN && a >= SCALE_MIN_LEN && a <= photoLen / 2 * SCALE_MAX) {
            //这种计算方法可以保持旋转图标坐标与触摸点同步缩放
            float scale = a / b;
            record.matrix.postScale(scale, scale, corners[8], corners[9]);
        }

        //旋转
        //根据移动坐标的变化构建两个向量，以便计算两个向量角度.
        PointF preVector = new PointF();
        PointF curVector = new PointF();
        preVector.set(preX - corners[8], preY - corners[9]);//旋转后向量
        curVector.set(curX - corners[8], curY - corners[9]);//旋转前向量
        //计算向量长度
        double preVectorLen = getVectorLength(preVector);
        double curVectorLen = getVectorLength(curVector);
        //计算两个向量的夹角.
        double cosAlpha = (preVector.x * curVector.x + preVector.y * curVector.y)
                / (preVectorLen * curVectorLen);
        //由于计算误差，可能会带来略大于1的cos，例如
        if (cosAlpha > 1.0f) {
            cosAlpha = 1.0f;
        }
        //本次的角度已经计算出来。
        double dAngle = Math.acos(cosAlpha) * 180.0 / Math.PI;
        // 判断顺时针和逆时针.
        //判断方法其实很简单，这里的v1v2其实相差角度很小的。
        //先转换成单位向量
        preVector.x /= preVectorLen;
        preVector.y /= preVectorLen;
        curVector.x /= curVectorLen;
        curVector.y /= curVectorLen;
        //作curVector的逆时针垂直向量。
        PointF verticalVec = new PointF(curVector.y, -curVector.x);

        //判断这个垂直向量和v1的点积，点积>0表示俩向量夹角锐角。=0表示垂直，<0表示钝角
        float vDot = preVector.x * verticalVec.x + preVector.y * verticalVec.y;
        if (vDot > 0) {
            //v2的逆时针垂直向量和v1是锐角关系，说明v1在v2的逆时针方向。
        } else {
            dAngle = -dAngle;
        }
        record.matrix.postRotate((float) dAngle, corners[8], corners[9]);
    }

    /**
     * 获取p1到p2的线段的长度
     *
     * @return
     */
    double getVectorLength(PointF vector) {
        return Math.sqrt(vector.x * vector.x + vector.y * vector.y);
    }
    private void onDragAction(float distanceX, float distanceY) {
        curPhotoRecord.matrix.postTranslate((int) distanceX, (int) distanceY);
    }


    private void touch_up() {
        if (onDrawChangedListener != null)
            onDrawChangedListener.onDrawChanged();
    }


    @NonNull
    public Bitmap getResultBitmap() {
        Bitmap newBM = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(newBM);
        canvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));//抗锯齿
        //绘制背景
        drawBackground(canvas);
        drawRecord(canvas, false);
        return newBM;
    }

    @NonNull
    public void createCurThumbnailBM() {
        curSketchData.thumbnailBM = getThumbnailResultBitmap();
    }

    @NonNull
    public Bitmap getThumbnailResultBitmap() {
        return BitmapUtils.createBitmapThumbnail(getResultBitmap(), true, ScreenUtils.dip2px(mContext, 200), ScreenUtils.dip2px(mContext, 200));
    }
    /*
     * 删除一笔
     */
    public void undo() {
        if (curSketchData.strokeRecordList.size() > 0) {
            curSketchData.strokeRedoList.add(curSketchData.strokeRecordList.get(curSketchData.strokeRecordList.size() - 1));
            curSketchData.strokeRecordList.remove(curSketchData.strokeRecordList.size() - 1);
            invalidate();
            touch_up(); // refresh redo/undo icon status
        }
    }


    /*
     * 撤销
     */
    public void redo() {
        if (curSketchData.strokeRedoList.size() > 0) {
            curSketchData.strokeRecordList.add(curSketchData.strokeRedoList.get(curSketchData.strokeRedoList.size() - 1));
            curSketchData.strokeRedoList.remove(curSketchData.strokeRedoList.size() - 1);
            invalidate();
            touch_up(); // refresh redo/undo icon status
        }

    }


    public int getRedoCount() {
        return curSketchData.strokeRedoList != null ? curSketchData.strokeRedoList.size() : 0;
    }


    public int getRecordCount() {
        return (curSketchData.strokeRecordList != null && curSketchData.photoRecordList != null) ? curSketchData.strokeRecordList.size() + curSketchData.photoRecordList.size() : 0;
    }

    public int getStrokeRecordCount() {
        return curSketchData.strokeRecordList != null ? curSketchData.strokeRecordList.size() : 0;
    }


    public int getStrokeSize() {
        return Math.round(this.strokeSize);
    }


    public void setSize(int size, int eraserOrStroke) {
        switch (eraserOrStroke) {
            case STROKE_TYPE_DRAW:
                strokeSize = size;
                break;
            case STROKE_TYPE_ERASER:
                eraserSize = size;
                break;
        }

    }


    public void erase() {
        // 先判断是否已经回收
        for (PhotoRecord record : curSketchData.photoRecordList) {
            if (record != null && record.bitmap != null && !record.bitmap.isRecycled()) {
                record.bitmap.recycle();
                record.bitmap = null;
            }
        }
        if (curSketchData.backgroundBM != null && !curSketchData.backgroundBM.isRecycled()) {
            // 回收并且置为null
            curSketchData.backgroundBM.recycle();
            curSketchData.backgroundBM = null;
        }
        curSketchData.strokeRecordList.clear();
        curSketchData.photoRecordList.clear();
        curSketchData.strokeRedoList.clear();
        curPhotoRecord = null;
        System.gc();
        invalidate();
    }


    public void setOnDrawChangedListener(OnDrawChangedListener listener) {
        this.onDrawChangedListener = listener;
    }

    public interface OnDrawChangedListener {

        public void onDrawChanged();
    }

    public void addPhotoByPath(String path) {
        Bitmap sampleBM = getSampleBitMap(path);
        if (sampleBM != null) {
            PhotoRecord newRecord = initPhotoRecord(sampleBM);
            setCurPhotoRecord(newRecord);
        } else {
            Toast.makeText(mContext, "图片文件路径有误！", Toast.LENGTH_SHORT).show();
        }
    }

    public void setBackgroundByPath(String path) {
        Bitmap sampleBM = getSampleBitMap(path);
        if (sampleBM != null) {
            curSketchData.backgroundBM = sampleBM;
            backgroundSrcRect = new Rect(0, 0, curSketchData.backgroundBM.getWidth(), curSketchData.backgroundBM.getHeight());
            backgroundDstRect = new Rect(0, 0, mWidth, mHeight);
            invalidate();
        } else {
            Toast.makeText(mContext, "图片文件路径有误！", Toast.LENGTH_SHORT).show();
        }
    }

    public Bitmap getSampleBitMap(String path) {
        Bitmap sampleBM = null;
        if (path.contains(Environment.getExternalStorageDirectory().toString())) {
            sampleBM = getSDCardPhoto(path);
        } else {
            sampleBM = getAssetsPhoto(path);
        }
        return sampleBM;
    }

    @NonNull
    private PhotoRecord initPhotoRecord(Bitmap bitmap) {
        PhotoRecord newRecord = new PhotoRecord();
        newRecord.bitmap = bitmap;
        newRecord.photoRectSrc = new RectF(0, 0, newRecord.bitmap.getWidth(), newRecord.bitmap.getHeight());
        newRecord.scaleMax = getMaxScale(newRecord.photoRectSrc);//放大倍数
        newRecord.matrix = new Matrix();
        newRecord.matrix.postTranslate(getWidth() / 2 - bitmap.getWidth() / 2, getHeight() / 2 - bitmap.getHeight() / 2);
        return newRecord;
    }

    private void setCurPhotoRecord(PhotoRecord record) {
        curSketchData.photoRecordList.remove(record);
        curSketchData.photoRecordList.add(record);
        curPhotoRecord = record;
        invalidate();
    }

    public Bitmap getSDCardPhoto(String path) {
        File file = new File(path);
        if (file.exists()) {
            return BitmapUtils.decodeSampleBitMapFromFile(mContext, path, simpleScale);
        } else {
            return null;
        }
    }

    public Bitmap getAssetsPhoto(String path) {
        return BitmapUtils.getBitmapFromAssets(mContext, path);
    }

    public void setEditMode(int editMode) {
        this.editMode = editMode;
        invalidate();
    }

    public int getEditMode() {
        return editMode;
    }

}
