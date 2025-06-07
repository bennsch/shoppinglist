package com.bennsch.shoppinglist;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;


public class OnboardingPopup {
    // Wrapper for a popup.  Using a Snackbar is just one possible implementation
    private final Snackbar mSnackbar;

    public OnboardingPopup(Context context, View root) {
        mSnackbar= Snackbar.make(root, "", Snackbar.LENGTH_INDEFINITE);
        // mSnackbar.setAnchorView(mBinding.fab);
        // mSnackbar.setAnchorViewLayoutListenerEnabled(true);
        mSnackbar.setAction("Dismiss", v -> {});
        mSnackbar.setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE);
        mSnackbar.setBackgroundTint(
                ThemeHelper.getColor(
                        com.google.android.material.R.attr.colorTertiary, context));
        TextView textView = mSnackbar.getView().findViewById(
                com.google.android.material.R.id.snackbar_text);
        textView.setMinLines(3);
        textView.setMaxLines(3);
        textView.setGravity(Gravity.CENTER);
        textView.setPadding(
                ThemeHelper.DpToPx(10, context), 0,
                ThemeHelper.DpToPx(10, context), 0);
        // textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_about, 0, 0,0);
        // textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        // textView.setEms(100);
    }

    public void show(String text) {
        mSnackbar.setText(text);
        if (mSnackbar.isShown()) {
            flashText();
        } else {
            mSnackbar.show();
        }
    }

    public void hide() {
        mSnackbar.dismiss();
    }

    private void flashText() {
        TextView view = mSnackbar.getView().findViewById(com.google.android.material.R.id.snackbar_text);
        Animation fadeOut = new ScaleAnimation(
                1.0f, 1.1f, 1.0f, 1.1f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        fadeOut.setDuration(250);
        fadeOut.setRepeatMode(Animation.REVERSE);
        fadeOut.setRepeatCount(1);
        view.startAnimation(fadeOut);
    }
}
