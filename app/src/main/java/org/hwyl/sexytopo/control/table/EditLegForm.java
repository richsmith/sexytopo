package org.hwyl.sexytopo.control.table;

import android.content.Context;
import android.graphics.Rect;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.Nullable;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import java.util.Locale;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.util.GeneralPreferences;
import org.hwyl.sexytopo.control.util.InputMode;
import org.hwyl.sexytopo.control.util.StationNameRule;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;

public class EditLegForm extends Form {
    private static final int SPINNER_FORWARD = 0;
    private static final int SPINNER_BACKWARD = 1;

    /**
     * A TextWatcher that, in addition to triggering validation, enables live-error display as soon
     * as the watched field contains any non-empty text. This lets out-of-range errors appear
     * immediately while the user is typing, without showing a "cannot be blank" error on an
     * untouched field.
     */
    private static class LiveErrorTrigger extends TextViewValidationTrigger {
        private final android.widget.EditText field;

        LiveErrorTrigger(Form form, android.widget.EditText field) {
            super(form);
            this.field = field;
        }

        @Override
        public void afterTextChanged(android.text.Editable editable) {
            if (field.getText().length() > 0) {
                form.enableLiveErrors();
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

    private EditText fromCommentField;
    private EditText toCommentField;
    private EditText legCommentField;

    // The station whose comment is shown
    private Station lastFromStation;

    private StationNameRule.Result existingStationResult;
    private StationNameRule.Result newStationResult;

    private TextInputLayout fromStationLayout;
    private TextInputLayout toStationLayout;
    // Full-width lines under the station rows for helper text or errors
    private TextView fromStationHelper;
    private TextView toStationHelper;
    private TextInputLayout distanceLayout;
    private TextInputLayout azimuthLayout;
    private TextInputLayout inclinationLayout;

    // TextInputLayout wrappers for DMS minutes/seconds fields, used to show live errors
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

    // Shown for splays only
    private CheckBox toWallCheckBox;
    private CheckBox enabledCheckBox;

    private TextView shotsWarning;

    // The readings the fields were filled with, rounded for display, if editing a leg
    @Nullable private Leg shownReadings;

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
        this.inputMode = getInitialInputMode(legToEdit);

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
        this.originalLeg = null; // No original leg when adding
        this.defaultToName = defaultToName;
        this.isSplay = isSplay;

        this.initialise(dialogView);
    }

    private void initialise(View dialogView) {
        this.isInitialising = true;
        this.lastFromStation = originalFromStation;
        this.initialiseFields(dialogView);
        this.initialiseInputMode(dialogView);
        this.initialiseSplayOptions(dialogView);
        this.initialiseStationDisplay();
        this.isInitialising = false;
    }

    private void initialiseSplayOptions(View dialogView) {
        if (isSplay) {
            dialogView.findViewById(R.id.splayOptionsContainer).setVisibility(View.VISIBLE);
            this.toWallCheckBox = dialogView.findViewById(R.id.toWallCheckBox);
            toWallCheckBox.setChecked(initialToWall(originalLeg));
            dialogView
                    .findViewById(R.id.toWallInfoButton)
                    .setOnClickListener(
                            v ->
                                    showInfo(
                                            R.string.manual_edit_to_wall_info_title,
                                            R.string.manual_edit_to_wall_info_message));
            this.enabledCheckBox = dialogView.findViewById(R.id.enabledCheckBox);
            enabledCheckBox.setChecked(initialEnabled(originalLeg));
            dialogView
                    .findViewById(R.id.enabledInfoButton)
                    .setOnClickListener(
                            v ->
                                    showInfo(
                                            R.string.manual_edit_enabled_info_title,
                                            R.string.manual_edit_enabled_info_message));
        }
    }

    private void initialiseFields(View dialogView) {
        // Find all view references from the dialog
        this.fromStationLayout = dialogView.findViewById(R.id.fromStationLayout);
        this.toStationLayout = dialogView.findViewById(R.id.toStationLayout);
        this.fromStationHelper = dialogView.findViewById(R.id.fromStationHelper);
        this.toStationHelper = dialogView.findViewById(R.id.toStationHelper);
        if (isSplay) {
            this.toStationHelper.setVisibility(View.GONE);
        }
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
        this.legCommentField = dialogView.findViewById(R.id.editLegComment);
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
        this.shotsWarning = dialogView.findViewById(R.id.shotsWarning);

        // Set up validation listeners
        this.fromStationField.addTextChangedListener(new TextViewValidationTrigger(this));
        if (!isSplay) {
            this.toStationField.addTextChangedListener(new TextViewValidationTrigger(this));
        }
        this.distanceField.addTextChangedListener(new TextViewValidationTrigger(this));
        this.azimuthField.addTextChangedListener(new LiveErrorTrigger(this, this.azimuthField));
        this.inclinationField.addTextChangedListener(
                new LiveErrorTrigger(this, this.inclinationField));

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

        // Keep a focused station box's helper text in view, such as when the keyboard opens
        dialogView.getViewTreeObserver().addOnGlobalLayoutListener(this::showFocusedStationBox);

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

        if (GeneralPreferences.isDegMinsSecsModeOn()) {
            this.azimuthMinutesField.setOnFocusChangeListener(enableErrorsOnTouch);
            this.azimuthSecondsField.setOnFocusChangeListener(enableErrorsOnTouch);
        }
        if (GeneralPreferences.isIncDegMinsSecsModeOn()) {
            this.inclinationMinutesField.setOnFocusChangeListener(enableErrorsOnTouch);
            this.inclinationSecondsField.setOnFocusChangeListener(enableErrorsOnTouch);
        }
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
        validateStations();

        // Validate measurements
        validateDistance();
        validateAzimuth();
        validateInclination();

        updateShotsWarning();
    }

    /** Warns that a leg averaged from several shots will lose them if its readings are changed. */
    private void updateShotsWarning() {
        boolean hasShots = !isSplay && originalLeg != null && originalLeg.wasPromoted();
        if (!hasShots) {
            return;
        }
        Leg entered = getEnteredReadings();
        boolean losesShots = entered == null || !isUnchanged(shownReadings, entered);
        shotsWarning.setVisibility(losesShots ? View.VISIBLE : View.GONE);
    }

    /** The readings entered, as taken, or null if they can't be read, such as a blank field. */
    @Nullable
    private Leg getEnteredReadings() {
        try {
            return new Leg(getDistance(), getAzimuth(), getInclination());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    /** Checks both station boxes with the shared rule and says what each will do. */
    private void validateStations() {
        String existingName = existingStationField().getText().toString();
        existingStationResult =
                isSplay
                        ? StationNameRule.checkSplayStation(
                                survey, originalFromStation, existingName)
                        : StationNameRule.checkExisting(
                                survey, originalFromStation, originalLeg, existingName);
        showStationResult(
                existingStationHelper(),
                existingStationResult,
                StationNameText.existingHelper(
                        context, survey, originalFromStation, existingStationResult, isSplay));
        updateFromComment();

        if (!isSplay) {
            Station newStation = originalLeg == null ? null : originalLeg.getDestination();
            String newName = newStationField().getText().toString();
            newStationResult =
                    StationNameRule.checkNew(survey, newStation, newName, existingStationResult);
            showStationResult(
                    newStationHelper(),
                    newStationResult,
                    StationNameText.newHelper(context, survey, newStation, newStationResult));
        }
    }

    /** Scrolls so a focused station box's helper line is in view. */
    private void showFocusedStationBox() {
        TextView line = null;
        if (fromStationLayout.hasFocus()) {
            line = fromStationHelper;
        } else if (toStationLayout.hasFocus()) {
            line = toStationHelper;
        }
        if (line != null) {
            line.requestRectangleOnScreen(new Rect(0, 0, line.getWidth(), line.getHeight()));
        }
    }

    /** Shows a station box's error, or else its helper text, on the line under its row. */
    private void showStationResult(
            TextView line, StationNameRule.Result result, CharSequence helper) {
        CharSequence error = StationNameText.error(context, result);
        this.valid = this.valid & (error == null);
        line.setText(error == null ? helper : error);
        int colour = error == null ? R.attr.colorOnSurfaceVariant : R.attr.colorError;
        line.setTextColor(MaterialColors.getColor(line, colour));
    }

    /** Shows the comment of the station the leg will run from, when that changes. */
    private void updateFromComment() {
        Station fromStation =
                existingStationResult.action == StationNameRule.Action.MOVE
                        ? existingStationResult.target
                        : originalFromStation;
        if (fromStation != lastFromStation) {
            fromCommentField.setText(fromStation.hasComment() ? fromStation.getComment() : "");
            lastFromStation = fromStation;
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
                    setLiveError(
                            this.azimuthLayout,
                            context.getString(
                                    R.string.validation_error_azimuth_range,
                                    Leg.MIN_AZIMUTH,
                                    Leg.MAX_AZIMUTH));
                } else {
                    setLiveError(this.azimuthLayout, (Integer) null);
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
        // Degrees has content - enable live error display, mirroring what
        // LiveErrorTrigger does for the decimal azimuth field.
        enableLiveErrors();
        validateAzimuthDegreesField();
        validateMinutesField(azimuthMinutesLayout, azimuthMinutesField);
        validateSecondsField(azimuthSecondsLayout, azimuthSecondsField);
    }

    /**
     * Validates the azimuth degrees field independently. The value must be in [0, 359]. The field's
     * inputType="number" guarantees a parseable integer.
     */
    private boolean validateAzimuthDegreesField() {
        int degrees = Integer.parseInt(azimuthDegreesField.getText().toString());
        if (Leg.MIN_AZIMUTH <= degrees && degrees < Leg.MAX_AZIMUTH) {
            setLiveError(azimuthDegreesLayout, (Integer) null);
            return true;
        }
        setLiveError(
                azimuthDegreesLayout,
                context.getString(
                        R.string.validation_error_azimuth_range, Leg.MIN_AZIMUTH, Leg.MAX_AZIMUTH));
        return false;
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
                    setLiveError(
                            this.inclinationLayout,
                            context.getString(R.string.validation_error_inclination_range));
                } else {
                    setLiveError(this.inclinationLayout, (Integer) null);
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
        // Degrees has content - enable live error display, mirroring what
        // LiveErrorTrigger does for the decimal inclination field.
        enableLiveErrors();
        validateInclinationDegreesField();
        validateInclinationMinutesField();
        validateInclinationSecondsField();
    }

    /**
     * Validates the inclination degrees field independently. The value must be in [-90, 90] or
     * [270, 360] (theodolite range).
     */
    private boolean validateInclinationDegreesField() {
        try {
            int degrees = Integer.parseInt(inclinationDegreesField.getText().toString());
            boolean inNormalRange =
                    degrees >= Leg.MIN_INCLINATION && degrees <= Leg.MAX_INCLINATION;
            boolean inTheodoliteRange =
                    degrees >= Leg.MIN_THEODOLITE_INC && degrees <= Leg.MAX_THEODOLITE_INC;
            if (!inNormalRange && !inTheodoliteRange) {
                setLiveError(
                        inclinationDegreesLayout,
                        context.getString(R.string.validation_error_inclination_range));
                return false;
            }
            setLiveError(inclinationDegreesLayout, (Integer) null);
            return true;
        } catch (NumberFormatException e) {
            // Partial input such as a lone "-"; not yet parseable, suppress error display
            this.valid = false;
            return false;
        }
    }

    /**
     * Validates the inclination minutes field. Applies the standard 0–59 whole-number check, plus
     * an additional constraint: if the degrees field contains ±90, minutes must be 0 or blank since
     * as a minute or second value would take it out of range.
     */
    private boolean validateInclinationMinutesField() {
        if (isAtInclinationBoundary() && !isZeroOrBlank(inclinationMinutesField)) {
            setError(
                    inclinationMinutesLayout,
                    context.getString(R.string.validation_error_must_be_zero_for_90));
            return false;
        }
        return validateMinutesField(inclinationMinutesLayout, inclinationMinutesField);
    }

    /**
     * Validates the inclination seconds field. Applies the standard 0–59.99 check, plus an
     * additional constraint: if the degrees field contains ±90, seconds must be 0 or blank.
     */
    private boolean validateInclinationSecondsField() {
        if (isAtInclinationBoundary() && !isZeroOrBlank(inclinationSecondsField)) {
            setError(
                    inclinationSecondsLayout,
                    context.getString(R.string.validation_error_must_be_zero_for_90));
            return false;
        }
        return validateSecondsField(inclinationSecondsLayout, inclinationSecondsField);
    }

    /**
     * Returns true if the inclination degrees field contains exactly 90 or −90, indicating that the
     * reading is at the vertical boundary and minutes/seconds must be zero.
     */
    private boolean isAtInclinationBoundary() {
        String text = inclinationDegreesField.getText().toString();
        return text.equals("90") || text.equals("-90");
    }

    /**
     * Returns true if the given field is blank or contains a value that is numerically zero (e.g.
     * "0", "0.0", "00"). Used to enforce the ±90° boundary constraint.
     */
    private boolean isZeroOrBlank(EditText field) {
        String text = field.getText().toString();
        if (text.isEmpty()) {
            return true;
        }
        try {
            return Float.parseFloat(text) == 0.0f;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Validates a minutes field. Blank is accepted (treated as zero). If non-blank, the value must
     * be in the range 0-59. The field's inputType="number" guarantees a parseable non-negative
     * integer.
     */
    private boolean validateMinutesField(TextInputLayout layout, EditText field) {
        String text = field.getText().toString();
        if (text.isEmpty()) {
            setError(layout, (Integer) null);
            return true;
        }
        int value = Integer.parseInt(text);
        if (value > 59) {
            setError(layout, context.getString(R.string.validation_error_mins_secs_range));
            return false;
        }
        setError(layout, (Integer) null);
        return true;
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
        String fromText = fromStationField.getText().toString();
        String toText = toStationField.getText().toString();
        fromStationField.setText(toText);
        toStationField.setText(fromText);
    }

    /** On a backward leg the From box holds the new station; a splay has only From. */
    private boolean isNewStationInFromBox() {
        return inputMode == InputMode.BACKWARD && !isSplay;
    }

    private EditText existingStationField() {
        return isNewStationInFromBox() ? toStationField : fromStationField;
    }

    private EditText newStationField() {
        return isNewStationInFromBox() ? fromStationField : toStationField;
    }

    private TextView existingStationHelper() {
        return isNewStationInFromBox() ? toStationHelper : fromStationHelper;
    }

    private TextView newStationHelper() {
        return isNewStationInFromBox() ? fromStationHelper : toStationHelper;
    }

    /** Initialise the station display based on current data */
    private void initialiseStationDisplay() {
        String fromComment = "";
        if (originalFromStation.hasComment()) {
            fromComment = originalFromStation.getComment();
        }
        existingStationField().setText(originalFromStation.getName());
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
            newStationField().setText(toName);
            toCommentField.setText(toComment);
        }

        if (legCommentField != null && originalLeg != null) {
            legCommentField.setText(originalLeg.getComment());
        }
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
     * Fills the reading fields with a leg's readings, as taken, to the same number of decimal
     * places as the table.
     */
    public void showReadings(Leg asTaken) {
        distanceField.setText(formatReading("%.3f", asTaken.getDistance()));

        if (GeneralPreferences.isDegMinsSecsModeOn()) {
            float[] azimuthDms = Leg.decomposeToDms(asTaken.getAzimuth());
            azimuthDegreesField.setText(Integer.toString((int) azimuthDms[0]));
            azimuthMinutesField.setText(Integer.toString((int) azimuthDms[1]));
            azimuthSecondsField.setText(Float.toString(azimuthDms[2]));
        } else {
            azimuthField.setText(formatReading("%.2f", asTaken.getAzimuth()));
        }

        if (GeneralPreferences.isIncDegMinsSecsModeOn()) {
            float[] inclinationDms = Leg.decomposeToDms(asTaken.getInclination());
            inclinationDegreesField.setText(Integer.toString((int) inclinationDms[0]));
            inclinationMinutesField.setText(Integer.toString((int) inclinationDms[1]));
            inclinationSecondsField.setText(Float.toString(inclinationDms[2]));
        } else {
            inclinationField.setText(formatReading("%.2f", asTaken.getInclination()));
        }

        shownReadings = getEnteredReadings();
        validate();
    }

    /** Fields are parsed with a full stop as the decimal point, whatever the locale. */
    private static String formatReading(String format, float reading) {
        return String.format(Locale.ROOT, format, reading);
    }

    /**
     * Create a Leg object from the form data with measurements and shot direction. For editing:
     * preserves the existing destination station object. For adding: creates a splay-like leg
     * (caller must create destination and reconstruct). Should only be called after validation
     * passes.
     */
    public Leg getUpdatedLeg() {
        Leg entered = new Leg(getDistance(), getAzimuth(), getInclination());
        boolean unchanged = originalLeg != null && isUnchanged(shownReadings, entered);
        Leg readings = unchanged ? originalLeg.toRecordedReading() : entered;
        float distance = readings.getDistance();
        float azimuth = readings.getAzimuth();
        float inclination = readings.getInclination();

        Leg leg;
        if (isSplay) {
            leg =
                    createSplay(
                            distance,
                            azimuth,
                            inclination,
                            toWallCheckBox.isChecked(),
                            enabledCheckBox.isChecked());
        } else if (originalLeg != null && originalLeg.hasDestination()) {
            // For editing: reuse existing destination station object
            Station destination = originalLeg.getDestination();
            Leg[] shots = unchanged ? originalLeg.getPromotedFrom() : new Leg[] {};
            leg = new Leg(distance, azimuth, inclination, destination, shots);
        } else {
            // For adding a new station: create a temporary splay-like leg
            // Caller will reconstruct with proper destination station
            leg = new Leg(distance, azimuth, inclination);
        }

        return applyInputMode(leg, isSplay, inputMode);
    }

    private void showInfo(int titleId, int messageId) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(titleId)
                .setMessage(messageId)
                .setPositiveButton(R.string.ok, null)
                .show();
    }

    /**
     * Whether the readings entered are still the ones the fields were filled with. If so, the leg
     * keeps its own readings, which would otherwise be rounded to what was shown, and the shots it
     * was averaged from, which wouldn't match changed readings.
     */
    static boolean isUnchanged(@Nullable Leg shown, Leg entered) {
        return shown != null && entered.hasEquivalentReadings(shown);
    }

    /**
     * Builds the splay for the readings entered, going to a wall or to another feature, and enabled
     * or disabled.
     */
    static Leg createSplay(
            float distance, float azimuth, float inclination, boolean toWall, boolean enabled) {
        Leg splay = new Leg(distance, azimuth, inclination).withToWall(toWall);
        splay.setEnabled(enabled);
        return splay;
    }

    /**
     * Whether the To Wall tick box starts ticked. A new splay goes to a wall, and an edited splay
     * keeps what it goes to unless the box is changed.
     */
    static boolean initialToWall(Leg originalSplay) {
        return originalSplay == null || originalSplay.isToWall();
    }

    /**
     * Whether the Enabled tick box starts ticked. A new splay is enabled, and an edited splay keeps
     * its state unless the box is changed.
     */
    static boolean initialEnabled(Leg originalSplay) {
        return originalSplay == null || originalSplay.isEnabled();
    }

    /**
     * The input mode a leg is edited in. Only a leg with a destination can be shot backwards; a
     * splay always runs from its station, so it is always edited as a forward shot.
     */
    static InputMode getInitialInputMode(Leg leg) {
        boolean backwards = leg.hasDestination() && leg.wasShotBackwards();
        return backwards ? InputMode.BACKWARD : InputMode.FORWARD;
    }

    /**
     * Applies the input mode to a freshly built leg. A backward shot is stored reversed and
     * flagged, but a splay is never reversed or flagged.
     */
    static Leg applyInputMode(Leg leg, boolean isSplay, InputMode inputMode) {
        if (inputMode == InputMode.BACKWARD && !isSplay) {
            return leg.reverse();
        }
        return leg;
    }

    /** What the existing station box will do. Only call after validation passes. */
    public StationNameRule.Result getExistingStationResult() {
        return existingStationResult;
    }

    /** What the new station box will do; null for a splay. Only call after validation passes. */
    public StationNameRule.Result getNewStationResult() {
        return isSplay ? null : newStationResult;
    }

    public String getUpdatedFromComment() {
        return getFromComment();
    }

    public String getUpdatedToComment() {
        return isSplay ? null : getToComment();
    }

    public String getUpdatedLegComment() {
        if (legCommentField != null) {
            return legCommentField.getText().toString();
        }
        return "";
    }
}
