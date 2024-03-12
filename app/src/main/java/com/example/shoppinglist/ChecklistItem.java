package com.example.shoppinglist;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ChecklistItem {
    private final Integer mUid;
    private String mName;
    private String mListTitle;
    private Boolean mIsChecked;
    private Integer mPosition;

    public ChecklistItem(String listTitle, String name, boolean isChecked) {
        mUid = null; // null means non-set (for room database)
        mListTitle = listTitle;
        mName = name;
        mIsChecked = isChecked;
        mPosition = null;
    }

    ChecklistItem(@NonNull Integer uid, String listTitle, String name, boolean isChecked, @NonNull Integer position) {
        // Only the database is allowed to generate the UID
        mUid = uid;
        mListTitle = listTitle;
        mName = name;
        mIsChecked = isChecked;
        mPosition = position;
    }

    public void flipChecked() {
        mIsChecked = !mIsChecked;
    }

    public Integer getUid() {
        return mUid;
    }

    public Boolean isChecked() {
        return mIsChecked;
    }

    public Integer getPosition() {
        return mPosition;
    }

    public void setPosition(@NonNull Integer position) {
        mPosition = position;
    }

    public String getName() {
        return mName;
    }

    public String getListTitle() {
        return mListTitle;
    }
}
