package com.blogspot.techzealous.sentinel;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.Instrumentation;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.blogspot.techzealous.sentinel.utils.ConstantsS;
import com.blogspot.techzealous.sentinel.utils.ConstantsText;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private ExecutorService mExecutor;

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
        mExecutor = Executors.newSingleThreadExecutor();

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
        ConstantsS.setThresholdDifference(mPrefs.getInt(ConstantsS.PREF_THRESHOLD_DIFFERENCE, ConstantsS.THRESHOLD_DIFFERENCE_DEFAULT));
        ConstantsS.setPlaySoundEnabled(mPrefs.getBoolean(ConstantsS.PREF_PLAY_SOUND, false));
        ConstantsS.setRecordPictures(mPrefs.getBoolean(ConstantsS.PREF_RECORD_PICTURES, false));
        ConstantsS.setRecordVideos(mPrefs.getBoolean(ConstantsS.PREF_RECORD_VIDEOS, true));
        ConstantsS.setDifferenceUpdate(mPrefs.getInt(ConstantsS.PREF_DIFFERENCE_UPDATE_MS, CameraActivity2.UPDATE_DIFF_INTERVAL_MS_NORMAL));

        Dialog dialog = showDialogLoading("Loading...");
        final WeakReference<MainActivity> weakThis = new WeakReference<>(this);
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                MainActivity strongThis = weakThis.get();
                if(strongThis == null) {
                    return;
                }
                strongThis.copyFiles();
                dialog.dismiss();
            }
        });
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

    private Dialog showDialogLoading(String text) {
        LayoutInflater layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.dialog_loading, null, false);
        TextView textView = view.findViewById(R.id.textLoading);
        textView.setText(text);

        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setView(view);
        adb.setCancelable(false);
        Dialog dialog = adb.create();
        dialog.show();
        return dialog;
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

    //show a ProgressBar when this method starts and remove it when it ends. We shouldn't disable the back button, because the user may still use the home button or
    //task manager to switch to another app or to force close the application.
    //Or we can run this in the CameraActivity2 just not right after the record stops, but on every second record and not copying/deleting the current record.
    private void copyFiles() {
        File[] files = CameraActivity2.getAllFiles(this);
        for(int x = 0; x < files.length; x++) {
            try {
                File file = files[x];
                CameraActivity2.copyFileToVideoGallery(this, file);
                file.delete();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
