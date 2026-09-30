package org.hwyl.sexytopo.control.calibration;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.hwyl.sexytopo.model.calibration.CalibrationReading;

/**
 * Summarises how well a set of calibration readings covers the directions a calibration needs: the
 * corners of a cube and the centres of its faces, each shot at several rolls. Works from
 * uncalibrated directions, which are plenty accurate enough to tell those directions apart.
 */
public class CalibrationCoverage {

    public static final int READINGS_PER_SET = 4;

    // Sets of the same direction taken at different times count once
    private static final double SAME_DIRECTION = 30; // degrees

    private static final Map<ReadingDirection.Pointing, Integer> TARGETS =
            new EnumMap<>(ReadingDirection.Pointing.class);

    static {
        TARGETS.put(ReadingDirection.Pointing.HORIZONTAL, 4);
        TARGETS.put(ReadingDirection.Pointing.CORNER_UP, 4);
        TARGETS.put(ReadingDirection.Pointing.CORNER_DOWN, 4);
        TARGETS.put(ReadingDirection.Pointing.UP, 1);
        TARGETS.put(ReadingDirection.Pointing.DOWN, 1);
    }

    private final int setCount;
    private final int completeSetCount;
    private final Map<ReadingDirection.Pointing, Integer> directionCounts =
            new EnumMap<>(ReadingDirection.Pointing.class);

    private CalibrationCoverage(List<CalibrationReading> readings) {
        List<List<Integer>> sets = CalibrationCalculator.findSets(readings);
        setCount = sets.size();
        completeSetCount =
                (int) sets.stream().filter(set -> set.size() >= READINGS_PER_SET).count();

        for (ReadingDirection.Pointing pointing : ReadingDirection.Pointing.values()) {
            directionCounts.put(pointing, 0);
        }
        List<Vector> directions = new ArrayList<>();
        for (List<Integer> set : sets) {
            Vector direction = getAverageDirection(readings, set);
            boolean isNew =
                    directions.stream()
                            .allMatch(known -> angleBetween(known, direction) > SAME_DIRECTION);
            if (isNew) {
                directions.add(direction);
                double inclination = Math.toDegrees(Math.asin(-direction.z));
                directionCounts.merge(ReadingDirection.getPointing(inclination), 1, Integer::sum);
            }
        }
    }

    /** Gaps (nulls) in the readings are ignored. */
    public static CalibrationCoverage of(List<CalibrationReading> readings) {
        return new CalibrationCoverage(readings);
    }

    public int getSetCount() {
        return setCount;
    }

    /** Sets with enough readings (rolls) to be used as a group. */
    public int getCompleteSetCount() {
        return completeSetCount;
    }

    /** The number of distinct directions covered that point this way. */
    public int getDirectionCount(ReadingDirection.Pointing pointing) {
        return directionCounts.get(pointing);
    }

    /** How many distinct directions pointing this way a full calibration has. */
    public static int getTarget(ReadingDirection.Pointing pointing) {
        return TARGETS.get(pointing);
    }

    public boolean isTargetMet(ReadingDirection.Pointing pointing) {
        return getDirectionCount(pointing) >= getTarget(pointing);
    }

    private static Vector getAverageDirection(
            List<CalibrationReading> readings, List<Integer> set) {
        return Vector.Normalized(
                set.stream()
                        .map(readings::get)
                        .filter(Objects::nonNull)
                        .map(
                                reading ->
                                        CalibrationCalculator.getUncalibratedDirection(reading)
                                                .toVector())
                        .reduce(Vector.getZero(), (a, b) -> a.plus(b)));
    }

    private static double angleBetween(Vector a, Vector b) {
        double cosine = Math.max(-1, Math.min(1, a.times(b)));
        return Math.toDegrees(Math.acos(cosine));
    }
}
