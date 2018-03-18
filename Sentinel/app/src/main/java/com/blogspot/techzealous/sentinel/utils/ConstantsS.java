package com.blogspot.techzealous.sentinel.utils;

import android.graphics.Color;
import android.media.Ringtone;
import android.os.Environment;

public class ConstantsS {

    private static boolean sStabilizationEnabled = false;
    private static int sThresholdStabilization = 70;
    private static int sThresholdDifference = 85;
    private static boolean sPlaySoundEnabled = false;
    private static boolean sRecordPictures = true;
    private static Ringtone sRingtone;

    public static final String PREF_STABILIZATION_ENABLED = "stabilizationenabled";
    public static final String PREF_THRESHOLD_STABILIZATION = "thresholdstabilization";
    public static final String PREF_THRESHOLD_DIFFERENCE = "thresholddifference";
    public static final String PREF_PLAY_SOUND = "playsound";
    public static final String PREF_RECORD_PICTURES = "recordpictures";

    public static final String STR_MIME_TYPE_IMAGE = "image/*";
    public static final String STR_MIME_TYPE_VIDEO = "video/*";
    public static final String STR_Stabilization = "Stabilization";
    public static final String STR_Stabilization_threshold = "Stabilization threshold";
    public static final String STR_Difference = "Difference";
    public static final String STR_Difference_threshold = "Difference threshold";

    private ConstantsS() {
        super();
    }

    //sStabilizationEnabled
    public static boolean isStabilizationEnabled() {
        return sStabilizationEnabled;
    }

    public static void setStabilizationEnabled(boolean aIsStabilizationEnabled) {
        sStabilizationEnabled = aIsStabilizationEnabled;
    }

    /**
     * sThresholdStagilization
     *
     * The threshold to use when calculating the difference in stabilization. If the difference is lower than the threshold it
     * won't be considered as different and will be discarded.
     * Bigger values of threshold are more forgiving for misalignments in the
     * compared images.
     */
    public static int getThresholdStabilization() {
        return sThresholdStabilization;
    }

    public static void setThresholdStabilization(int aSensitivity) {
        int colorPercent = (int)(255 * ((float)(100 - aSensitivity) / 100.0f));
        sThresholdStabilization = Color.argb(255, colorPercent, colorPercent, colorPercent);
    }

    /**
     * sThresholdDifference
     *
     * The threshold to use when calculating the difference in stabilization. If the difference is lower than the threshold it
     * won't be considered as different and will be discarded.
     * Bigger values of threshold are more forgiving for misalignments in the
     * compared images.
     */
    public static int getThresholdDifference() {
        return sThresholdDifference;
    }

    public static void setThresholdDifference(int aSensitivity) {
        int colorPercent = (int)(255 * ((float)(100 - aSensitivity) / 100.0f));
        sThresholdDifference = Color.argb(255, colorPercent, colorPercent, colorPercent);
    }

    /**
     * sPlaySoundEnabled
     * Play sound if there is difference detected and a difference rectangle drawn on the screen.
     */
    public static boolean getPlaySoundEnabled() {
        return sPlaySoundEnabled;
    }

    public static void setPlaySoundEnabled(boolean aIsEnabled) {
        sPlaySoundEnabled = aIsEnabled;
    }

    public static Ringtone getRingtone() {
        return sRingtone;
    }

    public static void setRingtone(Ringtone aRingtone) {
        sRingtone = aRingtone;
    }

    public static boolean getRecordPictures() {return sRecordPictures;}

    public static void setRecordPictures(boolean aIsRecordPictures) {sRecordPictures = aIsRecordPictures;}

    public static boolean isExternalStorageAvailable() {
        String state = Environment.getExternalStorageState();
        boolean externalStorageAvailable = false;
        if(Environment.MEDIA_MOUNTED.equals(state)) { externalStorageAvailable = true; }
        if(Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) { externalStorageAvailable = false; }
        return externalStorageAvailable;
    }
}
