package com.beijingepidial.ccpvirus;

import android.content.res.Resources;
import android.view.Display;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.Toast;

import org.apache.commons.lang3.StringUtils;


public class Utils {
    public static int px2dp(int px) {
        return (int) (px / Resources.getSystem().getDisplayMetrics().density);
    }

    public static int px2sp(int px) {
        return (int) (px / Resources.getSystem().getDisplayMetrics().scaledDensity);
    }

}
