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
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.widget.ImageView;

import com.blogspot.techzealous.sentinel.utils.ConstantsS;
import com.blogspot.techzealous.sentinel.utils.ImageUtils;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraActivity extends AppCompatActivity {

    private static final String TAG = "CameraActivity";
    private final int UPDATE_INTERVAL = 250;
    private int mSampleSize = 6;

    private ImageView mImageViewDiff;
    private TextureView mTextureView;

    private Handler mHandlerMain;
    private Camera mCamera;
    private ExecutorService mExecutorDiff;
    private Runnable mRunnableDiffPost;
    private Runnable mRunnableDiff;
    private Bitmap mBitmapPrevious;
    private Bitmap mBitmapCurrent;
    private volatile boolean mIsTextureViewDestroyed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        getSupportActionBar().hide();
        mImageViewDiff = (ImageView)findViewById(R.id.imageViewCameraActivity);
        mTextureView = (TextureView)findViewById(R.id.textureViewCameraActivity);

        if(!checkCameraHardware(this)) {
            AlertDialog.Builder adb = new AlertDialog.Builder(this);
            adb.setTitle("No camera");
            adb.setMessage("This device does not have a camera");
            adb.setPositiveButton("OK", new DialogInterface.OnClickListener() {
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
            adb.setTitle("No camera");
            adb.setMessage("Error connecting to the camera");
            adb.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    finish();
                }
            });
            return;
        }

        mHandlerMain = new Handler(Looper.getMainLooper());
        mExecutorDiff = Executors.newSingleThreadExecutor();
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
                Log.i(TAG, "mExecutorDiff start");
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
                Point pointOffset = imageUtils.stabilizeFrame(mBitmapPrevious, mBitmapCurrent, mSampleSize);
                Rect rect = imageUtils.getDifference(pointOffset, mBitmapPrevious, mBitmapCurrent, mSampleSize,
                        ConstantsS.THRESHOLD_30);

                if(rect.left < 0 || rect.top < 0 || rect.right < 0 || rect.bottom < 0
                        || rect.right <= rect.left || rect.bottom <= rect.top) {
                    Rect rectDiff = imageUtils.getDifference(pointOffset, mBitmapPrevious, mBitmapCurrent, mSampleSize,
                            ConstantsS.THRESHOLD_15);

                    mBitmapPrevious.recycle();
                    mBitmapPrevious = mBitmapCurrent;

                    final Bitmap bitmapRect = imageUtils.getBitmapDiffRect(rectDiff, mBitmapCurrent);

                    mHandlerMain.post(new Runnable() {
                        @Override
                        public void run() {
                            mImageViewDiff.setImageBitmap(bitmapRect);
                        }
                    });
                } else {
                    Bitmap bitmapPreviousTemp = Bitmap.createBitmap(mBitmapPrevious, rect.left, rect.top,
                            (rect.right - rect.left), (rect.bottom - rect.top));
                    Bitmap bitmapCurrentTemp = Bitmap.createBitmap(mBitmapCurrent, rect.left, rect.top,
                            (rect.right - rect.left), (rect.bottom - rect.top));
                    Rect rectDiff = imageUtils.getDifference(null, bitmapPreviousTemp, bitmapCurrentTemp, mSampleSize,
                            ConstantsS.THRESHOLD_15);
                    rectDiff.left = rectDiff.left + rect.left;
                    rectDiff.top = rectDiff.top + rect.top;
                    rectDiff.right = rectDiff.right + rect.left;
                    rectDiff.bottom = rectDiff.bottom + rect.top;

                    mBitmapPrevious.recycle();
                    mBitmapPrevious = mBitmapCurrent;

                    //display both rectangles
                    //final Bitmap bitmapRect = imageUtils.getBitmapDiffRect(rect, rectDiff, mBitmapCurrent);
                    final Bitmap bitmapRect = imageUtils.getBitmapDiffRect(rect, rectDiff, mBitmapCurrent);

                    mHandlerMain.post(new Runnable() {
                        @Override
                        public void run() {
                            mImageViewDiff.setImageBitmap(bitmapRect);
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
                Log.i(TAG, "onSurfaceTextureAvalable");
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
                Log.i(TAG, "onSurfaceTextureDestroyed");
                if(mCamera != null) {return false;}
                mIsTextureViewDestroyed = true;
                mCamera.stopPreview();
                mCamera.release();
                mHandlerMain.removeCallbacks(mRunnableDiffPost);
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
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
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

}


//package com.blogspot.techzealous.sentinel;
//
//import android.app.Activity;
//import android.content.Context;
//import android.content.DialogInterface;
//import android.content.pm.PackageManager;
//import android.graphics.Bitmap;
//import android.graphics.Canvas;
//import android.graphics.Color;
//import android.graphics.Paint;
//import android.graphics.Rect;
//import android.graphics.SurfaceTexture;
//import android.hardware.Camera;
//import android.os.Bundle;
//import android.os.Handler;
//import android.os.Looper;
//import android.support.v7.app.AlertDialog;
//import android.support.v7.app.AppCompatActivity;
//import android.util.Log;
//import android.view.Surface;
//import android.view.TextureView;
//import android.widget.ImageView;
//
//import java.io.IOException;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
//import Catalano.Imaging.FastBitmap;
//import Catalano.Imaging.Filters.Difference;
//
//public class CameraActivity extends AppCompatActivity {
//
//    private static final String TAG = "CameraActivity";
//    private final int UPDATE_INTERVAL = 1000;
//
//    private ImageView mImageViewDiff;
//    private TextureView mTextureView;
//
//    private Handler mHandlerMain;
//    private Camera mCamera;
//    private ExecutorService mExecutorDiff;
//    private Runnable mRunnableDiffPost;
//    private Runnable mRunnableDiff;
//    private Bitmap mBitmapPrevious;
//    private Bitmap mBitmapCurrent;
//    private Rect mRectDiff;
//    private Rect mRectDiffInProgress;
//    private volatile boolean mIsTextureViewDestroyed;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_camera);
//
//        getSupportActionBar().hide();
//        mImageViewDiff = (ImageView)findViewById(R.id.imageViewCameraActivity);
//        mTextureView = (TextureView)findViewById(R.id.textureViewCameraActivity);
//
//        if(!checkCameraHardware(this)) {
//            AlertDialog.Builder adb = new AlertDialog.Builder(this);
//            adb.setTitle("No camera");
//            adb.setMessage("This device does not have a camera");
//            adb.setPositiveButton("OK", new DialogInterface.OnClickListener() {
//                @Override
//                public void onClick(DialogInterface dialogInterface, int i) {
//                    finish();
//                }
//            });
//            return;
//        }
//
//        mCamera = getCameraInstance();
//        if(mCamera == null) {
//            AlertDialog.Builder adb = new AlertDialog.Builder(this);
//            adb.setTitle("No camera");
//            adb.setMessage("Error connecting to the camera");
//            adb.setPositiveButton("OK", new DialogInterface.OnClickListener() {
//                @Override
//                public void onClick(DialogInterface dialogInterface, int i) {
//                    finish();
//                }
//            });
//            return;
//        }
//
//        mHandlerMain = new Handler(Looper.getMainLooper());
//        mExecutorDiff = Executors.newSingleThreadExecutor();
//        mRunnableDiffPost = new Runnable() {
//            @Override
//            public void run() {
//                mBitmapCurrent = mTextureView.getBitmap();
//                mExecutorDiff.execute(mRunnableDiff);
//            }
//        };
//
//        mRunnableDiff = new Runnable() {
//            @Override
//            public void run() {
//                Log.i(TAG, "mExecutorDiff start");
//                if(mBitmapCurrent == null) {
//                    if(mIsTextureViewDestroyed) {
//                        mHandlerMain.removeCallbacks(mRunnableDiffPost);
//                    } else {
//                        mHandlerMain.postDelayed(mRunnableDiffPost, UPDATE_INTERVAL);
//                    }
//                    return;
//                }
//                if(mBitmapPrevious == null) {
//                    mBitmapPrevious = mBitmapCurrent;
//                    if(mIsTextureViewDestroyed) {
//                        mHandlerMain.removeCallbacks(mRunnableDiffPost);
//                    } else {
//                        mHandlerMain.postDelayed(mRunnableDiffPost, UPDATE_INTERVAL);
//                    }
//                    return;
//                }
//
//                FastBitmap bitmapSource = new FastBitmap(mBitmapPrevious);
//                FastBitmap bitmapOverlay = new FastBitmap(mBitmapCurrent);
//                Difference diff = new Difference(bitmapOverlay);
//                diff.applyInPlace(bitmapSource);
//
//                final Bitmap bitmapResult = bitmapSource.toBitmap();
//                mBitmapPrevious.recycle();
//                mBitmapPrevious = mBitmapCurrent;
//
//                int width = bitmapResult.getWidth();
//                int height = bitmapResult.getHeight();
//                int minX = -1;//left
//                int minY = -1;//top
//                int maxX = -1;//right
//                int maxY = -1;//bottom
//                for(int y = 0; y < height; y++) {
//                    for (int x = 0; x < width; x++) {
//                        int pixel = bitmapResult.getPixel(x, y);
//                        //-16777216, RGBA 0, 0, 0, 255, 0%
//                        //-16119286, RGBA 10, 10, 10, 255, 4%
//                        //-15132391, RGBA, 25, 25, 25, 255, 10%
//                        //-14211289, RGBA, 39, 39, 39, 255, 15%
//                        //-13421773, RGBA, 51, 51, 51, 255, 20%
//                        //-12566464, RGBA, 64, 64, 64, 255, 25%
//                        //-11776948, RGBA, 76, 76, 76, 255, 30%
//                        //-10921639, RGBA, 89, 89, 89, 255, 35%
//                        //-10066330, RGBA, 102, 102, 102, 255, 40%
//                        //-9211021, RGBA, 115, 115, 115, 255, 45%
//                        //-8355712, RGBA, 128, 128, 128. 255, 50%
//                        //-7566196, RGBA, 140, 140, 140, 255, 55%
//                        //-6710887, RGBA, 153, 153, 153, 255, 60%
//                        //-5855578, RGBA, 166, 166, 166, 255, 65%
//                        //-5000269, RGBA, 179, 179, 179, 255, 70%
//                        if(pixel > -15132391) {
////                            Log.i("Pixel", "mRunnableDiff, x=" + x + ", y=" + y + ", pixel=" + pixel);
////                            Log.i("Pixel", "mRunnableDiff, pixel=" + pixel +
////                                    ", red=" + Color.red(pixel) +
////                                    ", green=" + Color.green(pixel) +
////                                    ", blue=" + Color.blue(pixel) +
////                                    ", alpha=" + Color.alpha(pixel));
//                            if(minX < 0) {minX = x;}
//                            if(minY < 0) {minY = y;}
//                            if(minX > x) {minX = x;}
//                            if(minY > y) {minY = y;}
//
//                            if(maxX < x) {maxX = x;}
//                            if(maxY < y) {maxY = y;}
//                        }
//                    }
//                }
//                Log.i(TAG, "mExecutorDiff, width=" + width + ", height=" + height +
//                        ", minX=" + minX + ", minY=" + minY + ", maxX=" + maxX + ", maxY=" + maxY);
//                int color = Color.argb(255, 39, 39, 39);
//                Log.i(TAG, "color=" + color);
//
//                Bitmap bitmapCanvasTemp = null;
//                if(minX == -1 || minY == -1) {
//                    bitmapCanvasTemp = Bitmap.createBitmap(bitmapResult.getWidth(), bitmapResult.getHeight(), Bitmap.Config.ARGB_8888);
//                } else {
//                    Rect rect = new Rect(minX, minY, maxX, maxY);
//                    bitmapCanvasTemp = Bitmap.createBitmap(bitmapResult.getWidth(), bitmapResult.getHeight(), Bitmap.Config.ARGB_8888);
//                    Canvas canvasRect = new Canvas(bitmapCanvasTemp);
//                    Paint paintRect = new Paint(Paint.ANTI_ALIAS_FLAG);
//                    paintRect.setColor(Color.RED);
//                    paintRect.setStrokeWidth(2);
//                    paintRect.setStyle(Paint.Style.STROKE);
//                    canvasRect.drawRect(rect, paintRect);
//                }
//
//                final Bitmap bitmapCanvas = bitmapCanvasTemp;
//                mHandlerMain.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        mImageViewDiff.setImageBitmap(bitmapCanvas);
//                        //mImageViewDiff.setImageBitmap(bitmapResult);
//                    }
//                });
//
//                if(mIsTextureViewDestroyed) {
//                    mHandlerMain.removeCallbacks(mRunnableDiffPost);
//                } else {
//                    mHandlerMain.postDelayed(mRunnableDiffPost, UPDATE_INTERVAL);
//                }
//            }
//        };
//
//        mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
//            @Override
//            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
//                Log.i(TAG, "onSurfaceTextureAvalable");
//                //if(mCamera == null) {return;}
//                mIsTextureViewDestroyed = false;
//                try {
//                    mCamera.setPreviewTexture(surfaceTexture);
//                    setCameraDisplayOrientation(CameraActivity.this, 0, mCamera);
//                    mCamera.startPreview();
//                    mHandlerMain.postDelayed(mRunnableDiffPost, UPDATE_INTERVAL);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//
//            @Override
//            public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {
//
//            }
//
//            @Override
//            public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
//                Log.i(TAG, "onSurfaceTextureDestroyed");
//                //if(mCamera != null) {return false;}
//                mIsTextureViewDestroyed = true;
//                mCamera.stopPreview();
//                mCamera.release();
//                mHandlerMain.removeCallbacks(mRunnableDiffPost);
//                return true;
//            }
//
//            @Override
//            public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
//
//            }
//        });
//    }
//
//    @Override
//    protected void onResume() {
//        super.onResume();
//    }
//
//    @Override
//    protected void onPause() {
//        super.onPause();
//    }
//
//    private boolean checkCameraHardware(Context context) {
//        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
//            return true;
//        } else {
//            return false;
//        }
//    }
//
//    public static Camera getCameraInstance(){
//        Camera c = null;
//        try {
//            c = Camera.open();
//        } catch (Exception e){
//            e.printStackTrace();
//        }
//        return c;
//    }
//
//    public static void setCameraDisplayOrientation(Activity activity, int cameraId, android.hardware.Camera camera) {
//        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
//        android.hardware.Camera.getCameraInfo(cameraId, info);
//        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
//        int degrees = 0;
//        switch (rotation) {
//            case Surface.ROTATION_0: degrees = 0; break;
//            case Surface.ROTATION_90: degrees = 90; break;
//            case Surface.ROTATION_180: degrees = 180; break;
//            case Surface.ROTATION_270: degrees = 270; break;
//        }
//
//        int result;
//        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
//            result = (info.orientation + degrees) % 360;
//            result = (360 - result) % 360;  // compensate the mirror
//        } else {  // back-facing
//            result = (info.orientation - degrees + 360) % 360;
//        }
//        camera.setDisplayOrientation(result);
//    }
//
//}
