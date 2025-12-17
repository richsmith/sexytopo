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

    // Deg/Min/Sec fields (optional, used when in deg/min/sec mode)
    EditText azimuthDegreesField;
    EditText azimuthMinutesField;
    EditText azimuthSecondsField;

    /**
     * Constructor for editing an existing leg
     */
    public EditLegForm(Survey survey, Station fromStation, Leg legToEdit,
                       TextInputLayout fromStationLayout, EditText fromStationField,
                       TextInputLayout toStationLayout, EditText toStationField,
                       EditText distanceField, EditText azimuthField, EditText inclinationField,
                       EditText azimuthDegreesField, EditText azimuthMinutesField, EditText azimuthSecondsField) {
        super();
        this.survey = survey;
        this.originalLeg = legToEdit;
        this.originalFromStation = fromStation;
        this.isSplay = !legToEdit.hasDestination();

        // Use the provided layouts and fields from the dialog
        this.fromStationLayout = fromStationLayout;
        this.fromStationField = fromStationField;
        this.toStationLayout = toStationLayout;
        this.toStationField = toStationField;
        this.distanceField = distanceField;
        this.azimuthField = azimuthField;
        this.inclinationField = inclinationField;
        this.azimuthDegreesField = azimuthDegreesField;
        this.azimuthMinutesField = azimuthMinutesField;
        this.azimuthSecondsField = azimuthSecondsField;

        // Set up validation listeners
        this.fromStationField.addTextChangedListener(new TextViewValidationTrigger(this));
        if (!isSplay) {
            this.toStationField.addTextChangedListener(new TextViewValidationTrigger(this));
        }
        this.distanceField.addTextChangedListener(new TextViewValidationTrigger(this));
        this.azimuthField.addTextChangedListener(new TextViewValidationTrigger(this));
        this.inclinationField.addTextChangedListener(new TextViewValidationTrigger(this));

        // Populate the fields from the leg being edited
        this.fromStationField.setText(fromStation.getName());
        if (!isSplay) {
            this.toStationField.setText(legToEdit.getDestination().getName());
        }
    }

    /**
     * Constructor for adding a new leg (no existing leg to edit)
     */
    public EditLegForm(Survey survey, Station defaultFromStation, String defaultToName, boolean isSplay,
                       TextInputLayout fromStationLayout, EditText fromStationField,
                       TextInputLayout toStationLayout, EditText toStationField,
                       EditText distanceField, EditText azimuthField, EditText inclinationField,
                       EditText azimuthDegreesField, EditText azimuthMinutesField, EditText azimuthSecondsField) {
        super();
        this.survey = survey;
        this.originalLeg = null;  // No original leg when adding
        this.originalFromStation = defaultFromStation;
        this.isSplay = isSplay;

        // Use the provided layouts and fields from the dialog
        this.fromStationLayout = fromStationLayout;
        this.fromStationField = fromStationField;
        this.toStationLayout = toStationLayout;
        this.toStationField = toStationField;
        this.distanceField = distanceField;
        this.azimuthField = azimuthField;
        this.inclinationField = inclinationField;
        this.azimuthDegreesField = azimuthDegreesField;
        this.azimuthMinutesField = azimuthMinutesField;
        this.azimuthSecondsField = azimuthSecondsField;

        // Set up validation listeners
        this.fromStationField.addTextChangedListener(new TextViewValidationTrigger(this));
        if (!isSplay) {
            this.toStationField.addTextChangedListener(new TextViewValidationTrigger(this));
        }
        this.distanceField.addTextChangedListener(new TextViewValidationTrigger(this));
        this.azimuthField.addTextChangedListener(new TextViewValidationTrigger(this));
        this.inclinationField.addTextChangedListener(new TextViewValidationTrigger(this));

        // Populate with defaults for new leg
        this.fromStationField.setText(defaultFromStation.getName());
        if (!isSplay && defaultToName != null) {
            this.toStationField.setText(defaultToName);
        }
    }

    @Override
    protected void performValidation() {
        // Validate stations
        validateFromStation();
        if (!isSplay) {
            validateToStation();
        }
        // Validate measurements
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

        if (toStationName.isEmpty()) {
            setError(this.toStationField, "Cannot be blank");
        } else if (toStationName.equals("-")) {
            setError(this.toStationField, "Station cannot be named \"-\"");
        } else if (toStationName.equals(fromStationName)) {
            setError(this.toStationField, "Cannot be the same as from station");
        } else {
            // Check if we need to validate uniqueness
            boolean needsUniquenessCheck = true;

            // For edit mode: only check uniqueness if name is changing
            if (originalLeg != null && originalLeg.hasDestination()) {
                String currentToStationName = originalLeg.getDestination().getName();
                if (toStationName.equals(currentToStationName)) {
                    needsUniquenessCheck = false;
                }
            }

            if (needsUniquenessCheck) {
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

    /**
     * Parse and return the distance value
     * Should only be called after validation passes
     */
    public float getDistance() {
        return Float.parseFloat(this.distanceField.getText().toString());
    }

    /**
     * Parse and return the inclination value
     * Should only be called after validation passes
     */
    public float getInclination() {
        return Float.parseFloat(this.inclinationField.getText().toString());
    }

    /**
     * Parse and return the azimuth value
     * Handles both standard decimal and deg/min/sec modes
     * Should only be called after validation passes
     */
    public float getAzimuth() {
        // Check if we're using deg/min/sec mode by checking if those fields have values
        if (azimuthDegreesField != null && azimuthDegreesField.getText().length() > 0) {
            float degrees = Float.parseFloat(azimuthDegreesField.getText().toString());
            float minutes = Float.parseFloat(azimuthMinutesField.getText().toString());
            float seconds = Float.parseFloat(azimuthSecondsField.getText().toString());
            return degrees + (minutes * (1.0f / 60.0f)) + (seconds * (1.0f / 60.0f) * (1.0f / 60.0f));
        } else {
            // Standard decimal mode
            return Float.parseFloat(this.azimuthField.getText().toString());
        }
    }

    /**
     * Look up and return the from station
     * Should only be called after validation passes
     */
    public Station getFromStation() {
        return survey.getStationByName(getFromStationName());
    }

    /**
     * Look up and return the to station
     * Only valid for full legs (not splays)
     * Should only be called after validation passes
     */
    public Station getToStation() {
        if (isSplay) {
            throw new IllegalStateException("Cannot get to station for a splay");
        }
        return survey.getStationByName(getToStationName());
    }
}
