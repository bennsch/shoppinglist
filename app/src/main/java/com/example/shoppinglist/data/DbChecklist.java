package com.example.shoppinglist.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;


@Entity
public class DbChecklist {
    @PrimaryKey
    @NonNull
    private String checklistTitle;

    public DbChecklist(@NonNull String checklistTitle) {
        this.checklistTitle = checklistTitle;
    }

    @NonNull
    public String getChecklistTitle() {
        return checklistTitle;
    }
}