package org.hwyl.sexytopo.control.table;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import com.google.android.material.textfield.TextInputLayout;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.SexyTopoConstants;
import org.hwyl.sexytopo.control.util.GeneralPreferences;
import org.hwyl.sexytopo.control.util.InputMode;
import org.hwyl.sexytopo.control.util.SurveyTools;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;

public class EditLegForm extends Form {
    private static final int SPINNER_FORWARD = 0;
    private static final int SPINNER_BACKWARD = 1;

    /**
     * A TextWatcher that, in addition to triggering validation, enables range-error display as soon
     * as the watched field contains any non-empty text. This lets out-of-range errors appear
     * immediately while the user is typing, without showing a "cannot be blank" error on an
     * untouched field.
     */
    private static class RangeValidationTrigger extends TextViewValidationTrigger {
        private final android.widget.EditText field;

        RangeValidationTrigger(Form form, android.widget.EditText field) {
            super(form);
            this.field = field;
        }

        @Override
        public void afterTextChanged(android.text.Editable editable) {
            if (field.getText().length() > 0) {
                form.enableRangeErrors();
            }
            super.afterTextChanged(editable);
        }
    }

    private final Context context;
    private final Survey survey;
    private String defaultToName;
    private final Station originalFromStation;
    private final Leg originalLeg;
    private final boolean isSplay;
    private boolean isEditingLeg;

    private EditText fromStationField;
    private EditText toStationField;
    // These point to one of the above fields, depending on input mode
    private EditText graphFromStationField;
    private EditText graphToStationField;

    private EditText fromCommentField;
    private EditText toCommentField;

    private Station lastFromStation;

    private TextInputLayout fromStationLayout;
    private TextInputLayout toStationLayout;
    private TextInputLayout distanceLayout;
    private TextInputLayout azimuthLayout;
    private TextInputLayout inclinationLayout;

    // TextInputLayout wrappers for DMS minutes/seconds fields, used to show range errors
    private TextInputLayout azimuthMinutesLayout;
    private TextInputLayout azimuthSecondsLayout;
    private TextInputLayout inclinationMinutesLayout;
    private TextInputLayout inclinationSecondsLayout;
    private TextInputLayout azimuthDegreesLayout;
    private TextInputLayout inclinationDegreesLayout;

    private EditText distanceField;
    private EditText azimuthField;
    private EditText inclinationField;

    // Deg/Min/Sec fields (optional, used when in deg/min/sec mode)
    private EditText azimuthDegreesField;
    private EditText azimuthMinutesField;
    private EditText azimuthSecondsField;
    private EditText inclinationDegreesField;
    private EditText inclinationMinutesField;
    private EditText inclinationSecondsField;

    private Spinner inputModeSpinner;
    private InputMode inputMode = InputMode.FORWARD;

    private boolean isInitialising;

    /** Constructor for editing an existing leg */
    public EditLegForm(
            Context context, Survey survey, Station fromStation, Leg legToEdit, View dialogView) {
        super(context);
        this.context = context;
        this.survey = survey;
        this.originalFromStation = fromStation;
        this.originalLeg = legToEdit;
        this.isSplay = !legToEdit.hasDestination();
        this.inputMode = legToEdit.wasShotBackwards() ? InputMode.BACKWARD : InputMode.FORWARD;

        this.initialise(dialogView);
    }

    /** Constructor for adding a new leg (no existing leg to edit) */
    public EditLegForm(
            Context context,
            Survey survey,
            Station fromStation,
            String defaultToName,
            boolean isSplay,
            View dialogView) {
        super(context);
        this.context = context;
        this.survey = survey;
        this.originalFromStation = fromStation;
        this.lastFromStation = fromStation;
        this.originalLeg = null; // No original leg when adding
        this.defaultToName = defaultToName;
        this.isSplay = isSplay;

        this.initialise(dialogView);
    }

    private void initialise(View dialogView) {
        this.isInitialising = true;
        this.initialiseFields(dialogView);
        this.initialiseInputMode(dialogView);
        this.initialiseStationDisplay();
        this.isInitialising = false;
    }

    private void initialiseFields(View dialogView) {
        // Find all view references from the dialog
        this.fromStationLayout = dialogView.findViewById(R.id.fromStationLayout);
        this.toStationLayout = dialogView.findViewById(R.id.toStationLayout);
        this.distanceLayout = dialogView.findViewById(R.id.distanceLayout);
        this.azimuthLayout = dialogView.findViewById(R.id.azimuth_standard);
        this.inclinationLayout = dialogView.findViewById(R.id.inclinationLayout);
        this.azimuthMinutesLayout = dialogView.findViewById(R.id.editAzimuthMinutesLayout);
        this.azimuthSecondsLayout = dialogView.findViewById(R.id.editAzimuthSecondsLayout);
        this.inclinationMinutesLayout = dialogView.findViewById(R.id.editInclinationMinutesLayout);
        this.inclinationSecondsLayout = dialogView.findViewById(R.id.editInclinationSecondsLayout);
        this.azimuthDegreesLayout = dialogView.findViewById(R.id.editAzimuthDegreesLayout);
        this.inclinationDegreesLayout = dialogView.findViewById(R.id.editInclinationDegreesLayout);

        this.fromStationField = dialogView.findViewById(R.id.editFromStation);
        this.fromCommentField = dialogView.findViewById(R.id.editFromComment);
        this.toStationField = dialogView.findViewById(R.id.editToStation);
        this.toCommentField = dialogView.findViewById(R.id.editToComment);
        this.distanceField = dialogView.findViewById(R.id.editDistance);
        this.azimuthField = dialogView.findViewById(R.id.editAzimuth);
        this.inclinationField = dialogView.findViewById(R.id.editInclination);
        this.azimuthDegreesField = dialogView.findViewById(R.id.editAzimuthDegrees);
        this.azimuthMinutesField = dialogView.findViewById(R.id.editAzimuthMinutes);
        this.azimuthSecondsField = dialogView.findViewById(R.id.editAzimuthSeconds);
        this.inclinationDegreesField = dialogView.findViewById(R.id.editInclinationDegrees);
        this.inclinationMinutesField = dialogView.findViewById(R.id.editInclinationMinutes);
        this.inclinationSecondsField = dialogView.findViewById(R.id.editInclinationSeconds);
        this.inputModeSpinner = dialogView.findViewById(R.id.inputModeSpinner);

        // Set up validation listeners
        this.fromStationField.addTextChangedListener(new TextViewValidationTrigger(this));
        if (!isSplay) {
            this.toStationField.addTextChangedListener(new TextViewValidationTrigger(this));
        }
        this.distanceField.addTextChangedListener(new TextViewValidationTrigger(this));
        this.azimuthField.addTextChangedListener(
                new RangeValidationTrigger(this, this.azimuthField));
        this.inclinationField.addTextChangedListener(
                new RangeValidationTrigger(this, this.inclinationField));

        if (GeneralPreferences.isDegMinsSecsModeOn()) {
            this.azimuthDegreesField.addTextChangedListener(new TextViewValidationTrigger(this));
            this.azimuthMinutesField.addTextChangedListener(new TextViewValidationTrigger(this));
            this.azimuthSecondsField.addTextChangedListener(new TextViewValidationTrigger(this));
        }
        if (GeneralPreferences.isIncDegMinsSecsModeOn()) {
            this.inclinationDegreesField.addTextChangedListener(
                    new TextViewValidationTrigger(this));
            this.inclinationMinutesField.addTextChangedListener(
                    new TextViewValidationTrigger(this));
            this.inclinationSecondsField.addTextChangedListener(
                    new TextViewValidationTrigger(this));
        }

        // Enable error display once the user leaves a field for the first time.
        android.view.View.OnFocusChangeListener enableErrorsOnTouch =
                (v, hasFocus) -> {
                    if (!hasFocus) {
                        enableErrors();
                        validate();
                    }
                };
        this.fromStationField.setOnFocusChangeListener(enableErrorsOnTouch);
        if (!isSplay) {
            this.toStationField.setOnFocusChangeListener(enableErrorsOnTouch);
        }
        this.distanceField.setOnFocusChangeListener(enableErrorsOnTouch);
        this.azimuthField.setOnFocusChangeListener(enableErrorsOnTouch);
        this.inclinationField.setOnFocusChangeListener(enableErrorsOnTouch);
    }

    private void initialiseInputMode(View dialogView) {
        if (!isSplay) {
            View inputModeContainer = dialogView.findViewById(R.id.inputModeContainer);
            inputModeContainer.setVisibility(View.VISIBLE);

            ArrayAdapter<CharSequence> adapter =
                    ArrayAdapter.createFromResource(
                            context,
                            R.array.leg_edit_input_mode_options,
                            android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            this.inputModeSpinner.setAdapter(adapter);

            // Set initial selection
            this.inputModeSpinner.setSelection(
                    inputMode == InputMode.FORWARD ? SPINNER_FORWARD : SPINNER_BACKWARD);

            // Set up spinner listener to update display when selection changes
            this.inputModeSpinner.setOnItemSelectedListener(
                    new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(
                                AdapterView<?> parent, View view, int position, long id) {
                            InputMode newMode =
                                    position == SPINNER_FORWARD
                                            ? InputMode.FORWARD
                                            : InputMode.BACKWARD;
                            boolean isChange = inputMode != newMode;

                            if (isChange) {
                                inputMode = newMode;

                                if (!isInitialising) {
                                    swapStationDisplay();
                                }
                                validate();
                            }
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {
                            // Do nothing
                        }
                    });
        }
    }

    @Override
    protected void performValidation() {
        // Validate stations
        EditText graphFromField =
                inputMode == InputMode.BACKWARD ? toStationField : fromStationField;
        EditText graphToField = inputMode == InputMode.BACKWARD ? fromStationField : toStationField;

        Station fromStation = validateGraphFromField(graphFromField);
        if (!isSplay) {
            validateGraphToField(fromStation, graphToField);
        }

        // Validate measurements
        validateDistance();
        validateAzimuth();
        validateInclination();
    }

    private Station validateGraphFromField(EditText fromField) {
        String fromName = fromField.getText().toString();
        Station fromStation = survey.getStationByName(fromName);

        Integer error = null;

        if (fromName.isEmpty()) {
            error = R.string.validation_error_cannot_be_blank;
        } else if (fromName.equals(SexyTopoConstants.BLANK_STATION_NAME)) {
            error = R.string.validation_error_station_named_dash;
        } else if (survey.isOrigin(originalFromStation) && fromStation == null) {
            // Are we just renaming the origin station
            boolean isRenamingStation = !originalFromStation.getName().equals(fromName);

            // For edit mode: only check uniqueness if the graph from station name is changing
            if (isRenamingStation) {
                Station existing = survey.getStationByName(fromName);
                if (existing == null) {
                    fromStation =
                            survey.getOrigin(); // Send back the origin station so that it can be
                    // renamed
                } else {
                    error = R.string.validation_error_station_name_not_unique;
                }
            }
        } else if (fromStation == null) {
            error = R.string.validation_error_station_does_not_exist;
        } else if (originalLeg != null && originalLeg.hasDestination()) {
            Station originalStation = survey.getOriginatingStation(originalLeg);
            boolean isMovingLeg = originalStation != fromStation;

            if (isMovingLeg) {
                if (SurveyTools.isInSubtree(originalLeg.getDestination(), fromStation)) {
                    error = R.string.survey_update_error_descendant_station;
                }
            }
        }

        // Has the from station changed
        if (fromStation != null && fromStation != lastFromStation) {
            // Update the comment to the new station
            String fromComment = "";
            if (fromStation.hasComment()) {
                fromComment = fromStation.getComment();
            }

            fromCommentField.setText(fromComment);
            lastFromStation = fromStation;
        }

        TextInputLayout layout =
                (fromField == fromStationField) ? fromStationLayout : toStationLayout;
        if (layout != null) {
            setError(layout, error);
        } else {
            setError(fromField, error);
        }
        return fromStation;
    }

    private void validateGraphToField(Station fromStation, EditText toField) {
        String fromName = fromStation == null ? null : fromStation.getName();
        String toName = toField.getText().toString();

        Integer error = null;

        if (toName.isEmpty()) {
            error = R.string.validation_error_cannot_be_blank;
        } else if (toName.equals(SexyTopoConstants.BLANK_STATION_NAME)) {
            error = R.string.validation_error_station_named_dash;
        } else if (toName.equals(fromName)) {
            error = R.string.validation_error_same_as_from_station;
        } else if (originalLeg != null && originalLeg.hasDestination()) {
            // Only validate uniqueness for existing legs being edited
            Station originalStation = originalLeg.getDestination();
            boolean isRenamingStation = !originalStation.getName().equals(toName);

            // For edit mode: only check uniqueness if the graph to station name is changing
            if (isRenamingStation) {
                Station existing = survey.getStationByName(toName);
                if (existing != null) {
                    error = R.string.validation_error_station_name_not_unique;
                }
            }
        } else {
            // New leg - check that station name doesn't already exist
            Station existing = survey.getStationByName(toName);
            if (existing != null) {
                error = R.string.validation_error_station_name_not_unique;
            }
        }

        TextInputLayout layout = (toField == toStationField) ? toStationLayout : fromStationLayout;
        if (layout != null) {
            setError(layout, error);
        } else {
            setError(toField, error);
        }
    }

    private void validateDistance() {
        String distanceText = this.distanceField.getText().toString();
        try {
            if (distanceText.isEmpty()) {
                setError(this.distanceLayout, R.string.validation_error_cannot_be_blank);
            } else {
                float distance = Float.parseFloat(distanceText);
                if (!Leg.isDistanceLegal(distance)) {
                    setError(
                            this.distanceLayout,
                            context.getString(
                                    R.string.validation_error_distance_minimum, Leg.MIN_DISTANCE));
                } else {
                    setError(this.distanceLayout, (Integer) null);
                }
            }
        } catch (NumberFormatException e) {
            setError(
                    this.distanceLayout,
                    context.getString(R.string.validation_error_must_be_number));
        }
    }

    private void validateAzimuth() {
        if (GeneralPreferences.isDegMinsSecsModeOn()) {
            validateAzimuthDms();
        } else {
            validateAzimuthDecimal();
        }
    }

    private void validateAzimuthDecimal() {
        String azimuthText = this.azimuthField.getText().toString();
        try {
            if (azimuthText.isEmpty()) {
                setError(
                        this.azimuthLayout,
                        context.getString(R.string.validation_error_cannot_be_blank));
            } else {
                float azimuth = Float.parseFloat(azimuthText);
                if (!Leg.isAzimuthLegal(azimuth)) {
                    setRangeError(
                            this.azimuthLayout,
                            context.getString(
                                    R.string.validation_error_azimuth_range,
                                    Leg.MIN_AZIMUTH,
                                    Leg.MAX_AZIMUTH));
                } else {
                    setRangeError(this.azimuthLayout, (Integer) null);
                }
            }
        } catch (NumberFormatException e) {
            setError(
                    this.azimuthLayout,
                    context.getString(R.string.validation_error_must_be_number));
        }
    }

    private void validateAzimuthDms() {
        // Degrees is required; missing minutes/seconds are treated as zero.
        if (azimuthDegreesField.getText().toString().isEmpty()) {
            this.valid = false;
            return;
        }
        // Degrees has content - enable range error display, mirroring what
        // RangeValidationTrigger does for the decimal azimuth field.
        enableRangeErrors();
        boolean dmsValid =
                validateMinutesField(azimuthMinutesLayout, azimuthMinutesField)
                        & validateSecondsField(azimuthSecondsLayout, azimuthSecondsField);
        if (!dmsValid) return;

        // Compute decimal equivalent and range-check it, showing error on degrees layout
        try {
            float azimuth = getAzimuth();
            if (!Leg.isAzimuthLegal(azimuth)) {
                setRangeError(
                        this.azimuthDegreesLayout,
                        context.getString(
                                R.string.validation_error_azimuth_range,
                                Leg.MIN_AZIMUTH,
                                Leg.MAX_AZIMUTH));
            } else {
                setRangeError(this.azimuthDegreesLayout, (Integer) null);
            }
        } catch (NumberFormatException e) {
            // Degrees field contains a partial value (e.g. just "-"), not yet parseable
            this.valid = false;
        }
    }

    private void validateInclination() {
        if (GeneralPreferences.isIncDegMinsSecsModeOn()) {
            validateInclinationDms();
        } else {
            validateInclinationDecimal();
        }
    }

    private void validateInclinationDecimal() {
        String inclinationText = this.inclinationField.getText().toString();
        try {
            if (inclinationText.isEmpty()) {
                setError(
                        this.inclinationLayout,
                        context.getString(R.string.validation_error_cannot_be_blank));
            } else {
                float inclination = Float.parseFloat(inclinationText);
                if (!Leg.isInclinationLegal(inclination)) {
                    setRangeError(
                            this.inclinationLayout,
                            context.getString(R.string.validation_error_inclination_range));
                } else {
                    setRangeError(this.inclinationLayout, (Integer) null);
                }
            }
        } catch (NumberFormatException e) {
            setError(
                    this.inclinationLayout,
                    context.getString(R.string.validation_error_must_be_number));
        }
    }

    private void validateInclinationDms() {
        // Degrees is required; missing minutes/seconds are treated as zero.
        if (inclinationDegreesField.getText().toString().isEmpty()) {
            this.valid = false;
            return;
        }
        // Degrees has content - enable range error display, mirroring what
        // RangeValidationTrigger does for the decimal inclination field.
        enableRangeErrors();
        boolean dmsValid =
                validateMinutesField(inclinationMinutesLayout, inclinationMinutesField)
                        & validateSecondsField(inclinationSecondsLayout, inclinationSecondsField);
        if (!dmsValid) return;

        // Compute decimal equivalent and range-check it, showing error on degrees layout.
        // Wrap in try/catch to handle partial input such as a lone "-" which is not yet
        // parseable as a float but is a valid intermediate state while typing a negative value.
        try {
            float inclination = getInclination();
            if (!Leg.isInclinationLegal(inclination)) {
                setRangeError(
                        this.inclinationDegreesLayout,
                        context.getString(R.string.validation_error_inclination_range));
            } else {
                setRangeError(this.inclinationDegreesLayout, (Integer) null);
            }
        } catch (NumberFormatException e) {
            // Degrees field contains a partial value (e.g. just "-"), not yet parseable
            this.valid = false;
        }
    }

    /**
     * Validates a minutes field. Blank is accepted (treated as zero). If non-blank, the value must
     * be a whole number in the range 0–59. Sets an error on the layout and marks the form invalid
     * if not. Returns true if the field is valid (or blank), false otherwise.
     */
    private boolean validateMinutesField(TextInputLayout layout, EditText field) {
        String text = field.getText().toString();
        if (text.isEmpty()) {
            setError(layout, (Integer) null);
            return true;
        }
        if (text.contains(".")) {
            setError(layout, context.getString(R.string.validation_error_must_be_whole_number));
            return false;
        }
        try {
            int value = Integer.parseInt(text);
            if (value < 0 || value > 59) {
                setError(layout, context.getString(R.string.validation_error_mins_secs_range));
                return false;
            }
            setError(layout, (Integer) null);
            return true;
        } catch (NumberFormatException e) {
            setError(layout, context.getString(R.string.validation_error_must_be_whole_number));
            return false;
        }
    }

    /**
     * Validates a seconds field. Blank is accepted (treated as zero). If non-blank, the value must
     * be a non-negative number less than 60; decimal values (e.g. {@code 30.5}) are permitted. Sets
     * an error on the layout and marks the form invalid if not. Returns true if the field is valid
     * (or blank), false otherwise.
     */
    private boolean validateSecondsField(TextInputLayout layout, EditText field) {
        String text = field.getText().toString();
        if (text.isEmpty()) {
            setError(layout, (Integer) null);
            return true;
        }
        try {
            float value = Float.parseFloat(text);
            if (value < 0 || value >= 60) {
                setError(layout, context.getString(R.string.validation_error_secs_range));
                return false;
            }
            setError(layout, (Integer) null);
            return true;
        } catch (NumberFormatException e) {
            setError(layout, context.getString(R.string.validation_error_must_be_number));
            return false;
        }
    }

    /** Update the station display based on current shot direction */
    private void swapStationDisplay() {
        String fromText = graphFromStationField.getText().toString();
        String toText = graphToStationField.getText().toString();
        graphFromStationField.setText(toText);
        graphToStationField.setText(fromText);
    }

    private void mapGraphFields() {
        if (isSplay) {
            graphFromStationField = fromStationField;
            graphToStationField = toStationField; // not used for splay but probably safer to set
        } else {
            graphFromStationField =
                    inputMode == InputMode.BACKWARD ? toStationField : fromStationField;
            graphToStationField =
                    inputMode == InputMode.BACKWARD ? fromStationField : toStationField;
        }
    }

    /** Initialise the station display based on current data */
    private void initialiseStationDisplay() {
        mapGraphFields();
        String fromComment = "";
        if (originalFromStation.hasComment()) {
            fromComment = originalFromStation.getComment();
        }
        graphFromStationField.setText(originalFromStation.getName());
        fromCommentField.setText(fromComment);

        if (!isSplay) {
            String toName = "";
            String toComment = ""; // Not used for splay
            if (originalLeg != null) {
                toName = originalLeg.getDestination().getName();
                Station destStation = survey.getStationByName(toName);
                if (destStation != null && destStation.hasComment()) {
                    toComment = destStation.getComment();
                }

            } else if (defaultToName != null) {
                toName = defaultToName;
            }
            graphToStationField.setText(toName);
            toCommentField.setText(toComment);
        }
    }

    private String getFromStationName() {
        if (inputMode == InputMode.BACKWARD && !isSplay) {
            return toStationField.getText().toString();
        }
        return fromStationField.getText().toString();
    }

    private float getDistance() {
        return Float.parseFloat(this.distanceField.getText().toString());
    }

    private float getInclination() {
        // Check if we're using deg/min/sec mode by checking if those fields have values
        if (inclinationDegreesField != null && inclinationDegreesField.getText().length() > 0) {
            float degrees = Float.parseFloat(inclinationDegreesField.getText().toString());
            String minsText = inclinationMinutesField.getText().toString();
            String secsText = inclinationSecondsField.getText().toString();
            float minutes = minsText.isEmpty() ? 0.0f : Float.parseFloat(minsText);
            float seconds = secsText.isEmpty() ? 0.0f : Float.parseFloat(secsText);
            // The sign of the degrees component determines the direction of the inclination.
            // Minutes and seconds are always positive and added in the same direction.
            float sign = degrees < 0 ? -1.0f : 1.0f;
            return degrees
                    + sign * (minutes * (1.0f / 60.0f) + seconds * (1.0f / 60.0f) * (1.0f / 60.0f));
        } else {
            // Standard decimal mode
            return Float.parseFloat(this.inclinationField.getText().toString());
        }
    }

    private float getAzimuth() {
        // Check if we're using deg/min/sec mode by checking if those fields have values
        if (azimuthDegreesField != null && azimuthDegreesField.getText().length() > 0) {
            float degrees = Float.parseFloat(azimuthDegreesField.getText().toString());
            String minsText = azimuthMinutesField.getText().toString();
            String secsText = azimuthSecondsField.getText().toString();
            float minutes = minsText.isEmpty() ? 0.0f : Float.parseFloat(minsText);
            float seconds = secsText.isEmpty() ? 0.0f : Float.parseFloat(secsText);
            return degrees
                    + (minutes * (1.0f / 60.0f))
                    + (seconds * (1.0f / 60.0f) * (1.0f / 60.0f));
        } else {
            // Standard decimal mode
            return Float.parseFloat(this.azimuthField.getText().toString());
        }
    }

    private Station getFromStation() {
        Station fromStation = survey.getStationByName(getFromStationName());
        if (fromStation == null && survey.isOrigin(originalFromStation)) {
            // As a special case if we can't find the from station and we are
            // at the origin, we must be wanting to rename it - so return the origin
            return (originalFromStation);
        }
        return fromStation;
    }

    private String getToStationName() {
        if (inputMode == InputMode.BACKWARD) {
            return fromStationField.getText().toString();
        }
        return toStationField.getText().toString();
    }

    private String getToComment() {
        if (toCommentField != null) {
            return toCommentField.getText().toString();
        }
        return null;
    }

    private String getFromComment() {
        if (fromCommentField != null) {
            return fromCommentField.getText().toString();
        }
        return null;
    }

    /**
     * Create a Leg object from the form data with measurements and shot direction. For editing:
     * preserves the existing destination station object. For adding: creates a splay-like leg
     * (caller must create destination and reconstruct). Should only be called after validation
     * passes.
     */
    public Leg getUpdatedLeg() {
        float distance = getDistance();
        float azimuth = getAzimuth();
        float inclination = getInclination();

        Leg leg;
        if (isSplay) {
            leg = new Leg(distance, azimuth, inclination);
        } else if (originalLeg != null && originalLeg.hasDestination()) {
            // For editing: reuse existing destination station object
            Station destination = originalLeg.getDestination();
            leg = new Leg(distance, azimuth, inclination, destination, new Leg[] {});
        } else {
            // For adding a new station: create a temporary splay-like leg
            // Caller will reconstruct with proper destination station
            leg = new Leg(distance, azimuth, inclination);
        }

        // Apply backwards flag if needed
        if (inputMode == InputMode.BACKWARD) {
            leg = leg.reverse();
        }

        return leg;
    }

    /** Get the from station for the leg. Should only be called after validation passes. */
    public Station getUpdatedFromStation() {
        return getFromStation();
    }

    public String getUpdatedFromStationName() {
        return getFromStationName();
    }

    public String getUpdatedFromComment() {
        return getFromComment();
    }

    /**
     * Get the to station name for the leg. Returns null for splays. Should only be called after
     * validation passes.
     */
    public String getUpdatedToStationName() {
        return isSplay ? null : getToStationName();
    }

    public String getUpdatedToComment() {
        return isSplay ? null : getToComment();
    }
}
