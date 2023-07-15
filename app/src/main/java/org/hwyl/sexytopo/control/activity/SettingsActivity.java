package org.hwyl.sexytopo.control.activity;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import org.hwyl.sexytopo.R;

public class SettingsActivity extends SexyTopoActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.settings_container, new SettingsFragment())
            .commit();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
            addPreferencesFromResource(R.xml.general_preferences);
        }
    }
}