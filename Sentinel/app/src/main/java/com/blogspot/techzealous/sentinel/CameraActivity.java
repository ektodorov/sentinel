package com.blogspot.techzealous.sentinel;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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
import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CameraActivity extends AppCompatActivity {

    private static final String TAG = "CameraActivity";
    private static final int UPDATE_INTERVAL = 500;
    private static final int RECORD_INTERVAL = 250;//4 fps
    private static final int FPS = 1000 / RECORD_INTERVAL;
    private static final int MB = 1024 * 1024;
    private static final int kSampleSize = 16;

    private ImageView mImageViewDiff;
    private TextureView mTextureView;

    private Handler mHandlerMain;
    private Camera mCamera;
    private ExecutorService mExecutorDiff;
    private ExecutorService mExecutorRecord;
    private ScheduledExecutorService mScheduledExecutorRecord;
    private Future<?> mFutureRecordStop;
    private Runnable mRunnableDiffPost;
    private Runnable mRunnableDiff;
    private Runnable mRunnableRecordStop;
    private Bitmap mBitmapPrevious;
    private Bitmap mBitmapCurrent;
    private volatile boolean mIsTextureViewDestroyed;
    private volatile boolean mIsRecording;
    private FFmpeg mFFmpeg;
    private int mVideoSequence;
    private SimpleDateFormat mDateFormat;
    private Paint mPaintText;

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

        loadFFMpegBinary();
        mHandlerMain = new Handler(Looper.getMainLooper());
        mExecutorDiff = Executors.newSingleThreadExecutor();
        mExecutorRecord = Executors.newSingleThreadExecutor();
        mScheduledExecutorRecord = Executors.newSingleThreadScheduledExecutor();
        mDateFormat = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault());
        mPaintText = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaintText.setColor(Color.WHITE);
        mPaintText.setTextSize(12 * getResources().getDisplayMetrics().density);
        mRunnableDiffPost = new Runnable() {
            @Override
            public void run() {
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

                        if(hasDiff) {
                            //recordPicture(mBitmapCurrent.copy(Bitmap.Config.ARGB_8888, false));
                            recordStart(mTextureView);
                        }
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

                        if(hasDiff) {
                            //recordPicture(mBitmapCurrent.copy(Bitmap.Config.ARGB_8888, false));
                            recordStart(mTextureView);
                        }
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

                    if(hasDiff) {
                        //recordPicture(mBitmapCurrent.copy(Bitmap.Config.ARGB_8888, false));
                        recordStart(mTextureView);
                    }
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

        mRunnableRecordStop = new Runnable() {
            @Override
            public void run() {
                mIsRecording = false;
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

    private void recordStart(TextureView aTextureView) {
        Log.i(TAG, "recordStart, mIsRecording=" + mIsRecording);
        if(mIsRecording) {
            if(mFutureRecordStop != null) {mFutureRecordStop.cancel(false);}
            mFutureRecordStop = mScheduledExecutorRecord.schedule(mRunnableRecordStop, 2, TimeUnit.SECONDS);
            return;
        }

        mIsRecording = true;
        if(ConstantsS.getRecordPictures()) {
            recordPicture(aTextureView);
        } else if(ConstantsS.getRecordVideos()) {
            recordVideo(aTextureView);
        }
    }

    private void recordPicture(final TextureView aTextureView) {
        mExecutorRecord.execute(new Runnable() {
            @Override
            public void run() {
                Date date = null;
                String time = null;
                while(mIsRecording) {
                    File pictureFile = CameraActivity.getFilePicture(null, "jpg");
                    if (pictureFile == null){
                        Log.i(TAG, "recordPitcture, Error creating file");
                        return;
                    }
                    Bitmap bitmap = aTextureView.getBitmap().copy(Bitmap.Config.ARGB_8888, false);

                    date = new Date();
                    time = mDateFormat.format(date);
                    Canvas canvas = new Canvas(bitmap);
                    canvas.drawText(time, 10, 10, mPaintText);

                    try {
                        FileOutputStream fos = new FileOutputStream(pictureFile);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, fos);
                        fos.close();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        bitmap.recycle();
                    }

                    try {
                        Thread.sleep(RECORD_INTERVAL);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void recordVideo(final TextureView aTextureView) {
        Log.i(TAG, "recordVideo, 421");
        mExecutorRecord.execute(new Runnable() {
            @Override
            public void run() {
                int sequence = mVideoSequence;
                int index = 0;
                Date date = null;
                String time = null;
                while(mIsRecording) {
                    File pictureFile = CameraActivity.getFilePictureForVideo(String.valueOf(sequence),
                            String.valueOf(index), "jpg");
                    if (pictureFile == null){
                        Log.i(TAG, "recordVideo, Error creating file");
                        return;
                    }
                    Bitmap bitmap = aTextureView.getBitmap().copy(Bitmap.Config.ARGB_8888, false);

                    date = new Date();
                    time = mDateFormat.format(date);
                    Canvas canvas = new Canvas(bitmap);
                    canvas.drawText(time, 10, 10, mPaintText);

                    try {
                        FileOutputStream fos = new FileOutputStream(pictureFile);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, fos);
                        fos.close();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        bitmap.recycle();
                    }

                    try {
                        Thread.sleep(RECORD_INTERVAL);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    index++;
                }
                String dirForVideo = getDirForVideo(String.valueOf(sequence)).getPath();
                mVideoSequence++;

                String inputFileAbsolutePath = getDirForVideo(String.valueOf(sequence)).getPath()
                        + File.separator + "%d.jpg";
                String outputFileAbsolutePath = getFileVideo("mp4").getPath();
                Log.i(TAG, "inputFile=" + inputFileAbsolutePath + ", outputFile=" + outputFileAbsolutePath);
                String fps = String.valueOf(FPS);
                String[] command = {
                        "-y",//overwrite output file without asking
                        "-i",//input files
                        inputFileAbsolutePath,
                        "-s",//video output size
                        "640x480",
                        "-r",//frame rate
                        fps,
                        "-vcodec",//video codec
                        "mpeg4",
                        "-b:v",//video bitrate
                        "150k",
//                        "-b:a",//audio bitrate
//                        "48000",
//                        "-ac",//audio channels
//                        "2",
//                        "-ar",//sampling rate for audio stream
//                        "22050",
                        outputFileAbsolutePath};
                execFFmpegBinary(command, dirForVideo);
            }
        });
    }

    private void loadFFMpegBinary() {
        try {
            if (mFFmpeg == null) {
                mFFmpeg = FFmpeg.getInstance(this);
            }
            mFFmpeg.loadBinary(new LoadBinaryResponseHandler() {
                @Override
                public void onFailure() {
                    Log.i(TAG, "ffmpeg loadBinary, onFailure");
                }

                @Override
                public void onSuccess() {
                    Log.i(TAG, "ffmpeg loadBinary, onSuccess");
                }
            });
        } catch (FFmpegNotSupportedException e) {
            Log.d(TAG, "FFmpegNotSupportedException, e=" + e);
            e.printStackTrace();
            mFFmpeg = null;
        } catch (Exception e) {
            Log.d(TAG, "Exception, e=" + e);
            e.printStackTrace();
            mFFmpeg = null;
        }
    }

    private void execFFmpegBinary(final String[] command, final String dirForVideo) {
        try {
            mFFmpeg.execute(command, new ExecuteBinaryResponseHandler() {
                @Override
                public void onFailure(String s) {
                    Log.i(TAG, "execute, onFailure, s=" + s);
                }

                @Override
                public void onSuccess(String s) {
                    Log.i(TAG, "execute, onSuccess, s=" + s);
                }

                @Override
                public void onProgress(String s) {
                    Log.i(TAG, "execute, onProgress, s=" + s);
                }

                @Override
                public void onStart() {
                    Log.i(TAG, "execute, onStart, command=" + command);
                }

                @Override
                public void onFinish() {
                    Log.i(TAG, "execute, onFinish, command=" + command);
                    deleteDirectory(dirForVideo);
                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            e.printStackTrace();
        }
    }

    private static void deleteDirectory(String aDirectory) {
        File dir = new File(aDirectory);
        String[] files = dir.list();
        int count = files.length;
        for(int x = 0; x < count; x++) {
            File file = new File(dir, files[x]);
            file.delete();
        }
        dir.delete();
    }

    private static void freeUpSpace(File aDirectory, int aDeleteNumberOfFiles) {
        ArrayList<File> sortedFiles = new ArrayList<>();
        String[] files = aDirectory.list();
        for(int x = 0; x <files.length ; x++) {
            File file = new File(aDirectory.getPath(), files[x]);
            sortedFiles.add(file);
        }
        Collections.sort(sortedFiles, new Comparator<File>() {
            @Override
            public int compare(File file1, File file2) {
                long modified1 = file1.lastModified();
                long modified2 = file2.lastModified();
                if(modified1 > modified2) {
                    return 1;
                } else if(modified1 < modified2) {
                    return -1;
                }
                return 0;
            }
        });
        if(aDeleteNumberOfFiles > sortedFiles.size()) {
            aDeleteNumberOfFiles = sortedFiles.size();
        }
        for(int x = 0; x < aDeleteNumberOfFiles; x++) {
            File file = sortedFiles.get(x);
            file.delete();
        }
    }

    public static File getFilePicture(String aFileName, String aFileExtension){
        if(!ConstantsS.isExternalStorageAvailable()) {return null;}

        File mediaStorageDir = getDirForPictures();

        if (!mediaStorageDir.exists()){
            if (!mediaStorageDir.mkdirs()){return null;}
        }

        long freeSpace = mediaStorageDir.getFreeSpace();
        if(freeSpace < MB) {
            Log.i(TAG, "getFilePicture, Low on disk storage, freeSpace=" + freeSpace + ", bytes");
            freeUpSpace(mediaStorageDir, 9);
        }

        String timeStamp = aFileName;
        if(timeStamp != null) {
            timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        }
        File filePicture = new File(mediaStorageDir.getPath() + File.separator + timeStamp + "." + aFileExtension);
        return filePicture;
    }

    public static File getFilePictureForVideo(String aSequence, String aFileName, String aFileExtension){
        if(!ConstantsS.isExternalStorageAvailable()) {return null;}

        File mediaStorageDir = getDirForVideo(aSequence);

        if (!mediaStorageDir.exists()){
            if (!mediaStorageDir.mkdirs()){return null;}
        }

        long freeSpace = mediaStorageDir.getFreeSpace();
        if(freeSpace < MB) {
            Log.i(TAG, "getFilePicture, Low on disk storage, freeSpace=" + freeSpace + ", bytes");
            freeUpSpace(getDirForPictures(), 9);
        }

        File filePicture = new File(mediaStorageDir.getPath() + File.separator + aFileName + "." + aFileExtension);
        return filePicture;
    }

    public static File getDirForPictures() {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "sentinel");
        return mediaStorageDir;
    }

    public static File getDirForVideo(String aSequence) {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "sentinel" + aSequence);
        return mediaStorageDir;
    }

    public static File getFileVideo(String aFileExtension){
        if(!ConstantsS.isExternalStorageAvailable()) {return null;}

        File mediaStorageDir = getDirForPictures();

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

