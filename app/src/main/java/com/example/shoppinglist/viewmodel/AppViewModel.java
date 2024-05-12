package com.example.shoppinglist.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.shoppinglist.data.ChecklistRepository;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class AppViewModel extends AndroidViewModel {

    private static final String TAG = "ChecklistViewModel";

    protected static final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    protected final ChecklistRepository mChecklistRepo;
    protected LiveData<List<String>> mChecklistTitles;

    public AppViewModel(@NonNull Application application) {
        super(application);
        Log.d(TAG, "ChecklistViewModel: Ctor");
        mChecklistRepo = new ChecklistRepository(application);
        mChecklistTitles = mChecklistRepo.getAllChecklistTitles();
    }

    public LiveData<List<String>> getAllChecklistTitles() {
        return mChecklistTitles;
    }

    public void insertChecklist(String listTitle) {
        mExecutor.execute(() -> {
            mChecklistRepo.insertChecklist(listTitle);
        });
    }


    public void deleteChecklist(String checklistTitle) {
        mExecutor.execute(() -> {
            mChecklistRepo.deleteChecklist(checklistTitle);
        });
    }

    public void updateChecklistName(String checklistTitle, String newTitle) {
        if (mChecklistTitles.getValue().contains(newTitle)) {
            throw new IllegalArgumentException("Checklist title '" + newTitle + "' already present");
        } else {
            mExecutor.execute(() -> {
                mChecklistRepo.updateChecklistName(checklistTitle, newTitle);
            });
        }
    }


    @Override
    protected void onCleared() {
        Log.d(TAG, "onCleared: ");
        super.onCleared();
    }
}
