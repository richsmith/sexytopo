package org.hwyl.sexytopo.comms;

import org.hwyl.sexytopo.comms.distox.CalibrationProtocol;
import org.junit.Assert;
import org.junit.Test;


public class CalibrationProtocolTest {

    private final byte IGNORE = 0b0;
    private final byte[] TEST_ACCELERATION_PACKET = new byte[] {IGNORE, -102, -1,  86, -3, -52, 96, 11};
    private final byte[] TEST_MAGNETIC_PACKET =     new byte[] {IGNORE,   48, 31, -43, -7, -56, 62, 1};

    // Doesn't need to be exact; data input taken from Disto;
    // similar reading read via PocketTopo for check
    private static final double ALLOWED_DELTA = 80;


    @Test
    public void testReadDoubleByteGx() {
        double result = CalibrationProtocol.readDoubleByte(
                TEST_ACCELERATION_PACKET,
                CalibrationProtocol.ACCELERATION_GX_LOW_BYTE,
                CalibrationProtocol.ACCELERATION_GX_HIGH_BYTE);
        Assert.assertEquals(-79, result, ALLOWED_DELTA);
    }


    @Test
    public void testReadDoubleByteGy() {
        double result = CalibrationProtocol.readDoubleByte(
                TEST_ACCELERATION_PACKET,
                CalibrationProtocol.ACCELERATION_GY_LOW_BYTE,
                CalibrationProtocol.ACCELERATION_GY_HIGH_BYTE);
        Assert.assertEquals(-603, result, ALLOWED_DELTA);

    }


    @Test
    public void testReadDoubleByteGz() {
        double result = CalibrationProtocol.readDoubleByte(
                TEST_ACCELERATION_PACKET,
                CalibrationProtocol.ACCELERATION_GZ_LOW_BYTE,
                CalibrationProtocol.ACCELERATION_GZ_HIGH_BYTE);
        Assert.assertEquals(24785, result, ALLOWED_DELTA);
    }


    @Test
    public void testReadDoubleByteMx() {
        double result = CalibrationProtocol.readDoubleByte(
                TEST_MAGNETIC_PACKET,
                CalibrationProtocol.MAGNETIC_MX_LOW_BYTE,
                CalibrationProtocol.MAGNETIC_MX_HIGH_BYTE);
        Assert.assertEquals(7978, result, ALLOWED_DELTA);
    }


    @Test
    public void testReadDoubleByteMy() {
        double result = CalibrationProtocol.readDoubleByte(
                TEST_MAGNETIC_PACKET,
                CalibrationProtocol.MAGNETIC_MY_LOW_BYTE,
                CalibrationProtocol.MAGNETIC_MY_HIGH_BYTE);
        Assert.assertEquals(-1607, result, ALLOWED_DELTA);
    }


    @Test
    public void testReadDoubleByteMz() {
        double result = CalibrationProtocol.readDoubleByte(
                TEST_MAGNETIC_PACKET,
                CalibrationProtocol.MAGNETIC_MZ_LOW_BYTE,
                CalibrationProtocol.MAGNETIC_MZ_HIGH_BYTE);
        Assert.assertEquals(16090, result, ALLOWED_DELTA);
    }

}


