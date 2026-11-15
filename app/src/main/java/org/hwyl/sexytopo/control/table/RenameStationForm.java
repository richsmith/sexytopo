package org.hwyl.sexytopo.control.table;

import android.content.Context;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.EditText;
import com.google.android.material.textfield.TextInputLayout;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.components.DialogUtils;
import org.hwyl.sexytopo.control.util.StationNameRule;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;

/** Renames a station everywhere. The station is treated as its leg's new station. */
public class RenameStationForm extends Form {
    final Survey survey;
    final Station station;

    final EditText stationName;
    final TextInputLayout stationNameLayout;

    private StationNameRule.Result result;

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
        Context context = stationNameLayout.getContext();
        String name = stationName.getText().toString();
        result = StationNameRule.checkNew(survey, station, name, null);
        CharSequence error = StationNameText.error(context, result);
        CharSequence helper = StationNameText.newHelper(context, survey, station, result);

        // Setting the helper hides any error shown, so it is only set when it changes
        if (!TextUtils.equals(stationNameLayout.getHelperText(), helper)) {
            stationNameLayout.setHelperText(helper);
        }
        this.valid = this.valid & (error == null);
        stationNameLayout.setError(error);
    }

    /** What the name will do. Only call after validation passes. */
    public StationNameRule.Result getResult() {
        return result;
    }
}
