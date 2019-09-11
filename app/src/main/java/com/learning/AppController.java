package com.learning;

import android.app.Application;

public class AppController extends Application {

    static ObserveRecord observeRecord;
    static AppController instance;

    @Override
    public void onCreate() {
        super.onCreate();

        observeRecord = new ObserveRecord();
        instance = this;

    }

    public static ObserveRecord getObserver() {
        return observeRecord;
    }

    public static AppController getInstance(){
        return instance;
    }
}
