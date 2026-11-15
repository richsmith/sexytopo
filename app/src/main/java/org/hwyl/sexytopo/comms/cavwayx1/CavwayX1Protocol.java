package org.hwyl.sexytopo.comms.cavwayx1;

import org.hwyl.sexytopo.model.survey.Leg;

/** Decodes Cavway X1 measurement packets. */
public class CavwayX1Protocol {

    // The protocol maps every angle onto 0x0000 - 0xFFFF for 0 - 360 degrees. This is the scale
    // the manufacturer's own CavwayAssistant uses, so we match it.
    private static final float ANGLE_SCALE = 360.0f / 65535.0f; // 360.0 / 0xFFFF

    public static Leg parseMeasurementPacket(byte[] packet) {
        // According to the protocol document
        // Byte 2: distance (16 - 23 bit)
        // Byte 3: distance (0 – 7 bit)
        // Byte 4: distance (8 – 15 bit)
        int dist_p3 = (packet[3] & 0xFF);
        int dist_p4 = (packet[4] & 0xFF) << 8;
        int dist_p2 = (packet[2] & 0xFF) << 16;
        float distance = (dist_p2 | dist_p4 | dist_p3) / 1000.0f;

        // Byte 5-6: Azimuth
        // Byte 7-8: Inclination
        float azimuth = parseAngle(packet, 5) % 360; // 0xFFFF is a full turn, i.e. north
        float inclination = toInclination(parseAngle(packet, 7));

        return new Leg(distance, azimuth, inclination);
    }

    private static float parseAngle(byte[] data, int startIndex) {
        int lowByte = data[startIndex] & 0xFF;
        int highByte = data[startIndex + 1] & 0xFF;
        int combined = (highByte << 8) | lowByte;
        return combined * ANGLE_SCALE;
    }

    /**
     * The inclination comes round the same 0 - 360 circle as the azimuth, so a shot 20 degrees
     * downwards arrives as 340. Anything past half way round is below the horizontal.
     *
     * <p>The 0xFFFF scale overshoots by a hair, so a vertical shot can come out a fraction past 90;
     * it gets clamped back.
     */
    private static float toInclination(float angle) {
        float inclination = angle > 180 ? angle - 360 : angle;
        return Math.max(Leg.MIN_INCLINATION, Math.min(Leg.MAX_INCLINATION, inclination));
    }
}
