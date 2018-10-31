package org.hwyl.sexytopo.control.calibration;

import org.hwyl.sexytopo.control.calibration.topodroid.CalibAlgo;
import org.hwyl.sexytopo.control.calibration.topodroid.CalibAlgoBH;
import org.hwyl.sexytopo.control.calibration.topodroid.CalibCBlock;
import org.hwyl.sexytopo.model.calibration.CalibrationReading;

import java.util.ArrayList;
import java.util.List;


public class CalibrationCalculator {

    private CalibAlgo algorithm;

    public void calculate(List<CalibrationReading> calibrationReadings) {


        List<CalibCBlock> blocks = createCalibCBlockList(calibrationReadings);

        //GroupReadings.group(blocks, -1);

        algorithm = new CalibAlgoBH(0, false); // FIXME should this be FALSE?

        for (CalibCBlock block : blocks) {
            algorithm.AddValues(block);
        }

        algorithm.Calibrate();
    }


    public double getMaxError() {
        return algorithm.MaxError();
    }


    public byte[] getCoefficients() {
        return algorithm.getCoeff();
    }


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

}