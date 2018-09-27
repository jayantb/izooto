package com.app.izoototest;

import android.app.Application;
import android.widget.Toast;

import com.izooto.Lg;
import com.izooto.TokenReceivedListener;
import com.izooto.iZooto;

public class AppController extends Application implements TokenReceivedListener {

    @Override
    public void onCreate() {
        super.onCreate();
        iZooto.initialize(this).setTokenReceivedListener(this).build();
    }

    @Override
    public void onTokenReceived(String token) {
        Toast.makeText(this, "token1: "+token, Toast.LENGTH_LONG).show();
    }
}