package com.bennsch.shoppinglist.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;


@Entity
public class DbChecklist {

    @PrimaryKey
    @NonNull
    private String checklistTitle;

    private boolean selected;
    private boolean allCaps;

    public DbChecklist(@NonNull String checklistTitle, boolean allCaps, boolean selected) {
        this.checklistTitle = checklistTitle;
        this.selected = selected;
        this.allCaps = allCaps;
    }

    @NonNull
    public String getChecklistTitle() {
        return checklistTitle;
    }

    public boolean isAllCaps() {
        return allCaps;
    }

    public boolean isSelected() {
        return selected;
    }
}