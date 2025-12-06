package com.bennsch.shoppinglist.datamodel;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;


@Entity
public class DbChecklist {

    @PrimaryKey
    @NonNull private final String listTitle;
    // True if this Checklist is currently selected by the user, false otherwise.
    // Only one Checklist should be active at all times.
    // TODO: consider using separate table (1 column) for "active checklist"
    private boolean active;

    public DbChecklist(@NonNull String listTitle, boolean active) {
        this.listTitle = listTitle;
        this.active = active;
    }

    @NonNull
    public String getListTitle() {
        return listTitle;
    }

    public boolean isActive() {
        return active;
    }
}