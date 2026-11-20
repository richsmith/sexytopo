package org.hwyl.sexytopo.control.sketch;

/**
 * How near a finger has to be to count, and how small a drawn detail can be before it's discarded.
 * Each is a fixed size on screen, converted to metres at the current zoom, so it follows what the
 * user can actually see rather than being a fixed size in the cave.
 */
final class TouchTolerances {

    private static final float ERASE_RADIUS_DP = 10.0f;
    // typed lines spanning less than this on screen, and areas smaller than a square this size,
    // are discarded as specks
    private static final float MINIMUM_DETAIL_SIZE_DP = 10.0f;
    private static final float SELECTION_RADIUS_DP = 25.0f;
    private static final float SNAP_DISTANCE_DP = 20.0f;

    private TouchTolerances() {}

    /** How far from the finger the eraser reaches, in metres. */
    static float getEraseRadius(Viewport viewport) {
        return viewport.dpToSurvey(ERASE_RADIUS_DP);
    }

    /** How far from the finger a station or line can be and still be picked, in metres. */
    static float getSelectionRadius(Viewport viewport) {
        return viewport.dpToSurvey(SELECTION_RADIUS_DP);
    }

    /** How close line ends must be for Snap to Lines to join them, in metres. */
    static float getSnapDistance(Viewport viewport) {
        return viewport.dpToSurvey(SNAP_DISTANCE_DP);
    }

    /**
     * The smallest extent (in metres) a typed line must span to be kept, so a stray tap doesn't
     * leave a speck.
     */
    static float getMinimumLineSize(Viewport viewport) {
        return viewport.dpToSurvey(MINIMUM_DETAIL_SIZE_DP);
    }

    /** The smallest area (in square metres) worth keeping: a square of the minimum line size. */
    static double getMinimumArea(Viewport viewport) {
        double sizeInMetres = getMinimumLineSize(viewport);
        return sizeInMetres * sizeInMetres;
    }
}
