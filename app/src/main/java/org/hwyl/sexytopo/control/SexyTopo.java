package org.hwyl.sexytopo.control;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.res.Resources;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.hwyl.sexytopo.control.util.GeneralPreferences;
import org.hwyl.sexytopo.control.util.SketchPreferences;


public class SexyTopo extends Application {

    // This is extremely hacky but quite useful for getting a context when it's not sensible
    // to pass one around
    @SuppressLint("StaticFieldLeak") // shouldn't be problem, because using ApplicationContext
    public static Context context;
    private Thread.UncaughtExceptionHandler defaultHandler;

    private static Resources resources;

    @Override
    public void onCreate() {
        super.onCreate();
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler (this::handleUncaughtException);

        context = getApplicationContext(); // can we get rid of this?
        resources = getResources();

        GeneralPreferences.initialise(this.getApplicationContext());
        SketchPreferences.initialise(this.getApplicationContext());
    }

    public void handleUncaughtException (Thread thread, Throwable e) {
        Log.setContext(this);
        FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
        crashlytics.recordException(e);
        Log.e(e);
        defaultHandler.uncaughtException(thread, e);
    }

    public static String staticGetString(int id, Object... formatArgs) {
        return resources.getString(id, formatArgs);
    }
}