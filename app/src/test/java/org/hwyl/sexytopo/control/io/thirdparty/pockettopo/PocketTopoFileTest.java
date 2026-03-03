package org.hwyl.sexytopo.control.io.thirdparty.pockettopo;

import org.hwyl.sexytopo.model.sketch.Colour;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;


public class PocketTopoFileTest {

    // --- readByte ---

    @Test
    public void testReadByte() throws IOException {
        InputStream in = new ByteArrayInputStream(new byte[]{(byte) 0xAB});
        Assert.assertEquals(0xAB, PocketTopoFile.readByte(in));
    }

    @Test(expected = IOException.class)
    public void testReadByteThrowsOnEmpty() throws IOException {
        InputStream in = new ByteArrayInputStream(new byte[]{});
        PocketTopoFile.readByte(in);
    }

    // --- readInt16 ---

    @Test
    public void testReadInt16Zero() throws IOException {
        InputStream in = new ByteArrayInputStream(new byte[]{0x00, 0x00});
        Assert.assertEquals(0, PocketTopoFile.readInt16(in));
    }

    @Test
    public void testReadInt16LittleEndian() throws IOException {
        // 0x0102 little-endian = 02 01
        InputStream in = new ByteArrayInputStream(new byte[]{0x02, 0x01});
        Assert.assertEquals(0x0102, PocketTopoFile.readInt16(in));
    }

    @Test
    public void testReadInt16Negative() throws IOException {
        // -1 = 0xFFFF little-endian = FF FF
        InputStream in = new ByteArrayInputStream(new byte[]{(byte) 0xFF, (byte) 0xFF});
        Assert.assertEquals(-1, PocketTopoFile.readInt16(in));
    }

    // --- readInt32 ---

    @Test
    public void testReadInt32Zero() throws IOException {
        InputStream in = new ByteArrayInputStream(new byte[]{0x00, 0x00, 0x00, 0x00});
        Assert.assertEquals(0, PocketTopoFile.readInt32(in));
    }

    @Test
    public void testReadInt32LittleEndian() throws IOException {
        // 0x01020304 little-endian = 04 03 02 01
        InputStream in = new ByteArrayInputStream(new byte[]{0x04, 0x03, 0x02, 0x01});
        Assert.assertEquals(0x01020304, PocketTopoFile.readInt32(in));
    }

    @Test
    public void testReadInt32Negative() throws IOException {
        // -1 = 0xFFFFFFFF
        InputStream in = new ByteArrayInputStream(new byte[]{
                (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF});
        Assert.assertEquals(-1, PocketTopoFile.readInt32(in));
    }

    // --- readInt64 ---

    @Test
    public void testReadInt64Zero() throws IOException {
        InputStream in = new ByteArrayInputStream(new byte[]{
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00});
        Assert.assertEquals(0L, PocketTopoFile.readInt64(in));
    }

    @Test
    public void testReadInt64LittleEndian() throws IOException {
        // 0x0000000100000002 little-endian = 02 00 00 00 01 00 00 00
        InputStream in = new ByteArrayInputStream(new byte[]{
                0x02, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00});
        Assert.assertEquals(0x0000000100000002L, PocketTopoFile.readInt64(in));
    }

    // --- readString ---

    @Test
    public void testReadStringEmpty() throws IOException {
        InputStream in = new ByteArrayInputStream(new byte[]{0x00});
        Assert.assertEquals("", PocketTopoFile.readString(in));
    }

    @Test
    public void testReadStringShort() throws IOException {
        // length=3, "abc"
        InputStream in = new ByteArrayInputStream(new byte[]{0x03, 0x61, 0x62, 0x63});
        Assert.assertEquals("abc", PocketTopoFile.readString(in));
    }

    @Test
    public void testReadStringLengthMultiByteEncoding() throws IOException {
        // Length 128 requires two bytes: 0x80 0x01 (128 in 7-bit encoding)
        byte[] data = new byte[2 + 128];
        data[0] = (byte) 0x80;
        data[1] = 0x01;
        for (int i = 0; i < 128; i++) {
            data[2 + i] = 0x41; // 'A'
        }
        InputStream in = new ByteArrayInputStream(data);
        String result = PocketTopoFile.readString(in);
        Assert.assertEquals(128, result.length());
        for (char c : result.toCharArray()) {
            Assert.assertEquals('A', c);
        }
    }

    // --- idToName ---

    @Test
    public void testIdToNameUndefined() {
        Assert.assertNull(PocketTopoFile.idToName(0x80000000));
    }

    @Test
    public void testIdToNamePlainZero() {
        // Station 0: value = 0x80000001
        Assert.assertEquals("0", PocketTopoFile.idToName((int) 0x80000001L));
    }

    @Test
    public void testIdToNamePlainOne() {
        // Station 1: value = 0x80000002
        Assert.assertEquals("1", PocketTopoFile.idToName((int) 0x80000002L));
    }

    @Test
    public void testIdToNamePlainTen() {
        // Station 10: value = 0x8000000B
        Assert.assertEquals("10", PocketTopoFile.idToName((int) 0x8000000BL));
    }

    @Test
    public void testIdToNameMajorMinorZeroZero() {
        // 0.0 -> value = 0
        Assert.assertEquals("0.0", PocketTopoFile.idToName(0x00000000));
    }

    @Test
    public void testIdToNameMajorMinorOneZero() {
        // 1.0 -> value = 1<<16 = 0x00010000
        Assert.assertEquals("1.0", PocketTopoFile.idToName(0x00010000));
    }

    @Test
    public void testIdToNameMajorMinorOneTwo() {
        // 1.2 -> value = (1<<16) | 2 = 0x00010002
        Assert.assertEquals("1.2", PocketTopoFile.idToName(0x00010002));
    }

    // --- azimuthToDegrees ---

    @Test
    public void testAzimuthNorth() {
        Assert.assertEquals(0.0f, PocketTopoFile.azimuthToDegrees((short) 0), 0.01f);
    }

    @Test
    public void testAzimuthEast() {
        // East = 0x4000 = 16384
        Assert.assertEquals(90.0f, PocketTopoFile.azimuthToDegrees((short) 0x4000), 0.01f);
    }

    @Test
    public void testAzimuthSouth() {
        // South = 0x8000 = -32768 as signed short
        Assert.assertEquals(180.0f, PocketTopoFile.azimuthToDegrees((short) 0x8000), 0.01f);
    }

    @Test
    public void testAzimuthWest() {
        // West = 0xC000 = -16384 as signed short
        Assert.assertEquals(270.0f, PocketTopoFile.azimuthToDegrees((short) 0xC000), 0.01f);
    }

    // --- inclinationToDegrees ---

    @Test
    public void testInclinationLevel() {
        Assert.assertEquals(0.0f, PocketTopoFile.inclinationToDegrees((short) 0), 0.01f);
    }

    @Test
    public void testInclinationUp() {
        // Up = 0x4000 = 16384 = +90°
        Assert.assertEquals(90.0f, PocketTopoFile.inclinationToDegrees((short) 0x4000), 0.01f);
    }

    @Test
    public void testInclinationDown() {
        // Down = 0xC000 = -16384 as signed = -90°
        Assert.assertEquals(-90.0f, PocketTopoFile.inclinationToDegrees((short) 0xC000), 0.01f);
    }

    // --- distanceToMetres ---

    @Test
    public void testDistanceToMetresZero() {
        Assert.assertEquals(0.0f, PocketTopoFile.distanceToMetres(0), 0.001f);
    }

    @Test
    public void testDistanceToMetresOneThousand() {
        Assert.assertEquals(1.0f, PocketTopoFile.distanceToMetres(1000), 0.001f);
    }

    @Test
    public void testDistanceToMetres3500() {
        Assert.assertEquals(3.5f, PocketTopoFile.distanceToMetres(3500), 0.001f);
    }

    // --- ticksToDate ---

    @Test
    public void testTicksToDateEpoch() {
        // Java epoch (1 Jan 1970) in .NET ticks
        Date date = PocketTopoFile.ticksToDate(PocketTopoFile.TICKS_AT_EPOCH);
        Assert.assertEquals(0L, date.getTime());
    }

    @Test
    public void testTicksToDateKnownDate() {
        // Compute ticks for 1 July 2005 00:00:00 UTC via reverse calculation
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.set(2005, Calendar.JULY, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long expectedMillis = cal.getTimeInMillis();
        long ticks = PocketTopoFile.TICKS_AT_EPOCH
                + (expectedMillis * PocketTopoFile.TICKS_PER_MILLISECOND);

        Date date = PocketTopoFile.ticksToDate(ticks);
        Calendar result = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        result.setTime(date);
        Assert.assertEquals(2005, result.get(Calendar.YEAR));
        Assert.assertEquals(Calendar.JULY, result.get(Calendar.MONTH));
        Assert.assertEquals(1, result.get(Calendar.DAY_OF_MONTH));
    }

    // --- topoColourToColour ---

    @Test
    public void testColourBlack() {
        Assert.assertEquals(Colour.BLACK, PocketTopoFile.topoColourToColour(1));
    }

    @Test
    public void testColourGrey() {
        Assert.assertEquals(Colour.GREY, PocketTopoFile.topoColourToColour(2));
    }

    @Test
    public void testColourBrown() {
        Assert.assertEquals(Colour.BROWN, PocketTopoFile.topoColourToColour(3));
    }

    @Test
    public void testColourBlue() {
        Assert.assertEquals(Colour.BLUE, PocketTopoFile.topoColourToColour(4));
    }

    @Test
    public void testColourRed() {
        Assert.assertEquals(Colour.RED, PocketTopoFile.topoColourToColour(5));
    }

    @Test
    public void testColourGreen() {
        Assert.assertEquals(Colour.GREEN, PocketTopoFile.topoColourToColour(6));
    }

    @Test
    public void testColourOrange() {
        Assert.assertEquals(Colour.ORANGE, PocketTopoFile.topoColourToColour(7));
    }

    @Test
    public void testColourUnknownDefaultsToBlack() {
        Assert.assertEquals(Colour.BLACK, PocketTopoFile.topoColourToColour(99));
    }

    // --- readId from stream ---

    @Test
    public void testReadIdUndefined() throws IOException {
        // 0x80000000 little-endian = 00 00 00 80
        InputStream in = new ByteArrayInputStream(new byte[]{0x00, 0x00, 0x00, (byte) 0x80});
        Assert.assertNull(PocketTopoFile.readId(in));
    }

    @Test
    public void testReadIdMajorMinor() throws IOException {
        // 1.0 = 0x00010000 little-endian = 00 00 01 00
        InputStream in = new ByteArrayInputStream(new byte[]{0x00, 0x00, 0x01, 0x00});
        Assert.assertEquals("1.0", PocketTopoFile.readId(in));
    }

    @Test
    public void testReadIdPlainNumber() throws IOException {
        // Station 0 = 0x80000001 little-endian = 01 00 00 80
        InputStream in = new ByteArrayInputStream(new byte[]{0x01, 0x00, 0x00, (byte) 0x80});
        Assert.assertEquals("0", PocketTopoFile.readId(in));
    }
}
