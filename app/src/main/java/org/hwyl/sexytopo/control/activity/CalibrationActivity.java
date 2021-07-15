package org.hwyl.sexytopo.control.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.util.Pair;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.apache.commons.lang3.ArrayUtils;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.SexyTopo;
import org.hwyl.sexytopo.comms.Communicator;
import org.hwyl.sexytopo.comms.distox.DistoXCommunicator;
import org.hwyl.sexytopo.comms.distox.WriteCalibrationProtocol;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.calibration.CalibrationCalculator;
import org.hwyl.sexytopo.control.io.Util;
import org.hwyl.sexytopo.control.io.basic.CalibrationJsonTranslater;
import org.hwyl.sexytopo.control.io.basic.Loader;
import org.hwyl.sexytopo.control.io.basic.Saver;
import org.hwyl.sexytopo.control.util.TextTools;
import org.hwyl.sexytopo.model.calibration.CalibrationReading;
import org.hwyl.sexytopo.model.sketch.Colour;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;


public class CalibrationActivity extends SexyTopoActivity {

    public static double MAX_ERROR = 0.5;

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


        int stringId;

        CalibrationDirection(int stringId) {
            this.stringId = stringId;
        }
    }

    private enum Orientation {
        FACE_UP(R.string.orientation_face_up),
        FACE_RIGHT(R.string.orientation_face_right),
        FACE_DOWN(R.string.orientation_face_down),
        FACE_LEFT(R.string.orientation_face_left);

        int stringId;

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

    private List<CalibrationReading> calibrationReadings = new ArrayList<>();

    static {
        positions = new ArrayList<>();
        setUpPositions();
    }

    private static void setUpPositions() {
        for (CalibrationDirection direction : CalibrationDirection.values()) {
            for (Orientation orientation : Orientation.values()) {
                Pair<CalibrationDirection, Orientation> position = new Pair<>(direction, orientation);
                positions.add(position);
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_calibration);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        BroadcastReceiver updatedCalibrationReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                syncWithReadings();
            }
        };

        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        broadcastManager.registerReceiver(updatedCalibrationReceiver,
                new IntentFilter(SexyTopo.CALIBRATION_UPDATED_EVENT));
    }

    @Override
    protected void onResume() {
        super.onResume();
        syncWithReadings();
    }

    private void syncWithReadings() {
        calibrationReadings = getSurveyManager().getCalibrationReadings();
        updateFields();
        updateState();
    }

    private void updateFields() {

        if (calibrationReadings.size() > 0) {
            CalibrationReading lastReading =
                    calibrationReadings.get(calibrationReadings.size() - 1);
            setInfoField(R.id.calibrationFieldGx, lastReading.getGx());
            setInfoField(R.id.calibrationFieldGy, lastReading.getGy());
            setInfoField(R.id.calibrationFieldGz, lastReading.getGz());
            setInfoField(R.id.calibrationFieldMx, lastReading.getMx());
            setInfoField(R.id.calibrationFieldMy, lastReading.getMy());
            setInfoField(R.id.calibrationFieldMz, lastReading.getMz());
        } else {
            setInfoField(R.id.calibrationFieldGx, "");
            setInfoField(R.id.calibrationFieldGy, "");
            setInfoField(R.id.calibrationFieldGz, "");
            setInfoField(R.id.calibrationFieldMx, "");
            setInfoField(R.id.calibrationFieldMy, "");
            setInfoField(R.id.calibrationFieldMz, "");
        }

        String label = calibrationReadings.size() + "/" + positions.size();
        setInfoField(R.id.calibration_index, label);

        if (calibrationReadings.size() < positions.size()) {
            Pair<CalibrationDirection, Orientation> suggestedNext =
                    positions.get(calibrationReadings.size());
            setInfoField(R.id.calibration_next_direction,
                    getString(suggestedNext.first.stringId));
            setInfoField(R.id.calibration_next_orientation,
                    getString(suggestedNext.second.stringId));
            TextView assessmentField = findViewById(R.id.calibrationFieldAssessment);
            assessmentField.setTextColor(Color.BLACK);
            setInfoField(R.id.calibrationFieldAssessment, getString(R.string.not_applicable));
        } else {
            setInfoField(R.id.calibration_next_direction, getString(R.string.not_applicable));
            setInfoField(R.id.calibration_next_orientation, getString(R.string.not_applicable));

            boolean useNonLinearity = useNonLinearAlgorithm();
            final CalibrationCalculator calibrationCalculator =
                    new CalibrationCalculator(useNonLinearity);
            calibrationCalculator.calculate(calibrationReadings);
            double calibrationAssessment = calibrationCalculator.getDelta();

            TextView assessmentField = findViewById(R.id.calibrationFieldAssessment);
            if (calibrationAssessment <= 0.5) {
                assessmentField.setTextColor(Colour.SEA_GREEN.intValue);
            } else {
                assessmentField.setTextColor(Colour.RED.intValue);
            }
            setInfoField(
                    R.id.calibrationFieldAssessment,TextTools.formatTo2dp(calibrationAssessment));
        }
    }


    private void updateState() {

        if (calibrationReadings.size() >= positions.size()) {
            state = State.CALIBRATED;
        }

        if (calibrationReadings.size() < 1) {
            setButtonEnabled(R.id.calibration_save, false);
            setButtonEnabled(R.id.calibration_clear, false);

        } else {
            setButtonEnabled(R.id.calibration_save, true);
            setButtonEnabled(R.id.calibration_clear, true);

        }

        switch(state) {
            case READY:
                setButtonEnabled(R.id.calibration_start, true);
                //setButtonEnabled(R.id.calibration_stop, false);
                setButtonEnabled(R.id.calibration_complete, false);
                break;
            case CALIBRATING:
                setButtonEnabled(R.id.calibration_start, false);
                //setButtonEnabled(R.id.calibration_stop, true);
                setButtonEnabled(R.id.calibration_complete, false);
                break;
            case CALIBRATED:
                //setButtonEnabled(R.id.calibration_start, false);
                setButtonEnabled(R.id.calibration_complete, true);
                break;
        }
    }


    private void setButtonEnabled(int id, boolean enabled) {
        Button button = findViewById(id);
        button.setEnabled(enabled);
    }


    private void setInfoField(int id, Number value) {
        setInfoField(id, "" + value);
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
            showException(exception);
        }
        state = State.READY;
        updateState();
    }


    public void requestCompleteCalibration(final View view) {
        if (calibrationReadings.size() < positions.size()) {
            showSimpleToast(R.string.calibration_not_enough);
            return;
        }

        boolean useNonLinearity = useNonLinearAlgorithm();
        final CalibrationCalculator calibrationCalculator =
                new CalibrationCalculator(useNonLinearity);
        calibrationCalculator.calculate(calibrationReadings);
        double calibrationAssessment = calibrationCalculator.getDelta();
        String message = "Calibration assessment (should be under " + MAX_ERROR + "): " +
            TextTools.formatTo2dp(calibrationAssessment) +
                "\n\nCalibration algorithm: " + (useNonLinearity? "Non-Linear" : "Linear");

        new AlertDialog.Builder(this)
            .setTitle(getString(R.string.calibration_assessment))
            .setMessage(message)
            .setPositiveButton(getString(R.string.calibration_update), (dialog, whichButton) -> {
                try {
                    byte[] coeffs = calibrationCalculator.getCoefficients();
                    Byte[] coefficients = ArrayUtils.toObject(coeffs);
                    new WriteCalibrationTask(view).execute(coefficients);
                } catch (Exception exception) {
                    showException(exception);
                } finally {
                    updateState();
                }
            }).setNegativeButton(getString(R.string.cancel),
                (dialog, whichButton) -> {
                    // Do nothing.
                }).show();


    }


    public void requestDeleteLast(View view) {
        getSurveyManager().deleteLastCalibrationReading();
        syncWithReadings();
    }


    public void requestClearCalibration(View view) {
        new AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_confirm_clear_title))
            .setPositiveButton(getString(R.string.clear), (dialog, whichButton) -> {
                getSurveyManager().clearCalibrationReadings();
                syncWithReadings();
            }).setNegativeButton(getString(R.string.cancel), (dialog, whichButton) -> {
                // Do nothing.
            }).show();
    }


    public void requestSaveCalibration(View view) {

        final EditText input = new EditText(this);
        input.setText(R.string.calibration_default_filename);

        new AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_save_as_title))
            .setView(input)
            .setPositiveButton(getString(R.string.ok), (dialog, whichButton) -> {
                Editable value = input.getText();
                String name = value.toString();
                try {
                    saveCalibration(name);
                } catch (Exception exception) {
                    showException(exception);
                }
            }).setNegativeButton(getString(R.string.cancel), (dialog, whichButton) -> {
                // Do nothing.
                }).show();
    }


    private void saveCalibration(String filename) throws JSONException, IOException {
        String contents = CalibrationJsonTranslater.toText(calibrationReadings);
        String path = getFilePath(filename);
        Saver.saveFile(path, contents);
    }


    public void requestLoadCalibration(View view) {

        File[] calibrationFiles = Util.getCalibrationFiles(this);

        if (calibrationFiles.length == 0) {
            showSimpleToast(R.string.calibration_no_files);
            return;
        }

        AlertDialog.Builder builderSingle = new AlertDialog.Builder(
                this);

        builderSingle.setTitle(getString(R.string.calibration_load));
        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.select_dialog_item);

        for (File file : calibrationFiles) {
            arrayAdapter.add(file.getName());
        }

        builderSingle.setNegativeButton(getString(R.string.cancel),
                (dialog, which) -> dialog.dismiss());

        builderSingle.setAdapter(arrayAdapter,
                (dialog, which) -> {
                    try {
                        String filename = arrayAdapter.getItem(which);
                        loadCalibration(filename);
                    } catch (Exception exception) {
                        showException(exception);
                    }
                });
        builderSingle.show();
    }


    private void loadCalibration(String filename) throws JSONException {
        String path = getFilePath(filename);
        String content = Loader.slurpFile(new File(path));
        List<CalibrationReading> calibrationReadings =
                CalibrationJsonTranslater.toCalibrationReadings(content);
        getSurveyManager().setCalibrationReadings(calibrationReadings);
        syncWithReadings();
    }


    private String getFilePath(String filename) {
        File directory = Util.getCalibrationDirectory(this);
        String path = Util.getPath(directory, filename);
        return path;
    }


    private boolean useNonLinearAlgorithm() {

        String pref = getStringPreference("pref_calibration_algorithm");

        boolean useNonLinear = false; // linear probably safer as default

        try {
            switch (pref) {
                case "Auto":
                    useNonLinear = getComms().doesCurrentDistoPreferNonLinearCalibration();
                    break;
                case "Non-Linear":
                    useNonLinear = true;
                    break;
                case "Linear":
                    useNonLinear = false;
            }
        } catch (Exception exception) {
            // e.g. if disto not connected - just return false and deal with issues elsewhere
        }

        return useNonLinear;
    }

    private DistoXCommunicator getComms() throws NotConnectedToDistoException {
        Communicator communicator = requestComms();
        if (communicator instanceof DistoXCommunicator) {
            return (DistoXCommunicator) communicator;
        } else {
            throw new NotConnectedToDistoException();
        }
    }


    private class WriteCalibrationTask extends AsyncTask<Byte, Void, Boolean> {

        private final ProgressDialog progressDialog;

        private WriteCalibrationTask(View view) {
            progressDialog = new ProgressDialog(view.getContext(), ProgressDialog.STYLE_SPINNER);
        }

        protected void onPreExecute() {
            progressDialog.setMessage(getString(R.string.calibration_writing));
            progressDialog.show();
        }

        @Override
        protected Boolean doInBackground(Byte... coefficients) {

            try {
                byte[] coeffs = ArrayUtils.toPrimitive(coefficients);
                DistoXCommunicator comms = getComms();
                WriteCalibrationProtocol writeCalibrationProtocol = comms.writeCalibration(coeffs);
                waitForEnd(writeCalibrationProtocol, 60);
                if (!writeCalibrationProtocol.isFinished()) {
                    Log.device("Disto busy; forcing disconnect to write calibration");
                    comms.requestDisconnect(); // force it to stop what it's doing
                    waitForEnd(writeCalibrationProtocol, 80);
                }
                return writeCalibrationProtocol.wasSuccessful();

            } catch (NotConnectedToDistoException exception) {
                showException(exception);
                return false;
            }
        }


        private void waitForEnd(WriteCalibrationProtocol writeCalibrationProtocol, int attempts) {
            for (int i = 0; i < attempts; i++) {
                try {
                    if (writeCalibrationProtocol.isFinished()) {
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

    public class NotConnectedToDistoException extends Exception {

    }
}
