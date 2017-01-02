package com.blogspot.techzealous.sentinel;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.blogspot.techzealous.sentinel.utils.ConstantsS;
import com.blogspot.techzealous.sentinel.utils.ImageUtils;

public class PictureActivity extends AppCompatActivity {

    private static final String TAG = "PictureActivity";
    private static final int kRequestCode1 = 100;
    private static final int kRequestCode2 = 200;
    private Button mButtonSelect1;
    private Button mButtonSelect2;
    private Button mButtonCompare;
    private TextView mTextViewSelect1;
    private TextView mTextViewSelect2;
    private ImageView mImageViewDiff;

    private String mPathPicture1;
    private String mPathPicture2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture);

        mButtonSelect1 = (Button)findViewById(R.id.buttonSelect1PictureActivity);
        mButtonSelect2 = (Button)findViewById(R.id.buttonSelect2PictureActivity);
        mButtonCompare = (Button)findViewById(R.id.buttonComparePictureActivity);
        mTextViewSelect1 = (TextView)findViewById(R.id.textViewSelect1PictureActivity);
        mTextViewSelect2 = (TextView)findViewById(R.id.textViewSelect2PictureActivity);
        mImageViewDiff = (ImageView)findViewById(R.id.imageViewDifferencePictureActivity);

        mButtonSelect1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Build.VERSION.SDK_INT <= 19) {
                    Intent i = new Intent();
                    i.setType(ConstantsS.STR_MIME_TYPE_IMAGE);
                    i.setAction(Intent.ACTION_GET_CONTENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    startActivityForResult(i, kRequestCode1);
                } else if (Build.VERSION.SDK_INT > 19) {
                    Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(intent, kRequestCode1);
                }
            }
        });

        mButtonSelect2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Build.VERSION.SDK_INT <= 19) {
                    Intent i = new Intent();
                    i.setType(ConstantsS.STR_MIME_TYPE_IMAGE);
                    i.setAction(Intent.ACTION_GET_CONTENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    startActivityForResult(i, kRequestCode2);
                } else if (Build.VERSION.SDK_INT > 19) {
                    Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(intent, kRequestCode2);
                }
            }
        });

        mButtonCompare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mPathPicture1 == null || mPathPicture2 == null) {return;}

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(mPathPicture1, options);

                options.inJustDecodeBounds = false;
                int sampleSize = ImageUtils.calculateSampleSizePowerOfTwo(options.outWidth, options.outHeight,
                        mImageViewDiff.getWidth(), mImageViewDiff.getWidth(), false);
                options.inSampleSize = sampleSize;

                Bitmap bitmap1 = BitmapFactory.decodeFile(mPathPicture1, options);
                Bitmap bitmap2 = BitmapFactory.decodeFile(mPathPicture2, options);
                ImageUtils imageUtils = new ImageUtils();
                Rect rectDiff = imageUtils.getDifference(null, bitmap1, bitmap2, 1, ConstantsS.getThresholdDifference());

                Bitmap bitmapCanvas = bitmap2.copy(Bitmap.Config.ARGB_8888, true);
                Canvas canvas = new Canvas(bitmapCanvas);
                Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                paint.setColor(Color.RED);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(1);
                canvas.drawRect(rectDiff, paint);
                mImageViewDiff.setImageBitmap(bitmapCanvas);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == Activity.RESULT_OK) {
            if(kRequestCode1 == requestCode) {
                mPathPicture1 = ImageUtils.getPathFromURI(PictureActivity.this, data.getData());
                mTextViewSelect1.setText(mPathPicture1);
            } else if(kRequestCode2 == requestCode) {
                mPathPicture2 = ImageUtils.getPathFromURI(PictureActivity.this, data.getData());
                mTextViewSelect2.setText(mPathPicture2);
            }
        }
    }
}
