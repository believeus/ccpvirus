package com.beijingepidial.ccpvirus;

import android.content.res.Resources;


public class Utils {
    public static int px2dp(int px) {
        return (int) (px / Resources.getSystem().getDisplayMetrics().density);
    }

    public static int px2sp(int px) {
        return (int) (px / Resources.getSystem().getDisplayMetrics().scaledDensity);
    }
}
