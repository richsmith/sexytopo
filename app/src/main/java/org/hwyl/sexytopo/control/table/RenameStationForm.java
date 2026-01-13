package org.hwyl.sexytopo.control.table;

import android.content.Context;
import android.text.Editable;
import android.text.InputType;
import android.widget.EditText;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;

public class RenameStationForm extends Form {
    final Survey survey;
    final Station station;

    final EditText stationName;
    final TextInputLayout stationNameLayout;

    RenameStationForm(Context context, Survey survey, Station station) {
        super(context);
        this.survey = survey;
        this.station = station;

        this.stationNameLayout = new TextInputLayout(context);
        this.stationNameLayout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        this.stationNameLayout.setHint(context.getString(R.string.manual_rename_station_hint));
        this.stationNameLayout.setErrorEnabled(true);

        float density = context.getResources().getDisplayMetrics().density;
        int paddingH = (int) (24 * density);
        int paddingV = (int) (20 * density);
        this.stationNameLayout.setPadding(paddingH, paddingV, paddingH, 0);

        this.stationName = new TextInputEditText(context);
        this.stationName.setInputType(InputType.TYPE_CLASS_TEXT);
        this.stationName.setText(this.station.getName());
        this.stationName.addTextChangedListener(new TextViewValidationTrigger(this));

        this.stationNameLayout.addView(this.stationName);
    }

    @Override
    protected void performValidation() {
        String currentName = this.station.getName();
        Editable currentText = this.stationName.getText();
        String currentTextString = currentText.toString();

        // only check for non-null or max length
        if (currentTextString.isEmpty()) {
            setError(this.stationName, "Cannot be blank");
        } else if (currentTextString.equals("-")) {
            setError(this.stationName, "Station cannot be named \"-\"");
        } else if (!currentTextString.equals(currentName) && (survey.getStationByName(currentTextString) != null)) {
            setError(this.stationName, "Station name must be unique");
        } else {
            setError(this.stationName, (CharSequence) null);
        }
    }
}
