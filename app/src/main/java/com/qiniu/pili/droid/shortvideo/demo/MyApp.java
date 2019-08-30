package com.qiniu.pili.droid.shortvideo.demo;

import android.app.Application;

public class MyApp extends Application {

    private static MyApp myApp;

    @Override
    public void onCreate() {
        super.onCreate();
        myApp = this;
    }

    public static MyApp getInstance() {
        return myApp;
    }
}
