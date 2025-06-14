package com.bennsch.shoppinglist;

import android.content.Context;
import android.util.TypedValue;

import androidx.annotation.AnyRes;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;


public class ThemeHelper {

    public static @AnyRes int resolveAttribute(int resId, @NonNull Context context) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(resId, typedValue, true);
        return typedValue.resourceId;
    }

    public static @ColorInt int getColor(int resId, @NonNull Context context) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(resId, typedValue, true);
        return ContextCompat.getColor(context, typedValue.resourceId);
    }

    public static int DpToPx(int dp, Context context) {
        return (int)TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                (float)dp,
                context.getResources().getDisplayMetrics());
    }

}
