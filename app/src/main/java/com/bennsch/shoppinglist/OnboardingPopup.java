package com.bennsch.shoppinglist;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;


public class OnboardingPopup {
    /*
     *  Wrapper for a popup.  Using a Snackbar is just one possible
     *  implementation.
     */

    private final Snackbar mSnackbar;
    private final TextView mTextView;
    private final int mAnimDuration;

    public OnboardingPopup(Context context, View root) {
        mAnimDuration = context.getResources().getInteger(android.R.integer.config_shortAnimTime);
        mSnackbar= Snackbar.make(root, "", Snackbar.LENGTH_INDEFINITE);
        mSnackbar.setAction("Dismiss", v -> {});
        mSnackbar.setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE);
        mSnackbar.setBackgroundTint(
                ThemeHelper.getColor(
                        com.google.android.material.R.attr.colorTertiary, context));
        mTextView = mSnackbar.getView().findViewById(
                com.google.android.material.R.id.snackbar_text);
        mTextView.setMinLines(3);
        mTextView.setMaxLines(3);
        mTextView.setGravity(Gravity.CENTER);
    }

    public void show(String text) {
        if (mSnackbar.isShown()) {
            setTextAnimated(mTextView, text, mAnimDuration);
        } else {
            mTextView.setText(text);
            mSnackbar.show();
        }
    }

    public void hide() {
        mSnackbar.dismiss();
    }

    private void setTextAnimated(TextView textView, String text, int animDur) {
        Animation anim = new AlphaAnimation(1.0f, 0.0f);
        anim.setDuration(animDur);
        anim.setRepeatMode(Animation.REVERSE);
        anim.setRepeatCount(1);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {}

            @Override
            public void onAnimationRepeat(Animation animation) {
                textView.setText(text);
            }
        });
        textView.startAnimation(anim);
    }
}
