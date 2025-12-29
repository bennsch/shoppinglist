# Preserve this method used by ObjectAnimator.
-keep class com.google.android.material.snackbar.Snackbar {
    setBackgroundTint(int);
}

# Preserve the line number information for debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# Hide the original source file name.
#-renamesourcefileattribute SourceFile