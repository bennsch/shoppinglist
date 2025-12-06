package com.bennsch.shoppinglist.datamodel;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;


@Entity
public class DbChecklist {
    /*
     *  Represents a list that contains multiple DbCheckListItems. The items are linked to a
     *  specific list via "ForeignKeys" (see DbChecklistItem).
     */

    @PrimaryKey
    @NonNull private final String listTitle;

    // True, if this checklist is currently selected by the user, false otherwise.
    // TODO: consider using separate table (1 column) for "active checklist"
    private final boolean active;

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