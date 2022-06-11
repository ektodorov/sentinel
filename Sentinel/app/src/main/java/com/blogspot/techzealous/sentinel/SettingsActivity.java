package com.blogspot.techzealous.sentinel;

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
import com.blogspot.techzealous.sentinel.utils.DialogSlider;
import com.blogspot.techzealous.sentinel.utils.OnValueSetListener;

import java.io.File;

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

    private SharedPreferences mPrefs;
    private int mThresholdStabilization;
    private int mThresholdDifference;

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

        mPrefs = PreferenceManager.getDefaultSharedPreferences(SettingsActivity.this);
        boolean isStabilizationEnabled = mPrefs.getBoolean(ConstantsS.PREF_STABILIZATION_ENABLED, false);
        mThresholdStabilization = mPrefs.getInt(ConstantsS.PREF_THRESHOLD_STABILIZATION, 70);
        mThresholdDifference = mPrefs.getInt(ConstantsS.PREF_THRESHOLD_DIFFERENCE, 85);
        boolean isPlaySoundEnabled = mPrefs.getBoolean(ConstantsS.PREF_PLAY_SOUND, false);
        boolean isRecordPictures = mPrefs.getBoolean(ConstantsS.PREF_RECORD_PICTURES, false);
        boolean isRecordVideos = mPrefs.getBoolean(ConstantsS.PREF_RECORD_VIDEOS, true);

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "sentinel");
        mTextViewRecordPicturesDesc.setText(getResources().getString(R.string.record_description,
                mediaStorageDir.getAbsolutePath()));
        mTextViewRecordVideosDesc.setText(getResources().getString(R.string.record_description,
                mediaStorageDir.getAbsolutePath()));

        mCheckBoxStabilization.setChecked(isStabilizationEnabled);
        mCheckBoxPlaySound.setChecked(isPlaySoundEnabled);
        mCheckBoxRecordPictures.setChecked(isRecordPictures);
        mCheckBoxRecordVideos.setChecked(isRecordVideos);

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
    }
}
