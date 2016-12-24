package com.blogspot.techzealous.sentinel;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private Button mButtonCamera;
    private Button mButtonVideo;
    private Button mButtonPicture;
    private Button mButton1;
    private ImageView mImageView1;
    private ImageView mImageView2;
    private ImageView mImageView3;
    private ImageView mImageView1Overlay;

    private Bitmap mBitmap1;
    private Bitmap mBitmap2;
    private Resources mResources;
    private Handler mHandlerMain;
    private ExecutorService mExecutorService;

    private int mWidth = 131;
    private int mHeight = 65;
    private float mSampleSize = 6;//even sample sizes give better results

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mButtonCamera = (Button)findViewById(R.id.buttonCameraMain);
        mButtonVideo = (Button)findViewById(R.id.buttonVideoMain);
        mButtonPicture = (Button)findViewById(R.id.buttonPictureMain);
        mButton1 = (Button)findViewById(R.id.button1Main);
        mImageView1 = (ImageView)findViewById(R.id.imageView1);
        mImageView2 = (ImageView)findViewById(R.id.imageView2);
        mImageView3 = (ImageView)findViewById(R.id.imageView3);
        mImageView1Overlay = (ImageView)findViewById(R.id.imageView1Overlay);

        mResources = getResources();
        mHandlerMain = new Handler(Looper.getMainLooper());
        mExecutorService = Executors.newSingleThreadExecutor();

        mButtonCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(MainActivity.this, CameraActivity.class);
                startActivity(i);
            }
        });

        mButtonVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(MainActivity.this, VideoActivity.class);
                startActivity(i);
            }
        });

        mButtonPicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(MainActivity.this, PictureActivity.class);
                startActivity(i);
            }
        });

        mButton1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mExecutorService.execute(new Runnable() {
                    @Override
                    public void run() {

                    }
                });
            }
        });
    }
}
