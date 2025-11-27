package com.bennsch.shoppinglist;

public class ChecklistItem {
    /*
     *  Represents a Checklist-Item in the GUI, as opposed to the database.
     */

    private final String mName;
    private final long mIncidence;

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
