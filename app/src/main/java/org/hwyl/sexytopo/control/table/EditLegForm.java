package org.hwyl.sexytopo.control.table;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.SexyTopoConstants;
import org.hwyl.sexytopo.control.util.InputMode;
import org.hwyl.sexytopo.control.util.SurveyTools;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;

public class EditLegForm extends Form {
    private static final int SPINNER_FORWARD = 0;
    private static final int SPINNER_BACKWARD = 1;

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

    private EditText distanceField;
    private EditText azimuthField;
    private EditText inclinationField;

    // Deg/Min/Sec fields (optional, used when in deg/min/sec mode)
    private EditText azimuthDegreesField;
    private EditText azimuthMinutesField;
    private EditText azimuthSecondsField;

    private Spinner inputModeSpinner;
    private InputMode inputMode = InputMode.FORWARD;

    private boolean isInitialising;

    /**
     * Constructor for editing an existing leg
     */
    public EditLegForm(Context context, Survey survey, Station fromStation, Leg legToEdit, View dialogView) {
        super(context);
        this.context = context;
        this.survey = survey;
        this.originalFromStation = fromStation;
        this.originalLeg = legToEdit;
        this.isSplay = !legToEdit.hasDestination();
        this.inputMode = legToEdit.wasShotBackwards() ? InputMode.BACKWARD : InputMode.FORWARD;

        this.initialise(dialogView);
    }

    /**
     * Constructor for adding a new leg (no existing leg to edit)
     */
    public EditLegForm(Context context, Survey survey, Station fromStation, String defaultToName,
                       boolean isSplay, View dialogView) {
        super(context);
        this.context = context;
        this.survey = survey;
        this.originalFromStation = fromStation;
        this.originalLeg = null;  // No original leg when adding
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
        this.fromStationField = dialogView.findViewById(R.id.editFromStation);
        this.toStationField = dialogView.findViewById(R.id.editToStation);
        this.distanceField = dialogView.findViewById(R.id.editDistance);
        this.azimuthField = dialogView.findViewById(R.id.editAzimuth);
        this.inclinationField = dialogView.findViewById(R.id.editInclination);
        this.azimuthDegreesField = dialogView.findViewById(R.id.editAzimuthDegrees);
        this.azimuthMinutesField = dialogView.findViewById(R.id.editAzimuthMinutes);
        this.azimuthSecondsField = dialogView.findViewById(R.id.editAzimuthSeconds);
        this.inputModeSpinner = dialogView.findViewById(R.id.inputModeSpinner);

        // Set up validation listeners
        this.fromStationField.addTextChangedListener(new TextViewValidationTrigger(this));
        if (!isSplay) {
            this.toStationField.addTextChangedListener(new TextViewValidationTrigger(this));
        }
        this.distanceField.addTextChangedListener(new TextViewValidationTrigger(this));
        this.azimuthField.addTextChangedListener(new TextViewValidationTrigger(this));
        this.inclinationField.addTextChangedListener(new TextViewValidationTrigger(this));

    }

    private void initialiseInputMode(View dialogView) {
        if (!isSplay) {
            View inputModeContainer = dialogView.findViewById(R.id.inputModeContainer);
            inputModeContainer.setVisibility(View.VISIBLE);

            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                context, R.array.leg_edit_input_mode_options, android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            this.inputModeSpinner.setAdapter(adapter);

            // Set initial selection
            this.inputModeSpinner.setSelection(
                inputMode == InputMode.FORWARD ? SPINNER_FORWARD : SPINNER_BACKWARD);

            // Set up spinner listener to update display when selection changes
            this.inputModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    InputMode newMode = position == SPINNER_FORWARD ?
                        InputMode.FORWARD : InputMode.BACKWARD;
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
        EditText graphFromField = inputMode == InputMode.BACKWARD ? toStationField : fromStationField;
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

        setError(fromField, error);
        return fromStation;
    }

    private void validateGraphToField(Station fromStation, EditText toField) {
        String fromName = fromStation == null? null : fromStation.getName();
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

        setError(toField, error);
    }

    private void validateDistance() {
        String distanceText = this.distanceField.getText().toString();
        try {
            if (distanceText.isEmpty()) {
                setError(this.distanceField, R.string.validation_error_cannot_be_blank);
            } else {
                float distance = Float.parseFloat(distanceText);
                if (!Leg.isDistanceLegal(distance)) {
                    setError(this.distanceField,
                        context.getString(R.string.validation_error_distance_minimum,
                            Leg.MIN_DISTANCE));
                } else {
                    setError(this.distanceField, (Integer) null);
                }
            }
        } catch (NumberFormatException e) {
            setError(this.distanceField, context.getString(R.string.validation_error_must_be_number));
        }
    }

    private void validateAzimuth() {
        String azimuthText = this.azimuthField.getText().toString();
        try {
            if (azimuthText.isEmpty()) {
                setError(this.azimuthField, context.getString(R.string.validation_error_cannot_be_blank));
            } else {
                float azimuth = Float.parseFloat(azimuthText);
                if (!Leg.isAzimuthLegal(azimuth)) {
                    setError(this.azimuthField,
                        context.getString(R.string.validation_error_azimuth_range, Leg.MIN_AZIMUTH, Leg.MAX_AZIMUTH));
                } else {
                    setError(this.azimuthField, (Integer) null);
                }
            }
        } catch (NumberFormatException e) {
            setError(this.azimuthField, context.getString(R.string.validation_error_must_be_number));
        }
    }

    private void validateInclination() {
        String inclinationText = this.inclinationField.getText().toString();
        try {
            if (inclinationText.isEmpty()) {
                setError(this.inclinationField, context.getString(R.string.validation_error_cannot_be_blank));
            } else {
                float inclination = Float.parseFloat(inclinationText);
                if (!Leg.isInclinationLegal(inclination)) {
                    setError(this.inclinationField,
                        context.getString(R.string.validation_error_inclination_range, Leg.MIN_INCLINATION, Leg.MAX_INCLINATION));
                } else {
                    setError(this.inclinationField, (Integer) null);
                }
            }
        } catch (NumberFormatException e) {
            setError(this.inclinationField, context.getString(R.string.validation_error_must_be_number));
        }
    }

    /**
     * Update the station display based on current shot direction
     */
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
            graphFromStationField = inputMode == InputMode.BACKWARD ? toStationField : fromStationField;
            graphToStationField = inputMode == InputMode.BACKWARD ? fromStationField : toStationField;
        }

    }

    /**
     * Initialise the station display based on current data
     */
    private void initialiseStationDisplay() {
        mapGraphFields();
        graphFromStationField.setText(originalFromStation.getName());

        if (!isSplay) {
            String toName = "";
            if (originalLeg != null) {
                toName = originalLeg.getDestination().getName();
            } else if (defaultToName != null) {
                toName = defaultToName;
            }
            graphToStationField.setText(toName);
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
        return Float.parseFloat(this.inclinationField.getText().toString());
    }

    private float getAzimuth() {
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

    private Station getFromStation() {
        return survey.getStationByName(getFromStationName());
    }

    private String getToStationName() {
        if (inputMode == InputMode.BACKWARD) {
            return fromStationField.getText().toString();
        }
        return toStationField.getText().toString();
    }

    /**
     * Create a Leg object from the form data with measurements and shot direction.
     * For editing: preserves the existing destination station object.
     * For adding: creates a splay-like leg (caller must create destination and reconstruct).
     * Should only be called after validation passes.
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
            leg = new Leg(distance, azimuth, inclination, destination, new Leg[]{});
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

    /**
     * Get the from station for the leg.
     * Should only be called after validation passes.
     */
    public Station getUpdatedFromStation() {
        return getFromStation();
    }

    /**
     * Get the to station name for the leg.
     * Returns null for splays.
     * Should only be called after validation passes.
     */
    public String getUpdatedToStationName() {
        return isSplay ? null : getToStationName();
    }
}
