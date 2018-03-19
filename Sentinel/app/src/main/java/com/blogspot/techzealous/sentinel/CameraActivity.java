package com.blogspot.techzealous.sentinel;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.Ringtone;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.widget.ImageView;

import com.blogspot.techzealous.sentinel.utils.ConstantsS;
import com.blogspot.techzealous.sentinel.utils.ImageUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraActivity extends AppCompatActivity {

    private static final String TAG = "CameraActivity";
    private static final int UPDATE_INTERVAL_PICTURE = 1000;
    private static final int UPDATE_INTERVAL = 250;
    private static final int MB = 1024 * 1024;
    private static final int kSampleSize = 16;

    private ImageView mImageViewDiff;
    private TextureView mTextureView;

    private Handler mHandlerMain;
    private Camera mCamera;
    private ExecutorService mExecutorDiff;
    private ExecutorService mExecutorRecord;
    private Runnable mRunnableDiffPost;
    private Runnable mRunnableDiff;
    private Bitmap mBitmapPrevious;
    private Bitmap mBitmapCurrent;
    private volatile boolean mIsTextureViewDestroyed;
    private int mRecordIntervalMs = UPDATE_INTERVAL_PICTURE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {actionBar.hide();}
        mImageViewDiff = findViewById(R.id.imageViewCameraActivity);
        mTextureView = findViewById(R.id.textureViewCameraActivity);

        if(!checkCameraHardware(this)) {
            AlertDialog.Builder adb = new AlertDialog.Builder(this);
            adb.setTitle(R.string.no_camera);
            adb.setMessage(R.string.msg_nocamera);
            adb.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    finish();
                }
            });
            return;
        }

        mCamera = getCameraInstance();
        if(mCamera == null) {
            AlertDialog.Builder adb = new AlertDialog.Builder(this);
            adb.setTitle(R.string.no_camera);
            adb.setMessage(R.string.msg_errorcamera);
            adb.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    finish();
                }
            });
            return;
        }

        mHandlerMain = new Handler(Looper.getMainLooper());
        mExecutorDiff = Executors.newSingleThreadExecutor();
        mExecutorRecord = Executors.newSingleThreadExecutor();
        mRunnableDiffPost = new Runnable() {
            @Override
            public void run() {
                mRecordIntervalMs = mRecordIntervalMs - UPDATE_INTERVAL;
                mBitmapCurrent = mTextureView.getBitmap();
                mExecutorDiff.execute(mRunnableDiff);
            }
        };

        mRunnableDiff = new Runnable() {
            @Override
            public void run() {
                if(mBitmapCurrent == null) {
                    if(mIsTextureViewDestroyed) {
                        mHandlerMain.removeCallbacks(mRunnableDiffPost);
                    } else {
                        mHandlerMain.postDelayed(mRunnableDiffPost, UPDATE_INTERVAL);
                    }
                    return;
                }
                if(mBitmapPrevious == null) {
                    mBitmapPrevious = mBitmapCurrent;
                    if(mIsTextureViewDestroyed) {
                        mHandlerMain.removeCallbacks(mRunnableDiffPost);
                    } else {
                        mHandlerMain.postDelayed(mRunnableDiffPost, UPDATE_INTERVAL);
                    }
                    return;
                }

                ImageUtils imageUtils = new ImageUtils();
                if(ConstantsS.isStabilizationEnabled()) {
                    Point pointOffset = imageUtils.stabilizeFrame(mBitmapPrevious, mBitmapCurrent, kSampleSize * 2);
                    Rect rect = imageUtils.getDifference(pointOffset, mBitmapPrevious, mBitmapCurrent, kSampleSize,
                            ConstantsS.getThresholdStabilization());

                    //if there was no stabilization performed, we use the whole frame to look for movement
                    if (rect.left < 0 || rect.top < 0 || rect.right < 0 || rect.bottom < 0
                            || rect.right <= rect.left || rect.bottom <= rect.top) {
                        Rect rectDiff = imageUtils.getDifference(null, mBitmapPrevious, mBitmapCurrent, kSampleSize,
                                ConstantsS.getThresholdDifference());

                        mBitmapPrevious.recycle();
                        mBitmapPrevious = mBitmapCurrent;

                        final Bitmap bitmapRect = imageUtils.getBitmapDiffRect(rectDiff, mBitmapCurrent);
                        final boolean hasDiff = ImageUtils.hasDifference(rectDiff);

                        mHandlerMain.post(new Runnable() {
                            @Override
                            public void run() {
                                mImageViewDiff.setImageBitmap(bitmapRect);
                                if(hasDiff && ConstantsS.getPlaySoundEnabled()) {
                                    Ringtone ringtone = ConstantsS.getRingtone();
                                    if(ringtone != null && !ringtone.isPlaying()) {
                                        ringtone.play();
                                    }
                                }
                            }
                        });
                    } else {
                        //if there was stabilization performed we get the part of the frame that stayed the same
                        // (was not moved/changed due to camera movement) and look for movement in that part of the frame
                        Bitmap bitmapPreviousTemp = Bitmap.createBitmap(mBitmapPrevious, rect.left, rect.top,
                                (rect.right - rect.left), (rect.bottom - rect.top));
                        Bitmap bitmapCurrentTemp = Bitmap.createBitmap(mBitmapCurrent, rect.left, rect.top,
                                (rect.right - rect.left), (rect.bottom - rect.top));
                        Rect rectDiff = imageUtils.getDifference(null, bitmapPreviousTemp, bitmapCurrentTemp, kSampleSize,
                                ConstantsS.getThresholdDifference());
                        rectDiff.left = rectDiff.left + rect.left;
                        rectDiff.top = rectDiff.top + rect.top;
                        rectDiff.right = rectDiff.right + rect.left;
                        rectDiff.bottom = rectDiff.bottom + rect.top;

                        mBitmapPrevious.recycle();
                        mBitmapPrevious = mBitmapCurrent;

                        //display both rectangles
                        final Bitmap bitmapRect = imageUtils.getBitmapDiffRect(rect, rectDiff, mBitmapCurrent);
                        final boolean hasDiff = ImageUtils.hasDifference(rectDiff);

                        mHandlerMain.post(new Runnable() {
                            @Override
                            public void run() {
                                mImageViewDiff.setImageBitmap(bitmapRect);
                                if(hasDiff && ConstantsS.getPlaySoundEnabled()) {
                                    Ringtone ringtone = ConstantsS.getRingtone();
                                    if(ringtone != null && !ringtone.isPlaying()) {
                                        ringtone.play();
                                    }
                                }
                            }
                        });
                    }
                } else {
                    Rect rectDiff = imageUtils.getDifference(null, mBitmapPrevious, mBitmapCurrent, kSampleSize,
                            ConstantsS.getThresholdDifference());

                    mBitmapPrevious.recycle();
                    mBitmapPrevious = mBitmapCurrent;

                    final Bitmap bitmapRect = imageUtils.getBitmapDiffRect(rectDiff, mBitmapCurrent);
                    final boolean hasDiff = ImageUtils.hasDifference(rectDiff);

                    if(hasDiff) {recordPicture(mBitmapCurrent.copy(Bitmap.Config.ARGB_8888, false));}
                    mHandlerMain.post(new Runnable() {
                        @Override
                        public void run() {
                            mImageViewDiff.setImageBitmap(bitmapRect);
                            if(hasDiff && ConstantsS.getPlaySoundEnabled()) {
                                Ringtone ringtone = ConstantsS.getRingtone();
                                if(ringtone != null && !ringtone.isPlaying()) {
                                    ringtone.play();
                                }
                            }
                        }
                    });
                }

                if(mIsTextureViewDestroyed) {
                    mHandlerMain.removeCallbacks(mRunnableDiffPost);
                } else {
                    mHandlerMain.postDelayed(mRunnableDiffPost, UPDATE_INTERVAL);
                }
            }
        };

        mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
                if(mCamera == null) {return;}
                mIsTextureViewDestroyed = false;
                try {
                    mCamera.setPreviewTexture(surfaceTexture);
                    setCameraDisplayOrientation(CameraActivity.this, 0, mCamera);
                    mCamera.startPreview();
                    mHandlerMain.postDelayed(mRunnableDiffPost, UPDATE_INTERVAL);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                mIsTextureViewDestroyed = true;
                mHandlerMain.removeCallbacks(mRunnableDiffPost);
                if(mCamera != null) {
                    mCamera.stopPreview();
                    mCamera.release();
                }
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private boolean checkCameraHardware(Context context) {
        if(context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            return true;
        } else {
            return false;
        }
    }

    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open();
        } catch (Exception e){
            e.printStackTrace();
        }
        return c;
    }

    public static void setCameraDisplayOrientation(Activity activity, int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    private void recordPicture(final Bitmap aBitmap) {
        if(!ConstantsS.getRecordPictures()) {return;}
        if(mRecordIntervalMs > 0) {return;}
        mRecordIntervalMs = UPDATE_INTERVAL_PICTURE;

        mExecutorRecord.execute(new Runnable() {
            @Override
            public void run() {
                File pictureFile = CameraActivity.getFilePicture("jpg");
                if (pictureFile == null){
                    Log.i(TAG, "recordPitcture, Error creating file");
                    return;
                }
                try {
                    FileOutputStream fos = new FileOutputStream(pictureFile);
                    aBitmap.compress(Bitmap.CompressFormat.JPEG, 75, fos);
                    fos.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    aBitmap.recycle();
                }
            }
        });
    }

    public static File getFilePicture(String aFileExtension){
        if(!ConstantsS.isExternalStorageAvailable()) {return null;}

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "sentinel");

        if (!mediaStorageDir.exists()){
            if (!mediaStorageDir.mkdirs()){return null;}
        }

        long freeSpace = mediaStorageDir.getFreeSpace();
        if(freeSpace < MB) {
            Log.i(TAG, "getFilePicture, Low on disk storage, freeSpace=" + freeSpace + ", bytes");
            return null;
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File filePicture = new File(mediaStorageDir.getPath() + File.separator + timeStamp + "." + aFileExtension);
        return filePicture;
    }
}

