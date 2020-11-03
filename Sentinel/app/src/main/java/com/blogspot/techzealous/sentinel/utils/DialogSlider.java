package com.blogspot.techzealous.sentinel.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.blogspot.techzealous.sentinel.R;

public class DialogSlider {

    private static final String TAG = "DialogSlider";
    private String mTitle;
    private String mMessage;
    private int mMaximum = 100;
    private OnValueSetListener mOnValueSet;
    private AlertDialog mAlertDialog;
    private SeekBar mSeekBar;
    private TextView mTextViewProgress;
    private Button mButtonLeft;
    private Button mButtonRight;
    private Button mButtonCancel;
    private Button mButtonOk;

    private Handler mHandlerMain;
    private Runnable mRunnableProgressDecrease;
    private Runnable mRunnableProgressIncrease;

    public DialogSlider(String aTitle, String aMessage, OnValueSetListener aOnValueSetListener) {
        mTitle = aTitle;
        mMessage = aMessage;
        mOnValueSet = aOnValueSetListener;
    }

    public void createAlertDialog(Context aContext, ViewGroup aViewRoot, int aProgress, int aMaximum) {
        mMaximum = aMaximum;
        createAlertDialog(aContext, aViewRoot, aProgress);
    }

    public void createAlertDialog(Context aContext, ViewGroup aViewRoot, int aProgress) {
        mHandlerMain = new Handler(Looper.getMainLooper());
        mRunnableProgressDecrease = new Runnable() {
            @Override
            public void run() {
                mSeekBar.setProgress(mSeekBar.getProgress() - 1);
                mHandlerMain.removeCallbacks(mRunnableProgressDecrease);
                if(mButtonLeft.isPressed()) {
                    mHandlerMain.postDelayed(mRunnableProgressDecrease, 200);
                }
            }
        };

        mRunnableProgressIncrease = new Runnable() {
            @Override
            public void run() {
                mSeekBar.setProgress(mSeekBar.getProgress() + 1);
                mHandlerMain.removeCallbacks(mRunnableProgressIncrease);
                if(mButtonRight.isPressed()) {
                    mHandlerMain.postDelayed(mRunnableProgressIncrease, 200);
                }
            }
        };

        LayoutInflater layoutInflater = (LayoutInflater)aContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.dialog_slider, aViewRoot, false);
        mSeekBar = (SeekBar)view.findViewById(R.id.seekBarDialogSlider);
        mTextViewProgress = (TextView)view.findViewById(R.id.textViewProgressDialogSlider);
        mButtonLeft = (Button)view.findViewById(R.id.buttonLeftDialogSlider);
        mButtonRight = (Button)view.findViewById(R.id.buttonRightDialogSlider);
        mButtonCancel = (Button)view.findViewById(R.id.buttonCancelDialogSlider);
        mButtonOk = (Button)view.findViewById(R.id.buttonOkDialogSlider);

        AlertDialog.Builder adb = new AlertDialog.Builder(aContext);
        adb.setTitle(mTitle);
        adb.setMessage(mMessage);
        adb.setView(view);
        mAlertDialog = adb.create();

        mButtonLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSeekBar.setProgress(mSeekBar.getProgress() - 1);
            }
        });

        mButtonRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSeekBar.setProgress(mSeekBar.getProgress() + 1);
            }
        });

        mButtonLeft.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                mHandlerMain.post(mRunnableProgressDecrease);
                return true;
            }
        });

        mButtonRight.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                mHandlerMain.post(mRunnableProgressIncrease);
                return true;
            }
        });

        mButtonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mAlertDialog.dismiss();
            }
        });

        mButtonOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mOnValueSet.onValueSet(mSeekBar.getProgress());
                mAlertDialog.dismiss();
            }
        });

        mTextViewProgress.setText(String.valueOf(aProgress));
        mSeekBar.setMax(mMaximum);
        mSeekBar.setProgress(aProgress);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mTextViewProgress.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    public void setMax(int aMaximum) {
        mMaximum = aMaximum;
    }

    public void showDialog() {
        mAlertDialog.show();
    }

    public void dismissDialog() {
        mAlertDialog.dismiss();
    }

    public AlertDialog getAlertDialog() {
        return mAlertDialog;
    }
}