package com.bennsch.shoppinglist.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;


@Entity
public class DbChecklist {

    @PrimaryKey
    @NonNull private String checklistTitle;
    // True if this Checklist is currently selected by the user, false otherwise.
    // Only one Checklist should be active at all times.
    private boolean active;

    public DbChecklist(@NonNull String checklistTitle, boolean active) {
        this.checklistTitle = checklistTitle;
        this.active = active;
    }

    @NonNull
    public String getChecklistTitle() {
        return checklistTitle;
    }

    public boolean isActive() {
        return active;
    }
}