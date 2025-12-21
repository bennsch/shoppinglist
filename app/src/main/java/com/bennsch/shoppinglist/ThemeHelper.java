package com.bennsch.shoppinglist;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.util.TypedValue;

import androidx.annotation.AnyRes;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;


public class ThemeHelper {
    /*
     *  Helper class to perform common theme related tasks.
     */

    public static @AnyRes int resolveAttribute(int resId, @NonNull Context context) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(resId, typedValue, true);
        return typedValue.resourceId;
    }

    public static @ColorInt int getColor(int resId, @NonNull Context context) {
        if ((Build.VERSION.SDK_INT < 26) && (resId == android.R.attr.colorError)) {
            // android.R.attr.colorError was introduced in SDK 26
            return Color.rgb(186, 26, 26);
        } else {
            TypedValue typedValue = new TypedValue();
            context.getTheme().resolveAttribute(resId, typedValue, true);
            return ContextCompat.getColor(context, typedValue.resourceId);
        }
    }

    public static int dpToTx(int dp, Context context) {
        return (int)TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                (float)dp,
                context.getResources().getDisplayMetrics());
    }
}
