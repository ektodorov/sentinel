package com.blogspot.techzealous.sentinel.utils;

import android.graphics.Color;

public class ConstantsS {

//    public static final int THRESHOLD_0 = -16777216;    // RGBA 0, 0, 0, 255, 0%
//    public static final int THRESHOLD_4 = -16119286;    // RGBA 10, 10, 10, 255, 4%
//    public static final int THRESHOLD_10 = -15132391;   // RGBA, 25, 25, 25, 255, 10%
//    public static final int THRESHOLD_15 = -14211289;   // RGBA, 39, 39, 39, 255, 15%
//    public static final int THRESHOLD_20 = -13421773;   // RGBA, 51, 51, 51, 255, 20%
//    public static final int THRESHOLD_25 = -12566464;   // RGBA, 64, 64, 64, 255, 25%
//    public static final int THRESHOLD_30 = -11776948;   // RGBA, 76, 76, 76, 255, 30%
//    public static final int THRESHOLD_35 = -10921639;   // RGBA, 89, 89, 89, 255, 35%
//    public static final int THRESHOLD_40 = -10066330;   // RGBA, 102, 102, 102, 255, 40%
//    public static final int THRESHOLD_45 = -9211021;    // RGBA, 115, 115, 115, 255, 45%
//    public static final int THRESHOLD_50 = -8355712;    // RGBA, 128, 128, 128. 255, 50%
//    public static final int THRESHOLD_55 = -7566196;    // RGBA, 140, 140, 140, 255, 55%
//    public static final int THRESHOLD_60 = -6710887;    // RGBA, 153, 153, 153, 255, 60%
//    public static final int THRESHOLD_65 = -5855578;    // RGBA, 166, 166, 166, 255, 65%
//    public static final int THRESHOLD_70 = -5000269;    // RGBA, 179, 179, 179, 255, 70%

    private static boolean sStabilizationEnabled = false;
    private static int sThresholdStabilization = 70;
    private static int sThresholdDifference = 85;

    public static final String PREF_STABILIZATION_ENABLED = "stabilizationenabled";
    public static final String PREF_THRESHOLD_STABILIZATION = "thresholdstabilization";
    public static final String PREF_THRESHOLD_DIFFERENCE = "thresholddifference";

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
