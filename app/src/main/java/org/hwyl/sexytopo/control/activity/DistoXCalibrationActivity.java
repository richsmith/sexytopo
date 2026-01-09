package org.hwyl.sexytopo.control.activity;

import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.documentfile.provider.DocumentFile;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.apache.commons.lang3.ArrayUtils;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.SexyTopoConstants;
import org.hwyl.sexytopo.comms.Communicator;
import org.hwyl.sexytopo.comms.DistoX;
import org.hwyl.sexytopo.comms.distox.DistoXCommunicator;
import org.hwyl.sexytopo.comms.distox.DistoXStyleCommunicator;
import org.hwyl.sexytopo.comms.distox.WriteCalibrationProtocol;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.calibration.CalibrationCalculator;
import org.hwyl.sexytopo.control.io.IoUtils;
import org.hwyl.sexytopo.control.io.StartLocation;
import org.hwyl.sexytopo.control.io.basic.CalibrationJsonTranslater;
import org.hwyl.sexytopo.control.util.GeneralPreferences;
import org.hwyl.sexytopo.control.util.TextTools;
import org.hwyl.sexytopo.model.calibration.CalibrationReading;
import org.hwyl.sexytopo.model.sketch.Colour;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class DistoXCalibrationActivity extends SexyTopoActivity {

    public static final double MAX_ERROR = 0.5;

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
        setupMaterialToolbar();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        applyEdgeToEdgeInsets(R.id.rootLayout, true, true);

        BroadcastReceiver updatedCalibrationReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                syncWithReadings();
            }
        };

        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        broadcastManager.registerReceiver(updatedCalibrationReceiver,
                new IntentFilter(SexyTopoConstants.CALIBRATION_UPDATED_EVENT));
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

        if (!calibrationReadings.isEmpty()) {
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

        if (calibrationReadings.isEmpty()) {
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
            showExceptionAndLog(exception);
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
        String result = TextTools.formatTo2dp(calibrationAssessment);
        String algorithm = useNonLinearity? "Non-Linear" : "Linear";
        String message = getString(
                R.string.device_distox_calibration_result, MAX_ERROR, result, algorithm);

        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.calibration_assessment)
            .setMessage(message)
            .setPositiveButton(R.string.calibration_update, (dialog, whichButton) -> {
                try {
                    byte[] coeffs = calibrationCalculator.getCoefficients();
                    Byte[] coefficients = ArrayUtils.toObject(coeffs);
                    requestWriteCalibration(view, coefficients);
                } catch (Exception exception) {
                    showExceptionAndLog(exception);
                } finally {
                    updateState();
                }
            }).setNegativeButton(R.string.cancel, null)
            .show();


    }


    public void requestDeleteLast(View view) {
        getSurveyManager().deleteLastCalibrationReading();
        syncWithReadings();
    }


    public void requestClearCalibration(View view) {
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_confirm_clear_title)
            .setPositiveButton(getString(R.string.clear), (dialog, whichButton) -> {
                getSurveyManager().clearCalibrationReadings();
                syncWithReadings();
            }).setNegativeButton(R.string.cancel, null)
            .show();
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
            DistoXStyleCommunicator comms = getComms();

            if (comms instanceof DistoXCommunicator) {
                // This is a bit hacky, but the old Disto needs some real-time management
                new WriteCalibrationTask(view).execute(coefficients);
            } else {
                comms.writeCalibration(coefficients);
            }

        } catch (Exception exception) {
            showExceptionAndLog(exception);
        }

    }

    private void saveCalibration(Uri uri) throws JSONException, IOException {
        DocumentFile file = DocumentFile.fromSingleUri(this, uri);
        String contents = CalibrationJsonTranslater.toText(calibrationReadings);
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

    private DistoX getDistox() throws SecurityException {
        BluetoothDevice device = getInstrument().getBluetoothDevice();
        DistoX distoX = DistoX.fromDevice(device);
        return distoX;
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {

        if (resultData == null) {
            return;
        }

        if (resultCode != Activity.RESULT_OK) {
            Exception exception = new Exception(
                getString(R.string.request_code_error, resultCode, requestCode));
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
            android.widget.ProgressBar progressBar = new android.widget.ProgressBar(view.getContext());
            progressBar.setIndeterminate(true);
            progressBar.setPadding(0, 50, 0, 50);

            progressDialog = new MaterialAlertDialogBuilder(view.getContext())
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
                DistoXCommunicator comms = (DistoXCommunicator)getComms();
                WriteCalibrationProtocol writeCalibrationProtocol =
                        comms.writeCalibration(coefficients);
                waitForEnd(writeCalibrationProtocol, 60);
                if (!writeCalibrationProtocol.isFinished()) {
                    Log.device(R.string.device_distox_force_disconnect_for_calibration);
                    comms.requestDisconnect(); // force it to stop what it's doing
                    waitForEnd(writeCalibrationProtocol, 80);
                }
                return writeCalibrationProtocol.wasSuccessful();

            } catch (NotConnectedToDistoException exception) {
                showExceptionAndLog(exception);
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

    public static class NotConnectedToDistoException extends Exception {
    }
}
