package org.hwyl.sexytopo.control.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.util.GeneralPreferences;

public class SettingsActivity extends SexyTopoActivity
        implements PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    private SharedPreferences.OnSharedPreferenceChangeListener prefListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setupMaterialToolbar();
        applyEdgeToEdgeInsets(R.id.rootLayout, true, true);

        // Always start fresh at the main settings page when activity is created
        // Clear any saved fragment state and back stack
        getSupportFragmentManager()
                .popBackStackImmediate(
                        null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settingsFragment, new MainSettingsFragment())
                .commitNow();

        getSupportFragmentManager()
                .addOnBackStackChangedListener(
                        () -> {
                            boolean isAtRoot =
                                    getSupportFragmentManager().getBackStackEntryCount() == 0;
                            if (isAtRoot) {
                                setTitle(R.string.title_activity_settings);
                            }
                            if (getSupportActionBar() != null) {
                                getSupportActionBar().setDisplayHomeAsUpEnabled(!isAtRoot);
                            }
                        });

        prefListener =
                (prefs, key) -> {
                    if ("pref_theme".equals(key)) {
                        setTheme();
                    }
                };
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                getSupportFragmentManager().popBackStack();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
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

    @Override
    public boolean onPreferenceStartFragment(
            @NonNull PreferenceFragmentCompat caller, @NonNull Preference pref) {
        String fragmentClassName = pref.getFragment();
        if (fragmentClassName == null) {
            return false;
        }

        Fragment fragment =
                getSupportFragmentManager()
                        .getFragmentFactory()
                        .instantiate(getClassLoader(), fragmentClassName);
        fragment.setArguments(pref.getExtras());

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settingsFragment, fragment)
                .addToBackStack(null)
                .commit();

        setTitle(pref.getTitle());
        return true;
    }

    public static class MainSettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
            setPreferencesFromResource(R.xml.preferences_main, rootKey);
        }
    }

    public static class GeneralFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
            setPreferencesFromResource(R.xml.preferences_general, rootKey);
        }
    }

    public static class SketchingFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
            setPreferencesFromResource(R.xml.preferences_sketching, rootKey);
        }
    }

    public static class ExportFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
            setPreferencesFromResource(R.xml.preferences_export, rootKey);
        }
    }

    public static class ExportSvgFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
            setPreferencesFromResource(R.xml.preferences_export_svg, rootKey);
        }
    }

    public static class ExportTherionFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
            setPreferencesFromResource(R.xml.preferences_export_therion, rootKey);
        }
    }

    public static class ManualDataEntryFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
            setPreferencesFromResource(R.xml.preferences_manual_data_entry, rootKey);
        }
    }

    public static class TeamFragment extends Fragment {
        @Nullable
        @Override
        public View onCreateView(
                @NonNull LayoutInflater inflater,
                @Nullable ViewGroup container,
                @Nullable Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_settings_team, container, false);
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            refreshList(view);

            view.findViewById(R.id.add_caver_button)
                    .setOnClickListener(
                            v -> {
                                TextInputEditText field = view.findViewById(R.id.new_caver_field);
                                TextInputLayout layout = view.findViewById(R.id.new_caver_layout);
                                String name = field.getText().toString().trim();
                                if (name.isEmpty()) {
                                    layout.setError(getString(R.string.trip_dialog_name_required));
                                    return;
                                }
                                layout.setError(null);
                                GeneralPreferences.addKnownCaver(name);
                                field.setText("");
                                refreshList(view);
                            });
        }

        private void refreshList(View root) {
            LinearLayout list = root.findViewById(R.id.cavers_list);
            list.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(requireContext());
            for (String name : GeneralPreferences.getKnownCavers()) {
                View row = inflater.inflate(R.layout.trip_team_member_item, list, false);
                ((TextView) row.findViewById(R.id.name_field)).setText(name);
                row.findViewById(R.id.role_field).setVisibility(View.GONE);
                row.findViewById(R.id.icon).setVisibility(View.GONE);
                row.setOnClickListener(v -> showEditDialog(root, name));
                row.findViewById(R.id.delete_button)
                        .setOnClickListener(
                                v -> {
                                    GeneralPreferences.removeKnownCaver(name);
                                    refreshList(root);
                                });
                list.addView(row);
            }
        }

        private void showEditDialog(View root, String currentName) {
            View dialogView =
                    LayoutInflater.from(requireContext())
                            .inflate(R.layout.dialog_edit_caver_name, null);
            TextInputLayout layout = dialogView.findViewById(R.id.name_input_layout);
            TextInputEditText field = dialogView.findViewById(R.id.name_field);
            field.setText(currentName);
            field.selectAll();

            AlertDialog dialog =
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle(R.string.edit)
                            .setView(dialogView)
                            .setPositiveButton(R.string.save, null)
                            .setNegativeButton(R.string.cancel, null)
                            .show();

            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setOnClickListener(
                            v -> {
                                String newName = field.getText().toString().trim();
                                if (newName.isEmpty()) {
                                    layout.setError(getString(R.string.trip_dialog_name_required));
                                    return;
                                }
                                GeneralPreferences.removeKnownCaver(currentName);
                                GeneralPreferences.addKnownCaver(newName);
                                dialog.dismiss();
                                refreshList(root);
                            });
        }
    }

    public static class InstrumentsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
            setPreferencesFromResource(R.xml.preferences_instruments, rootKey);
        }
    }

    public static class DeveloperFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
            setPreferencesFromResource(R.xml.preferences_developer, rootKey);
        }
    }
}
