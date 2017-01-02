package com.blogspot.techzealous.sentinel.utils;

import android.graphics.Color;

public class ConstantsS {

    private static boolean sStabilizationEnabled = false;
    private static int sThresholdStabilization = 70;
    private static int sThresholdDifference = 85;

    public static final String PREF_STABILIZATION_ENABLED = "stabilizationenabled";
    public static final String PREF_THRESHOLD_STABILIZATION = "thresholdstabilization";
    public static final String PREF_THRESHOLD_DIFFERENCE = "thresholddifference";

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
}
