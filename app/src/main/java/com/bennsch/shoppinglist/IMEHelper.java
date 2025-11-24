package com.bennsch.shoppinglist;

import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDialog;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsAnimationCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.List;


public class IMEHelper {

    public interface OnIMEToggledListener {
        void onIMEToggled(View view, boolean imeVisible, int imeHeight);
    }

    private final Context context;
    private OnIMEToggledListener onIMEToggledListener = null;

    public IMEHelper(@NonNull Context context) {
        this.context = context;
    }

    public boolean isIMEVisible(View view) {
        WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(view);
        assert insets != null;
        return insets.isVisible(WindowInsetsCompat.Type.ime());
    }

    public void showIME(View view, boolean show) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (show) {
            // Need to use SHOW_FORCED in landscape mode if EditText is not using imeOptions="flagNoFullscreen"
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        } else {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public static void showIME(AppCompatDialog dialog) {
        Window window = dialog.getWindow();
        assert window != null : "dialog.getWindow() returned null";
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }

    public void setOnIMEToggledListener(View view, @NonNull OnIMEToggledListener listener) {
        ViewCompat.setOnApplyWindowInsetsListener(view, new OnApplyWindowInsetsListener() {
            private boolean wasIMEVisible = false; // isIMEVisible(view);

            @NonNull
            @Override
            public WindowInsetsCompat onApplyWindowInsets(@NonNull View v, @NonNull WindowInsetsCompat insets) {
                WindowInsetsCompat rootWindowInsets = ViewCompat.getRootWindowInsets(v);
                assert rootWindowInsets != null;
                boolean imeVisible = rootWindowInsets.isVisible(WindowInsetsCompat.Type.ime());
                int imeHeight = rootWindowInsets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
                // Only call listener if IME visibility actually changed.
                if (imeVisible != wasIMEVisible) {
                    onIMEToggledListener.onIMEToggled(v, imeVisible, imeHeight);
                    wasIMEVisible = imeVisible;
                }
                // From AndroidDev: Don't consume WindowInsets in setWindowInsetsApplyListener
                // for any parent ViewGroup objects. Instead, let WindowInsetsAnimatorCompat
                // handle them on Android 10 and lower.
                return insets;
                // return WindowInsetsCompat.CONSUMED;
            }
        });
        onIMEToggledListener = listener;
    }

    public void enableIMETransitionAnimation(View view) {
        ViewCompat.setWindowInsetsAnimationCallback(
            view,
            new WindowInsetsAnimationCompat.Callback(
                    WindowInsetsAnimationCompat.Callback.DISPATCH_MODE_STOP) {

                float startBottom;
                float endBottom;

                @Override
                public void onPrepare(@NonNull WindowInsetsAnimationCompat animation) {
                    super.onPrepare(animation);
                    startBottom = view.getBottom();
                }

                @NonNull
                @Override
                public WindowInsetsAnimationCompat.BoundsCompat onStart(
                        @NonNull WindowInsetsAnimationCompat animation,
                        @NonNull WindowInsetsAnimationCompat.BoundsCompat bounds
                ) {
                    endBottom = view.getBottom();
                    return bounds;
                }


                @Override
                public void onEnd(@NonNull WindowInsetsAnimationCompat animation) {
                    super.onEnd(animation);
                }

                @NonNull
                @Override
                public WindowInsetsCompat onProgress(
                        @NonNull WindowInsetsCompat insets,
                        @NonNull List<WindowInsetsAnimationCompat> runningAnimations
                ) {
                    // Find an IME animation.
                    WindowInsetsAnimationCompat imeAnimation = null;
                    for (WindowInsetsAnimationCompat animation : runningAnimations) {
                        if ((animation.getTypeMask() & WindowInsetsCompat.Type.ime()) != 0) {
                            imeAnimation = animation;
                            break;
                        }
                    }
                    if (imeAnimation != null) {
                        // Offset the view based on the interpolated fraction of the IME animation.
                        view.setTranslationY((startBottom - endBottom) * (1 - imeAnimation.getInterpolatedFraction()));
                    }
                    return insets;
                }
        });
    }
}
