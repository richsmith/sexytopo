package org.hwyl.sexytopo.control.calibration;

import org.hwyl.sexytopo.R;

/**
 * Which way the instrument was pointing for a reading, worked out from its gravity and magnetic
 * vectors. The laser is the sensors' x axis; roll is about that axis.
 */
public class ReadingDirection {

    public final double inclination; // degrees, up is positive
    public final double azimuth; // degrees from magnetic north, 0 to 360
    public final double roll; // degrees

    private ReadingDirection(double inclination, double azimuth, double roll) {
        this.inclination = inclination;
        this.azimuth = azimuth;
        this.roll = roll;
    }

    public static ReadingDirection fromVectors(Vector g, Vector m) {
        Vector down = Vector.Normalized(g);
        Vector east = Vector.Normalized(down.crossProduct(m));
        Vector north = east.crossProduct(down);
        double inclination = Math.toDegrees(Math.asin(-down.x));
        double azimuth = (Math.toDegrees(Math.atan2(east.x, north.x)) + 360) % 360;
        double roll = Math.toDegrees(Math.atan2(g.y, g.z));
        return new ReadingDirection(inclination, azimuth, roll);
    }

    /**
     * Roughly where the laser was pointing, from its inclination: level, straight up or down, or
     * part way between (the corners of a calibration).
     */
    public enum Pointing {
        HORIZONTAL(R.string.calibration_pointing_horizontal),
        UP(R.string.direction_up),
        DOWN(R.string.direction_down),
        CORNER_UP(R.string.calibration_pointing_corner_up),
        CORNER_DOWN(R.string.calibration_pointing_corner_down);

        public final int stringId;

        Pointing(int stringId) {
            this.stringId = stringId;
        }

        /** Straight up or down, where roll can't be told apart from which way it faces. */
        public boolean isVertical() {
            return this == UP || this == DOWN;
        }
    }

    /** Which way up the instrument was, from its roll about the laser. */
    public enum Face {
        UP(R.string.orientation_face_up),
        DOWN(R.string.orientation_face_down),
        SIDE(R.string.calibration_face_side);

        public final int stringId;

        Face(int stringId) {
            this.stringId = stringId;
        }
    }

    private static final double HORIZONTAL_LIMIT = 20; // degrees either side of level
    private static final double VERTICAL_LIMIT = 65; // degrees from level

    /** A rough description of where the laser was pointing, enough to tell positions apart. */
    public Pointing getPointing() {
        return getPointing(inclination);
    }

    public static Pointing getPointing(double inclination) {
        double steepness = Math.abs(inclination);
        if (steepness < HORIZONTAL_LIMIT) {
            return Pointing.HORIZONTAL;
        } else if (steepness > VERTICAL_LIMIT) {
            return inclination > 0 ? Pointing.UP : Pointing.DOWN;
        } else {
            return inclination > 0 ? Pointing.CORNER_UP : Pointing.CORNER_DOWN;
        }
    }

    /** Which way up the instrument was. Meaningless when pointing straight up or down. */
    public Face getFace() {
        double absoluteRoll = Math.abs(roll);
        if (absoluteRoll <= 45) {
            return Face.UP;
        } else if (absoluteRoll >= 135) {
            return Face.DOWN;
        } else {
            return Face.SIDE;
        }
    }

    /** The laser as a unit vector in north, east, down coordinates. */
    public Vector toVector() {
        double inc = Math.toRadians(inclination);
        double az = Math.toRadians(azimuth);
        return new Vector(
                (float) (Math.cos(inc) * Math.cos(az)),
                (float) (Math.cos(inc) * Math.sin(az)),
                (float) -Math.sin(inc));
    }
}
