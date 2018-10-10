package org.hwyl.sexytopo.control.activity;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.SexyTopo;
import org.hwyl.sexytopo.comms.DistoXCommunicator;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.SurveyManager;
import org.hwyl.sexytopo.control.calibration.CalibrationCalculator;
import org.hwyl.sexytopo.model.calibration.CalibrationReading;

import java.util.ArrayList;
import java.util.List;


public class CalibrationActivity extends SexyTopoActivity {

    public static double MAX_ERROR = 0.5;

    private enum CalibrationDirection {
        FORWARD(R.string.direction_forward),
        RIGHT(R.string.direction_right),
        BACK(R.string.direction_back),
        LEFT(R.string.direction_left),
        UP(R.string.direction_up),
        DOWN(R.string.direction_down),
        FORWARD_LEFT_UP(R.string.direction_forward_left_up),
        FORWARD_LEFT_DOWN(R.string.direction_forward_left_down),
        FORWARD_RIGHT_UP(R.string.direction_forward_right_up),
        FORWARD_RIGHT_DOWN(R.string.direction_forward_right_down),
        BACK_RIGHT_UP(R.string.direction_back_right_up),
        BACK_RIGHT_DOWN(R.string.direction_back_right_down),
        BACK_LEFT_UP(R.string.direction_back_left_up),
        BACK_LEFT_DOWN(R.string.direction_back_left_down);

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

    private DistoXCommunicator comms = getComms();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibration);

        BroadcastReceiver updatedCalibrationReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                syncWithReadings();
            }
        };

        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        broadcastManager.registerReceiver(updatedCalibrationReceiver,
                new IntentFilter(SexyTopo.CALIBRATION_UPDATED_EVENT));

        dataManager = SurveyManager.getInstance(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        syncWithReadings();
        comms = DistoXCommunicator.getInstance(this, dataManager);
    }

    private void syncWithReadings() {
        calibrationReadings = dataManager.getCalibrationReadings();
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
        }

        String label = calibrationReadings.size() + "/" + positions.size() + "+";
        setInfoField(R.id.calibration_index, label);

        if (calibrationReadings.size() < positions.size()) {
            Pair<CalibrationDirection, Orientation> suggestedNext =
                    positions.get(calibrationReadings.size());
            setInfoField(R.id.calibration_next_direction,
                    getString(suggestedNext.first.stringId));
            setInfoField(R.id.calibration_next_orientation,
                    getString(suggestedNext.second.stringId));
            setInfoField(R.id.calibrationFieldAssessment, getString(R.string.not_applicable));
        } else {
            setInfoField(R.id.calibration_next_direction, getString(R.string.not_applicable));
            setInfoField(R.id.calibration_next_orientation, getString(R.string.not_applicable));

            double assessment = CalibrationCalculator.calculate(calibrationReadings);

            TextView assessmentField = (TextView)(findViewById(R.id.calibrationFieldAssessment));
            if (assessment < 0.5) {
                assessmentField.setTextColor(Color.RED);
            } else {
                assessmentField.setTextColor(Color.BLACK);
            }
            setInfoField(R.id.calibrationFieldAssessment, assessment);
        }
    }


    private void updateState() {

        if (calibrationReadings.size() >= positions.size()) {
            state = State.CALIBRATED;
        }

        switch(state) {
            case READY:
                setButtonEnabled(R.id.calibration_start, true);
                setButtonEnabled(R.id.calibration_complete, false);
                break;
            case CALIBRATING:
                setButtonEnabled(R.id.calibration_start, false);
                setButtonEnabled(R.id.calibration_complete, false);
                break;
            case CALIBRATED:
                setButtonEnabled(R.id.calibration_start, false);
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
            comms.startCalibration();
            state = State.CALIBRATING;
            updateState();
        } catch (Exception exception) {
            Log.e(exception);
            showSimpleToast("Error starting calibration: " + exception);
        }
    }


    public void requestStopCalibration(View view) throws Exception {
        Log.device("Stop calibration requested");
        comms.stopCalibration();
        state = State.READY;
        updateState();
    }


    public void completeCalibration(View view) {
        if (calibrationReadings.size() < positions.size()) {
            showSimpleToast(R.string.calibration_not_enough);
            return;
        }

        double calibrationAssessment = CalibrationCalculator.calculate(calibrationReadings);
        String message = "Calibration assessment (should be under " + MAX_ERROR + "): " +
                calibrationAssessment;

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.calibration_assessment))
                .setMessage(message)
                .setPositiveButton(getString(R.string.calibration_update), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        try {
                            //comms.update()
                            updateState();
                        } catch (Exception exception) {
                            Log.e(exception);
                            showSimpleToast(R.string.calibration_error_updating_device);
                            Log.e(exception);
                        }
                    }
                }).setNegativeButton(getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Do nothing.
                    }
                }).show();


    }

}
