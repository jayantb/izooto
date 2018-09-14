package com.app.izoototest;

import android.app.Application;
import com.izooto.iZooto;

public class AppController extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        iZooto.initialize(this).build();
    }
}