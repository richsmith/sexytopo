package org.hwyl.sexytopo.control.calibration;

import org.hwyl.sexytopo.model.calibration.CalibrationReading;

import java.util.List;






public class CalibrationCalculator {

    public static double calculate(List<CalibrationReading> calibrationReadings) {

        /*
        List<CalibCBlock> blocks = createCalibCBlockList(calibrationReadings);

        GroupReadings.group(blocks, -1);

        CalibAlgoBH beatHeebCalibrationAlgorithm = new CalibAlgoBH(0, false); // FIXME should this be FALSE?

        for (CalibCBlock block : blocks) {
            beatHeebCalibrationAlgorithm.AddValues(block);
        }

        beatHeebCalibrationAlgorithm.Calibrate();
        return beatHeebCalibrationAlgorithm.MaxError();
        */

        return 0;
    }


    /*
    private static List<CalibCBlock> createCalibCBlockList(List<CalibrationReading> readings) {
        List<CalibCBlock> blocks = new ArrayList<>();
        for (CalibrationReading reading : readings) {
            blocks.add(createCalibCBlock(reading));
        }
        return blocks;
    }

    private static CalibCBlock createCalibCBlock(CalibrationReading reading) {
        CalibCBlock block = new CalibCBlock();
        block.setData(
                reading.getGx(),
                reading.getGy(),
                reading.getGz(),
                reading.getMx(),
                reading.getMy(),
                reading.getMz()
        );
        return block;
    }

    /*
    public static void go(List<CalibrationReading> calibrationReadings) {

        double sa = 0;
        double ca = 0;

        double avG = 0;
        double avM = 0;

        for (CalibrationReading reading : calibrationReadings) {
            RealVector
        }
    }*/

    /*
    private Set<Set<CalibrationReading>> groupReadings(List<CalibrationReading> readings) {

    }*/

}