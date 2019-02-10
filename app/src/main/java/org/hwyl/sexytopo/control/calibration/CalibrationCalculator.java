package org.hwyl.sexytopo.control.calibration;

import org.hwyl.sexytopo.model.calibration.CalibrationReading;

import java.util.List;


public class CalibrationCalculator {

    private boolean useNonLinearity;
    private  MutableFloat delta = new MutableFloat(0);

    public CalibrationCalculator(boolean useNonLinearity) {
        this.useNonLinearity = useNonLinearity;
    }


    public int calculate(List<CalibrationReading> calibrationReadings) {

        Vector[] g = new Vector[56];
        Vector[] m = new Vector[56];

        for (int i = 0; i < 56; i++) {
            CalibrationReading calibrationReading = calibrationReadings.get(i);
            CalibAlgorithm.AddValues(
                    calibrationReading.getGx(),
                    calibrationReading.getGy(),
                    calibrationReading.getGz(),
                    calibrationReading.getMx(),
                    calibrationReading.getMy(),
                    calibrationReading.getMz(),
                    g, m, i);
        }

        int iterations = CalibAlgorithm.Optimize(g, m, delta, useNonLinearity);
        return iterations;
    }

    public byte[] getCoefficients() {
        return CalibAlgorithm.GetCoeff(useNonLinearity);
    }

    public double getDelta() {
        return delta.value;
    }

}
