package org.hwyl.sexytopo.control.components;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.calibration.ReadingDirection;
import org.hwyl.sexytopo.control.util.TextTools;
import org.hwyl.sexytopo.model.calibration.CalibrationReading;
import org.hwyl.sexytopo.model.sketch.Colour;

/**
 * Shows one calibration reading: its number and guided position, which way the instrument was
 * pointing, its raw sensor values, and how far it disagrees with the rest of its set.
 */
public class CalibrationReadingView extends LinearLayout {

    public static final double GOOD_ERROR = 1.0; // degrees

    public CalibrationReadingView(Context context) {
        this(context, null);
    }

    public CalibrationReadingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
        inflate(context, R.layout.calibration_reading_view, this);
    }

    /**
     * The position is the guided direction and orientation the reading should have been taken in.
     * The error is in degrees from the rest of the reading's set, or null if not known yet. The
     * direction is uncalibrated when there is no calibration to apply yet.
     */
    public void showReading(
            int number,
            String positionDirection,
            String positionOrientation,
            CalibrationReading reading,
            ReadingDirection direction,
            boolean isCalibrated,
            Double error) {
        showHeader(number, positionDirection, positionOrientation);
        setMessage(null);
        findViewById(R.id.readingDetails).setVisibility(VISIBLE);

        setText(R.id.readingInclination, formatDegrees(direction.inclination));
        setText(R.id.readingAzimuth, formatDegrees(direction.azimuth));
        setText(R.id.readingRoll, formatDegrees(direction.roll));
        findViewById(R.id.readingUncalibrated).setVisibility(isCalibrated ? GONE : VISIBLE);

        setText(R.id.readingGx, String.valueOf(reading.getGx()));
        setText(R.id.readingGy, String.valueOf(reading.getGy()));
        setText(R.id.readingGz, String.valueOf(reading.getGz()));
        setText(R.id.readingMx, String.valueOf(reading.getMx()));
        setText(R.id.readingMy, String.valueOf(reading.getMy()));
        setText(R.id.readingMz, String.valueOf(reading.getMz()));

        TextView errorView = findViewById(R.id.readingError);
        if (error == null) {
            errorView.setVisibility(GONE);
        } else {
            errorView.setVisibility(VISIBLE);
            errorView.setText(
                    getContext()
                            .getString(R.string.calibration_degrees, TextTools.formatTo1dp(error)));
            errorView.setTextColor(
                    error <= GOOD_ERROR ? Colour.SEA_GREEN.intValue : Colour.RED.intValue);
        }
    }

    public void showDeleted(int number, String positionDirection, String positionOrientation) {
        showHeader(number, positionDirection, positionOrientation);
        findViewById(R.id.readingError).setVisibility(GONE);
        findViewById(R.id.readingDetails).setVisibility(GONE);
        setMessage(getContext().getString(R.string.calibration_reading_deleted));
    }

    public void showNoReadings() {
        setText(R.id.readingNumber, "");
        setText(R.id.readingDirectionName, "");
        setText(R.id.readingOrientationName, "");
        findViewById(R.id.readingError).setVisibility(GONE);
        findViewById(R.id.readingDetails).setVisibility(GONE);
        setMessage(getContext().getString(R.string.calibration_no_readings));
    }

    private void showHeader(int number, String positionDirection, String positionOrientation) {
        setText(
                R.id.readingNumber,
                getContext().getString(R.string.calibration_reading_number, number));
        setText(R.id.readingDirectionName, positionDirection);
        setText(R.id.readingOrientationName, positionOrientation);
    }

    private void setMessage(String message) {
        TextView messageView = findViewById(R.id.readingMessage);
        messageView.setVisibility(message == null ? GONE : VISIBLE);
        messageView.setText(message);
    }

    private String formatDegrees(double degrees) {
        return getContext().getString(R.string.calibration_degrees, TextTools.formatTo0dp(degrees));
    }

    private void setText(int id, String text) {
        TextView textView = findViewById(id);
        textView.setText(text);
    }
}
