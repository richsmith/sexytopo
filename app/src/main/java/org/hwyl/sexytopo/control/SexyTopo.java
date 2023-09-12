package org.hwyl.sexytopo.control;

import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.widget.Toast;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.hwyl.sexytopo.control.util.GeneralPreferences;
import org.hwyl.sexytopo.control.util.SketchPreferences;
import org.hwyl.sexytopo.model.sketch.Symbol;


public class SexyTopo extends Application {

    // This is extremely hacky but quite useful for getting a context when it's not sensible
    // to pass one around
    // (Do we have to make it static?)
    public static Context context;
    private Thread.UncaughtExceptionHandler defaultHandler;

    private static Resources resources;

    @Override
    public void onCreate() {
        super.onCreate();

        defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler (this::handleUncaughtException);

        context = getApplicationContext();
        GeneralPreferences.initialise(context);
        SketchPreferences.initialise(context);
        Log.setContext(context);

        resources = getResources();
        Symbol.setResources(resources);
    }

    public void handleUncaughtException (Thread thread, Throwable e) {
        FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
        crashlytics.recordException(e);
        Log.e(e);
        defaultHandler.uncaughtException(thread, e);
    }

    public static String staticGetString(int id, Object... formatArgs) {
        if (resources == null) {  // e.g. during tests
            return Integer.toString(id);
        }
        return resources.getString(id, formatArgs);
    }

    public static void showToast(int id, Object... formatArgs) {
        String message = resources.getString(id, formatArgs);
        showToast(message);
    }

    public static void showToast(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public static float dpToPixels(float dp){
        return dp * ((float)
            resources.getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }
}