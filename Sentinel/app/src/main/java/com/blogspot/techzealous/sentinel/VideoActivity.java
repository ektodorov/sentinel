package com.blogspot.techzealous.sentinel;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.blogspot.techzealous.sentinel.utils.ConstantsS;
import com.blogspot.techzealous.sentinel.utils.ImageUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VideoActivity extends AppCompatActivity implements MediaPlayer.OnBufferingUpdateListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnVideoSizeChangedListener {

    private static final String TAG = "VideoActivity";
    private static final int kRequestCode = 300;
    private final int UPDATE_INTERVAL = 250;
    private int mSampleSize = 6;

    private Button mButtonSelect;
    private Button mButtonPlay;
    private Button mButtonStop;
    private TextureView mTextureView;
    private ImageView mImageViewDiff;

    private String mPathVideo;
    private MediaPlayer mMediaPlayer;
    private int mVideoWidth;
    private int mVideoHeight;
    private boolean mIsVideoSizeKnown;
    private boolean mIsVideoReadyToBePlayed;
    private SurfaceTexture mSurfaceTexture;
    private Handler mHandlerMain;
    private ExecutorService mExecutorDiff;
    private Runnable mRunnableDiffPost;
    private Runnable mRunnableDiff;
    private Bitmap mBitmapPrevious;
    private Bitmap mBitmapCurrent;
    private volatile boolean mIsTextureViewDestroyed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        mButtonSelect = (Button)findViewById(R.id.buttonSelectVideoActivity);
        mButtonPlay = (Button)findViewById(R.id.buttonPlayVideoActivity);
        mButtonStop = (Button)findViewById(R.id.buttonStopVideoActivity);
        mTextureView = (TextureView)findViewById(R.id.textureViewVideoActivity);
        mImageViewDiff = (ImageView)findViewById(R.id.imageViewDifferenceVideoActivity);

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
                    Point pointOffset = imageUtils.stabilizeFrame(mBitmapPrevious, mBitmapCurrent, mSampleSize);
                    Rect rect = imageUtils.getDifference(pointOffset, mBitmapPrevious, mBitmapCurrent, mSampleSize,
                            ConstantsS.getThresholdStabilization());

                    if (rect.left < 0 || rect.top < 0 || rect.right < 0 || rect.bottom < 0
                            || rect.right <= rect.left || rect.bottom <= rect.top) {
                        Rect rectDiff = imageUtils.getDifference(pointOffset, mBitmapPrevious, mBitmapCurrent, mSampleSize,
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
                        Bitmap bitmapPreviousTemp = Bitmap.createBitmap(mBitmapPrevious, rect.left, rect.top,
                                (rect.right - rect.left), (rect.bottom - rect.top));
                        Bitmap bitmapCurrentTemp = Bitmap.createBitmap(mBitmapCurrent, rect.left, rect.top,
                                (rect.right - rect.left), (rect.bottom - rect.top));
                        Rect rectDiff = imageUtils.getDifference(null, bitmapPreviousTemp, bitmapCurrentTemp, mSampleSize,
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
                    Rect rectDiff = imageUtils.getDifference(null, mBitmapPrevious, mBitmapCurrent, mSampleSize,
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
                }

                if(mIsTextureViewDestroyed) {
                    mHandlerMain.removeCallbacks(mRunnableDiffPost);
                } else {
                    mHandlerMain.postDelayed(mRunnableDiffPost, UPDATE_INTERVAL);
                }
            }
        };

        mButtonSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Build.VERSION.SDK_INT <= 19) {
                    Intent i = new Intent();
                    i.setType(ConstantsS.STR_MIME_TYPE_VIDEO);
                    i.setAction(Intent.ACTION_GET_CONTENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    startActivityForResult(i, kRequestCode);
                } else if (Build.VERSION.SDK_INT > 19) {
                    Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(intent, kRequestCode);
                }
            }
        });

        mButtonPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mPathVideo == null) {
                    Toast.makeText(VideoActivity.this, R.string.msg_novideoselected, Toast.LENGTH_SHORT).show();
                    return;
                }
                if(mMediaPlayer == null) {
                    videoPrepareMediaPlayer(mPathVideo, new Surface(mSurfaceTexture));
                } else if(!mMediaPlayer.isPlaying()) {
                    mMediaPlayer.start();
                }
            }
        });

        mButtonStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                videoStop();
            }
        });

        mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
                mIsTextureViewDestroyed = false;
                mSurfaceTexture = surfaceTexture;
//                mHandlerMain.postDelayed(mRunnableDiffPost, UPDATE_INTERVAL);
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {
                mSurfaceTexture = surfaceTexture;
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                mHandlerMain.removeCallbacks(mRunnableDiffPost);
                mIsTextureViewDestroyed = true;
                mSurfaceTexture = null;
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
                mSurfaceTexture = surfaceTexture;
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
        videoReleaseMediaPlayer();
        videoCleanUp();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        videoReleaseMediaPlayer();
        videoCleanUp();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == Activity.RESULT_OK) {
            if(kRequestCode == requestCode) {
                mPathVideo = ImageUtils.getPathFromURI(VideoActivity.this, data.getData());
            }
        }
    }

    private void videoStop() {
        mHandlerMain.removeCallbacks(mRunnableDiffPost);
        if(mMediaPlayer != null) {
            mMediaPlayer.stop();
            videoReleaseMediaPlayer();
        }
    }

    private void videoStartPlayback() {
        mMediaPlayer.start();
        mHandlerMain.postDelayed(mRunnableDiffPost, UPDATE_INTERVAL);
    }

    private void videoReleaseMediaPlayer() {
        if (mMediaPlayer != null) {
            if(mMediaPlayer.isPlaying()) {mMediaPlayer.stop();}
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    private void videoCleanUp() {
        mVideoWidth = 0;
        mVideoHeight = 0;
        mIsVideoReadyToBePlayed = false;
        mIsVideoSizeKnown = false;
    }

    private void videoPrepareMediaPlayer(String aPathVideo, Surface aSurface) {
        videoCleanUp();
        try {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setDataSource(aPathVideo);
            mMediaPlayer.setSurface(aSurface);
            mMediaPlayer.prepare();
            mMediaPlayer.setOnBufferingUpdateListener(this);
            mMediaPlayer.setOnCompletionListener(this);
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setOnVideoSizeChangedListener(this);
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        } catch (Exception e) {
            Log.e(TAG, "videoPrepareMediaPlayer, error=" + e.getMessage(), e);
            e.printStackTrace();
        }
    }

    //OnBufferingUpdateListener
    @Override
    public void onBufferingUpdate(MediaPlayer mediaPlayer, int percent) {}

    //OnCompletionListener
    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        videoStop();
    }

    //OnPreparedListener
    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        mIsVideoReadyToBePlayed = true;
        videoStartPlayback();
    }

    //OnVideoSizeChangedListener
    @Override
    public void onVideoSizeChanged(MediaPlayer mediaPlayer, int width, int height) {
        if (width == 0 || height == 0) {
            Log.e(TAG, "onVideoSizeChanged, invalid video size, width=" + width + ", height=" + height);
            return;
        }
        mIsVideoSizeKnown = true;
        mVideoWidth = width;
        mVideoHeight = height;
    }
}
