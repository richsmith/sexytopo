package org.hwyl.sexytopo.comms;

import org.hwyl.sexytopo.comms.cavwayx1.CavwayX1Protocol;
import org.hwyl.sexytopo.model.survey.Leg;
import org.junit.Assert;
import org.junit.Test;

public class CavwayX1ProtocolTest {

    @Test
    public void testUpMeasurementGetsParsedCorrectly() {
        // 5.462m, 286.94°, 3.21°
        byte[] packet = createPacket(5462, 0xCC0B, 0x0248);
        Leg actual = CavwayX1Protocol.parseMeasurementPacket(packet);
        Leg expected = new Leg(5.462f, 286.94f, 3.21f);
        MeasurementProtocolTest.assertLegEquality(expected, actual);
    }

    @Test
    public void testDownMeasurementGetsParsedAsNegative() {
        // 8.601m, 325.37°, 339.28° on the device's 0-360 circle
        byte[] packet = createPacket(8601, 0xE75F, 0xF143);
        Leg actual = CavwayX1Protocol.parseMeasurementPacket(packet);
        Leg expected = new Leg(8.601f, 325.37f, -20.72f);
        MeasurementProtocolTest.assertLegEquality(expected, actual);
    }

    @Test
    public void testVerticalMeasurementsGetParsedCorrectly() {
        Leg up = CavwayX1Protocol.parseMeasurementPacket(createPacket(1000, 0, 0x4000));
        Assert.assertEquals(90.0f, up.getInclination(), 0.01);

        Leg down = CavwayX1Protocol.parseMeasurementPacket(createPacket(1000, 0, 0xBFFF));
        Assert.assertEquals(-90.0f, down.getInclination(), 0.01);
    }

    @Test
    public void testFullTurnAzimuthGetsParsedAsNorth() {
        Leg leg = CavwayX1Protocol.parseMeasurementPacket(createPacket(1000, 0xFFFF, 0));
        Assert.assertEquals(0.0f, leg.getAzimuth(), 0.01);
    }

    private static byte[] createPacket(int millimetres, int rawAzimuth, int rawInclination) {
        byte[] packet = new byte[64];
        packet[0] = 0x01;
        packet[2] = (byte) (millimetres >> 16);
        packet[3] = (byte) millimetres;
        packet[4] = (byte) (millimetres >> 8);
        packet[5] = (byte) rawAzimuth;
        packet[6] = (byte) (rawAzimuth >> 8);
        packet[7] = (byte) rawInclination;
        packet[8] = (byte) (rawInclination >> 8);
        return packet;
    }
}
