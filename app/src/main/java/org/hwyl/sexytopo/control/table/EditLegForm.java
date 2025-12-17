package org.hwyl.sexytopo.control.table;

import android.text.Editable;
import android.widget.EditText;

import com.google.android.material.textfield.TextInputLayout;

import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;

public class EditLegForm extends Form {
    Survey survey;
    Leg originalLeg;
    Station originalFromStation;
    boolean isSplay;

    EditText fromStationField;
    TextInputLayout fromStationLayout;
    EditText toStationField;
    TextInputLayout toStationLayout;

    EditText distanceField;
    EditText azimuthField;
    EditText inclinationField;

    public EditLegForm(Survey survey, Station fromStation, Leg leg,
                       TextInputLayout fromStationLayout, EditText fromStationField,
                       TextInputLayout toStationLayout, EditText toStationField,
                       EditText distanceField, EditText azimuthField, EditText inclinationField) {
        super();
        this.survey = survey;
        this.originalLeg = leg;
        this.originalFromStation = fromStation;
        this.isSplay = !leg.hasDestination();

        // Use the provided layouts and fields from the dialog
        this.fromStationLayout = fromStationLayout;
        this.fromStationField = fromStationField;
        this.toStationLayout = toStationLayout;
        this.toStationField = toStationField;
        this.distanceField = distanceField;
        this.azimuthField = azimuthField;
        this.inclinationField = inclinationField;

        // Set up validation listeners
        this.fromStationField.addTextChangedListener(new TextViewValidationTrigger(this));
        if (!isSplay) {
            this.toStationField.addTextChangedListener(new TextViewValidationTrigger(this));
        }
        this.distanceField.addTextChangedListener(new TextViewValidationTrigger(this));
        this.azimuthField.addTextChangedListener(new TextViewValidationTrigger(this));
        this.inclinationField.addTextChangedListener(new TextViewValidationTrigger(this));

        // Populate the fields
        this.fromStationField.setText(fromStation.getName());
        if (!isSplay) {
            this.toStationField.setText(leg.getDestination().getName());
        }
    }

    @Override
    protected void performValidation() {
        validateFromStation();
        if (!isSplay) {
            validateToStation();
        }
        validateDistance();
        validateAzimuth();
        validateInclination();
    }

    private void validateFromStation() {
        String fromStationName = this.fromStationField.getText().toString();

        if (fromStationName.isEmpty()) {
            setError(this.fromStationField, "Cannot be blank");
        } else if (fromStationName.equals("-")) {
            setError(this.fromStationField, "Station cannot be named \"-\"");
        } else {
            Station fromStation = survey.getStationByName(fromStationName);
            if (fromStation == null) {
                setError(this.fromStationField, "Station does not exist");
            } else {
                setError(this.fromStationField, null);
            }
        }
    }

    private void validateToStation() {
        String toStationName = this.toStationField.getText().toString();
        String fromStationName = this.fromStationField.getText().toString();
        String currentToStationName = originalLeg.getDestination().getName();

        if (toStationName.isEmpty()) {
            setError(this.toStationField, "Cannot be blank");
        } else if (toStationName.equals("-")) {
            setError(this.toStationField, "Station cannot be named \"-\"");
        } else if (toStationName.equals(fromStationName)) {
            setError(this.toStationField, "Cannot be the same as from station");
        } else if (!toStationName.equals(currentToStationName)) {
            // Only check for uniqueness if we're changing the name
            Station existing = survey.getStationByName(toStationName);
            if (existing != null) {
                setError(this.toStationField, "Station name must be unique");
            } else {
                setError(this.toStationField, null);
            }
        } else {
            setError(this.toStationField, null);
        }
    }

    private void validateDistance() {
        String distanceText = this.distanceField.getText().toString();
        try {
            if (distanceText.isEmpty()) {
                setError(this.distanceField, "Cannot be blank");
            } else {
                float distance = Float.parseFloat(distanceText);
                if (!Leg.isDistanceLegal(distance)) {
                    setError(this.distanceField, "Must be >= " + Leg.MIN_DISTANCE);
                } else {
                    setError(this.distanceField, null);
                }
            }
        } catch (NumberFormatException e) {
            setError(this.distanceField, "Must be a number");
        }
    }

    private void validateAzimuth() {
        String azimuthText = this.azimuthField.getText().toString();
        try {
            if (azimuthText.isEmpty()) {
                setError(this.azimuthField, "Cannot be blank");
            } else {
                float azimuth = Float.parseFloat(azimuthText);
                if (!Leg.isAzimuthLegal(azimuth)) {
                    setError(this.azimuthField, "Must be " + Leg.MIN_AZIMUTH + "-" + Leg.MAX_AZIMUTH);
                } else {
                    setError(this.azimuthField, null);
                }
            }
        } catch (NumberFormatException e) {
            setError(this.azimuthField, "Must be a number");
        }
    }

    private void validateInclination() {
        String inclinationText = this.inclinationField.getText().toString();
        try {
            if (inclinationText.isEmpty()) {
                setError(this.inclinationField, "Cannot be blank");
            } else {
                float inclination = Float.parseFloat(inclinationText);
                if (!Leg.isInclinationLegal(inclination)) {
                    setError(this.inclinationField, "Must be " + Leg.MIN_INCLINATION + " to " + Leg.MAX_INCLINATION);
                } else {
                    setError(this.inclinationField, null);
                }
            }
        } catch (NumberFormatException e) {
            setError(this.inclinationField, "Must be a number");
        }
    }

    public String getFromStationName() {
        return this.fromStationField.getText().toString();
    }

    public String getToStationName() {
        return this.toStationField.getText().toString();
    }
}
