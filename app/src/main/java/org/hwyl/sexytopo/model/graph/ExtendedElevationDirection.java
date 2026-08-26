package org.hwyl.sexytopo.model.graph;

public enum ExtendedElevationDirection {
    LEFT(true),
    RIGHT(true),
    VERTICAL(false);

    /** The direction a station extends in until something says otherwise. */
    public static final ExtendedElevationDirection DEFAULT = RIGHT;

    private final boolean propagates;

    ExtendedElevationDirection(boolean propagates) {
        this.propagates = propagates;
    }

    /**
     * Whether setting this direction on a station also applies it to everything below that station.
     *
     * <p>Left and right describe which way the survey continues, so they carry down the subtree
     * until something changes them. Vertical describes one leg only: it says to draw that leg using
     * just its height change, and the survey resumes its previous direction afterwards.
     */
    public boolean propagates() {
        return propagates;
    }
}
