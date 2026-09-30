package org.hwyl.sexytopo.model.calibration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Calibration readings in the order they are taken. A reading's place in the list says which
 * position it was taken in. Replacing a reading leaves a gap (a null) in its place, which the next
 * reading taken fills; deleting one removes it and moves the later readings up one place.
 */
public class CalibrationReadingList {

    private final List<CalibrationReading> readings = new ArrayList<>();
    private CalibrationReading lastAdded = null;

    public void add(CalibrationReading reading) {
        int gap = readings.indexOf(null);
        if (gap >= 0) {
            readings.set(gap, reading);
        } else {
            readings.add(reading);
        }
        lastAdded = reading;
    }

    /** Leaves a gap for the next reading taken to fill. */
    public void replace(int index) {
        readings.set(index, null);
        trimTrailingGaps();
    }

    /** Removes the reading (or gap), moving the later readings up one place. */
    public void delete(int index) {
        readings.remove(index);
        trimTrailingGaps();
    }

    /**
     * Deletes the reading shown as the last one. If it filled a gap it leaves the gap again, so the
     * later readings keep their places; otherwise it is simply removed.
     */
    public void deleteLastAdded() {
        CalibrationReading last = getLastAdded();
        if (last != null) {
            replace(readings.indexOf(last));
        }
    }

    public void clear() {
        readings.clear();
        lastAdded = null;
    }

    public void setAll(List<CalibrationReading> newReadings) {
        readings.clear();
        readings.addAll(newReadings);
        trimTrailingGaps();
        lastAdded = null;
        for (int i = readings.size() - 1; i >= 0 && lastAdded == null; i--) {
            lastAdded = readings.get(i);
        }
    }

    /** All positions up to the last reading taken, with null for a deleted reading. */
    public List<CalibrationReading> getAll() {
        return Collections.unmodifiableList(readings);
    }

    /**
     * The reading most recently taken, or if that has since been deleted, the latest one still in
     * the list. Null if there are none.
     */
    public CalibrationReading getLastAdded() {
        if (lastAdded != null && readings.contains(lastAdded)) {
            return lastAdded;
        }
        for (int i = readings.size() - 1; i >= 0; i--) {
            if (readings.get(i) != null) {
                return readings.get(i);
            }
        }
        return null;
    }

    /** The position the next reading will fill. */
    public int getNextIndex() {
        int gap = readings.indexOf(null);
        return gap >= 0 ? gap : readings.size();
    }

    public int getCount() {
        return (int) readings.stream().filter(Objects::nonNull).count();
    }

    public boolean isEmpty() {
        return readings.isEmpty();
    }

    /** Whether there are at least count readings and no gaps waiting to be retaken. */
    public boolean isComplete(int count) {
        return !readings.contains(null) && readings.size() >= count;
    }

    private void trimTrailingGaps() {
        while (!readings.isEmpty() && readings.get(readings.size() - 1) == null) {
            readings.remove(readings.size() - 1);
        }
    }
}
