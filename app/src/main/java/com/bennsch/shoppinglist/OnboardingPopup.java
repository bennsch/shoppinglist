package com.bennsch.shoppinglist;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
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
        Animation animAlpha = new AlphaAnimation(1.0f, 0.0f);
        animAlpha.setDuration(animDur);
        animAlpha.setRepeatMode(Animation.REVERSE);
        animAlpha.setRepeatCount(1);

        animAlpha.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {}

            @Override
            public void onAnimationRepeat(Animation animation) {
                // Update the text in the middle of the animation.
                textView.setText(text);
            }
        });

        Animation animScale = new ScaleAnimation(
                1.0f, 1.2f, 1.0f, 1.2f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        animScale.setDuration(250);
        animScale.setRepeatMode(Animation.REVERSE);
        animScale.setRepeatCount(1);

        AnimationSet animSet = new AnimationSet(true);
        animSet.addAnimation(animScale);
        animSet.addAnimation(animAlpha);

        textView.startAnimation(animSet);
    }
}
