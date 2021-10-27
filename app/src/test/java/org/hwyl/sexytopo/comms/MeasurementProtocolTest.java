package org.hwyl.sexytopo.comms;

import org.hwyl.sexytopo.comms.distox.MeasurementProtocol;
import org.hwyl.sexytopo.model.survey.Leg;
import org.junit.Assert;
import org.junit.Test;


public class MeasurementProtocolTest {


    @Test
    public void testFlatMeasurementGetsParsedCorrectly() {
        byte[] packet = new byte[]{1, -31, 7, -94, 50, 58, 3, -5};
        Assert.assertTrue(MeasurementProtocol.isDataPacket(packet));
        Leg actual = MeasurementProtocol.parseDataPacket(packet);
        Leg expected = new Leg(2.017f, 71.2f, 4.5f);
        assertLegEquality(expected, actual);
    }


    @Test
    public void testDownMeasurementGetsParsedCorrectly() {
        byte[] packet = new byte[]{1, 84, 3, 113, -87, -83, -54, -13};
        Assert.assertTrue(MeasurementProtocol.isDataPacket(packet));
        Leg actual = MeasurementProtocol.parseDataPacket(packet);
        Leg expected = new Leg(0.852f, 238.3f, -75.0f);
        assertLegEquality(expected, actual);
    }


    public static void assertLegEquality(Leg one, Leg two) {
        Assert.assertEquals(one.getDistance(), two.getDistance(), 0.01);
        Assert.assertEquals(one.getAzimuth(), two.getAzimuth(), 0.1);
        Assert.assertEquals(one.getInclination(), two.getInclination(), 0.1);
    }
}
