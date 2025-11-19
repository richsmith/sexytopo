package org.hwyl.sexytopo.control.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import androidx.preference.PreferenceFragmentCompat;

import org.hwyl.sexytopo.R;

public class SettingsActivity extends SexyTopoActivity {

    private SharedPreferences.OnSharedPreferenceChangeListener prefListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        applyEdgeToEdgeInsets(R.id.rootLayout, true, true);

        getSupportFragmentManager().beginTransaction()
            .replace(R.id.rootLayout, new SettingsFragment())
            .commit();

        prefListener = (prefs, key) -> {
            if (key.equals("pref_theme")) {
                setTheme();
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(prefListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.unregisterOnSharedPreferenceChangeListener(prefListener);
    }


    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
            addPreferencesFromResource(R.xml.general_preferences);
        }
    }
}
