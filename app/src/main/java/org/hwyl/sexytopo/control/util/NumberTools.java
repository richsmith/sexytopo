package org.hwyl.sexytopo.control.util;

import org.hwyl.sexytopo.SexyTopo;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;


public class NumberTools {

    public static boolean isWithinDelta(double first, double second) {
        double diff = Math.abs(first - second);
        return diff < SexyTopo.ALLOWED_DOUBLE_DELTA;
    }

    public static int getUint8(byte[] bytes, int index) {
        return bytes[index] & 0xFF;
    }

    public static int getUint16(byte[] bytes, int offset) {
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getShort(offset);
    }

    public static int getUint32(byte[] bytes, int offset) {
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt(offset);
    }

    public static float getFloat(byte[] bytes, int offset) {
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getFloat(offset);
    }
}
