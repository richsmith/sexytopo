package org.hwyl.sexytopo.control.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import java.util.ArrayList;
import java.util.List;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.util.GeneralPreferences;
import org.hwyl.sexytopo.model.survey.LicenseOption;

/**
 * One-time, first-run screen that asks the user to choose a default license, rather than one being
 * silently applied on their behalf. Shown from TripActivity the first time a new trip would
 * otherwise be seeded with a default license - see {@link
 * GeneralPreferences#isLicenseDefaultConfirmed()}. Returns the confirmed default license name via
 * {@link #RESULT_LICENSE_NAME_EXTRA} in the result Intent.
 */
public class LicenseChoiceActivity extends SexyTopoActivity {

    public static final String RESULT_LICENSE_NAME_EXTRA = "licenseName";

    private static final String PRESELECTED_LICENSE_NAME = "GPLv3.0+";

    private Spinner licenseSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_license_choice);
        setupMaterialToolbar();
        applyEdgeToEdgeInsets(R.id.rootLayout, true, true);

        List<String> names = new ArrayList<>();
        for (LicenseOption option : GeneralPreferences.getLicenseOptions()) {
            names.add(option.getName());
        }

        licenseSpinner = findViewById(R.id.license_choice_spinner);
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names);
        licenseSpinner.setAdapter(adapter);

        int preselectedIndex = names.indexOf(PRESELECTED_LICENSE_NAME);
        licenseSpinner.setSelection(Math.max(preselectedIndex, 0));

        Button confirmButton = findViewById(R.id.license_choice_confirm_button);
        confirmButton.setOnClickListener(v -> confirmAndFinish());
    }

    @Override
    public void onBackPressed() {
        // A default license must be chosen before leaving this screen - treat the hardware
        // back button as accepting whatever is currently selected, rather than letting the
        // user leave without a choice being recorded.
        confirmAndFinish();
    }

    private void confirmAndFinish() {
        String selected = (String) licenseSpinner.getSelectedItem();
        if (selected != null) {
            GeneralPreferences.setDefaultLicenseOption(selected);
        }
        GeneralPreferences.setLicenseDefaultConfirmed(true);

        Intent result = new Intent();
        result.putExtra(RESULT_LICENSE_NAME_EXTRA, GeneralPreferences.getDefaultLicenseName());
        setResult(RESULT_OK, result);
        finish();
    }
}
