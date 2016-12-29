package com.blogspot.techzealous.sentinel;

import android.app.Activity;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.blogspot.techzealous.sentinel.utils.ImageUtils;

public class VideoActivity extends AppCompatActivity implements MediaPlayer.OnBufferingUpdateListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnVideoSizeChangedListener {

    private static final String TAG = "VideoActivity";
    private static final int kRequestCode = 300;
    private Button mButtonSelect;
    private Button mButtonPlay;
    private Button mButtonStop;
    private TextureView mTextureView;

    private String mPathVideo;
    private MediaPlayer mMediaPlayer;
    private int mVideoWidth;
    private int mVideoHeight;
    private boolean mIsVideoSizeKnown;
    private boolean mIsVideoReadyToBePlayed;
    private SurfaceTexture mSurfaceTexture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        mButtonSelect = (Button)findViewById(R.id.buttonSelectVideoActivity);
        mButtonPlay = (Button)findViewById(R.id.buttonPlayVideoActivity);
        mButtonStop = (Button)findViewById(R.id.buttonStopVideoActivity);
        mTextureView = (TextureView)findViewById(R.id.textureViewVideoActivity);

        mButtonSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Build.VERSION.SDK_INT <= 19) {
                    Intent i = new Intent();
                    i.setType("video/*");
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
                    Toast.makeText(VideoActivity.this, "No video file is selected.", Toast.LENGTH_SHORT).show();
                    return;
                }
                videoPrepareMediaPlayer(mPathVideo, new Surface(mSurfaceTexture));
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
                Log.i(TAG, "onSurfaceTextureAvalable");
                mSurfaceTexture = surfaceTexture;
                //videoPrepareMediaPlayer(mPathVideo, new Surface(surfaceTexture));
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {
                mSurfaceTexture = surfaceTexture;
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                Log.i(TAG, "onSurfaceTextureDestroyed");
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
        videoStop();
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
        if(mMediaPlayer != null) {
            mMediaPlayer.stop();
            videoReleaseMediaPlayer();
        }
    }

    private void videoStartPlayback() {
        Log.i(TAG, "videoStartPlayback");
        mMediaPlayer.start();
    }

    private void videoReleaseMediaPlayer() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
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
    public void onCompletion(MediaPlayer mediaPlayer) {}

    //OnPreparedListener
    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        Log.d(TAG, "onPrepared");
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
