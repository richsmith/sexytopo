package org.hwyl.sexytopo.control.table;

import android.content.Context;
import android.text.Editable;
import android.text.InputType;
import android.widget.EditText;

import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;

public class RenameStationForm extends Form {
    Survey survey;
    Station station;

    EditText stationName;

    RenameStationForm(Context context, Survey survey, Station station) {
        super();
        this.survey = survey;
        this.station = station;

        this.stationName = new EditText(context);
        this.stationName.setInputType(InputType.TYPE_CLASS_TEXT);
        this.stationName.setText(this.station.getName());
        this.stationName.addTextChangedListener(new TextViewValidationTrigger(this));
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
            setError(this.stationName, null);
        }
    }
}
