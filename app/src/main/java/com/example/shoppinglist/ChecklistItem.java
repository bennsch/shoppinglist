package com.example.shoppinglist;

public class ChecklistItem {

    private Boolean mIsChecked;

    public ChecklistItem(Boolean isChecked) {
        this.mIsChecked = isChecked;
    }

    public Boolean isChecked() {
        return mIsChecked;
    }
}
