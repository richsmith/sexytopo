package org.hwyl.sexytopo.control;

import android.app.Application;


public class SexyTopo extends Application {


    @Override
    public void onCreate() {
        super.onCreate();
        Thread.setDefaultUncaughtExceptionHandler (new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException (Thread thread, Throwable e)
            {
                handleUncaughtException (thread, e);
            }
        });
    }


    public void handleUncaughtException (Thread thread, Throwable e) {
        Log.setContext(this);
        Log.e(e);
        throw new RuntimeException(e);
    }
}