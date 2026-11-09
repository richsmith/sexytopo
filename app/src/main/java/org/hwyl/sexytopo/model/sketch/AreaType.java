package org.hwyl.sexytopo.model.sketch;

import android.view.View;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.hwyl.sexytopo.R;

/**
 * The kind of feature an AreaDetail represents, e.g. a pool of water or a patch of sand. Each type
 * is exported to Therion as an area of the same name.
 *
 * <p>Each type is filled with a repeating pattern, defined once here as a square tile of marks so
 * that the on-screen rendering and the SVG export draw the same thing. Tile measurements are in dp:
 * on screen the pattern stays a fixed size whatever the zoom, and the SVG export converts dp to
 * metres at a fixed ratio (see Fill.METRES_PER_DP).
 */
public enum AreaType {
    WATER(
            R.string.area_type_water,
            R.drawable.area_water,
            "water",
            true,
            new Fill(8, Mark.line(0, 4, 8, 4))),
    // cross-hatched, to read as water filling the passage
    SUMP(
            R.string.area_type_sump,
            R.drawable.area_sump,
            "sump",
            true,
            new Fill(8, Mark.line(0, 4, 8, 4), Mark.line(4, 0, 4, 8))),
    SAND(
            R.string.area_type_sand,
            R.drawable.area_sand,
            "sand",
            false,
            new Fill(
                    16,
                    Mark.dot(2.5f, 3, 0.8f),
                    Mark.dot(9, 1.8f, 0.8f),
                    Mark.dot(13.5f, 6.5f, 0.8f),
                    Mark.dot(6, 8.5f, 0.8f),
                    Mark.dot(11, 12, 0.8f),
                    Mark.dot(2.5f, 13.5f, 0.8f),
                    Mark.dot(14.5f, 14.5f, 0.8f),
                    Mark.dot(7.5f, 14.8f, 0.8f))),
    // short wavy strokes, scattered
    CLAY(
            R.string.area_type_clay,
            R.drawable.area_clay,
            "clay",
            false,
            new Fill(
                    20,
                    Mark.line(1.9f, 6.3f, 3.8f, 4.8f, 5.6f, 6.3f, 7.5f, 4.8f),
                    Mark.line(11.9f, 3.1f, 13.8f, 1.6f, 15.6f, 3.1f, 17.5f, 1.6f),
                    Mark.line(6.9f, 13.1f, 8.8f, 11.6f, 10.6f, 13.1f, 12.5f, 11.6f),
                    Mark.line(14.4f, 18.1f, 16.3f, 16.6f, 18.1f, 18.1f),
                    Mark.line(1.3f, 11, 3.1f, 9.5f, 5, 11))),
    PEBBLES(
            R.string.area_type_pebbles,
            R.drawable.area_pebbles,
            "pebbles",
            false,
            new Fill(
                    24,
                    Mark.ring(5, 6, 2.2f),
                    Mark.ring(15, 4.5f, 1.6f),
                    Mark.ring(19, 13, 2.4f),
                    Mark.ring(8, 15.5f, 1.8f),
                    Mark.ring(15, 20.5f, 1.5f),
                    Mark.ring(3.5f, 21, 1.3f))),
    // small angular fragments
    DEBRIS(
            R.string.area_type_debris,
            R.drawable.area_debris,
            "debris",
            false,
            new Fill(
                    24,
                    Mark.polygon(3, 3, 7.5f, 2, 6, 6.5f),
                    Mark.polygon(13, 4, 18, 5, 17, 8.5f, 12.5f, 7.5f),
                    Mark.polygon(4, 13, 9, 12, 7.5f, 16.5f),
                    Mark.polygon(16.5f, 14, 21.5f, 16, 18, 20),
                    Mark.polygon(8, 19.5f, 12.5f, 20.5f, 10, 23))),
    // larger angular boulders
    BLOCKS(
            R.string.area_type_blocks,
            R.drawable.area_blocks,
            "blocks",
            false,
            new Fill(
                    40,
                    Mark.polygon(3, 5, 15, 2, 18, 12, 6, 16),
                    Mark.polygon(23, 4, 35, 7, 33, 17, 22, 15),
                    Mark.polygon(8, 22, 20, 20, 22, 33, 10, 36, 5, 29),
                    Mark.polygon(27, 23, 37, 25, 35, 36, 26, 33)));

    /**
     * One mark in a fill tile. Lines are open polylines and polygons closed outlines, both given as
     * x, y pairs; dots (filled) and rings (outlined) are circles given as centre x, centre y,
     * radius. Everything apart from dots is stroked.
     */
    public static final class Mark {

        public enum Kind {
            LINE,
            POLYGON,
            DOT,
            RING
        }

        private final Kind kind;
        private final float[] values;

        private Mark(Kind kind, float... values) {
            this.kind = kind;
            this.values = values;
        }

        static Mark line(float... xyPairs) {
            return new Mark(Kind.LINE, xyPairs);
        }

        static Mark polygon(float... xyPairs) {
            return new Mark(Kind.POLYGON, xyPairs);
        }

        static Mark dot(float x, float y, float radius) {
            return new Mark(Kind.DOT, x, y, radius);
        }

        static Mark ring(float x, float y, float radius) {
            return new Mark(Kind.RING, x, y, radius);
        }

        public Kind getKind() {
            return kind;
        }

        public float[] getValues() {
            return values.clone();
        }
    }

    /**
     * A square tile of marks, repeated to fill an area. Marks are kept inside the tile, apart from
     * lines meant to join up with the next tile (such as water's hatching), so the repeats butt
     * together seamlessly.
     */
    public static final class Fill {

        /**
         * Converts tile measurements to survey metres for export. Chosen so that water's hatching
         * comes out roughly as on screen at the default zoom.
         */
        public static final float METRES_PER_DP = 0.15f / 8;

        /** The stroke width of marks, in dp. */
        public static final float STROKE_WIDTH_DP = 1.0f;

        private final float tileSize;
        private final List<Mark> marks;

        Fill(float tileSize, Mark... marks) {
            this.tileSize = tileSize;
            this.marks = Collections.unmodifiableList(Arrays.asList(marks));
        }

        public float getTileSize() {
            return tileSize;
        }

        public List<Mark> getMarks() {
            return marks;
        }
    }

    /** The opacity of the outline of an area with no defined edge, from 0 to 1. */
    public static final float LIGHT_OUTLINE_OPACITY = 0.5f;

    private static final AreaType DEFAULT = WATER;

    private final int stringId;
    private final int drawableId;
    private final String therionName;
    private final boolean isWater;
    private final Fill fill;
    private final int viewId;

    AreaType(int stringId, int drawableId, String therionName, boolean isWater, Fill fill) {
        this.stringId = stringId;
        this.drawableId = drawableId;
        this.therionName = therionName;
        this.isWater = isWater;
        this.fill = fill;
        this.viewId = View.generateViewId();
    }

    public static AreaType fromString(String name) {
        return name == null ? DEFAULT : AreaType.valueOf(name);
    }

    public int getStringId() {
        return stringId;
    }

    public int getDrawableId() {
        return drawableId;
    }

    public int getButtonViewId() {
        return viewId;
    }

    public String getTherionName() {
        return therionName;
    }

    public boolean isWater() {
        return isWater;
    }

    /**
     * Whether the area's edge is a real feature, drawn at full line weight: the shoreline of water.
     * Other areas are deposits with no sharp edge, so their outline is drawn lightly (thin, and
     * faded to LIGHT_OUTLINE_OPACITY), to show their extent without competing with walls and other
     * lines.
     */
    public boolean hasDefinedEdge() {
        return isWater;
    }

    public Fill getFill() {
        return fill;
    }
}
