package com.example.androidocr.whiteboard;

import static com.example.androidocr.whiteboard.bean.StrokeRecord.*;

import java.io.File;
import java.io.FileOutputStream;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.example.androidocr.R;
import com.example.androidocr.SketchView;
import com.example.androidocr.whiteboard.bean.StrokeRecord;
import com.example.androidocr.whiteboard.utils.CommonUtils;
import com.example.androidocr.whiteboard.utils.ScreenUtils;
import com.example.androidocr.whiteboard.utils.SdCardStatus;
import com.example.androidocr.whiteboard.utils.TimeUtils;
import com.example.androidocr.whiteboard.utils.ViewUtils;


public class WhiteBoardFragment extends Fragment implements OnClickListener, SketchView.OnDrawChangedListener {
    static final int COLOR_BLACK = Color.parseColor("#ff000000");
    static final int COLOR_RED = Color.parseColor("#ffff4444");
    static final int COLOR_GREEN = Color.parseColor("#ff99cc00");
    static final int COLOR_ORANGE = Color.parseColor("#ffffbb33");
    static final int COLOR_BLUE = Color.parseColor("#ff33b5e5");
    private static final float BTN_ALPHA = 0.4f;
    final String TAG = getClass().getSimpleName();
    public  String FILE_PATH;

    Activity activity;//上下文
    private SketchView mSketchView; // 画布
    private ImageView btn_pen; // 画笔
    private ImageView btn_eraser;
    private ImageView btn_undo;
    private ImageView btn_redo;
//    private ImageView btn_pic;
//    private ImageView btn_background;
//    private ImageView btn_drag;
    private ImageView btn_save;
//    private ImageView btn_empty;
    private ImageView btn_screenshot;

    private int penBaseSize;
    private int textOffX,textOffY;

    private EditText saveET; // 保存文件的EditText
    private EditText textET; // 输入文字的EditText
    private AlertDialog saveDialog,dialog, screenshotDialog;

    private int mPenType = STROKE_TYPE_DRAW; // 画笔类型
    private int mPenColor = COLOR_BLACK; // 画笔颜色

    private PopupWindow mPenPopupWindow; // 画笔弹窗
    private PopupWindow mEraserPopupWindow;// 橡皮弹窗
    private PopupWindow mTextPopupWindow;// 文字输入

    private View mPenLayoutView; // 画笔布局
//    private View mEraserLayoutView;// 橡皮布局
//    private View mTextLayoutView; // 文字输入框

    private ImageView mPenSizeCircle, mPenAlphaCircle;
    private SeekBar mPenSizeSeekBar, mPenAlphaSeekBar;
    private RadioGroup mPenTypeGroup, mPenColorGroup;

//    private ImageView mEraserSizeCircle;
//    private SeekBar mEraserSizeSeekBar;

    private static boolean isScreenShoting = false; // 是否正在截屏，保持两次截屏时间间隔大于3秒

    private static Handler mHandler = new Handler(){

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            isScreenShoting = false;
        }

    };


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = getActivity();//初始化上下文
        FILE_PATH = SdCardStatus.getDefaulstCacheDirInSdCard(activity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_white_board, container,  false);
        initView(rootView, inflater);
        initDrawParams();//初始化绘画参数
        initPopupWindow();//初始化弹窗
        initSaveDialog();//初始化保存文件对话框
        return rootView;
    }
    private void initDrawParams() {
        //画笔宽度缩放基准参数
        Drawable circleDrawable = getResources().getDrawable(R.drawable.circle);
        if (circleDrawable != null){
            penBaseSize = circleDrawable.getIntrinsicWidth();
        }
    }

    private void initPopupWindow() {
        initPenPopupWindow();
//        initEraserPopupWindow();
//        initTextPop();
    }

    private void initSaveDialog(){
        saveET = new EditText(activity);
        saveET.setHint("新文件名");
        saveET.setGravity(Gravity.CENTER);
        saveET.setSingleLine();
        saveET.setInputType(EditorInfo.TYPE_CLASS_TEXT);
        saveET.setImeOptions(EditorInfo.IME_ACTION_DONE);
        saveET.setSelectAllOnFocus(true);
        saveET.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    ScreenUtils.hideInput(saveDialog.getCurrentFocus());
                    saveDialog.dismiss();
                    String input = saveET.getText().toString();
                    Log.e("wmb", "--onEditorAction--input:"+input);
                    saveInUI(input + ".png");
                }
                return true;
            }
        });
        saveDialog = new AlertDialog.Builder(activity)
                .setTitle("请输入保存文件名")
                .setMessage("")
                .setView(saveET)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ScreenUtils.hideInput(saveDialog.getCurrentFocus());
                        String input = saveET.getText().toString();
                        Log.e("wmb", "--onClick--input:"+input);
                        saveInUI(input + ".png");
                    }
                }).setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ScreenUtils.hideInput(saveDialog.getCurrentFocus());
                    }
                })
                .setCancelable(false)
                .create();
    }

    /**
     * 初始化橡皮弹窗
     */
    private void initEraserPopupWindow() {
        mEraserPopupWindow = new PopupWindow(activity);
//        mEraserPopupWindow.setContentView(mEraserLayoutView);
        mEraserPopupWindow.setWidth(550);
        mEraserPopupWindow.setHeight(240);
        mEraserPopupWindow.setBackgroundDrawable(new BitmapDrawable());
        mEraserPopupWindow.setOutsideTouchable(true);

//        mEraserSizeSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
//
//            @Override
//            public void onStopTrackingTouch(SeekBar arg0) {
//
//            }
//
//            @Override
//            public void onStartTrackingTouch(SeekBar arg0) {
//
//            }
//
//            @Override
//            public void onProgressChanged(SeekBar arg0, int progress, boolean arg2) {
//                setSeekBarProgress(progress, STROKE_TYPE_ERASER);
//            }
//        });
//        mEraserSizeSeekBar.setProgress(SketchView.DEFAULT_ERASER_SIZE);
    }

    /**
     * 初始化画笔弹窗
     */
    private void initPenPopupWindow() {
        mPenPopupWindow = new PopupWindow(activity);
        mPenPopupWindow.setContentView(mPenLayoutView);
        mPenPopupWindow.setWidth(550);
        mPenPopupWindow.setHeight(650);
        mPenPopupWindow.setBackgroundDrawable(new BitmapDrawable());
        mPenPopupWindow.setOutsideTouchable(true);
        // 画笔类型
        mPenTypeGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
//                int resId = R.drawable.stroke_type_rbtn_draw_checked;
                switch (checkedId) {
                    case R.id.type_pen:
                        mPenType = STROKE_TYPE_DRAW;
                        break;
                    case R.id.type_line:
                        mPenType = STROKE_TYPE_LINE;
//                        resId = R.drawable.stroke_type_rbtn_line_checked;
                        break;
//                    case R.id.type_circle:
//                        mPenType = STROKE_TYPE_CIRCLE;
//                        resId = R.drawable.stroke_type_rbtn_circle_checked;
//                        break;
//                    case R.id.type_rectangle:
//                        mPenType = STROKE_TYPE_RECTANGLE;
//                        resId = R.drawable.stroke_type_rbtn_rectangle_checked;
//                        break;
//                    case R.id.type_text:
//                        mPenType = STROKE_TYPE_TEXT;
//                        resId = R.drawable.stroke_type_rbtn_text_checked;
//                        break;
                    default:
                        break;
                }
//                btn_pen.setImageResource(resId);
                mSketchView.setStrokeType(mPenType);
                mPenPopupWindow.dismiss();
            }
        });
        // 画笔颜色
        mPenColorGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.color_black:
                        mPenColor = COLOR_BLACK;
                        break;
                    case R.id.color_red:
                        mPenColor = COLOR_RED;
                        break;
                    case R.id.color_green:
                        mPenColor = COLOR_GREEN;
                        break;
                    case R.id.color_orange:
                        mPenColor = COLOR_ORANGE;
                        break;
                    case R.id.color_blue:
                        mPenColor = COLOR_BLUE;
                        break;

                    default:
                        break;
                }
                mSketchView.setStrokeColor(mPenColor);
            }
        });
        // 画笔大小
        mPenSizeSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar arg0) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar arg0) {

            }

            @Override
            public void onProgressChanged(SeekBar arg0, int progress, boolean arg2) {
                setSeekBarProgress(progress, STROKE_TYPE_DRAW);
            }
        });
        mPenSizeSeekBar.setProgress(SketchView.DEFAULT_STROKE_SIZE);
        // 画笔透明度
        mPenAlphaSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar arg0) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar arg0) {

            }

            @Override
            public void onProgressChanged(SeekBar arg0, int progress, boolean arg2) {
                int alpha = progress * 255 / 100; // 百分比转换为透明度
                mSketchView.setStrokeAlpha(alpha);
//                mPenAlphaCircle.setAlpha(alpha);
            }
        });
        mPenAlphaSeekBar.setProgress(SketchView.DEFAULT_STROKE_ALPHA);
    }
    // 初始化文字输入
    private void initTextPop() {
        mTextPopupWindow = new PopupWindow(activity);
//        mTextPopupWindow.setContentView(mTextLayoutView);
        mTextPopupWindow.setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
        mTextPopupWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        mTextPopupWindow.setFocusable(true);
        mTextPopupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        mTextPopupWindow.setInputMethodMode(PopupWindow.INPUT_METHOD_NEEDED);
//        mTextPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
//            @Override
//            public void onDismiss() {
//                if (!textET.getText().toString().equals("")) {
//                    StrokeRecord record = new StrokeRecord(mPenType);
//                    record.text = textET.getText().toString();
//                }
//            }
//        });
    }

    protected void setSeekBarProgress(int progress, int strokeTypeDraw) {
        int realProgress = progress > 1 ? progress : 1;
        int newSize = Math.round(penBaseSize * realProgress / 100); // 百分比转换为大小
        int offset = Math.round((penBaseSize - newSize) / 2);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(newSize, newSize);
        lp.setMargins(offset, offset, offset, offset);
        if(strokeTypeDraw == STROKE_TYPE_DRAW){
            mPenSizeCircle.setLayoutParams(lp);
        }else{
            // 橡皮
//            mEraserSizeCircle.setLayoutParams(lp);
        }
        mSketchView.setSize(newSize, strokeTypeDraw);
    }

    private void initView(View root, LayoutInflater inflater){
        mSketchView = ViewUtils.findViewById(root, R.id.id_sketch_view);
        btn_pen = ViewUtils.findViewById(root, R.id.id_pen);
        btn_eraser = ViewUtils.findViewById(root, R.id.id_eraser);
        btn_undo = ViewUtils.findViewById(root, R.id.id_undo);
        btn_redo = ViewUtils.findViewById(root, R.id.id_redo);
//        btn_pic = ViewUtils.findViewById(root, R.id.id_pic);
//        btn_background = ViewUtils.findViewById(root, R.id.id_bacground);
//        btn_drag = ViewUtils.findViewById(root, R.id.id_drag);
        btn_save = ViewUtils.findViewById(root, R.id.id_save);
//        btn_empty = ViewUtils.findViewById(root, R.id.id_empty);
        btn_screenshot = ViewUtils.findViewById(root, R.id.id_screenshot);


        btn_pen.setOnClickListener(this);
        btn_eraser.setOnClickListener(this);
        btn_undo.setOnClickListener(this);
        btn_redo.setOnClickListener(this);
//        btn_pic.setOnClickListener(this);
//        btn_background.setOnClickListener(this);
//        btn_drag.setOnClickListener(this);
        btn_save.setOnClickListener(this);
//        btn_empty.setOnClickListener(this);
        btn_screenshot.setOnClickListener(this);
        mSketchView.setOnDrawChangedListener(this); // 设置绘画监听
        mSketchView.setTextWindowCallback(new SketchView.TextWindowCallback() {

            @Override
            public void onText(View view, StrokeRecord record) {
                textOffX = record.textOffX;
                textOffY = record.textOffY;
                showTextPopupWindow(view, record);
            }
        });
        // 初始化画笔弹窗views
        mPenLayoutView = inflater.inflate(R.layout.popup_pen, null);
        mPenSizeCircle = ViewUtils.findViewById(mPenLayoutView, R.id.pen_size_circle);
//        mPenAlphaCircle = ViewUtils.findViewById(mPenLayoutView, R.id.pen_alpha_circle);
        mPenSizeSeekBar = ViewUtils.findViewById(mPenLayoutView, R.id.pen_size_seek_bar);
        mPenAlphaSeekBar = ViewUtils.findViewById(mPenLayoutView, R.id.pen_alpha_seek_bar);
        mPenTypeGroup = ViewUtils.findViewById(mPenLayoutView, R.id.pen_type_radio_group);
        mPenColorGroup = ViewUtils.findViewById(mPenLayoutView, R.id.pen_color_radio_group);

        //初始化橡皮弹窗views
//        mEraserLayoutView = inflater.inflate(R.layout.popup_eraser, null);
//        mEraserSizeCircle = ViewUtils.findViewById(mEraserLayoutView, R.id.eraser_size_circle);
//        mEraserSizeSeekBar = ViewUtils.findViewById(mEraserLayoutView, R.id.eraser_size_seek_bar);

        //初始化文字输入
//        mTextLayoutView = inflater.inflate(R.layout.popup_text, null);
//        textET = ViewUtils.findViewById(mTextLayoutView, R.id.text_pupwindow_et);
        btn_undo.setAlpha(0.4f);
        btn_redo.setAlpha(0.4f);
        showBtn(btn_pen);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.id_pen:
                if(mSketchView.getEditMode() == SketchView.EDIT_STROKE && mSketchView.getStrokeType() != STROKE_TYPE_ERASER){
                    showPopupWindow(view, STROKE_TYPE_DRAW);
                }else{
                    int checkedId = mPenTypeGroup.getCheckedRadioButtonId();
                    switch (checkedId) {
                        case R.id.type_pen:
                            mPenType = STROKE_TYPE_DRAW;
                            break;
                        case R.id.type_line:
                            mPenType = STROKE_TYPE_LINE;
                            break;
//                        case R.id.type_circle:
//                            mPenType = STROKE_TYPE_CIRCLE;
//                            break;
//                        case R.id.type_rectangle:
//                            mPenType = STROKE_TYPE_RECTANGLE;
//                            break;
//                        case R.id.type_text:
//                            mPenType = STROKE_TYPE_TEXT;
//                            break;
                        default:
                            break;
                    }
                    mSketchView.setStrokeType(mPenType);
                }
                mSketchView.setEditMode(SketchView.EDIT_STROKE);
                showBtn(btn_pen);
                break;
            case R.id.id_eraser:
                if(mSketchView.getEditMode() == SketchView.EDIT_STROKE && mSketchView.getStrokeType() == STROKE_TYPE_ERASER){
                    showPopupWindow(view, STROKE_TYPE_ERASER);
                }else{
                    mSketchView.setStrokeType(STROKE_TYPE_ERASER);
                }
                mSketchView.setEditMode(SketchView.EDIT_STROKE);
                showBtn(btn_eraser);
                break;
            case R.id.id_undo:
                mSketchView.undo();
                break;
            case R.id.id_redo:
                mSketchView.redo();
                break;
//            case R.id.id_pic:
//                mSketchView.addPhotoByPath("persion.jpg");
//                mSketchView.setEditMode(SketchView.EDIT_PHOTO);
//                showBtn(btn_drag);
//                break;
//            case R.id.id_bacground:
//                mSketchView.setBackgroundByPath("najing.jpeg");
//                break;
//            case R.id.id_drag:
//                mSketchView.setEditMode(SketchView.EDIT_PHOTO);
//                showBtn(btn_drag);
//                break;
            case R.id.id_save:
                if(mSketchView.getRecordCount() == 0){
                    CommonUtils.showToast("您还没有绘图呢~", 3000);
                }else{
                    showSaveDialog();
                }
                break;
//            case R.id.id_empty:
//                askforErase();
//                break;
            case R.id.id_screenshot:

                break;

            default:
                break;
        }

    }
    /**
     * 截屏方法
     */
    private File screenShot(String filepath, String fileName){
        if(null == activity)
            return null;
        Point screenPoint = ScreenUtils.getScreenSize(activity);
        if(null == screenPoint)
            return null;
        int statusBarAndTitleHeight = ScreenUtils.getStatusBarAndTitleHeight(activity);
        View view = activity.getWindow().getDecorView();
        view.setDrawingCacheEnabled(true);
        Bitmap bitmapTemp = view.getDrawingCache();
        Bitmap bmp = Bitmap.createBitmap(bitmapTemp, 0, statusBarAndTitleHeight, screenPoint.x, screenPoint.y - statusBarAndTitleHeight);
        view.destroyDrawingCache();
        try {
            File dir = new File(filepath);
            if(!dir.exists()){
                boolean mk = dir.mkdirs();
            }
            File f = new File(dir, fileName);
            if(!f.exists()){
                f.createNewFile();
            }else{
                f.delete();
            }
            FileOutputStream out = new FileOutputStream(f);
            bmp.compress(Bitmap.CompressFormat.PNG, 90, out);
            out.flush();
            out.close();
            bitmapTemp.recycle();
            bmp.recycle();
            bitmapTemp = null;
            bmp = null;
            return f;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    private void showTextPopupWindow(View anchor, final StrokeRecord record) {
        textET.requestFocus();
        mTextPopupWindow.showAsDropDown(anchor, record.textOffX, record.textOffY - mSketchView.getHeight());
        mTextPopupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED);
        InputMethodManager imm = (InputMethodManager) activity
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
//        ScreenUtils.showInput(textET);
        mTextPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                if (!textET.getText().toString().equals("")) {
                    record.text = textET.getText().toString();
                    record.textPaint.setTextSize(textET.getTextSize());
                    record.textWidth = textET.getMaxWidth();
                    mSketchView.addStrokeRecord(record);
                }
            }
        });
    }

    private void showSaveDialog() {
        saveDialog.show();
        saveET.setText(TimeUtils.getNowTimeString());
        saveET.selectAll();
        ScreenUtils.showInput(mSketchView);
    }
    private void askforErase() {
        new AlertDialog.Builder(activity)
                .setMessage("擦除手绘?")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mSketchView.erase();
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {

                    }
                })
                .create()
                .show();
    }

    private void showBtn(ImageView iv) {
        btn_eraser.setAlpha(BTN_ALPHA);
        btn_pen.setAlpha(BTN_ALPHA);
//        btn_drag.setAlpha(BTN_ALPHA);
        iv.setAlpha(1f);
    }
    private void showPopupWindow(View anchor, int drawMode){
        if(drawMode == STROKE_TYPE_DRAW){
            mPenPopupWindow.showAsDropDown(anchor, 0, 0);
        }else if(drawMode == STROKE_TYPE_ERASER){
            mEraserPopupWindow.showAsDropDown(anchor, 0, 0);
        }
    }
    private void saveInUI(final String imgName) {
        new saveToFileTask().execute(imgName);
    }
    public File saveInOI(String filePath, String imgName) {
        return saveInOI(filePath, imgName, 100);
    }

    public File saveInOI(String filePath, String imgName, int compress) {
        Log.e("wmb", "--saveInOI--filePath:" + filePath + "--imgName:"
                + imgName + "--compress:" + compress);
        if (!imgName.contains(".png")) {
            imgName += ".png";
        }
        Log.e(TAG, "saveInOI: " + System.currentTimeMillis());
        Bitmap newBM = mSketchView.getResultBitmap();
        Log.e(TAG, "saveInOI: " + System.currentTimeMillis());

        try {
            File dir = new File(filePath);
            if (!dir.exists()) {
                boolean mk = dir.mkdirs();
                Log.e("wmb", "--saveInOI--mk:" + mk);
            }
            File f = new File(filePath, imgName);
            if (!f.exists()) {
                boolean cr = f.createNewFile();
                Log.e("wmb", "--saveInOI--cr:" + cr);
            } else {
                f.delete();
            }
            FileOutputStream out = new FileOutputStream(f);
            Log.e(TAG, "saveInOI: " + System.currentTimeMillis());

            if (compress >= 1 && compress <= 100)
                newBM.compress(Bitmap.CompressFormat.PNG, compress, out);
            else {
                newBM.compress(Bitmap.CompressFormat.PNG, 80, out);
            }
            Log.e(TAG, "saveInOI: " + System.currentTimeMillis());

            out.close();
            newBM.recycle();
            newBM = null;
            return f;
        } catch (Exception e) {
            Log.e("wmb", "--e:" + e.getStackTrace());
            return null;
        }
    }

    class saveToFileTask extends AsyncTask<String, Void, File> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.e("wmb", "--onPreExecute");
            dialog = new AlertDialog.Builder(activity)
                    .setTitle("保存画板")
                    .setMessage("保存中...")
                    .show();
        }

        @Override
        protected File doInBackground(String... photoName) {
            Log.e("wmb", "--doInBackground");
            return saveInOI(FILE_PATH, photoName[0]);
        }

        @Override
        protected void onPostExecute(File file) {
            super.onPostExecute(file);
            if (null != file && file.exists())
                Toast.makeText(activity, file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(activity, "保存失败！", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        }
    }

    @Override
    public void onDrawChanged() {
        Log.e("wmb", "--onDrawChanged-StrokeRecordCount:"+mSketchView.getStrokeRecordCount()+"--RedoCount:"+mSketchView.getRedoCount());
        if(mSketchView.getStrokeRecordCount() > 0){
            btn_undo.setAlpha(1f);
        }else{
            btn_undo.setAlpha(0.4f);
        }
        if(mSketchView.getRedoCount() > 0){
            btn_redo.setAlpha(1f);
        }else{
            btn_redo.setAlpha(0.4f);
        }
    }


}

