package org.hwyl.sexytopo.control.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by rls on 01/08/15.
 */
public class PreferenceAccess {

    private static SharedPreferences getPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static int getInt(Context context, String id, int defaultInt) {

        return Integer.parseInt(getPreferences(context)
                .getString(id, Integer.toString(defaultInt)));
    }


}
