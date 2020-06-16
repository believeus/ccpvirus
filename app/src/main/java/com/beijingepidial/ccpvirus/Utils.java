package com.beijingepidial.ccpvirus;

import android.content.res.Resources;

public class Utils {
    public static int px4dp(int px) {
        return (int) (px / Resources.getSystem().getDisplayMetrics().density);
    }
}
