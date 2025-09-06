package com.blogspot.techzealous.sentinel;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.blogspot.techzealous.sentinel.utils.ConstantsS;
import com.blogspot.techzealous.sentinel.utils.ConstantsText;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE_PERMISSIONS = 101;

    private Button mButtonSettings;
    private Button mButtonCamera;
    private Button mButtonVideo;
    private Button mButtonPicture;
    private Button mButtonPrivacyPolicy;
    private Button mButtonDisclaimer;
    private Button mButtonTermsAndConditions;
    private Button mButtonLicense;

    private SharedPreferences mPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mButtonSettings = (Button)findViewById(R.id.buttonSettingsMain);
        mButtonCamera = (Button)findViewById(R.id.buttonCameraMain);
        mButtonVideo = (Button)findViewById(R.id.buttonVideoMain);
        mButtonPicture = (Button)findViewById(R.id.buttonPictureMain);
        mButtonPrivacyPolicy = findViewById(R.id.buttonPrivacyPolicy);
        mButtonDisclaimer = findViewById(R.id.buttonDisclaimer);
        mButtonTermsAndConditions = findViewById(R.id.buttonTermsAndConditions);
        mButtonLicense = findViewById(R.id.buttonLicense);

        Uri soundUri = Uri.parse("android.resource://" + getPackageName() + "/" + String.valueOf(R.raw.beep07));
        Ringtone ringtone = RingtoneManager.getRingtone(this, soundUri);
        ConstantsS.setRingtone(ringtone);

        final WeakReference<MainActivity> weakThis = new WeakReference<>(this);
        mButtonSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity strongThis = weakThis.get();
                if(strongThis == null) {
                    return;
                }
                Intent i = new Intent(MainActivity.this, SettingsActivity.class);
                strongThis.startActivity(i);
            }
        });

        mButtonCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity strongThis = weakThis.get();
                if(strongThis == null) {
                    return;
                }
                Intent i = new Intent(MainActivity.this, CameraActivity2.class);
                strongThis.startActivity(i);
            }
        });

        mButtonVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity strongThis = weakThis.get();
                if(strongThis == null) {
                    return;
                }
                Intent i = new Intent(MainActivity.this, VideoActivity.class);
                strongThis.startActivity(i);
            }
        });

        mButtonPicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity strongThis = weakThis.get();
                if(strongThis == null) {
                    return;
                }
                Intent i = new Intent(MainActivity.this, PictureActivity.class);
                strongThis.startActivity(i);
            }
        });

        mButtonPrivacyPolicy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity strongThis = weakThis.get();
                if(strongThis == null) {
                    return;
                }
                strongThis.showDialog(ConstantsText.PRIVACY_POLICY);
            }
        });

        mButtonDisclaimer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity strongThis = weakThis.get();
                if(strongThis == null) {
                    return;
                }
                strongThis.showDialog(ConstantsText.DISCLAIMER);
            }
        });
        mButtonTermsAndConditions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity strongThis = weakThis.get();
                if(strongThis == null) {
                    return;
                }
                strongThis.showDialog(ConstantsText.TERMS_AND_CONDITIONS);
            }
        });
        mButtonLicense.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity strongThis = weakThis.get();
                if(strongThis == null) {
                    return;
                }
                strongThis.showDialog(ConstantsText.LICENSE);
            }
        });

        mPrefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);

        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        int permissionWriteDiskCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int permissionRecordAudio = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        List<String> permissionList = new ArrayList<>();
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.CAMERA);
        }
        if (permissionWriteDiskCheck != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (permissionRecordAudio != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.RECORD_AUDIO);
        }
        String[] permissions = new String[permissionList.size()];
        for(int x = 0; x < permissionList.size(); x++) {
            permissions[x] = permissionList.get(x);
        }
        if(permissions.length > 0) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_PERMISSIONS);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        ConstantsS.setStabilizationEnabled(mPrefs.getBoolean(ConstantsS.PREF_STABILIZATION_ENABLED, false));
        ConstantsS.setThresholdStabilization(mPrefs.getInt(ConstantsS.PREF_THRESHOLD_STABILIZATION, 70));
        ConstantsS.setThresholdDifference(mPrefs.getInt(ConstantsS.PREF_THRESHOLD_DIFFERENCE, 85));
        ConstantsS.setPlaySoundEnabled(mPrefs.getBoolean(ConstantsS.PREF_PLAY_SOUND, false));
        ConstantsS.setRecordPictures(mPrefs.getBoolean(ConstantsS.PREF_RECORD_PICTURES, false));
        ConstantsS.setRecordVideos(mPrefs.getBoolean(ConstantsS.PREF_RECORD_VIDEOS, true));
    }

    private void showDialog(String text) {
        LayoutInflater layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.dialog_about, null, false);
        TextView textView = view.findViewById(R.id.textViewContentDialogAbout);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            textView.setText(Html.fromHtml(text, Html.FROM_HTML_MODE_COMPACT));
        } else {
            textView.setText(Html.fromHtml(text));
        }
        textView.setMovementMethod(LinkMovementMethod.getInstance());

        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setView(view);
        adb.setCancelable(true);
        adb.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        adb.create().show();
    }

    public static void requestStoragePermission(Activity activity, int requestCode) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {//API 29, Android 10
            int permissionCheck = PermissionChecker.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, requestCode);
            }
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {//API 32, Android 12
            int permissionCheck = PermissionChecker.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, requestCode);
            }
        } else {//API 33+, Android 13+
            int permissionImages = PermissionChecker.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_IMAGES);
            int permissionVideo = PermissionChecker.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_VIDEO);
            List<String> permissions = new ArrayList<>();
            if (permissionImages != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
            if (permissionVideo != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_VIDEO);
            }
            if (permissions.size() != 0) {
                ActivityCompat.requestPermissions(activity, permissions.toArray(new String[0]), requestCode);
            }
        }
    }
}
