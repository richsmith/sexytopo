package org.hwyl.sexytopo.control.components;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;

import com.google.android.material.textfield.TextInputLayout;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;

/**
 * Reusable autocomplete field for selecting stations with validation.
 */
public class StationAutoCompleteField {

    private final TextInputLayout inputLayout;
    private final AutoCompleteTextView input;
    private final Survey survey;
    private Button validationButton;

    public StationAutoCompleteField(Context context, Survey survey, String hint) {
        this.survey = survey;

        inputLayout = (TextInputLayout) LayoutInflater.from(context)
                .inflate(R.layout.dialog_station_autocomplete, null, false);
        inputLayout.setHint(hint);

        input = inputLayout.findViewById(R.id.station_autocomplete);
        String[] stationNames = survey.getAllStations().stream()
            .map(Station::getName)
            .toArray(String[]::new);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            context,
            android.R.layout.simple_dropdown_item_1line,
            stationNames
        );
        input.setAdapter(adapter);
        input.setFocusableInTouchMode(true);

        int paddingH = (int) (24 * context.getResources().getDisplayMetrics().density);
        int paddingV = (int) (20 * context.getResources().getDisplayMetrics().density);
        inputLayout.setPadding(paddingH, paddingV, paddingH, 0);
    }

    /**
     * Get the TextInputLayout containing the autocomplete field.
     * This should be added to your dialog or view.
     */
    public TextInputLayout getView() {
        return inputLayout;
    }

    /**
     * Get the autocomplete input field directly.
     */
    public AutoCompleteTextView getInput() {
        return input;
    }

    /**
     * Get the currently selected station, or null if invalid/empty.
     */
    public Station getSelectedStation() {
        String stationName = input.getText().toString().trim();
        return survey.getStationByName(stationName);
    }

    /**
     * Enable validation that disables a button when no valid station is selected.
     * Call this after the dialog is shown so the button exists.
     *
     * @param button The button to enable/disable based on validity
     */
    public void enableValidation(Button button) {
        this.validationButton = button;
        button.setEnabled(false);

        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String stationName = s.toString().trim();
                if (stationName.isEmpty()) {
                    inputLayout.setError(null);
                    validationButton.setEnabled(false);
                } else {
                    Station station = survey.getStationByName(stationName);
                    if (station != null) {
                        inputLayout.setError(null);
                        validationButton.setEnabled(true);
                    } else {
                        inputLayout.setError(inputLayout.getContext().getString(
                            R.string.tool_find_station_error_invalid));
                        validationButton.setEnabled(false);
                    }
                }
            }
        });
    }

    /**
     * Request focus on the input field.
     */
    public void requestFocus() {
        input.requestFocus();
    }
}
