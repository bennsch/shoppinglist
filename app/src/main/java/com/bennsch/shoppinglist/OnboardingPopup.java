package com.bennsch.shoppinglist;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.IntegerRes;
import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;

import com.google.android.material.snackbar.Snackbar;


public class OnboardingPopup {
    /*
     *  Wrapper for a popup.  Using a Snackbar is just one possible
     *  implementation.
     */

    // Use background color from theme.
    private static final int COLOR_BG = com.google.android.material.R.attr.colorTertiary;
    // To flash the background, we blend it with a more contrasted hue of the same background color.
    private static final int COLOR_BG_FLASH = com.google.android.material.R.attr.colorTertiaryContainer;
    // Color blend ratio.
    private static final float COLOR_BG_FLASH_BLEND = 0.7f;
    // Use default animation duration.
    private static final @IntegerRes int ANIM_DUR = android.R.integer.config_shortAnimTime;

    private final Snackbar mSnackbar;
    private final TextView mTextView;
    private final int mAnimDur;
    private final @ColorInt int mBgColor;
    private final @ColorInt int mBgColorFlash;

    public OnboardingPopup(@NonNull Context context, @NonNull View root) {
        mBgColor = ThemeHelper.getColor(COLOR_BG, context);
        mBgColorFlash = ColorUtils.blendARGB(
                mBgColor,
                ThemeHelper.getColor(COLOR_BG_FLASH, context),
                COLOR_BG_FLASH_BLEND
        );
        mAnimDur = context.getResources().getInteger(ANIM_DUR);
        mSnackbar= Snackbar.make(root, "", Snackbar.LENGTH_INDEFINITE);
        mSnackbar.setAction("Dismiss", v -> {});
        mSnackbar.setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE);
        mSnackbar.setBackgroundTint(mBgColor);
        mTextView = mSnackbar.getView().findViewById(
                com.google.android.material.R.id.snackbar_text);
        mTextView.setMinLines(3);
        mTextView.setMaxLines(5);
        mTextView.setGravity(Gravity.CENTER);
    }

    public void show(String text) {
        if (mSnackbar.isShown()) {
            // If the snackbar is already visible, play an animation to make the text update more
            // obvious.
            ObjectAnimator animFlashBg = makeAnimFlashBg(mSnackbar, mBgColor, mBgColorFlash);
            ObjectAnimator animFadeText = makeAnimFadeText(mTextView, text);
            AnimatorSet animSet = new AnimatorSet();
            animSet.play(animFlashBg).with(animFadeText);
            animSet.setDuration(mAnimDur);
            animSet.start();
        } else {
            mTextView.setText(text);
            mSnackbar.show();
        }
    }

    public void hide() {
        mSnackbar.dismiss();
    }

    private ObjectAnimator makeAnimFadeText(TextView textView, String newText) {
        // Go from opaque to fully transparent and back.
        ObjectAnimator anim = ObjectAnimator.ofFloat(textView, "alpha", 1.0f, 0.0f);
        anim.setRepeatMode(ObjectAnimator.REVERSE);
        anim.setRepeatCount(1);
        anim.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(@NonNull Animator animation) {}

            @Override
            public void onAnimationEnd(@NonNull Animator animation) {}

            @Override
            public void onAnimationCancel(@NonNull Animator animation) {}

            @Override
            public void onAnimationRepeat(@NonNull Animator animation) {
                // Update the text once the TextView fully transparent.
                textView.setText(newText);
            }
        });
        return anim;
    }

    private ObjectAnimator makeAnimFlashBg(Snackbar snackbar,
                                           @ColorInt int colorFrom,
                                           @ColorInt int colorTo) {
        // Flash the SnackBar's background.
        ObjectAnimator anim = ObjectAnimator.ofInt(snackbar, "backgroundTint", colorFrom, colorTo);
        anim.setEvaluator(new ArgbEvaluator());
        anim.setRepeatMode(ObjectAnimator.REVERSE);
        anim.setRepeatCount(1);
        return anim;
    }
}
