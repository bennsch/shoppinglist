package com.example.shoppinglist;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ChecklistItem {
    private String mName;
    private Boolean mIsChecked;

    public ChecklistItem(String name, boolean isChecked) {
        mName = name;
        mIsChecked = isChecked;
    }

    public void flipChecked() {
        mIsChecked = !mIsChecked;
    }

    public Boolean isChecked() {
        return mIsChecked;
    }

    public String getName() {
        return mName;
    }
}
