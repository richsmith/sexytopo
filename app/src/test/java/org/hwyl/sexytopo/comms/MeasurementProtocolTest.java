package org.hwyl.sexytopo.comms;

import org.hwyl.sexytopo.model.survey.Leg;
import org.junit.Assert;
import org.junit.Test;


public class MeasurementProtocolTest {


    @Test
    public void testFlatMeasurementGetsParsedCorrectly() {
        byte[] packet = new byte[]{1, -31, 7, -94, 50, 58, 3, -5};
        Assert.assertTrue(MeasurementProtocol.isDataPacket(packet));
        Leg actual = MeasurementProtocol.parseDataPacket(packet);
        Leg expected = new Leg(2.017, 71.2, 4.5);
        assertLegEquality(expected, actual);
    }


    @Test
    public void testDownMeasurementGetsParsedCorrectly() {
        byte[] packet = new byte[]{1, 84, 3, 113, -87, -83, -54, -13};
        Assert.assertTrue(MeasurementProtocol.isDataPacket(packet));
        Leg actual = MeasurementProtocol.parseDataPacket(packet);
        Leg expected = new Leg(0.852, 238.3, -75.0);
        assertLegEquality(expected, actual);
    }


    public static void assertLegEquality(Leg one, Leg two) {
        Assert.assertEquals(one.getDistance(), two.getDistance(), 0.01);
        Assert.assertEquals(one.getAzimuth(), two.getAzimuth(), 0.1);
        Assert.assertEquals(one.getInclination(), two.getInclination(), 0.1);
    }
}
