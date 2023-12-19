package com.example.androidocr;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class TestActivity extends AppCompatActivity {

    public static final String TESS_DATA = "/tessdata";
    private static final String TARGET_FILENAME = "red.png";
    private static final String TARGET_FILENAME_DEMO = "sample.jpg";
    private static final String DATA_FILENAME = "eng.traineddata";
//private static final String DATA_FILENAME = "chi_sim.traineddata";
    private static final String TAG = MainActivity.class.getSimpleName();

    private Button main_bt_recognize,main_bt_recognize_2;
    private TextView main_tv_result;
    private ImageView main_iv_image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 设置布局文件
        setContentView(R.layout.activity_test);
        // 检查并请求应用所需权限
        checkPermission();
        // 获取控件对象
        initView();
        // 设置控件的监听器
        setListener();
    }

    private void setListener() {
        // 设置识别按钮的监听器
        main_bt_recognize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 识别之前需要再次检查一遍权限
                checkPermission();
                // 点击后的主程序
                mainProgram(TARGET_FILENAME);
            }
        });
        main_bt_recognize_2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 识别之前需要再次检查一遍权限
                checkPermission();
                // 点击后的主程序
                mainProgram(TARGET_FILENAME_DEMO);
            }
        });
    }

    // 获得界面需要交互的控件
    private void initView() {
        main_bt_recognize = findViewById(R.id.main_bt_recognize);
        main_bt_recognize_2 = findViewById(R.id.main_bt_recognize_2);
        main_tv_result = findViewById(R.id.main_tv_result);
        main_iv_image = findViewById(R.id.main_iv_image);
    }

    // 检查应用所需的权限，如不满足则发出权限请求
    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(TestActivity.this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 120);
        }
        if (ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(TestActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 121);
        }
    }

    // OCR识别的主程序
    private void mainProgram(String filename) {
        // 从assets中获取一张Bitmap图片
        Bitmap bitmap = getBitmapFromAssets(TestActivity.this, filename);
        // 同时显示在界面
        main_iv_image.setImageBitmap(bitmap);
        if (bitmap != null) {
            // 准备工作：创建路径和Tesserect的数据
            prepareTess();
            // 初始化Tesserect
            TessBaseAPI tessBaseAPI = new TessBaseAPI();
            String dataPath = getExternalFilesDir("/").getPath() + "/";
            tessBaseAPI.init(dataPath, "eng");
            // 识别并显示结果
            String result = getOCRResult(tessBaseAPI, bitmap);
            main_tv_result.setText(result);
        }
    }

    // 从assets中读取一张Bitmap类型的图片
    private Bitmap getBitmapFromAssets(Context context, String filename) {
        Bitmap bitmap = null;
        AssetManager assetManager = context.getAssets();
        try {
            InputStream is = assetManager.open(filename);
            bitmap = BitmapFactory.decodeStream(is);
            is.close();
            Log.i(TAG, "图片读取成功。");
            Toast.makeText(getApplicationContext(), "图片读取成功。", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.i(TAG, "图片读取失败。");
            Toast.makeText(getApplicationContext(), "图片读取失败。", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
        return bitmap;
    }

    // 为Tesserect复制(从assets中复制过去)所需的数据
    private void prepareTess() {
        try{
            // 先创建必须的目录
            File dir = getExternalFilesDir(TESS_DATA);
            if(!dir.exists()){
                if (!dir.mkdir()) {
                    Toast.makeText(getApplicationContext(), "目录" + dir.getPath() + "没有创建成功", Toast.LENGTH_SHORT).show();
                }
            }
            // 从assets中复制必须的数据
            String pathToDataFile = dir + "/" + DATA_FILENAME;
            if (!(new File(pathToDataFile)).exists()) {
                InputStream in = getAssets().open(DATA_FILENAME);
                OutputStream out = new FileOutputStream(pathToDataFile);
                byte[] buff = new byte[1024];
                int len;
                while ((len = in.read(buff)) > 0) {
                    out.write(buff, 0, len);
                }
                in.close();
                out.close();
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    // 进行OCR并返回识别结果
    private String getOCRResult(TessBaseAPI tessBaseAPI, Bitmap bitmap) {
        tessBaseAPI.setImage(bitmap);
        String result = "-";
        try{
            result = tessBaseAPI.getUTF8Text();
        }catch (Exception e){
            Log.e(TAG, e.getMessage());
        }
        tessBaseAPI.end();
        return result;
    }
}
