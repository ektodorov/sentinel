package com.blogspot.techzealous.sentinel;

import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.blogspot.techzealous.sentinel.utils.ConstantsS;
import com.blogspot.techzealous.sentinel.utils.DeviceAdminReceiverS;
import com.blogspot.techzealous.sentinel.utils.DialogSlider;
import com.blogspot.techzealous.sentinel.utils.OnValueSetListener;

import java.io.File;
import java.lang.ref.WeakReference;

public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "SettingsActivity";
    private LinearLayout mLinearLayoutRoot;
    private RelativeLayout mRelativeLayoutStabilization;
    private CheckBox mCheckBoxStabilization;
    private TextView mTextViewStabilizationSensitivity;
    private TextView mTextViewDifferenceSensitivity;
    private RelativeLayout mRelativeLayoutPlaySound;
    private CheckBox mCheckBoxPlaySound;
    private RelativeLayout mRelativeLayoutPictures;
    private TextView mTextViewRecordPicturesDesc;
    private CheckBox mCheckBoxRecordPictures;
    private RelativeLayout mRelativeLayoutVideos;
    private TextView mTextViewRecordVideosDesc;
    private CheckBox mCheckBoxRecordVideos;
    private RelativeLayout mRelativeLayoutBlankScreen;
    private CheckBox mCheckBoxBlankScreen;
    private TextView mTextViewDifferenceUpdate;
    private TextView mTextViewBackgroundRecordTime;
    private RelativeLayout mRelativeLayoutDeviceAdmin;
    private CheckBox mCheckBoxDeviceAdmin;

    private SharedPreferences mPrefs;
    private int mThresholdStabilization;
    private int mThresholdDifference;
    private int mDifferenceUpdate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mLinearLayoutRoot = findViewById(R.id.linearLayoutRootSettings);
        mRelativeLayoutStabilization = findViewById(R.id.relativeLayoutStabilizationSettings);
        mCheckBoxStabilization = findViewById(R.id.checkBoxStabilizationSettings);
        mTextViewStabilizationSensitivity = findViewById(R.id.textViewStabilizationSensitivitySettings);
        mTextViewDifferenceSensitivity = findViewById(R.id.textViewDifferenceSensitivitySettings);
        mRelativeLayoutPlaySound = findViewById(R.id.relativeLayoutPlaySoundSettings);
        mCheckBoxPlaySound = findViewById(R.id.checkBoxPlaySoundSettings);
        mRelativeLayoutPictures = findViewById(R.id.relativeLayoutPicturesSettings);
        mTextViewRecordPicturesDesc = findViewById(R.id.textViewRecordPicturesDescSettings);
        mCheckBoxRecordPictures = findViewById(R.id.checkBoxRecordPicturesSettings);
        mRelativeLayoutVideos = findViewById(R.id.relativeLayoutVideosSettings);
        mTextViewRecordVideosDesc = findViewById(R.id.textViewRecordVideosDescSettings);
        mCheckBoxRecordVideos = findViewById(R.id.checkBoxRecordVideosSettings);
        mRelativeLayoutBlankScreen = findViewById(R.id.relativeLayoutBlankScreenSettings);
        mCheckBoxBlankScreen = findViewById(R.id.checkBoxBlankScreenSettings);
        mTextViewDifferenceUpdate = findViewById(R.id.textViewDifferenceUpdate);
        mTextViewBackgroundRecordTime = findViewById(R.id.textViewBackgroundRecordTime);
        mRelativeLayoutDeviceAdmin = findViewById(R.id.relativeLayoutDeviceAdmin);
        mCheckBoxDeviceAdmin = findViewById(R.id.checkBoxDeviceAdmin);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(SettingsActivity.this);
        boolean isStabilizationEnabled = mPrefs.getBoolean(ConstantsS.PREF_STABILIZATION_ENABLED, false);
        mThresholdStabilization = mPrefs.getInt(ConstantsS.PREF_THRESHOLD_STABILIZATION, 70);
        mThresholdDifference = mPrefs.getInt(ConstantsS.PREF_THRESHOLD_DIFFERENCE, ConstantsS.THRESHOLD_DIFFERENCE_DEFAULT);
        mDifferenceUpdate = mPrefs.getInt(ConstantsS.PREF_DIFFERENCE_UPDATE_MS, CameraActivity2.UPDATE_DIFF_INTERVAL_MS_NORMAL);
        boolean isPlaySoundEnabled = mPrefs.getBoolean(ConstantsS.PREF_PLAY_SOUND, false);
        boolean isRecordPictures = mPrefs.getBoolean(ConstantsS.PREF_RECORD_PICTURES, false);
        boolean isRecordVideos = mPrefs.getBoolean(ConstantsS.PREF_RECORD_VIDEOS, true);
        boolean isBlankScreen = mPrefs.getBoolean(ConstantsS.PREF_BLANK_SCREEN, true);
        int backgroundRecordTime = mPrefs.getInt(ConstantsS.PREF_BACKGROUND_RECORD_TIME, ConstantsS.getBackgroundRecordTime());
        boolean isDeviceAdmin = mPrefs.getBoolean(ConstantsS.PREF_DEVICE_ADMIN, ConstantsS.isDeviceAdmin());

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "sentinel");
        mTextViewRecordPicturesDesc.setText(getResources().getString(R.string.record_description,
                mediaStorageDir.getAbsolutePath()));
        mTextViewRecordVideosDesc.setText(getResources().getString(R.string.record_description,
                mediaStorageDir.getAbsolutePath()));

        mCheckBoxStabilization.setChecked(isStabilizationEnabled);
        mCheckBoxPlaySound.setChecked(isPlaySoundEnabled);
        mCheckBoxRecordPictures.setChecked(isRecordPictures);
        mCheckBoxRecordVideos.setChecked(isRecordVideos);
        mCheckBoxBlankScreen.setChecked(isBlankScreen);
        mCheckBoxDeviceAdmin.setChecked(isDeviceAdmin);

        final WeakReference<SettingsActivity> weakThis = new WeakReference<>(this);
        mRelativeLayoutStabilization.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean isChecked = !mCheckBoxStabilization.isChecked();
                mCheckBoxStabilization.setChecked(isChecked);
                ConstantsS.setStabilizationEnabled(isChecked);
                mPrefs.edit().putBoolean(ConstantsS.PREF_STABILIZATION_ENABLED, isChecked).commit();
            }
        });

        mTextViewStabilizationSensitivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DialogSlider dialog = new DialogSlider(ConstantsS.STR_Stabilization,
                        ConstantsS.STR_Stabilization_threshold, new OnValueSetListener() {
                    @Override
                    public void onValueSet(int aValue) {
                        ConstantsS.setThresholdStabilization(aValue);
                        mThresholdStabilization = aValue;
                        mPrefs.edit().putInt(ConstantsS.PREF_THRESHOLD_STABILIZATION, aValue).commit();
                    }
                });
                dialog.createAlertDialog(SettingsActivity.this, mLinearLayoutRoot, mThresholdStabilization);
                dialog.showDialog();
            }
        });

        mTextViewDifferenceSensitivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DialogSlider dialog = new DialogSlider(ConstantsS.STR_Difference,
                        ConstantsS.STR_Difference_threshold, new OnValueSetListener() {
                    @Override
                    public void onValueSet(int aValue) {
                        ConstantsS.setThresholdDifference(aValue);
                        mThresholdDifference = aValue;
                        mPrefs.edit().putInt(ConstantsS.PREF_THRESHOLD_DIFFERENCE, aValue).commit();
                    }
                });
                dialog.createAlertDialog(SettingsActivity.this, mLinearLayoutRoot, mThresholdDifference);
                dialog.showDialog();
            }
        });

        mRelativeLayoutPlaySound.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean isChecked = !mCheckBoxPlaySound.isChecked();
                mCheckBoxPlaySound.setChecked(isChecked);
                ConstantsS.setPlaySoundEnabled(isChecked);
                mPrefs.edit().putBoolean(ConstantsS.PREF_PLAY_SOUND, isChecked).commit();
            }
        });


        mRelativeLayoutPictures.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean isChecked = !mCheckBoxRecordPictures.isChecked();
                mCheckBoxRecordPictures.setChecked(isChecked);
                //mCheckBoxRecordVideos.setChecked(!isChecked);
                ConstantsS.setRecordPictures(isChecked);
                //ConstantsS.setRecordVideos(!isChecked);
                mPrefs.edit().putBoolean(ConstantsS.PREF_RECORD_PICTURES, isChecked).commit();
                //mPrefs.edit().putBoolean(ConstantsS.PREF_RECORD_VIDEOS, !isChecked).commit();
            }
        });

        mRelativeLayoutVideos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean isChecked = !mCheckBoxRecordVideos.isChecked();
                //mCheckBoxRecordPictures.setChecked(!isChecked);
                mCheckBoxRecordVideos.setChecked(isChecked);
                //ConstantsS.setRecordPictures(!isChecked);
                ConstantsS.setRecordVideos(isChecked);
                //mPrefs.edit().putBoolean(ConstantsS.PREF_RECORD_PICTURES, !isChecked).commit();
                mPrefs.edit().putBoolean(ConstantsS.PREF_RECORD_VIDEOS, isChecked).commit();
            }
        });

        mRelativeLayoutBlankScreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean isChecked = !mCheckBoxBlankScreen.isChecked();
                mCheckBoxBlankScreen.setChecked(isChecked);
                ConstantsS.setPlaySoundEnabled(isChecked);
                mPrefs.edit().putBoolean(ConstantsS.PREF_BLANK_SCREEN, isChecked).commit();
            }
        });

        mTextViewDifferenceUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DialogSlider dialog = new DialogSlider("Difference Update",
                        "Check for difference (seconds)", new OnValueSetListener() {
                    @Override
                    public void onValueSet(int aValue) {
                        if(aValue < 1) {
                            aValue = 1;
                        }
                        ConstantsS.setDifferenceUpdate(aValue * 1000);
                        mDifferenceUpdate = aValue;
                        mPrefs.edit().putInt(ConstantsS.PREF_DIFFERENCE_UPDATE_MS, aValue).commit();
                    }
                });
                dialog.createAlertDialog(SettingsActivity.this, mLinearLayoutRoot, mDifferenceUpdate, 4);
                dialog.showDialog();
            }
        });

        mTextViewBackgroundRecordTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DialogSlider dialog = new DialogSlider("Background record time",
                        "Record in background (minutes)", new OnValueSetListener() {
                    @Override
                    public void onValueSet(int aValue) {
                        if(aValue < 1) {
                            aValue = 1;
                        }
                        ConstantsS.setBackgroundRecordTime(aValue);
                        mPrefs.edit().putInt(ConstantsS.PREF_BACKGROUND_RECORD_TIME, aValue).commit();
                    }
                });
                dialog.createAlertDialog(SettingsActivity.this, mLinearLayoutRoot, ConstantsS.getBackgroundRecordTime(), 1440);
                dialog.showDialog();
            }
        });

        mRelativeLayoutDeviceAdmin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SettingsActivity strongThis = weakThis.get();
                if(strongThis == null) {
                    return;
                }

                boolean isChecked = !strongThis.mCheckBoxDeviceAdmin.isChecked();
                strongThis.mCheckBoxDeviceAdmin.setChecked(isChecked);
                ConstantsS.setDeviceAdmin(isChecked);
                strongThis.mPrefs.edit().putBoolean(ConstantsS.PREF_DEVICE_ADMIN, isChecked).commit();

                ComponentName cn = new ComponentName(strongThis, DeviceAdminReceiverS.class);
                if(isChecked) {
                    Intent i = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                    i.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, cn);
                    i.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Now");
                    strongThis.startActivity(i);
                } else {
                    DevicePolicyManager devicePolicyManager = (DevicePolicyManager) strongThis.getSystemService(Context.DEVICE_POLICY_SERVICE);
                    devicePolicyManager.removeActiveAdmin(cn);
                }
            }
        });
    }
}
