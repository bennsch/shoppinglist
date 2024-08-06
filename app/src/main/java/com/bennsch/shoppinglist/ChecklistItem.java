package com.bennsch.shoppinglist;

public class ChecklistItem {
    private String mName;
    private long mIncidence;

    public ChecklistItem(String name, long incidence) {
        mName = name;
        mIncidence = incidence;
    }

    public String getName() {
        return mName;
    }

    public long getIncidence() {
        return mIncidence;
    }
}
