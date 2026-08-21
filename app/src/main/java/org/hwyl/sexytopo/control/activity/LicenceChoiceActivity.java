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
import org.hwyl.sexytopo.model.survey.LicenceOption;

/**
 * One-time, first-run screen that asks the user to choose a default licence, rather than one being
 * silently applied on their behalf. Shown from TripActivity the first time a new trip would
 * otherwise be seeded with a default licence - see {@link
 * GeneralPreferences#isLicenceDefaultConfirmed()}. Returns the confirmed default licence name via
 * {@link #RESULT_LICENCE_NAME_EXTRA} in the result Intent.
 */
public class LicenceChoiceActivity extends SexyTopoActivity {

    public static final String RESULT_LICENCE_NAME_EXTRA = "licenceName";

    private static final String PRESELECTED_LICENCE_NAME = "GPLv3.0+";

    private Spinner licenceSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_licence_choice);
        setupMaterialToolbar();
        applyEdgeToEdgeInsets(R.id.rootLayout, true, true);

        List<String> names = new ArrayList<>();
        for (LicenceOption option : GeneralPreferences.getLicenceOptions()) {
            names.add(option.getName());
        }

        licenceSpinner = findViewById(R.id.licence_choice_spinner);
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names);
        licenceSpinner.setAdapter(adapter);

        int preselectedIndex = names.indexOf(PRESELECTED_LICENCE_NAME);
        licenceSpinner.setSelection(Math.max(preselectedIndex, 0));

        Button confirmButton = findViewById(R.id.licence_choice_confirm_button);
        confirmButton.setOnClickListener(v -> confirmAndFinish());
    }

    @Override
    public void onBackPressed() {
        // A default licence must be chosen before leaving this screen - treat the hardware
        // back button as accepting whatever is currently selected, rather than letting the
        // user leave without a choice being recorded.
        confirmAndFinish();
    }

    private void confirmAndFinish() {
        String selected = (String) licenceSpinner.getSelectedItem();
        if (selected != null) {
            GeneralPreferences.setDefaultLicenceOption(selected);
        }
        GeneralPreferences.setLicenceDefaultConfirmed(true);

        Intent result = new Intent();
        result.putExtra(RESULT_LICENCE_NAME_EXTRA, GeneralPreferences.getDefaultLicenceName());
        setResult(RESULT_OK, result);
        finish();
    }
}
