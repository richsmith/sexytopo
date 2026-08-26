package org.hwyl.sexytopo.comms;

import android.os.Handler;
import android.os.Looper;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.util.GeneralPreferences;

/**
 * Decides whether a communicator should reconnect after losing its device, and schedules the
 * attempts. Only unexpected disconnections are worth reconnecting after, so communicators report
 * user intent as well as trouble.
 *
 * <p>Attempts stop once the window set in the preferences has elapsed, measured from the first
 * failure in a run, so a device left behind in a cave doesn't keep the radio busy all day.
 *
 * <p>Not thread safe, and it schedules onto the main looper: call it from the main thread.
 */
public class ReconnectionPolicy {

    private static final long RETRY_INTERVAL_MS = 3000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final String deviceName;
    private final Runnable reconnect;

    private boolean userRequestedDisconnect = false;

    /** When the current run of attempts must give up; null if no run is in progress. */
    private Long giveUpAt = null;

    public ReconnectionPolicy(String deviceName, Runnable reconnect) {
        this.deviceName = deviceName;
        this.reconnect = reconnect;
    }

    /** Call when the user asks to connect, so a later drop counts as unexpected. */
    public void noteUserRequestedConnect() {
        userRequestedDisconnect = false;
    }

    /** Call when the user asks to disconnect, so we leave the device alone. */
    public void noteUserRequestedDisconnect() {
        userRequestedDisconnect = true;
        cancel();
    }

    /** Call on connecting, so the next failure starts a fresh window. */
    public void noteConnected() {
        giveUpAt = null;
    }

    /** Call when the device drops out or fails to connect. */
    public void onUnexpectedDisconnection() {

        if (userRequestedDisconnect || !GeneralPreferences.isAutoReconnectOn()) {
            return;
        }

        long now = System.currentTimeMillis();

        if (giveUpAt == null) {
            giveUpAt = now + GeneralPreferences.getAutoReconnectWindowMinutes() * 60_000L;
        } else if (now >= giveUpAt) {
            Log.device(R.string.device_ble_auto_reconnect_gave_up, deviceName);
            giveUpAt = null;
            return;
        }

        Log.device(R.string.device_ble_auto_reconnecting, deviceName);
        handler.postDelayed(reconnect, RETRY_INTERVAL_MS);
    }

    /** Call when the communicator is being torn down, to drop any pending attempt. */
    public void cancel() {
        handler.removeCallbacksAndMessages(null);
        giveUpAt = null;
    }

    /**
     * How long a retry loop should keep going, in milliseconds, or zero if auto-reconnect is off.
     * The DistoX communicators drive their own loops from a background thread, so they can't use
     * the scheduling above but can still apply the same preference and time limit.
     */
    public static long getRetryWindowMs() {

        if (!GeneralPreferences.isAutoReconnectOn()) {
            return 0;
        }

        return GeneralPreferences.getAutoReconnectWindowMinutes() * 60_000L;
    }
}
