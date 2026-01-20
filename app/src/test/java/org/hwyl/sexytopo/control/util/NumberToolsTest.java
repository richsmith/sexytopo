package org.hwyl.sexytopo.control.util;

import org.junit.Assert;
import org.junit.Test;

public class NumberToolsTest {

    private static final double DELTA = 0.0001;

    @Test
    public void testIsWithinDeltaReturnsTrueForSimilarValues() {
        Assert.assertTrue(NumberTools.isWithinDelta(1.0, 1.00005));
        Assert.assertTrue(NumberTools.isWithinDelta(0.0, 0.00005));
    }

    @Test
    public void testIsWithinDeltaReturnsFalseForDifferentValues() {
        Assert.assertFalse(NumberTools.isWithinDelta(1.0, 1.5));
        Assert.assertFalse(NumberTools.isWithinDelta(0.0, 0.01));
    }

    @Test
    public void testGetUint8ReadsZero() {
        byte[] bytes = {0x00};
        Assert.assertEquals(0, NumberTools.getUint8(bytes, 0));
    }

    @Test
    public void testGetUint8ReadsMaxValue() {
        byte[] bytes = {(byte) 0xFF};
        Assert.assertEquals(255, NumberTools.getUint8(bytes, 0));
    }

    @Test
    public void testGetUint8ReadsPositiveValue() {
        byte[] bytes = {0x7F};
        Assert.assertEquals(127, NumberTools.getUint8(bytes, 0));
    }

    @Test
    public void testGetUint8HandlesNegativeBytesAsUnsigned() {
        byte[] bytes = {(byte) 0x80};
        Assert.assertEquals(128, NumberTools.getUint8(bytes, 0));
    }

    @Test
    public void testGetUint8AtDifferentOffsets() {
        byte[] bytes = {0x10, 0x20, 0x30};
        Assert.assertEquals(0x10, NumberTools.getUint8(bytes, 0));
        Assert.assertEquals(0x20, NumberTools.getUint8(bytes, 1));
        Assert.assertEquals(0x30, NumberTools.getUint8(bytes, 2));
    }

    @Test
    public void testGetUint16LittleEndianZero() {
        byte[] bytes = {0x00, 0x00};
        Assert.assertEquals(0, NumberTools.getUint16(bytes, 0));
    }

    @Test
    public void testGetUint16LittleEndianSimpleValue() {
        byte[] bytes = {0x01, 0x00};
        Assert.assertEquals(1, NumberTools.getUint16(bytes, 0));
    }

    @Test
    public void testGetUint16LittleEndianByteOrder() {
        byte[] bytes = {0x34, 0x12};
        Assert.assertEquals(0x1234, NumberTools.getUint16(bytes, 0));
    }

    @Test
    public void testGetUint16LittleEndianMaxValue() {
        byte[] bytes = {(byte) 0xFF, (byte) 0xFF};
        Assert.assertEquals(-1, NumberTools.getUint16(bytes, 0));
    }

    @Test
    public void testGetUint16AtOffset() {
        byte[] bytes = {0x00, 0x00, 0x34, 0x12};
        Assert.assertEquals(0x1234, NumberTools.getUint16(bytes, 2));
    }

    @Test
    public void testGetUint32LittleEndianZero() {
        byte[] bytes = {0x00, 0x00, 0x00, 0x00};
        Assert.assertEquals(0, NumberTools.getUint32(bytes, 0));
    }

    @Test
    public void testGetUint32LittleEndianSimpleValue() {
        byte[] bytes = {0x01, 0x00, 0x00, 0x00};
        Assert.assertEquals(1, NumberTools.getUint32(bytes, 0));
    }

    @Test
    public void testGetUint32LittleEndianByteOrder() {
        byte[] bytes = {0x78, 0x56, 0x34, 0x12};
        Assert.assertEquals(0x12345678, NumberTools.getUint32(bytes, 0));
    }

    @Test
    public void testGetUint32LittleEndianMaxValue() {
        byte[] bytes = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
        Assert.assertEquals(-1, NumberTools.getUint32(bytes, 0));
    }

    @Test
    public void testGetUint32AtOffset() {
        byte[] bytes = {0x00, 0x00, 0x78, 0x56, 0x34, 0x12};
        Assert.assertEquals(0x12345678, NumberTools.getUint32(bytes, 2));
    }

    @Test
    public void testGetFloatLittleEndianZero() {
        byte[] bytes = {0x00, 0x00, 0x00, 0x00};
        Assert.assertEquals(0.0f, NumberTools.getFloat(bytes, 0), DELTA);
    }

    @Test
    public void testGetFloatLittleEndianOne() {
        byte[] bytes = {0x00, 0x00, (byte) 0x80, 0x3F};
        Assert.assertEquals(1.0f, NumberTools.getFloat(bytes, 0), DELTA);
    }

    @Test
    public void testGetFloatLittleEndianNegativeValue() {
        byte[] bytes = {0x00, 0x00, (byte) 0x80, (byte) 0xBF};
        Assert.assertEquals(-1.0f, NumberTools.getFloat(bytes, 0), DELTA);
    }

    @Test
    public void testGetFloatLittleEndianArbitraryValue() {
        byte[] bytes = {(byte) 0xDB, 0x0F, 0x49, 0x40};
        Assert.assertEquals(3.14159f, NumberTools.getFloat(bytes, 0), 0.00001);
    }

    @Test
    public void testGetFloatAtOffset() {
        byte[] bytes = {0x00, 0x00, 0x00, 0x00, (byte) 0x80, 0x3F};
        Assert.assertEquals(1.0f, NumberTools.getFloat(bytes, 2), DELTA);
    }

    @Test
    public void testGetFloatHandlesNaN() {
        byte[] bytes = {0x00, 0x00, (byte) 0xC0, 0x7F};
        Assert.assertTrue(Float.isNaN(NumberTools.getFloat(bytes, 0)));
    }

    @Test
    public void testGetFloatHandlesPositiveInfinity() {
        byte[] bytes = {0x00, 0x00, (byte) 0x80, 0x7F};
        Assert.assertTrue(Float.isInfinite(NumberTools.getFloat(bytes, 0)));
        Assert.assertTrue(NumberTools.getFloat(bytes, 0) > 0);
    }

    @Test
    public void testGetFloatHandlesNegativeInfinity() {
        byte[] bytes = {0x00, 0x00, (byte) 0x80, (byte) 0xFF};
        Assert.assertTrue(Float.isInfinite(NumberTools.getFloat(bytes, 0)));
        Assert.assertTrue(NumberTools.getFloat(bytes, 0) < 0);
    }
}
