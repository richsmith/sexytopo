package org.hwyl.sexytopo.control;

import android.app.Application;

import com.google.firebase.crashlytics.FirebaseCrashlytics;


public class SexyTopo extends Application {

    private Thread.UncaughtExceptionHandler defaultHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
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
        FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
        crashlytics.recordException(e);
        Log.e(e);
        defaultHandler.uncaughtException(thread, e);
    }
}