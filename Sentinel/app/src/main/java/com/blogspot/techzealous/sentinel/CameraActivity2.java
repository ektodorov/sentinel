package com.blogspot.techzealous.sentinel;

import android.Manifest;
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
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.media.Ringtone;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.blogspot.techzealous.sentinel.utils.ConstantsS;
import com.blogspot.techzealous.sentinel.utils.ImageUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CameraActivity2 extends AppCompatActivity {

   private static final String TAG = "CameraActivity";
   private static final int UPDATE_DIFF_INTERVAL_MS_WHILERECORDING = 10000;
   private static final int UPDATE_DIFF_INTERVAL_MS_NORMAL = 2000;
   private static int UPDATE_DIFF_INTERVAL_MS = UPDATE_DIFF_INTERVAL_MS_NORMAL;
   private static final int RECORD_PICTURE_INTERVAL_MS = 500;//2 pictures per second
   private static final int RECORD_VIDEO_INTERVAL_SECONDS = 15;//seconds
   private static final int FPS = 15;
   private static final int MB = 1024 * 1024;
   private static final int kSampleSize = 16;
   private static final int REQUEST_CODE_STORAGE = 1234;
   private static final int REQUEST_CODE_CAMERA = 2345;

   private ImageView mImageViewDiff;
   private TextureView mTextureView;
   private Button mButtonStart;

   private CameraDevice mCameraDevice;
   private ImageReader imageReader;
   private MediaRecorder mediaRecorder;
   private boolean isMediaRecorderPrepared = false;
   private String mCameraId;
   private Handler backgroundHandler;
   private HandlerThread backgroundHandlerThread;
   protected CaptureRequest.Builder captureRequestBuilder;
   protected CameraCaptureSession cameraCaptureSessions;

   private Handler mHandlerMain;
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
   private SimpleDateFormat mDateFormat;
   private SimpleDateFormat mDateFormatFile;
   private Paint mPaintText;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_camera);

      final WeakReference<CameraActivity2> weakThis = new WeakReference<>(this);
      ActionBar actionBar = getSupportActionBar();
      if(actionBar != null) {actionBar.hide();}
      mImageViewDiff = findViewById(R.id.imageViewCameraActivity);
      mTextureView = findViewById(R.id.textureViewCameraActivity);
      mButtonStart = findViewById(R.id.buttonStart);

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

      mHandlerMain = new Handler(Looper.getMainLooper());
      mExecutorDiff = Executors.newSingleThreadExecutor();
      mExecutorRecord = Executors.newSingleThreadExecutor();
      mScheduledExecutorRecord = Executors.newSingleThreadScheduledExecutor();
      mDateFormat = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault());
      mDateFormatFile = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.getDefault());
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
                  mHandlerMain.postDelayed(mRunnableDiffPost, UPDATE_DIFF_INTERVAL_MS);
               }
               return;
            }
            if(mBitmapPrevious == null) {
               mBitmapPrevious = mBitmapCurrent;
               if(mIsTextureViewDestroyed) {
                  mHandlerMain.removeCallbacks(mRunnableDiffPost);
               } else {
                  mHandlerMain.postDelayed(mRunnableDiffPost, UPDATE_DIFF_INTERVAL_MS);
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
                  //final Bitmap bitmapRect = imageUtils.getBitmapDiffRect(rect, rectDiff, mBitmapCurrent);
                  //display only one rectangle
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
               mHandlerMain.postDelayed(mRunnableDiffPost, UPDATE_DIFF_INTERVAL_MS);
            }
         }
      };

      mRunnableRecordStop = new Runnable() {
         @Override
         public void run() {
            CameraActivity2 strongThis = weakThis.get();
            if(strongThis == null) {
               return;
            }
            strongThis.mHandlerMain.post(new Runnable() {
               @Override
               public void run() {
                  CameraActivity2 strongThis = weakThis.get();
                  if(strongThis == null) {
                     return;
                  }
                  strongThis.recordStop();
               }
            });
         }
      };

      mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
         @Override
         public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
            CameraActivity2 strongThis = weakThis.get();
            if(strongThis == null) {
               return;
            }
            strongThis.mIsTextureViewDestroyed = false;
            strongThis.openCamera();
         }

         @Override
         public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

         }

         @Override
         public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            CameraActivity2 strongThis = weakThis.get();
            if(strongThis == null) {
               return false;
            }
            strongThis.mIsTextureViewDestroyed = true;
            strongThis.mHandlerMain.removeCallbacks(strongThis.mRunnableDiffPost);
            strongThis.closeCamera();
            return true;
         }

         @Override
         public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

         }
      });

      mButtonStart.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View view) {
            if(mIsRecording) {
               mButtonStart.setText("Start");
               recordVideoStop(true);
               stopRecordingPreview();
               initRecorder();
               createCameraPreview();

            } else {
               mButtonStart.setText("Stop");
               recordVideo();
            }
         }
      });
   }

   @Override
   protected void onResume() {
      super.onResume();
      if(backgroundHandlerThread == null) {
         backgroundHandlerThread = new HandlerThread("videocapture");
         backgroundHandlerThread.start();
         backgroundHandler = new Handler(backgroundHandlerThread.getLooper());
      }
   }

   @Override
   protected void onPause() {
      super.onPause();
      closeCamera();
      try {
         backgroundHandlerThread.quitSafely();
         backgroundHandlerThread.join();
         backgroundHandlerThread = null;
      } catch (InterruptedException e) {
         e.printStackTrace();
      }
   }

   @Override
   public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
      super.onRequestPermissionsResult(requestCode, permissions, grantResults);
      if(requestCode == REQUEST_CODE_STORAGE) {

      } else if(requestCode == REQUEST_CODE_CAMERA) {

      }
   }

   private boolean checkCameraHardware(Context context) {
      if(context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
         return true;
      } else {
         return false;
      }
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
      if(mIsRecording) {
         if(mFutureRecordStop != null) {mFutureRecordStop.cancel(false);}
         mFutureRecordStop = mScheduledExecutorRecord.schedule(mRunnableRecordStop,
                 RECORD_VIDEO_INTERVAL_SECONDS,
                 TimeUnit.SECONDS);
         return;
      }

      if(ConstantsS.getRecordPictures()) {
         recordPicture(aTextureView);
      } else if(ConstantsS.getRecordVideos()) {
         recordVideo();
      }
   }

   private void recordStop() {
      boolean wasRecording = mIsRecording;
      mIsRecording = false;
      if(wasRecording) {
         recordVideoStop(wasRecording);
      }
   }

   private void recordPicture(final TextureView aTextureView) {
      mExecutorRecord.execute(new Runnable() {
         @Override
         public void run() {
            Date date = null;
            String time = null;
            while(mIsRecording) {
               File pictureFile = CameraActivity2.getFilePicture(null, "jpg");
               if (pictureFile == null){
                  return;
               }
               Bitmap bitmapTexture = aTextureView.getBitmap();
               if(bitmapTexture == null) {
                  return;
               }
               Bitmap bitmap = bitmapTexture.copy(Bitmap.Config.ARGB_8888, true);

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
                  Thread.sleep(RECORD_PICTURE_INTERVAL_MS);
               } catch (InterruptedException e) {
                  e.printStackTrace();
               }
            }
         }
      });
   }

   private void recordVideo() {
      if(mCameraDevice == null || mediaRecorder == null || mIsRecording || !isMediaRecorderPrepared) {
         return;
      }
      try {
         mediaRecorder.start();
         mIsRecording = true;
         UPDATE_DIFF_INTERVAL_MS = UPDATE_DIFF_INTERVAL_MS_WHILERECORDING;
      } catch (IllegalStateException ex) {
         ex.printStackTrace();
      }
   }

   private void recordVideoStop(boolean wasRecording) {
      if(mFutureRecordStop != null) {mFutureRecordStop.cancel(false);}
      if(wasRecording && mediaRecorder != null && isMediaRecorderPrepared) {
         mediaRecorder.stop();
         mediaRecorder = null;
         isMediaRecorderPrepared = false;

         stopRecordingPreview();
         initRecorder();
         createCameraPreview();
         UPDATE_DIFF_INTERVAL_MS = UPDATE_DIFF_INTERVAL_MS_NORMAL;
      }
   }

   private void openCamera() {
      if(mCameraDevice != null) {
         return;
      }
      CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
      try {
         mCameraId = manager.getCameraIdList()[0];
         //CameraCharacteristics characteristics = manager.getCameraCharacteristics(mCameraId);
         //StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
         //imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
         if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_CAMERA);
            return;
         }
         final WeakReference<CameraActivity2> weakThis = new WeakReference<>(this);
         manager.openCamera(mCameraId, new CameraDevice.StateCallback() {
            @Override
            public void onOpened(@NonNull CameraDevice cameraDevice) {
               CameraActivity2 strongThis = weakThis.get();
               if(strongThis == null) {
                  return;
               }
               strongThis.mCameraDevice = cameraDevice;
               strongThis.mHandlerMain.postDelayed(new Runnable() {
                  @Override
                  public void run() {
                     CameraActivity2 strongThis = weakThis.get();
                     if(strongThis == null) {
                        return;
                     }
                     strongThis.initRecorder();
                     strongThis.createCameraPreview();
                  }
               }, 500);
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice cameraDevice) {
               CameraActivity2 strongThis = weakThis.get();
               strongThis.closeCamera();
               strongThis.displayCameraError();
            }

            @Override
            public void onError(@NonNull CameraDevice cameraDevice, int i) {
               CameraActivity2 strongThis = weakThis.get();
               strongThis.closeCamera();
               strongThis.displayCameraError();
            }
         }, null);
      } catch (CameraAccessException e) {
         e.printStackTrace();
      }
   }

   protected void createCameraPreview() {
      try {
         CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
         CameraCharacteristics characteristics = manager.getCameraCharacteristics(mCameraDevice.getId());
         Size[] sizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getHighSpeedVideoSizes();
         if(sizes != null) {
            for(Size size : sizes) {
               Log.i(TAG, "CameraActivity2, 606, createCameraPreview size=" + size);
            }
         }

         SurfaceTexture texture = mTextureView.getSurfaceTexture();
         texture.setDefaultBufferSize(mTextureView.getWidth(), mTextureView.getHeight());
         Surface surface = new Surface(texture);
         Surface recordingSurface = null;
         if(mediaRecorder != null) {
            recordingSurface = mediaRecorder.getSurface();
         }
         captureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
         captureRequestBuilder.addTarget(surface);
         if(recordingSurface != null) {
            captureRequestBuilder.addTarget(recordingSurface);
         }
         List<Surface> surfaceList = new ArrayList<>();
         surfaceList.add(surface);
         if(recordingSurface != null) {
            surfaceList.add(recordingSurface);
         }
         final WeakReference<CameraActivity2> weakThis = new WeakReference<>(this);
         mCameraDevice.createCaptureSession(surfaceList, new CameraCaptureSession.StateCallback(){
            @Override
            public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
               CameraActivity2 strongThis = weakThis.get();
               if(strongThis == null) {
                  return;
               }
               if (null == strongThis.mCameraDevice) {
                  return;
               }
               strongThis.cameraCaptureSessions = cameraCaptureSession;
//               recorder.start();
//               recording = true;
               updatePreview();
               strongThis.mHandlerMain.removeCallbacks(strongThis.mRunnableDiffPost);
               strongThis.mHandlerMain.postDelayed(strongThis.mRunnableDiffPost, CameraActivity2.UPDATE_DIFF_INTERVAL_MS);
            }
            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
               Log.i(TAG, "CameraActivity2, 620, onConfigureFailed");
            }
         }, null);
      } catch (CameraAccessException e) {
         e.printStackTrace();
      }
   }

   protected void updatePreview() {
      captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
      try {
         cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, backgroundHandler);
      } catch (CameraAccessException e) {
         e.printStackTrace();
      }
   }

   private void stopRecordingPreview() {
      if(cameraCaptureSessions != null) {
         try {
            cameraCaptureSessions.stopRepeating();
            cameraCaptureSessions.abortCaptures();
            cameraCaptureSessions.close();
            cameraCaptureSessions = null;
         } catch (CameraAccessException e) {
            e.printStackTrace();
         }
      }
   }

   private void closeCamera() {
      recordStop();
      stopRecordingPreview();
      if (null != mCameraDevice) {
         mCameraDevice.close();
         mCameraDevice = null;
      }
      if (null != imageReader) {
         imageReader.close();
         imageReader = null;
      }
   }

   private void displayCameraError() {
      android.app.AlertDialog.Builder adb = new android.app.AlertDialog.Builder(this);
      adb.setTitle(R.string.no_camera);
      adb.setMessage(R.string.msg_errorcamera);
      adb.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
         @Override
         public void onClick(DialogInterface dialogInterface, int i) {
            finish();
         }
      });
   }

   private boolean checkPermission() {
      if(ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
         requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_STORAGE);
         return false;
      }
      if(ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
         requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA}, REQUEST_CODE_CAMERA);
         return false;
      }

      return true;
   }

   private void initRecorder() {
      if(!ConstantsS.getRecordVideos()) {
         return;
      }
      File file = getFilePictureForVideo(mDateFormatFile.format(new Date()), "mp4");
      Log.i(TAG, "initRecorder, 662, file=" + file);

//      recorder = new MediaRecorder();
//      recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
//      recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
//      CamcorderProfile cpHigh = CamcorderProfile.get(CamcorderProfile.QUALITY_480P);
//      recorder.setProfile(cpHigh);
//      recorder.setOutputFile(file.getAbsolutePath());

      mediaRecorder = new MediaRecorder();
      mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
      mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
      mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
      mediaRecorder.setOutputFile(file.getAbsolutePath());
      mediaRecorder.setVideoEncodingBitRate(1600 * 1000);
      mediaRecorder.setVideoFrameRate(FPS);
      //800, 600
      //1280, 720
      mediaRecorder.setVideoSize(1280, 720);
      mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
      mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

      try {
         mediaRecorder.prepare();
      } catch (IllegalStateException e) {
         e.printStackTrace();
      } catch (IOException e) {
         e.printStackTrace();
      }
      isMediaRecorderPrepared = true;
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

   public static File getFilePictureForVideo(String aFileName, String aFileExtension){
      if(!ConstantsS.isExternalStorageAvailable()) {return null;}

      File mediaStorageDir = getDirForVideo();

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

   public static File getDirForVideo() {
      File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
              Environment.DIRECTORY_PICTURES), "sentinel");
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