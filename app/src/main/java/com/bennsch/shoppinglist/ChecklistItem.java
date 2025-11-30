package com.bennsch.shoppinglist;

public class ChecklistItem {
    /*
     *  Represents a Checklist-Item in the UI, as opposed to the database.
     */

    private final String mName;
    // Used to sort "checked" items. Will be incremented everytime the user flips an item.
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
