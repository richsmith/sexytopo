package org.hwyl.sexytopo.control;

import android.app.Application;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.hwyl.sexytopo.control.util.PreferenceHelper;

public class SexyTopo extends Application {

    private Thread.UncaughtExceptionHandler defaultHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler (this::handleUncaughtException);
        PreferenceHelper.initPreferences(this);
    }

    public void handleUncaughtException (Thread thread, Throwable e) {
        Log.setContext(this);
        FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
        crashlytics.recordException(e);
        Log.e(e);
        defaultHandler.uncaughtException(thread, e);
    }
}