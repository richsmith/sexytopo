package org.hwyl.sexytopo.comms.fcl;

import android.bluetooth.BluetoothDevice;
import android.view.View;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.comms.Communicator;
import org.hwyl.sexytopo.comms.fcl.FCLBLE;
import org.hwyl.sexytopo.comms.fcl.EnhancedLegData;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.SurveyManager;
import org.hwyl.sexytopo.control.activity.DeviceActivity;
import org.hwyl.sexytopo.model.survey.Leg;

import java.util.HashMap;
import java.util.Map;

import kotlin.Unit;

/**
 * FCL Enhanced Split Protocol v2.0
 * =============================================
 *
 * Handles communication with FCL cave survey instruments using the enhanced
 * split-packet BLE protocol that overcomes MTU limitations while providing
 * comprehensive shot quality data and environmental monitoring.
 *
 * Protocol Features:
 * - Split packet reception: Primary (20 bytes) + Extended (14 bytes)
 * - Real-time shot quality assessment and environmental monitoring
 * - Full 3-axis orientation data with roll angle
 * - Battery and device health monitoring
 * - Magnetic field anomaly detection
 * - Temperature monitoring
 */
public class FCLCommunicator implements Communicator {

    private final DeviceActivity activity;
    private final FCLBLE fclBLE;
    private final SurveyManager datamanager;

    private static final int LASER_ON_ID = View.generateViewId();
    private static final int SHOT_ID = View.generateViewId();
    private static final int LASER_OFF_ID = View.generateViewId();
    private static final int DEVICE_OFF_ID = View.generateViewId();

    private static final Map<Integer, Integer> CUSTOM_COMMANDS = new HashMap<>();

    static {
        CUSTOM_COMMANDS.put(LASER_ON_ID, R.string.device_sap_command_laser_on);
        CUSTOM_COMMANDS.put(SHOT_ID, R.string.device_sap_command_take_shot);
        CUSTOM_COMMANDS.put(LASER_OFF_ID, R.string.device_sap_command_laser_off);
        CUSTOM_COMMANDS.put(DEVICE_OFF_ID, R.string.device_sap_command_device_off);
    }

    public FCLCommunicator(DeviceActivity activity, BluetoothDevice bluetoothDevice) {
        this.activity = activity;
        this.datamanager = activity.getSurveyManager();

        // Initialize FCLBLE with split protocol callbacks
        this.fclBLE = new FCLBLE(
                bluetoothDevice,
                activity,
                this::legCallback,
                this::enhancedLegCallback,
                this::statusCallback
        );
    }

    @Override
    public boolean isConnected() {
        return fclBLE.isConnected();
    }

    @Override
    public void requestConnect() {
        fclBLE.connect();
    }

    @Override
    public void requestDisconnect() {
        fclBLE.disconnect();
    }

    @Override
    public Map<Integer, Integer> getCustomCommands() {
        return CUSTOM_COMMANDS;
    }

    @Override
    public boolean handleCustomCommand(int viewId) {
        if (viewId == LASER_ON_ID) {
            Log.device("FCL: Laser ON command sent");
            fclBLE.laserOn();
            return true;
        } else if (viewId == LASER_OFF_ID) {
            Log.device("FCL: Laser OFF command sent");
            fclBLE.laserOff();
            return true;
        } else if (viewId == SHOT_ID) {
            Log.device("FCL: Take shot command sent");
            fclBLE.takeShot();
            return true;
        } else if (viewId == DEVICE_OFF_ID) {
            Log.device("FCL: Device OFF command sent");
            fclBLE.deviceOff();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Basic callback for core survey functionality
     * This is called for every measurement to maintain compatibility
     */
    public Unit legCallback(float azimuth, float inclination, float distance) {
        // Always create and update survey with basic leg data
        Leg leg = new Leg(distance, azimuth, inclination);
        datamanager.updateSurvey(leg);

        Log.device(String.format("Basic leg: %.1f° %+.1f° %.3fm",
                azimuth, inclination, distance));

        return Unit.INSTANCE;
    }

    /**
     * Enhanced callback with comprehensive quality data from split packets
     * This processes the reconstructed data from primary + extended packets
     */
    public Unit enhancedLegCallback(EnhancedLegData enhancedData) {

        // Core measurement with environmental summary
        Log.device(String.format("FCL [%d]: %.1f° %+.1f° %.3fm - %s (%.0f%%)",
                enhancedData.getMeasurementId(),
                enhancedData.getAzimuth(),
                enhancedData.getInclination(),
                enhancedData.getDistance(),
                enhancedData.getQualityDescription(),
                enhancedData.getShotQuality() * 100.0f));

        // Environmental summary - always show to confirm enhanced data is working
        Log.device(String.format("   Mag: %.1f/%.1f µT, Dip: %.1f/%.1f°, Batt: %d%%, Roll: %.1f°",
                enhancedData.getCurrentMagneticField(),
                enhancedData.getExpectedMagneticField(),
                enhancedData.getCurrentMagneticDip(),
                enhancedData.getExpectedMagneticDip(),
                enhancedData.getBatteryLevel(),
                enhancedData.getRollAngle()));

        // Critical warnings only
        if (enhancedData.hasLowBattery()) {
            Log.device("WARNING: FCL battery low (" + enhancedData.getBatteryLevel() + "%)");
        }

        if (enhancedData.hasTemperatureWarning()) {
            Log.device("WARNING: Temperature extreme (" +
                    String.format("%.1f°C", enhancedData.getTemperature()) + ")");
        }

        if (enhancedData.hasInterferenceWarning()) {
            Log.device("WARNING: Magnetic interference detected");
        }

        if (enhancedData.hasPoorQuality()) {
            Log.device("WARNING: Poor shot quality - consider retaking");
        }

        // Significant anomalies only
        float magDeviation = enhancedData.getMagneticFieldDeviation();
        if (Math.abs(magDeviation) > 10.0f) {
            Log.device(String.format("ANOMALY: Magnetic field deviation %+.1f µT", magDeviation));
        }

        float dipDeviation = enhancedData.getMagneticDipDeviation();
        if (Math.abs(dipDeviation) > 10.0f) {
            Log.device(String.format("ANOMALY: Magnetic dip deviation %+.1f°", dipDeviation));
        }

        // Quality recommendations
        if (enhancedData.getShotQuality() < 0.5f) {
            Log.device("RECOMMEND: Strongly consider retaking this shot");
        }

        // Packet integrity check
        if (!enhancedData.isValid()) {
            Log.device("WARNING: Packet integrity check failed");
        }

        return Unit.INSTANCE;
    }

    /**
     * Status callback for connection state changes
     */
    public Unit statusCallback(int status, String msg) {
        switch (status) {
            case FCLBLE.CONNECTED:
                Log.device("FCL Connected - Protocol v2.0");
                break;

            case FCLBLE.DISCONNECTED:
                Log.device("FCL Disconnected");
                activity.updateConnectionStatus();
                break;

            case FCLBLE.CONNECTION_FAILED:
                Log.device("FCL Connection Failed: " + msg);
                activity.updateConnectionStatus();
                break;
        }
        return Unit.INSTANCE;
    }

    /**
     * Get protocol description for UI display
     */
    public String getProtocolDescription() {
        return "FCL Protocol v2.0";
    }

    /**
     * Get detailed protocol information
     */
    public String getProtocolDetails() {
        return "Primary(20B) + Extended(14B) packets with state machine recovery";
    }

    /**
     * Debug method to log protocol information
     */
    public void logProtocolInfo() {
        Log.device("Protocol: FCL Protocol v2.0");
    }

    /**
     * Get current connection statistics (if needed for debugging)
     */
    public void logConnectionStats() {
        if (isConnected()) {
            Log.device("FCL Connection Status: ACTIVE");
            Log.device("Protocol: Split packet transmission");
            Log.device("Characteristics: PRIMARY + EXTENDED");
        } else {
            Log.device("FCL Connection Status: INACTIVE");
        }
    }
}
