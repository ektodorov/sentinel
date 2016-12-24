package com.blogspot.techzealous.sentinel.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;

import Catalano.Imaging.FastBitmap;
import Catalano.Imaging.Filters.Difference;

public class ImageUtils {

    private static final String TAG = "ImageUtils";

    /**
     * Stabilizes the viewport. Returns a point of offset with which to move the current frame so that it is aligned with the content
     * of the previous frame.
     * @param aBitmapPrevious bitmap from the previous frame
     * @param aBitmapCurrent bitmap from the current frame
     * @param aSampleSize sample to shrink the bitmaps before doing the stabilization calculations.
     * @return Point point of offset, which is multiplied with the sample size, so that it can be used with the original
     *      dimensions of the frame.
     */
    public Point stabilizeFrame(Bitmap aBitmapPrevious, Bitmap aBitmapCurrent, int aSampleSize) {
        int width = (int)(aBitmapCurrent.getWidth() / aSampleSize);
        int height = (int)(aBitmapCurrent.getHeight() / aSampleSize);
        Bitmap bitmapPrevious = Bitmap.createScaledBitmap(aBitmapPrevious, width, height, false);
        Bitmap bitmapCurrent = Bitmap.createScaledBitmap(aBitmapCurrent, width, height, false);
        Log.i(TAG, "imageStabilization, width=" + width + ", height=" + height);

        int width20 = bitmapPrevious.getWidth() * 10/100;
        int height20 = bitmapPrevious.getHeight() * 10/100;
        int xOrigin20 = (bitmapPrevious.getWidth() / 2) - (width20 / 2);
        int yOrigin20 = (bitmapPrevious.getHeight() / 2) - (height20 / 2);
        int xOriginCurrent = (bitmapCurrent.getWidth() / 2) - width20;
        int yOriginCurrent = (bitmapCurrent.getHeight() / 2) - height20;
        Bitmap bitmapPrev = Bitmap.createBitmap(bitmapPrevious, xOrigin20, yOrigin20, width20, height20);
        //Log.i(TAG, "imageStabilization, width20=" + width20 + ", height20=" + height20);

        long minDiff = Long.MAX_VALUE;
        int xCurrentMin = 0;
        int yCurrentMin = 0;
        int xCurrentMax = xOriginCurrent + (width20 * 2);
        int yCurrentMax = yOriginCurrent + (height20 * 2);
        //Log.i(TAG, "imageStabilization, xOriginCurrent=" + xOriginCurrent + ", yOriginCurrent=" + yOriginCurrent +
        //    ", xCurrentMax=" + xCurrentMax + ", yCurrentMax=" + yCurrentMax);
        long timeStart = System.currentTimeMillis();
        for(int y = yOriginCurrent; y < yCurrentMax; y++) {
            //Log.i(TAG, "imageStabilization, y=" + y);
            for (int x = xOriginCurrent; x < xCurrentMax; x++) {
                Bitmap bitmapCur = Bitmap.createBitmap(bitmapCurrent, x, y, width20, height20);

                FastBitmap bitmapPrevSource = new FastBitmap(bitmapPrev);
                FastBitmap bitmapCurrentOverlay = new FastBitmap(bitmapCur);
                Difference diff20 = new Difference(bitmapPrevSource);
                diff20.applyInPlace(bitmapCurrentOverlay);
                Bitmap bitmapResult20 = bitmapCurrentOverlay.toBitmap();
                long colorSum = (long)getColorSum(bitmapResult20);
                //Log.i(TAG, "imageStabilization, x=" + x + ", y=" + y + ", colorSum=" + colorSum + ", minDiff=" + minDiff);
                if(minDiff > colorSum) {
                    minDiff = colorSum;
                    xCurrentMin = x;
                    yCurrentMin = y;
                    //Log.i(TAG, "imageStabilization, x=" + x +  ", y=" + y + ", minDiff=" + minDiff +
                    //    ", xCurrentMin=" + x + ", yCurrentMin=" + y);
                }
            }
        }
        Log.i(TAG, "imageStabilization, time=" + (System.currentTimeMillis() - timeStart) + "ms");

        int offsetX = xOrigin20 - xCurrentMin;
        int offsetY = yOrigin20 - yCurrentMin;
//        Log.i(TAG, "imageStabilization, xCurrentMin=" + xCurrentMin + ", yCurrentMin=" + yCurrentMin +
//                ", offsetX=" + offsetX + ", offsetY=" + offsetY + ", minDiff=" + minDiff);
        return new Point(offsetX, offsetY);
    }

    /**
     *
     * @param aPointStabilizationOffset point of offset from the previous frame. Calculated with stabilizeFrame
     * @param aBitmapPrev bitmap from the previous frame
     * @param aBitmapCurrent bitmap from the current frame
     * @param aSampleSize sample size to shrink the bitmaps before doing the difference calculations.
     * @param aThreshold do not detect difference if the difference between the images is less than:
        -16777216, RGBA 0, 0, 0, 255, 0%<br>
        -16119286, RGBA 10, 10, 10, 255, 4%<br>
        -15132391, RGBA, 25, 25, 25, 255, 10%<br>
        -14211289, RGBA, 39, 39, 39, 255, 15%<br>
        -13421773, RGBA, 51, 51, 51, 255, 20%<br>
        -12566464, RGBA, 64, 64, 64, 255, 25%<br>
        -11776948, RGBA, 76, 76, 76, 255, 30%<br>
        -10921639, RGBA, 89, 89, 89, 255, 35%<br>
        -10066330, RGBA, 102, 102, 102, 255, 40%<br>
        -9211021, RGBA, 115, 115, 115, 255, 45%<br>
        -8355712, RGBA, 128, 128, 128. 255, 50%<br>
        -7566196, RGBA, 140, 140, 140, 255, 55%<br>
        -6710887, RGBA, 153, 153, 153, 255, 60%<br>
        -5855578, RGBA, 166, 166, 166, 255, 65%<br>
        -5000269, RGBA, 179, 179, 179, 255, 70%<br>
     * @return Rect - rectangle of the area that is different between the two images
     */
    public Rect getDifference(Point aPointStabilizationOffset, Bitmap aBitmapPrev, Bitmap aBitmapCurrent,
            int aSampleSize, int aThreshold)
    {
        long timeStart = System.currentTimeMillis();
        Bitmap bitmapPrevious = null;
        Bitmap bitmapCurrent = null;
        int widthScaled= aBitmapCurrent.getWidth() / aSampleSize;
        int heightScaled = aBitmapCurrent.getHeight() / aSampleSize;
        if(aPointStabilizationOffset == null) {
            bitmapPrevious = Bitmap.createScaledBitmap(aBitmapPrev, widthScaled, heightScaled, false);
            bitmapCurrent = Bitmap.createScaledBitmap(aBitmapCurrent, widthScaled, heightScaled, false);
        } else {
            Bitmap bitmapPreviousScaled = Bitmap.createScaledBitmap(aBitmapPrev, widthScaled, heightScaled, false);
            Bitmap bitmapCurrentScaled = Bitmap.createScaledBitmap(aBitmapCurrent, widthScaled, heightScaled, false);

            int offsetX = aPointStabilizationOffset.x;
            int offsetY = aPointStabilizationOffset.y;
            int xCurrent = 0;
            int yCurrent = 0;
            int xPrev = 0;
            int yPrev = 0;
            int width = 0;
            int height = 0;
            if(offsetX > 0) {
                xCurrent = 0;
                xPrev = offsetX;
                width = (widthScaled) - offsetX;
            } else {
                xCurrent = Math.abs(offsetX);
                xPrev = 0;
                width = widthScaled - Math.abs(offsetX);
            }

            if(offsetY > 0) {
                yCurrent = 0;
                yPrev = offsetY;
                height = heightScaled - offsetY;
            } else {
                yCurrent = Math.abs(offsetY);
                yPrev = 0;
                height = heightScaled - Math.abs(offsetY);
            }

            //Log.i(TAG, "showDifference, xPrev=" + xPrev + ", yPrev=" + yPrev +
            //    ", xCurrent=" + xCurrent + ", yCurrent=" + yCurrent + ", width=" + width + ", height=" + height);

            bitmapPrevious = Bitmap.createBitmap(bitmapPreviousScaled, xPrev, yPrev, width, height);
            bitmapCurrent = Bitmap.createBitmap(bitmapCurrentScaled, xCurrent, yCurrent, width, height);
        }

        FastBitmap bitmapSource = new FastBitmap(bitmapPrevious);
        FastBitmap bitmapOverlay = new FastBitmap(bitmapCurrent);
        Difference diff = new Difference(bitmapOverlay);
        diff.applyInPlace(bitmapSource);

        Bitmap bitmapResult = bitmapSource.toBitmap();

        int width = bitmapResult.getWidth();
        int height = bitmapResult.getHeight();
        int minX = -1;//left
        int minY = -1;//top
        int maxX = -1;//right
        int maxY = -1;//bottom
        for(int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = bitmapResult.getPixel(x, y);
                if(pixel > aThreshold) {
                    if(minX < 0) {minX = x;}
                    if(minY < 0) {minY = y;}
                    if(minX > x) {minX = x;}
                    if(minY > y) {minY = y;}

                    if(maxX < x) {maxX = x;}
                    if(maxY < y) {maxY = y;}
                }
            }
        }

        minX = (minX * aSampleSize);
        minY = (minY * aSampleSize);
        maxX = (maxX * aSampleSize);
        maxY = (maxY * aSampleSize);
        //Log.i(TAG, "showDifference, minX=" + minX + ", minY=" + minY + ", maxX=" + maxX + ", maxY=" + maxY);
        Rect rect = new Rect(minX, minY, maxX, maxY);
        Log.i(TAG, "getDifference, time=" + (System.currentTimeMillis() - timeStart) + "ms");
        return rect;
    }

    /**
     * Creates a bitmap with the size of the passed in bitmap,
     * containing colored rectangle with the dimensions of the passed in rectangle.
     * @param aRectDiff rectangle to be drawn
     * @param aBitmapCurrent bitmap to be used for dimensions of the new bitmap
     * @return Bitmap bitmap with size of aBitmapCurrent containing a drawing of aRectDiff
     */
    public Bitmap getBitmapDiffRect(Rect aRectDiff, Bitmap aBitmapCurrent) {
//        aRectDiff.left = (aRectDiff.left / aSampleSize);
//        aRectDiff.top = (aRectDiff.top / aSampleSize);
//        aRectDiff.right = (aRectDiff.right / aSampleSize);
//        aRectDiff.bottom = (aRectDiff.bottom / aSampleSize);
//        Bitmap bitmapCanvas = Bitmap.createBitmap((aBitmapCurrent.getWidth() / aSampleSize),
//                (aBitmapCurrent.getHeight() / aSampleSize), Bitmap.Config.ARGB_8888);
        Bitmap bitmapCanvas = Bitmap.createBitmap(aBitmapCurrent.getWidth(), aBitmapCurrent.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvasRect = new Canvas(bitmapCanvas);
        Paint paintRect = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintRect.setColor(Color.RED);
        paintRect.setStrokeWidth(1);
        paintRect.setStyle(Paint.Style.STROKE);
        canvasRect.drawRect(aRectDiff, paintRect);
        return bitmapCanvas;
    }

    /**
     * Creates a bitmap with the size of the passed in bitmap,
     * containing two colored rectangles with the dimensions of the passed in rectangles.
     * It is intendet to be used to show the rectangle that stayed the same between two frames calculated in stabilizeImage and
     * the rectangle of difference in that rectangle returned by getDifference.
     * @param aRect rectangle to be drawn
     * @param aRectDiff rectangle to be drawn
     * @param aBitmapCurrent bitmap to be used for dimensions of the new bitmap
     * @return Bitmap bitmap with size of aBitmapCurrent containing a drawing of aRectDiff
     */
    public Bitmap getBitmapDiffRect(Rect aRect, Rect aRectDiff, Bitmap aBitmapCurrent) {
        Bitmap bitmapCanvas = Bitmap.createBitmap(aBitmapCurrent.getWidth(), aBitmapCurrent.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvasRect = new Canvas(bitmapCanvas);
        Paint paintRect = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintRect.setColor(Color.RED);
        paintRect.setStrokeWidth(1);
        paintRect.setStyle(Paint.Style.STROKE);
        canvasRect.drawRect(aRectDiff, paintRect);
        paintRect.setColor(Color.YELLOW);
        paintRect.setStrokeWidth(2);
        canvasRect.drawRect(aRect, paintRect);
        return bitmapCanvas;
    }

    /**
     * Calculates the sum of RGB color values of all pixels in the image
     * @param aBitmap bitmap of which to calculate colors
     * @return - color sum of RGB values of all pixels in the passed in Bitmap
     */
    public float getColorSum(Bitmap aBitmap) {
        float retVal = 0;
        int width = aBitmap.getWidth();
        int height = aBitmap.getHeight();
        for(int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = aBitmap.getPixel(x, y);
                retVal = retVal + (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3;
            }
        }
        return retVal;
    }

    public static int calculateSampleSize(int sourceWidth, int sourceHeight, int reqWidth, int reqHeight) {
        Log.i(TAG, "calculateSampleSize, sourceWidth=" + sourceWidth + ", sourceHeight=" + sourceHeight +
                ", reqWidth=" + reqWidth + ", reqHeight=" + reqHeight);
        int sampleSize = 1;
        if (sourceHeight > reqHeight || sourceWidth > reqWidth) {
            int heightRatio = (int)((float)sourceHeight / (float)reqHeight);
            int widthRatio = (int)((float)sourceWidth / (float)reqWidth);

		    /* Choose the higher ratio as sampleSize value */
            if(heightRatio > widthRatio) {sampleSize = heightRatio;} else {sampleSize = widthRatio;}
            /* Round to the nearest even number. */
            while(sampleSize % 2 != 0) {
                sampleSize++;
            }
        }
        Log.i(TAG, "calculateSampleSize, sampleSize=" + sampleSize);
        return sampleSize;
    }
}
