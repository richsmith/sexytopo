package org.hwyl.sexytopo.control.io.basic;

import java.util.Arrays;
import java.util.List;
import org.hwyl.sexytopo.model.calibration.CalibrationReading;
import org.junit.Assert;
import org.junit.Test;

public class CalibrationJsonTranslaterTest {

    @Test
    public void testDeletedReadingsSurviveARoundTrip() throws Exception {
        CalibrationReading reading = new CalibrationReading();
        reading.updateAccelerationValues(1, -2, 3);
        reading.updateMagneticValues(-4, 5, -6);

        String text = CalibrationJsonTranslater.toText(Arrays.asList(reading, null, reading));
        List<CalibrationReading> loaded = CalibrationJsonTranslater.toCalibrationReadings(text);

        Assert.assertEquals(3, loaded.size());
        Assert.assertNull(loaded.get(1));
        Assert.assertEquals(-2, loaded.get(2).getGy());
        Assert.assertEquals(-6, loaded.get(2).getMz());
    }
}
