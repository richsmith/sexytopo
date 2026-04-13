package org.hwyl.sexytopo.control.table;

import android.content.Context;
import android.text.Editable;
import android.text.InputType;
import android.widget.EditText;
import com.google.android.material.textfield.TextInputLayout;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.components.DialogUtils;
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

        this.stationNameLayout =
                DialogUtils.createStandardTextInputLayout(
                        context, R.string.manual_rename_station_hint);
        this.stationNameLayout.setErrorEnabled(true);

        this.stationName = DialogUtils.getEditText(this.stationNameLayout);
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
            setError(this.stationNameLayout, "Cannot be blank");
        } else if (currentTextString.equals("-")) {
            setError(this.stationNameLayout, "Station cannot be named \"-\"");
        } else if (!currentTextString.equals(currentName)
                && (survey.getStationByName(currentTextString) != null)) {
            setError(this.stationNameLayout, "Station name must be unique");
        } else {
            setError(this.stationNameLayout, (CharSequence) null);
        }
    }
}
