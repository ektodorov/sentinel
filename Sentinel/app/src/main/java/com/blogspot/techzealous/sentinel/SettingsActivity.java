package com.blogspot.techzealous.sentinel;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.blogspot.techzealous.sentinel.utils.ConstantsS;
import com.blogspot.techzealous.sentinel.utils.DialogSlider;
import com.blogspot.techzealous.sentinel.utils.OnValueSetListener;

public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "SettingsActivity";
    private LinearLayout mLinearLayoutRoot;
    private CheckBox mCheckBoxStabilization;
    private TextView mTextViewStabilizationSensitivity;
    private TextView mTextViewDifferenceSensitivity;

    private SharedPreferences mPrefs;
    private int mThresholdStabilization;
    private int mThresholdDifference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mLinearLayoutRoot = (LinearLayout)findViewById(R.id.linearLayoutRootSettings);
        mCheckBoxStabilization = (CheckBox)findViewById(R.id.checkBoxStabilizationSettings);
        mTextViewStabilizationSensitivity = (TextView)findViewById(R.id.textViewStabilizationSensitivitySettings);
        mTextViewDifferenceSensitivity = (TextView)findViewById(R.id.textViewDifferenceSensitivitySettings);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(SettingsActivity.this);
        boolean isStabilizationEnabled = mPrefs.getBoolean(ConstantsS.PREF_STABILIZATION_ENABLED, false);
        mThresholdStabilization = mPrefs.getInt(ConstantsS.PREF_THRESHOLD_STABILIZATION, 70);
        mThresholdDifference = mPrefs.getInt(ConstantsS.PREF_THRESHOLD_DIFFERENCE, 85);

        mCheckBoxStabilization.setChecked(isStabilizationEnabled);

        mCheckBoxStabilization.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean isChecked = mCheckBoxStabilization.isChecked();
                ConstantsS.setStabilizationEnabled(isChecked);
                mPrefs.edit().putBoolean(ConstantsS.PREF_STABILIZATION_ENABLED, isChecked).commit();
            }
        });

        mTextViewStabilizationSensitivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DialogSlider dialog = new DialogSlider(ConstantsS.STR_Stabilization,
                        ConstantsS.STR_Stabilization_threshold, new OnValueSetListener()
                {
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
                        ConstantsS.STR_Difference_threshold, new OnValueSetListener()
                {
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
    }
}
