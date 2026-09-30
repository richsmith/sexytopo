package org.hwyl.sexytopo.control.calibration;

import java.util.ArrayList;
import java.util.List;
import org.hwyl.sexytopo.model.calibration.CalibrationReading;
import org.junit.Assert;
import org.junit.Test;

public class CalibrationCoverageTest {

    @Test
    public void testAFullCalibrationMeetsEveryTarget() {
        CalibrationCoverage coverage =
                CalibrationCoverage.of(
                        CalibrationCalculatorTest.toCalibrationReadings(
                                CalibrationCalculatorTest.EXAMPLE_CALIBRATION));

        Assert.assertEquals(14, coverage.getSetCount());
        Assert.assertEquals(14, coverage.getCompleteSetCount());
        for (ReadingDirection.Pointing pointing : ReadingDirection.Pointing.values()) {
            Assert.assertTrue(pointing.toString(), coverage.isTargetMet(pointing));
        }
    }

    @Test
    public void testARepeatedDirectionCountsOnce() {
        List<CalibrationReading> readings =
                CalibrationCalculatorTest.toCalibrationReadings(
                        CalibrationCalculatorTest.EXAMPLE_CALIBRATION);
        // shoot the first horizontal set (readings 9 to 12) again instead of the next one
        List<CalibrationReading> repeated = new ArrayList<>(readings);
        for (int i = 0; i < 4; i++) {
            repeated.set(12 + i, readings.get(8 + i));
        }

        CalibrationCoverage coverage = CalibrationCoverage.of(repeated);

        Assert.assertEquals(13, coverage.getSetCount());
        Assert.assertEquals(3, coverage.getDirectionCount(ReadingDirection.Pointing.HORIZONTAL));
        Assert.assertFalse(coverage.isTargetMet(ReadingDirection.Pointing.HORIZONTAL));
    }
}
