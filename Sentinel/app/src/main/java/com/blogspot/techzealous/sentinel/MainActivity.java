package com.blogspot.techzealous.sentinel;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.blogspot.techzealous.sentinel.utils.ConstantsS;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private Button mButtonSettings;
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
    private SharedPreferences mPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mButtonSettings = (Button)findViewById(R.id.buttonSettingsMain);
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
        Uri soundUri = Uri.parse("android.resource://" + getPackageName() + "/" + String.valueOf(R.raw.beep07));
        Ringtone ringtone = RingtoneManager.getRingtone(this, soundUri);
        ConstantsS.setRingtone(ringtone);

        mButtonSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(i);
            }
        });

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

        mPrefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        ConstantsS.setStabilizationEnabled(mPrefs.getBoolean(ConstantsS.PREF_STABILIZATION_ENABLED, false));
        ConstantsS.setThresholdStabilization(mPrefs.getInt(ConstantsS.PREF_THRESHOLD_STABILIZATION, 70));
        ConstantsS.setThresholdDifference(mPrefs.getInt(ConstantsS.PREF_THRESHOLD_DIFFERENCE, 85));
        ConstantsS.setPlaySoundEnabled(mPrefs.getBoolean(ConstantsS.PREF_PLAY_SOUND, false));
    }
}
