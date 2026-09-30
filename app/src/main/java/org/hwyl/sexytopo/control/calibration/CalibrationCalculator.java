package org.hwyl.sexytopo.control.calibration;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.hwyl.sexytopo.model.calibration.CalibrationReading;

public class CalibrationCalculator {

    // The algorithm expects its first 16 readings to be four sets of four
    private static final int GROUPED_SET_COUNT = 4;
    private static final int READINGS_PER_GROUPED_SET = 4;

    // Uncalibrated readings in one set can wander by 40 degrees on a poorly behaved instrument,
    // while consecutive directions in a calibration are at least about 70 degrees apart
    private static final double SET_TOLERANCE = 50;

    private final boolean useNonLinearity;
    private final MutableFloat delta = new MutableFloat(0);
    private List<CalibrationReading> readings = new ArrayList<>();
    private List<List<Integer>> sets = new ArrayList<>();

    public CalibrationCalculator(boolean useNonLinearity) {
        this.useNonLinearity = useNonLinearity;
    }

    /** Uses every reading given, which must not include any gaps. */
    public int calculate(List<CalibrationReading> calibrationReadings) {

        readings = new ArrayList<>(calibrationReadings);
        sets = findSets(readings);
        List<Integer> order = getAlgorithmOrder(sets, readings);

        Vector[] g = new Vector[readings.size()];
        Vector[] m = new Vector[readings.size()];

        for (int i = 0; i < readings.size(); i++) {
            CalibrationReading calibrationReading = readings.get(order.get(i));
            CalibAlgorithm.AddValues(
                    calibrationReading.getGx(),
                    calibrationReading.getGy(),
                    calibrationReading.getGz(),
                    calibrationReading.getMx(),
                    calibrationReading.getMy(),
                    calibrationReading.getMz(),
                    g,
                    m,
                    i);
        }

        return CalibAlgorithm.Optimize(g, m, delta, useNonLinearity);
    }

    public byte[] getCoefficients() {
        return CalibAlgorithm.GetCoeff(useNonLinearity);
    }

    public double getDelta() {
        return delta.value;
    }

    /**
     * For each reading, the angle in degrees between its calibrated laser direction and the average
     * direction of its set, so a large angle marks a reading that disagrees with the rest of its
     * set. NaN for a reading that is alone in its set. Only valid straight after calculate.
     */
    public double[] getReadingErrors() {
        Vector[] directions =
                readings.stream()
                        .map(reading -> getCalibratedDirection(reading).toVector())
                        .toArray(Vector[]::new);
        double[] errors = new double[directions.length];
        for (List<Integer> set : sets) {
            Vector average =
                    Vector.Normalized(
                            set.stream()
                                    .map(i -> directions[i])
                                    .reduce(Vector.getZero(), (a, b) -> a.plus(b)));
            for (int i : set) {
                errors[i] = set.size() > 1 ? angleBetween(directions[i], average) : Double.NaN;
            }
        }
        return errors;
    }

    /** Each reading's direction using the calibration. Only valid straight after calculate. */
    public ReadingDirection[] getReadingDirections() {
        return readings.stream()
                .map(CalibrationCalculator::getCalibratedDirection)
                .toArray(ReadingDirection[]::new);
    }

    /**
     * Groups consecutive readings that point the same way, each set being one direction shot at
     * several rolls. Uses uncalibrated directions, so it works before there is a calibration. A gap
     * (null) ends a set. Returns the indices of each set's readings.
     */
    public static List<List<Integer>> findSets(List<CalibrationReading> readings) {
        List<List<Integer>> sets = new ArrayList<>();
        List<Integer> current = null;
        Vector sum = null;
        for (int i = 0; i < readings.size(); i++) {
            CalibrationReading reading = readings.get(i);
            if (reading == null) {
                current = null;
                continue;
            }
            Vector direction = getUncalibratedDirection(reading).toVector();
            if (current != null
                    && angleBetween(direction, Vector.Normalized(sum)) <= SET_TOLERANCE) {
                current.add(i);
                sum = sum.plus(direction);
            } else {
                current = new ArrayList<>();
                current.add(i);
                sets.add(current);
                sum = direction;
            }
        }
        return sets;
    }

    /**
     * A reading's direction from its raw sensor values. Without a calibration this is only
     * approximate, but close enough to tell which way the instrument was pointing.
     */
    public static ReadingDirection getUncalibratedDirection(CalibrationReading reading) {
        Vector g = new Vector(reading.getGx(), reading.getGy(), reading.getGz());
        Vector m = new Vector(reading.getMx(), reading.getMy(), reading.getMz());
        return ReadingDirection.fromVectors(g, m);
    }

    /**
     * The order to give the readings to the algorithm: the first four sets of at least four
     * readings (four readings from each), then the rest in the order taken. For readings taken in
     * the usual order this is the order taken. Falls back to the order taken if there are not
     * enough sets of four.
     */
    private static List<Integer> getAlgorithmOrder(
            List<List<Integer>> sets, List<CalibrationReading> readings) {
        List<List<Integer>> groupedSets =
                sets.stream()
                        .filter(set -> set.size() >= READINGS_PER_GROUPED_SET)
                        .limit(GROUPED_SET_COUNT)
                        .collect(Collectors.toList());

        if (groupedSets.size() < GROUPED_SET_COUNT) {
            return IntStream.range(0, readings.size()).boxed().collect(Collectors.toList());
        }

        List<Integer> order =
                groupedSets.stream()
                        .flatMap(set -> set.subList(0, READINGS_PER_GROUPED_SET).stream())
                        .collect(Collectors.toList());
        IntStream.range(0, readings.size()).filter(i -> !order.contains(i)).forEach(order::add);
        return order;
    }

    private static ReadingDirection getCalibratedDirection(CalibrationReading reading) {
        Vector[] calibrated =
                CalibAlgorithm.Apply(
                        reading.getGx(),
                        reading.getGy(),
                        reading.getGz(),
                        reading.getMx(),
                        reading.getMy(),
                        reading.getMz());
        return ReadingDirection.fromVectors(calibrated[0], calibrated[1]);
    }

    private static double angleBetween(Vector a, Vector b) {
        double cosine = Math.max(-1, Math.min(1, a.times(b)));
        return Math.toDegrees(Math.acos(cosine));
    }
}
