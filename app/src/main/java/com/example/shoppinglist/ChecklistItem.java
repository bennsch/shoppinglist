package com.example.shoppinglist;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ChecklistItem {
    private final Integer mUid;
    private String mName;
    private String mListTitle;
    private Boolean mIsChecked;

    public ChecklistItem(String listTitle, String name, boolean isChecked) {
        // Only the database is allowed to generate the UID
        mUid = null; // null means non-set (for room database)
        mListTitle = listTitle;
        mName = name;
        mIsChecked = isChecked;
    }

    ChecklistItem(@NonNull Integer uid, String listTitle, String name, boolean isChecked) {
        mUid = uid;
        mListTitle = listTitle;
        mName = name;
        mIsChecked = isChecked;
    }

    public Integer getUid() {
        return mUid;
    }

    public Boolean isChecked() {
        return mIsChecked;
    }

    public String getName() {
        return mName;
    }

    public String getListTitle() {
        return mListTitle;
    }
}
