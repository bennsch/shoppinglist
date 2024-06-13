package com.bennsch.shoppinglist.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;


@Entity
public class DbChecklist {

    @PrimaryKey
    @NonNull
    private String checklistTitle;

    private boolean allCaps;

    public DbChecklist(@NonNull String checklistTitle, boolean allCaps) {
        this.checklistTitle = checklistTitle;
        this.allCaps = allCaps;
    }

    @NonNull
    public String getChecklistTitle() {
        return checklistTitle;
    }

    public boolean isAllCaps() {
        return allCaps;
    }
}