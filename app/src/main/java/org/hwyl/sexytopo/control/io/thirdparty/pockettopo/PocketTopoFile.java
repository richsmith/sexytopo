package org.hwyl.sexytopo.control.io.thirdparty.pockettopo;

import org.hwyl.sexytopo.model.sketch.Colour;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;


/**
 * Utility class for reading PocketTopo .top binary file primitives
 * and performing format-specific unit conversions.
 *
 * See docs/PocketTopoFileFormat.txt for the format specification.
 */
public class PocketTopoFile {

    static final int FULL_CIRCLE = 65536; // 2^16
    static final int UNDEFINED_ID = 0x80000000;

    // .NET ticks at the Java epoch (1 Jan 1970)
    // .NET ticks are 100ns units from 1 Jan 0001
    static final long TICKS_AT_EPOCH = 621355968000000000L;
    static final long TICKS_PER_MILLISECOND = 10000L;


    // --- Binary reading methods ---

    public static int readByte(InputStream in) throws IOException {
        int b = in.read();
        if (b < 0) {
            throw new IOException("Unexpected end of stream");
        }
        return b;
    }

    public static short readInt16(InputStream in) throws IOException {
        int b0 = readByte(in);
        int b1 = readByte(in);
        return (short) (b0 | (b1 << 8));
    }

    public static int readInt32(InputStream in) throws IOException {
        int b0 = readByte(in);
        int b1 = readByte(in);
        int b2 = readByte(in);
        int b3 = readByte(in);
        return b0 | (b1 << 8) | (b2 << 16) | (b3 << 24);
    }

    public static long readInt64(InputStream in) throws IOException {
        long lo = readInt32(in) & 0xFFFFFFFFL;
        long hi = readInt32(in) & 0xFFFFFFFFL;
        return lo | (hi << 32);
    }

    /**
     * Read a .NET binary-encoded string: 7-bit encoded length prefix + UTF-8 content.
     * Length is encoded in 7-bit chunks, little endian, with bit 7 set in all but the last byte.
     */
    public static String readString(InputStream in) throws IOException {
        int length = 0;
        int shift = 0;
        while (true) {
            int b = readByte(in);
            length |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0) {
                break;
            }
            shift += 7;
        }

        if (length == 0) {
            return "";
        }

        byte[] bytes = new byte[length];
        int totalRead = 0;
        while (totalRead < length) {
            int read = in.read(bytes, totalRead, length - totalRead);
            if (read < 0) {
                throw new IOException("Unexpected end of stream reading string");
            }
            totalRead += read;
        }
        return new String(bytes, "UTF-8");
    }


    // --- Station ID handling ---

    /**
     * Read a station ID from the stream and convert to a human-readable name.
     * Returns null for undefined stations (splays).
     */
    public static String readId(InputStream in) throws IOException {
        int value = readInt32(in);
        return idToName(value);
    }

    /**
     * Convert a raw PocketTopo station ID value to a station name string.
     *
     * From the format spec:
     *   0x80000000: undefined (splay)
     *   < 0: plain numbers, station number = value - 0x80000001 (unsigned arithmetic)
     *   >= 0: major.minor format, major = value >> 16, minor = value & 0xFFFF
     */
    static String idToName(int value) {
        if (value == UNDEFINED_ID) {
            return null;
        } else if (value < 0) {
            long plainNumber = (value & 0xFFFFFFFFL) - 0x80000001L;
            return String.valueOf(plainNumber);
        } else {
            int major = (value >> 16) & 0xFFFF;
            int minor = value & 0xFFFF;
            return major + "." + minor;
        }
    }


    // --- Unit conversions ---

    /**
     * Convert PocketTopo internal angle units to degrees for azimuth (bearing).
     * Full circle = 2^16 = 65536. North = 0, East = 0x4000 (90°).
     * Uses unsigned interpretation (0–360°).
     */
    public static float azimuthToDegrees(short rawAngle) {
        int unsigned = rawAngle & 0xFFFF;
        return (unsigned * 360.0f) / FULL_CIRCLE;
    }

    /**
     * Convert PocketTopo internal angle units to degrees for inclination.
     * Full circle = 2^16. Up = 0x4000 (+90°), Down = 0xC000 (-90°).
     * Uses signed interpretation (-180° to +180°).
     */
    public static float inclinationToDegrees(short rawAngle) {
        return (rawAngle * 360.0f) / FULL_CIRCLE;
    }

    /** Convert millimetres to metres. */
    public static float distanceToMetres(int mm) {
        return mm / 1000.0f;
    }

    /** Convert .NET DateTime ticks to Java Date. */
    public static Date ticksToDate(long ticks) {
        long millis = (ticks - TICKS_AT_EPOCH) / TICKS_PER_MILLISECOND;
        return new Date(millis);
    }

    /**
     * Convert PocketTopo colour byte to SexyTopo Colour.
     * PocketTopo: black=1, gray=2, brown=3, blue=4, red=5, green=6, orange=7
     */
    public static Colour topoColourToColour(int colourByte) {
        switch (colourByte) {
            case 1: return Colour.BLACK;
            case 2: return Colour.GREY;
            case 3: return Colour.BROWN;
            case 4: return Colour.BLUE;
            case 5: return Colour.RED;
            case 6: return Colour.GREEN;
            case 7: return Colour.ORANGE;
            default: return Colour.BLACK;
        }
    }
}
