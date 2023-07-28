package org.hwyl.sexytopo.control;

import android.app.Application;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.hwyl.sexytopo.control.util.DisplayPreferences;
import org.hwyl.sexytopo.control.util.GeneralPreferences;


public class SexyTopo extends Application {

    private Thread.UncaughtExceptionHandler defaultHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler (this::handleUncaughtException);

        GeneralPreferences.initialise(this.getApplicationContext());
        DisplayPreferences.initialise(this.getApplicationContext());
    }

    public void handleUncaughtException (Thread thread, Throwable e) {
        Log.setContext(this);
        FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
        crashlytics.recordException(e);
        Log.e(e);
        defaultHandler.uncaughtException(thread, e);
    }
}