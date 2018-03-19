package com.blogspot.techzealous.sentinel;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.blogspot.techzealous.sentinel.utils.ConstantsS;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE_CAMERA = 101;
    private static final int REQUEST_CODE_WRITE_EXTERNAL_STORAGE = 102;
    private static final int REQUEST_CODE_CAMERA_STORAGE = 103;

    private Button mButtonSettings;
    private Button mButtonCamera;
    private Button mButtonVideo;
    private Button mButtonPicture;

    private SharedPreferences mPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mButtonSettings = (Button)findViewById(R.id.buttonSettingsMain);
        mButtonCamera = (Button)findViewById(R.id.buttonCameraMain);
        mButtonVideo = (Button)findViewById(R.id.buttonVideoMain);
        mButtonPicture = (Button)findViewById(R.id.buttonPictureMain);

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

        mPrefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        ConstantsS.setStabilizationEnabled(mPrefs.getBoolean(ConstantsS.PREF_STABILIZATION_ENABLED, false));
        ConstantsS.setThresholdStabilization(mPrefs.getInt(ConstantsS.PREF_THRESHOLD_STABILIZATION, 70));
        ConstantsS.setThresholdDifference(mPrefs.getInt(ConstantsS.PREF_THRESHOLD_DIFFERENCE, 85));
        ConstantsS.setPlaySoundEnabled(mPrefs.getBoolean(ConstantsS.PREF_PLAY_SOUND, false));
        ConstantsS.setRecordPictures(mPrefs.getBoolean(ConstantsS.PREF_RECORD_PICTURES, true));

        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        int permissionWriteDiskCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED
                && permissionWriteDiskCheck != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_CAMERA_STORAGE);
        } else if(permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CODE_CAMERA);
        } else if(permissionWriteDiskCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_CODE_WRITE_EXTERNAL_STORAGE);
        }
    }
}
