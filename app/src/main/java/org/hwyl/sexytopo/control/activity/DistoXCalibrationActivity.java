package org.hwyl.sexytopo.control.activity;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.documentfile.provider.DocumentFile;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.lang3.ArrayUtils;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.SexyTopoConstants;
import org.hwyl.sexytopo.comms.Communicator;
import org.hwyl.sexytopo.comms.DistoX;
import org.hwyl.sexytopo.comms.distox.CalibrationWrite;
import org.hwyl.sexytopo.comms.distox.DistoXCommunicator;
import org.hwyl.sexytopo.comms.distox.DistoXStyleCommunicator;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.calibration.CalibrationCalculator;
import org.hwyl.sexytopo.control.calibration.CalibrationCoverage;
import org.hwyl.sexytopo.control.calibration.ReadingDirection;
import org.hwyl.sexytopo.control.components.CalibrationReadingView;
import org.hwyl.sexytopo.control.io.IoUtils;
import org.hwyl.sexytopo.control.io.StartLocation;
import org.hwyl.sexytopo.control.io.basic.CalibrationJsonTranslater;
import org.hwyl.sexytopo.control.util.GeneralPreferences;
import org.hwyl.sexytopo.control.util.TextTools;
import org.hwyl.sexytopo.model.calibration.CalibrationReading;
import org.hwyl.sexytopo.model.calibration.CalibrationReadingList;
import org.hwyl.sexytopo.model.sketch.Colour;
import org.json.JSONException;

public class DistoXCalibrationActivity extends SexyTopoActivity {

    public static final double MAX_ERROR = 0.5;

    private static final int WORST_READINGS_SHOWN = 5;

    private enum CalibrationDirection {
        FORWARD(R.string.direction_forward),
        BACK(R.string.direction_back),
        LEFT(R.string.direction_left),
        RIGHT(R.string.direction_right),
        UP(R.string.direction_up),
        DOWN(R.string.direction_down),
        FORWARD_LEFT_UP(R.string.direction_forward_left_up),
        FORWARD_LEFT_DOWN(R.string.direction_forward_left_down),
        FORWARD_RIGHT_UP(R.string.direction_forward_right_up),
        FORWARD_RIGHT_DOWN(R.string.direction_forward_right_down),
        BACK_LEFT_UP(R.string.direction_back_left_up),
        BACK_LEFT_DOWN(R.string.direction_back_left_down),
        BACK_RIGHT_UP(R.string.direction_back_right_up),
        BACK_RIGHT_DOWN(R.string.direction_back_right_down);

        final int stringId;

        CalibrationDirection(int stringId) {
            this.stringId = stringId;
        }
    }

    private enum Orientation {
        FACE_UP(R.string.orientation_face_up),
        FACE_RIGHT(R.string.orientation_face_right),
        FACE_DOWN(R.string.orientation_face_down),
        FACE_LEFT(R.string.orientation_face_left);

        final int stringId;

        Orientation(int stringId) {
            this.stringId = stringId;
        }
    }

    private static final List<Pair<CalibrationDirection, Orientation>> positions;

    private enum State {
        NOT_READY,
        READY,
        CALIBRATING,
        CALIBRATED
    }

    private State state = State.READY;

    private CalibrationReadingList calibrationReadings = new CalibrationReadingList();

    private ColorStateList defaultAssessmentColours;

    /** The calibration from the current readings, or null until they are complete. */
    private Analysis analysis = null;

    /** The adapter of the readings list while it is open, so it can be refreshed. */
    private BaseAdapter readingsAdapter = null;

    /** The set (1 upwards) each reading belongs to, detected from which way they point. */
    private int[] setNumbers = new int[0];

    private static class Analysis {
        final double delta;
        final double[] errors;
        final ReadingDirection[] directions;

        Analysis(CalibrationCalculator calculator) {
            delta = calculator.getDelta();
            errors = calculator.getReadingErrors();
            directions = calculator.getReadingDirections();
        }
    }

    static {
        positions = new ArrayList<>();
        setUpPositions();
    }

    private static void setUpPositions() {
        for (CalibrationDirection direction : CalibrationDirection.values()) {
            for (Orientation orientation : Orientation.values()) {
                Pair<CalibrationDirection, Orientation> position =
                        new Pair<>(direction, orientation);
                positions.add(position);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_calibration);
        setupMaterialToolbar();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        applyEdgeToEdgeInsets(R.id.rootLayout, true, true);

        TextView assessmentField = findViewById(R.id.calibrationFieldAssessment);
        defaultAssessmentColours = assessmentField.getTextColors();

        BroadcastReceiver updatedCalibrationReceiver =
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        syncWithReadings();
                    }
                };

        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        broadcastManager.registerReceiver(
                updatedCalibrationReceiver,
                new IntentFilter(SexyTopoConstants.CALIBRATION_UPDATED_EVENT));
    }

    @Override
    protected void onResume() {
        super.onResume();
        syncWithReadings();
    }

    private void syncWithReadings() {
        calibrationReadings = getSurveyManager().getCalibrationReadings();
        analysis = null;
        setNumbers = findSetNumbers(calibrationReadings.getAll());
        if (isComplete()) {
            CalibrationCalculator calculator = new CalibrationCalculator(useNonLinearAlgorithm());
            calculator.calculate(calibrationReadings.getAll());
            analysis = new Analysis(calculator);
        }
        updateFields();
        updateState();
        if (readingsAdapter != null) {
            readingsAdapter.notifyDataSetChanged();
        }
    }

    private void updateFields() {

        CalibrationReadingView lastReadingView = findViewById(R.id.calibration_last_reading);
        CalibrationReading lastReading = calibrationReadings.getLastAdded();
        if (lastReading == null) {
            lastReadingView.showNoReadings();
        } else {
            bindReading(lastReadingView, calibrationReadings.getAll().indexOf(lastReading));
        }

        String label = calibrationReadings.getCount() + "/" + positions.size();
        setInfoField(R.id.calibration_index, label);

        updateCoverage();
        updateWorstReadings();

        int nextIndex = calibrationReadings.getNextIndex();
        if (!isComplete() && nextIndex < positions.size()) {
            Pair<CalibrationDirection, Orientation> suggestedNext = positions.get(nextIndex);
            setInfoField(R.id.calibration_next_direction, getString(suggestedNext.first.stringId));
            setInfoField(
                    R.id.calibration_next_orientation, getString(suggestedNext.second.stringId));
        } else {
            setInfoField(R.id.calibration_next_direction, getString(R.string.not_applicable));
            setInfoField(R.id.calibration_next_orientation, getString(R.string.not_applicable));
        }

        if (analysis == null) {
            TextView assessmentField = findViewById(R.id.calibrationFieldAssessment);
            assessmentField.setTextColor(defaultAssessmentColours);
            setInfoField(R.id.calibrationFieldAssessment, getString(R.string.not_applicable));
        } else {
            double calibrationAssessment = analysis.delta;

            TextView assessmentField = findViewById(R.id.calibrationFieldAssessment);
            if (calibrationAssessment <= 0.5) {
                assessmentField.setTextColor(Colour.SEA_GREEN.intValue);
            } else {
                assessmentField.setTextColor(Colour.RED.intValue);
            }
            setInfoField(
                    R.id.calibrationFieldAssessment, TextTools.formatTo2dp(calibrationAssessment));
        }
    }

    private void updateCoverage() {
        CalibrationCoverage coverage = CalibrationCoverage.of(calibrationReadings.getAll());
        setInfoField(
                R.id.calibration_sets,
                getString(
                        R.string.calibration_sets_value,
                        coverage.getSetCount(),
                        coverage.getCompleteSetCount()));
        showCoverage(
                coverage,
                R.id.calibration_coverage_horizontal,
                ReadingDirection.Pointing.HORIZONTAL);
        showCoverage(
                coverage, R.id.calibration_coverage_corner_up, ReadingDirection.Pointing.CORNER_UP);
        showCoverage(
                coverage,
                R.id.calibration_coverage_corner_down,
                ReadingDirection.Pointing.CORNER_DOWN);
        showCoverage(coverage, R.id.calibration_coverage_up, ReadingDirection.Pointing.UP);
        showCoverage(coverage, R.id.calibration_coverage_down, ReadingDirection.Pointing.DOWN);
    }

    private void showCoverage(
            CalibrationCoverage coverage, int id, ReadingDirection.Pointing pointing) {
        TextView field = findViewById(id);
        field.setText(
                getString(
                        R.string.calibration_coverage_value,
                        coverage.getDirectionCount(pointing),
                        CalibrationCoverage.getTarget(pointing)));
        if (coverage.isTargetMet(pointing)) {
            field.setTextColor(Colour.SEA_GREEN.intValue);
        } else {
            field.setTextColor(defaultAssessmentColours);
        }
    }

    private boolean isComplete() {
        return calibrationReadings.isComplete(positions.size());
    }

    private void updateState() {

        if (isComplete()) {
            state = State.CALIBRATED;
        } else if (state == State.CALIBRATED) {
            state = State.READY; // a reading has been deleted and needs retaking
        }

        boolean hasReadings = !calibrationReadings.isEmpty();
        setButtonEnabled(R.id.calibration_save, hasReadings);
        setButtonEnabled(R.id.calibration_clear, hasReadings);
        setButtonEnabled(R.id.calibration_view_readings, hasReadings);
        setButtonEnabled(R.id.calibration_delete_last, hasReadings);

        switch (state) {
            case READY:
                setButtonEnabled(R.id.calibration_start, true);
                // setButtonEnabled(R.id.calibration_stop, false);
                setButtonEnabled(R.id.calibration_complete, false);
                break;
            case CALIBRATING:
                setButtonEnabled(R.id.calibration_start, false);
                // setButtonEnabled(R.id.calibration_stop, true);
                setButtonEnabled(R.id.calibration_complete, false);
                break;
            case CALIBRATED:
                // setButtonEnabled(R.id.calibration_start, false);
                setButtonEnabled(R.id.calibration_complete, true);
                break;
        }
    }

    private void setButtonEnabled(int id, boolean enabled) {
        Button button = findViewById(id);
        button.setEnabled(enabled);
    }

    private void setInfoField(int id, String text) {
        TextView textView = findViewById(id);
        textView.setText(text);
    }

    public void requestStartCalibration(View view) {
        Log.device("Start calibration requested");

        try {
            getComms().startCalibration();
            state = State.CALIBRATING;
        } catch (Exception exception) {
            state = State.READY;
            Log.e(exception);
            showSimpleToast("Error starting calibration: " + exception);
        } finally {
            updateState();
        }
    }

    public void requestStopCalibration(View view) {
        Log.device("Stop calibration requested");
        try {
            getComms().stopCalibration();
        } catch (Exception exception) {
            showExceptionAndLog(exception);
        }
        state = State.READY;
        updateState();
    }

    public void requestCompleteCalibration(final View view) {
        if (!isComplete()) {
            showSimpleToast(R.string.calibration_not_enough);
            return;
        }

        boolean useNonLinearity = useNonLinearAlgorithm();
        final CalibrationCalculator calibrationCalculator =
                new CalibrationCalculator(useNonLinearity);
        calibrationCalculator.calculate(calibrationReadings.getAll());
        double calibrationAssessment = calibrationCalculator.getDelta();
        String result = TextTools.formatTo2dp(calibrationAssessment);
        String algorithm =
                getString(
                        useNonLinearity
                                ? R.string.calibration_algorithm_nonlinear
                                : R.string.calibration_algorithm_linear);
        String message =
                getString(
                        R.string.device_distox_calibration_result,
                        TextTools.formatTo1dp(MAX_ERROR),
                        result,
                        algorithm);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.calibration_assessment)
                .setMessage(message)
                .setPositiveButton(
                        R.string.calibration_update,
                        (dialog, whichButton) -> {
                            try {
                                byte[] coeffs = calibrationCalculator.getCoefficients();
                                Byte[] coefficients = ArrayUtils.toObject(coeffs);
                                requestWriteCalibration(view, coefficients);
                            } catch (Exception exception) {
                                showExceptionAndLog(exception);
                            } finally {
                                updateState();
                            }
                        })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    public void requestDeleteLast(View view) {
        getSurveyManager().deleteLastCalibrationReading();
        syncWithReadings();
    }

    public void requestClearCalibration(View view) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_confirm_clear_title)
                .setPositiveButton(
                        getString(R.string.clear),
                        (dialog, whichButton) -> {
                            getSurveyManager().clearCalibrationReadings();
                            syncWithReadings();
                        })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /**
     * The readings that disagree with the others shot in the same direction by more than is
     * expected, worst first.
     */
    private List<Integer> getWorstReadings(double[] errors) {
        return IntStream.range(0, errors.length)
                .filter(i -> errors[i] > CalibrationReadingView.GOOD_ERROR) // false for NaN
                .boxed()
                .sorted(Comparator.comparingDouble(i -> -errors[i]))
                .limit(WORST_READINGS_SHOWN)
                .collect(Collectors.toList());
    }

    /** Lists the least consistent readings; tapping one shows it in the list of readings. */
    private void updateWorstReadings() {
        View section = findViewById(R.id.calibration_worst_section);
        LinearLayout list = findViewById(R.id.calibration_worst_list);
        list.removeAllViews();
        List<Integer> worstReadings =
                analysis == null ? new ArrayList<>() : getWorstReadings(analysis.errors);
        section.setVisibility(worstReadings.isEmpty() ? View.GONE : View.VISIBLE);

        LayoutInflater inflater = LayoutInflater.from(this);
        for (int index : worstReadings) {
            double error = analysis.errors[index];
            View row = inflater.inflate(R.layout.calibration_worst_reading_row, list, false);
            TextView label = row.findViewById(R.id.worstReadingLabel);
            label.setText(
                    getString(
                            R.string.calibration_reading_label,
                            index + 1,
                            describeMeasuredPosition(index)));
            TextView value = row.findViewById(R.id.worstReadingError);
            value.setText(getString(R.string.calibration_degrees, TextTools.formatTo1dp(error)));
            value.setTextColor(Colour.RED.intValue);
            row.setOnClickListener(clicked -> showReadingsList(index));
            list.addView(row);
        }
    }

    public void requestViewReadings(View view) {
        showReadingsList(0);
    }

    /** Shows every reading, scrolled to the one at the index. */
    private void showReadingsList(int index) {
        BaseAdapter adapter =
                new BaseAdapter() {
                    @Override
                    public int getCount() {
                        return calibrationReadings.getAll().size();
                    }

                    @Override
                    public Object getItem(int position) {
                        return calibrationReadings.getAll().get(position);
                    }

                    @Override
                    public long getItemId(int position) {
                        return position;
                    }

                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        View item =
                                convertView != null
                                        ? convertView
                                        : LayoutInflater.from(parent.getContext())
                                                .inflate(
                                                        R.layout.calibration_reading_list_item,
                                                        parent,
                                                        false);
                        bindReading(item.findViewById(R.id.readingView), position);
                        return item;
                    }
                };

        readingsAdapter = adapter;
        AlertDialog dialog =
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.calibration_title_readings)
                        .setAdapter(adapter, null)
                        .setPositiveButton(R.string.ok, null)
                        .setOnDismissListener(dismissed -> readingsAdapter = null)
                        .create();
        dialog.show();
        // A list dialog closes when an item is tapped; stay open so several can be edited
        dialog.getListView()
                .setOnItemClickListener((parent, item, tapped, id) -> showReadingOptions(tapped));
        dialog.getListView().setSelection(index);
    }

    /**
     * Shows the reading at the index, using the calibration for its direction and error if there is
     * one, otherwise estimating its direction from the raw sensor values. It is labelled by which
     * way it was measured pointing, not by the position it was meant to be taken in.
     */
    private void bindReading(CalibrationReadingView view, int index) {
        CalibrationReading reading = calibrationReadings.getAll().get(index);
        if (reading == null) {
            view.showDeleted(index + 1, "", "");
            return;
        }
        ReadingDirection direction = getMeasuredDirection(index);
        boolean isCalibrated = analysis != null && index < analysis.directions.length;
        Double error =
                isCalibrated && !Double.isNaN(analysis.errors[index])
                        ? analysis.errors[index]
                        : null;

        List<String> details = new ArrayList<>();
        if (!direction.getPointing().isVertical()) {
            details.add(getString(direction.getFace().stringId));
        }
        details.add(getString(R.string.calibration_set_number, setNumbers[index]));

        view.showReading(
                index + 1,
                getString(direction.getPointing().stringId),
                TextUtils.join(getString(R.string.calibration_detail_separator), details),
                reading,
                direction,
                isCalibrated,
                error);
    }

    private ReadingDirection getMeasuredDirection(int index) {
        return analysis != null && index < analysis.directions.length
                ? analysis.directions[index]
                : CalibrationCalculator.getUncalibratedDirection(
                        calibrationReadings.getAll().get(index));
    }

    private String describeMeasuredPosition(int index) {
        ReadingDirection direction = getMeasuredDirection(index);
        String pointing = getString(direction.getPointing().stringId);
        return !direction.getPointing().isVertical()
                ? getString(
                        R.string.calibration_position,
                        pointing,
                        getString(direction.getFace().stringId))
                : pointing;
    }

    private static int[] findSetNumbers(List<CalibrationReading> readings) {
        int[] numbers = new int[readings.size()];
        List<List<Integer>> sets = CalibrationCalculator.findSets(readings);
        for (int set = 0; set < sets.size(); set++) {
            for (int index : sets.get(set)) {
                numbers[index] = set + 1;
            }
        }
        return numbers;
    }

    /** Offers to replace the reading (keeping its place) or delete it (closing up the gap). */
    private void showReadingOptions(int index) {
        boolean isGap = calibrationReadings.getAll().get(index) == null;
        MaterialAlertDialogBuilder builder =
                new MaterialAlertDialogBuilder(this)
                        .setTitle(getString(R.string.calibration_reading_title, index + 1))
                        .setNegativeButton(
                                R.string.delete,
                                (dialog, whichButton) -> {
                                    getSurveyManager().deleteCalibrationReading(index);
                                    syncWithReadings();
                                })
                        .setNeutralButton(R.string.cancel, null);
        if (isGap) {
            builder.setMessage(R.string.calibration_gap_options_message);
        } else {
            builder.setMessage(R.string.calibration_reading_options_message)
                    .setPositiveButton(
                            R.string.calibration_replace,
                            (dialog, whichButton) -> {
                                getSurveyManager().replaceCalibrationReading(index);
                                syncWithReadings();
                            });
        }
        builder.show();
    }

    public void requestSaveCalibration(View view) {
        createFile(
                SexyTopoConstants.REQUEST_CODE_SAVE_CALIBRATION,
                StartLocation.TOP_LEVEL,
                SexyTopoConstants.MIME_TYPE_DEFAULT,
                null);
    }

    public void requestLoadCalibration(View view) {
        selectFile(SexyTopoConstants.REQUEST_CODE_OPEN_CALIBRATION, StartLocation.TOP_LEVEL, null);
    }

    public void requestWriteCalibration(View view, Byte[] coefficients) {
        try {
            getComms(); // fail fast if not connected
            new WriteCalibrationTask(view).execute(coefficients);
        } catch (Exception exception) {
            showExceptionAndLog(exception);
        }
    }

    private void saveCalibration(Uri uri) throws JSONException, IOException {
        DocumentFile file = DocumentFile.fromSingleUri(this, uri);
        String contents = CalibrationJsonTranslater.toText(calibrationReadings.getAll());
        IoUtils.saveToFile(this, file, contents);
        Log.i(R.string.calibration_saved_to_file);
    }

    private void loadCalibration(Uri uri) throws JSONException, IOException {
        DocumentFile file = DocumentFile.fromSingleUri(this, uri);
        String content = IoUtils.slurpFile(this, file);
        List<CalibrationReading> calibrationReadings =
                CalibrationJsonTranslater.toCalibrationReadings(content);
        getSurveyManager().setCalibrationReadings(calibrationReadings);
        syncWithReadings();
        Log.i(R.string.calibration_loaded_file);
    }

    @SuppressWarnings("ConstantConditions")
    private boolean useNonLinearAlgorithm() {

        String algorithm = GeneralPreferences.getCalibrationAlgorithm();

        boolean useNonLinear = false; // linear probably safer as default

        try {
            switch (algorithm) {
                case "auto":
                    useNonLinear = getDistox().prefersNonLinearCalibration();
                    break;
                case "nonlinear":
                    useNonLinear = true;
                    break;
                case "linear":
                    useNonLinear = false;
                    break;
            }
        } catch (Exception exception) {
            // e.g. if disto not connected - just return false and deal with issues elsewhere
        }

        return useNonLinear;
    }

    private DistoXStyleCommunicator getComms() throws NotConnectedToDistoException {
        Communicator communicator = requestComms();
        if (communicator instanceof DistoXStyleCommunicator) {
            return (DistoXStyleCommunicator) communicator;
        } else {
            throw new NotConnectedToDistoException();
        }
    }

    private DistoX getDistox() {
        return DistoX.fromName(getInstrument().getName());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {

        if (resultData == null) {
            return;
        }

        if (resultCode != Activity.RESULT_OK) {
            Exception exception =
                    new Exception(getString(R.string.request_code_error, resultCode, requestCode));
            showExceptionAndLog(exception);
            return;
        }

        Uri uri = resultData.getData();

        if (requestCode == SexyTopoConstants.REQUEST_CODE_OPEN_CALIBRATION) {
            try {
                loadCalibration(uri);
            } catch (Exception exception) {
                showExceptionAndLog(R.string.calibration_load_error, exception);
            }

        } else if (requestCode == SexyTopoConstants.REQUEST_CODE_SAVE_CALIBRATION) {
            try {
                saveCalibration(uri);
            } catch (Exception exception) {
                showExceptionAndLog(R.string.calibration_save_error, exception);
            }
        }

        super.onActivityResult(requestCode, resultCode, resultData);
    }

    private class WriteCalibrationTask extends AsyncTask<Byte, Void, Boolean> {

        private final Dialog progressDialog;

        private WriteCalibrationTask(View view) {
            android.widget.ProgressBar progressBar =
                    new android.widget.ProgressBar(view.getContext());
            progressBar.setIndeterminate(true);
            progressBar.setPadding(0, 50, 0, 50);

            progressDialog =
                    new MaterialAlertDialogBuilder(view.getContext())
                            .setMessage(getString(R.string.calibration_writing))
                            .setView(progressBar)
                            .setCancelable(false)
                            .create();
        }

        protected void onPreExecute() {
            progressDialog.show();
        }

        @Override
        protected Boolean doInBackground(Byte... coefficients) {

            try {
                DistoXStyleCommunicator comms = getComms();
                CalibrationWrite calibrationWrite = comms.writeCalibration(coefficients);
                waitForEnd(calibrationWrite, 60);
                if (!calibrationWrite.isFinished() && comms instanceof DistoXCommunicator) {
                    // This is a bit hacky, but the old Disto needs some real-time management
                    Log.device(R.string.device_distox_force_disconnect_for_calibration);
                    comms.requestDisconnect(); // force it to stop what it's doing
                    waitForEnd(calibrationWrite, 80);
                }
                return calibrationWrite.wasSuccessful();

            } catch (NotConnectedToDistoException exception) {
                showExceptionAndLog(exception);
                return false;
            }
        }

        private void waitForEnd(CalibrationWrite calibrationWrite, int attempts) {
            for (int i = 0; i < attempts; i++) {
                try {
                    if (calibrationWrite.isFinished()) {
                        return;
                    }
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }

        @Override
        protected void onPostExecute(Boolean wasSuccessful) {

            if (isFinishing()) {
                return;
            }

            progressDialog.dismiss();
            if (wasSuccessful) {
                showSimpleToast(R.string.calibration_success);
            } else {
                showSimpleToast(R.string.calibration_error_updating_device);
            }
            updateState();
        }
    }

    public static class NotConnectedToDistoException extends Exception {}
}
