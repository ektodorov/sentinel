package com.blogspot.techzealous.sentinel;

import android.Manifest;
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
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.blogspot.techzealous.sentinel.utils.ConstantsS;
import com.blogspot.techzealous.sentinel.utils.ConstantsText;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE_PERMISSIONS = 101;

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
                Intent i = new Intent(MainActivity.this, CameraActivity2.class);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        LayoutInflater layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.dialog_about, null, false);
        TextView textView = view.findViewById(R.id.textViewContentDialogAbout);

        String text = "";
        switch (item.getItemId()) {
            case R.id.menuPrivacyPolicy:
                text = ConstantsText.PRIVACY_POLICY;
                break;
            case R.id.menuDisclaimer:
                text = ConstantsText.DISCLAIMER;
                break;
            case R.id.menuTC:
                text = ConstantsText.TERMS_AND_CONDITIONS;
                break;
            case R.id.menuAbout:
                text = ConstantsText.LICENSE;
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

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
        return true;
    }
}
