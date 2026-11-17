package org.hwyl.sexytopo.model.sketch;

import android.graphics.DashPathEffect;
import android.graphics.Path;
import android.graphics.PathDashPathEffect;
import android.graphics.PathEffect;
import android.view.View;
import org.hwyl.sexytopo.R;

/**
 * The kind of cave feature a PathDetail represents. SKETCH is a free-form sketched line (the
 * historical behaviour); the other types carry cartographic meaning and are exported to Therion as
 * first-class line commands.
 *
 * <p>Each type is defined entirely by the values it is constructed with: a stroke width factor, an
 * optional dash pattern, an optional tick stamp, and whether it is a wall. Ornamented types (ticks
 * for pits etc.) are built as a small stamp path repeated along the line via PathDashPathEffect, so
 * rendering them is just an ordinary drawPath call. Where a type needs both dashes and ticks, the
 * gaps are built into the stamp itself, which avoids composing effects.
 *
 * <p>Stamp and dash measurements are multiples of the drawn line's ornament size (see
 * PathDetail.getOrnamentSize), which is a survey-space quantity fixed when the line was drawn. So
 * ornamentation is anchored to the cave: it stays put over the rock and scales with everything else
 * as the view is zoomed, rather than crawling along the line.
 *
 * <p>Directional note: tick stamps are drawn on one side of the line, determined by the direction
 * the user drew the path in. Wall types are instead auto-oriented on completion (see LineOrienter).
 */
public enum LineType {
    SKETCH(
            R.string.line_type_sketch,
            R.drawable.pencil,
            null,
            false,
            false,
            false,
            1.0f,
            null,
            null),
    WALL(
            R.string.line_type_wall,
            R.drawable.line_wall,
            "wall",
            true,
            false,
            false,
            1.5f,
            null,
            null),
    // a drop too small to be a pit: as PIT, with shorter ticks (Therion draws the two alike, but
    // they need telling apart while sketching)
    FLOOR_STEP(
            R.string.line_type_floor_step,
            R.drawable.line_floor_step,
            "floor-step",
            false,
            false,
            false,
            1.0f,
            null,
            Stamp.ticked(1.2f, 1.2f, -0.35f)),
    // solid baseline with a perpendicular tick, repeating every ornament size
    PIT(
            R.string.line_type_pit,
            R.drawable.line_pit,
            "pit",
            false,
            true,
            false,
            1.0f,
            null,
            Stamp.ticked(1.2f, 1.2f, -0.6f)),
    // dashed baseline (the bar covers two thirds of the advance) with a tick on the opposite
    // side to PIT
    CHIMNEY(
            R.string.line_type_chimney,
            R.drawable.line_chimney,
            "chimney",
            false,
            true,
            false,
            1.0f,
            new float[] {0.8f, 0.4f},
            Stamp.ticked(1.2f, 0.8f, 0.5f)),
    // the ceiling's counterpart to FLOOR_STEP: as CHIMNEY, with shorter ticks
    CEILING_STEP(
            R.string.line_type_ceiling_step,
            R.drawable.line_ceiling_step,
            "ceiling-step",
            false,
            false,
            false,
            1.0f,
            new float[] {0.8f, 0.4f},
            Stamp.ticked(1.2f, 0.8f, 0.3f)),
    // solid baseline with a row of filled teeth on the same side as PIT's ticks
    OVERHANG(
            R.string.line_type_overhang,
            R.drawable.line_overhang,
            "overhang",
            false,
            false,
            false,
            1.0f,
            null,
            Stamp.toothed(1.2f, -0.9f)),
    // hachures alternating long and short, hanging down the slope on the line's left in Therion's
    // frame (which the y-flip on export makes the right on screen), as Therion draws them
    SLOPE(
            R.string.line_type_slope,
            R.drawable.line_slope,
            "slope",
            false,
            false,
            false,
            1.0f,
            null,
            Stamp.hachured(1.0f, 1.2f, 0.6f)),
    // the outline of a boulder: a plain line, thinner than a wall in Therion's proportions
    ROCK_BORDER(
            R.string.line_type_rock_border,
            R.drawable.line_rock_border,
            "rock-border",
            false,
            false,
            true,
            0.75f,
            null,
            null),
    // the edges within a boulder: thinner still than its border
    ROCK_EDGE(
            R.string.line_type_rock_edge,
            R.drawable.line_rock_edge,
            "rock-edge",
            false,
            false,
            false,
            0.5f,
            null,
            null),
    PRESUMED_WALL(
            R.string.line_type_presumed_wall,
            R.drawable.line_presumed_wall,
            "wall:presumed",
            true,
            false,
            false,
            1.0f,
            new float[] {0.8f, 0.4f},
            null),
    // the wall of a passage passing beneath this one: a wall with shorter dashes than presumed
    UNDERLYING_WALL(
            R.string.line_type_underlying_wall,
            R.drawable.line_underlying_wall,
            "wall:underlying",
            true,
            false,
            false,
            1.5f,
            new float[] {0.4f, 0.4f},
            null);

    /**
     * A repeating stamp, measured in multiples of the line's ornament size: a thin baseline bar of
     * barLength (repeating every advance, so a shorter bar leaves a dashed gap) plus either
     * perpendicular ticks of the given lengths spaced evenly along the bar, or a triangular tooth
     * spanning the bar with its point tickLength away from it. The sign of a length picks the side.
     */
    private static final class Stamp {
        final float advance;
        final float barLength;
        final float[] tickLengths;
        final boolean isTooth;

        private Stamp(float advance, float barLength, float[] tickLengths, boolean isTooth) {
            this.advance = advance;
            this.barLength = barLength;
            this.tickLengths = tickLengths;
            this.isTooth = isTooth;
        }

        static Stamp ticked(float advance, float barLength, float tickLength) {
            return new Stamp(advance, barLength, new float[] {tickLength}, false);
        }

        // a long and a short tick per advance; the bar runs the full advance
        static Stamp hachured(float advance, float longTickLength, float shortTickLength) {
            return new Stamp(
                    advance, advance, new float[] {longTickLength, shortTickLength}, false);
        }

        boolean isHachured() {
            return !isTooth && tickLengths.length > 1;
        }

        float getExtent() {
            float extent = 0;
            for (float tickLength : tickLengths) {
                extent = Math.max(extent, Math.abs(tickLength));
            }
            return extent;
        }

        // teeth abut, so the bar runs the full advance
        static Stamp toothed(float advance, float toothHeight) {
            return new Stamp(advance, advance, new float[] {toothHeight}, true);
        }

        /** Builds the effect for an ornament size already converted to pixels. */
        PathEffect createEffect(float ornamentSizePixels, float strokeWidth) {
            float half = strokeWidth / 2;
            float barEnd = barLength * ornamentSizePixels;
            Path stamp = new Path();
            stamp.addRect(0, -half, barEnd, half, Path.Direction.CW);
            if (isTooth) {
                stamp.moveTo(0, 0);
                stamp.lineTo(barEnd, 0);
                stamp.lineTo(barEnd / 2, tickLengths[0] * ornamentSizePixels);
                stamp.close();
            } else {
                for (int i = 0; i < tickLengths.length; i++) {
                    float tickX = barEnd * (i + 0.5f) / tickLengths.length;
                    float tickEnd = tickLengths[i] * ornamentSizePixels;
                    stamp.addRect(
                            tickX - half,
                            Math.min(0, tickEnd),
                            tickX + half,
                            Math.max(0, tickEnd),
                            Path.Direction.CW);
                }
            }
            return new PathDashPathEffect(
                    stamp, advance * ornamentSizePixels, 0, PathDashPathEffect.Style.MORPH);
        }
    }

    /**
     * Below this on-screen advance (in pixels) the ornamentation is too small to read and would
     * render as a smear, so it is dropped in favour of a plain stroke.
     */
    private static final float MINIMUM_VISIBLE_ADVANCE_PIXELS = 4f;

    private final int stringId;
    private final int drawableId;
    private final String therionName;
    private final boolean isWall;
    private final boolean isHoleEdge;
    private final boolean isOutline;
    private final float strokeWidthFactor;
    private final float[] dashPattern; // {on, off} in ornament-size multiples, or null for solid
    private final Stamp stamp;
    private final int viewId;

    private static final LineType DEFAULT = SKETCH;

    LineType(
            int stringId,
            int drawableId,
            String therionName,
            boolean isWall,
            boolean isHoleEdge,
            boolean isOutline,
            float strokeWidthFactor,
            float[] dashPattern,
            Stamp stamp) {
        this.stringId = stringId;
        this.drawableId = drawableId;
        this.therionName = therionName;
        this.isWall = isWall;
        this.isHoleEdge = isHoleEdge;
        this.isOutline = isOutline;
        this.strokeWidthFactor = strokeWidthFactor;
        this.dashPattern = dashPattern;
        this.stamp = stamp;
        this.viewId = View.generateViewId();
    }

    public static LineType fromString(String name) {
        return name == null ? DEFAULT : LineType.valueOf(name);
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

    /**
     * The Therion line type this exports as, or null for SKETCH (which is not semantic and only
     * appears in the XVI tracing background).
     */
    public String getTherionName() {
        return therionName;
    }

    public boolean isSemantic() {
        return therionName != null;
    }

    /**
     * Wall types are auto-oriented when drawn (see LineOrienter): their correct side is the passage
     * interior, which can be inferred from the centreline. Ticked types can't be — only the user
     * knows which side the drop is — so they keep the drawn direction.
     */
    public boolean isWall() {
        return isWall;
    }

    /**
     * Whether this is the edge of a hole in the floor or ceiling (a pit or chimney). Drawn round
     * its hole, such a line always has its ticks on the inside, which Auto-Orientation uses.
     */
    public boolean isHoleEdge() {
        return isHoleEdge;
    }

    /**
     * Whether this outlines something (a boulder), so a line of it whose ends nearly meet is meant
     * to be closed, and is joined up when drawn.
     */
    public boolean isOutline() {
        return isOutline;
    }

    /**
     * Whether a line of this type drawn as a loop, alone or closed by a wall, can be turned to face
     * its inside: a hole edge's ticks point into the hole, and a boulder outline has the rock
     * inside. Auto-Orientation uses this.
     */
    public boolean isOrientedByLoop() {
        return isHoleEdge || isOutline;
    }

    /**
     * Which side of the drawing direction, as seen on screen, the inside of such a loop belongs on.
     * For a hole edge it's the side its ticks fall on. A boulder outline has its rock on the left
     * in Therion's frame (the Therion wiki's convention for rock-border), which the y-flip on
     * export makes the right on screen.
     */
    public boolean hasLoopInsideOnRight() {
        return isOutline || hasTicksOnRight();
    }

    /**
     * Therion's l-size for a line of this type drawn with the given ornament size (in metres), or 0
     * if it takes none. Therion requires one for a slope: the length of its hachures.
     */
    public float getTherionLineSize(float ornamentSize) {
        return stamp != null && stamp.isHachured() ? stamp.getExtent() * ornamentSize : 0;
    }

    /**
     * Whether the ticks (or teeth) fall on the right of the direction the line was drawn in, as
     * seen on screen; false for the left, or if there are none.
     */
    public boolean hasTicksOnRight() {
        // the stamp's y axis is the line's right-hand side, and its sign picks the side
        return stamp != null && stamp.tickLengths[0] > 0;
    }

    /**
     * Whether a new line of this type is clipped to the walls around it (when Clip to Walls is on):
     * true for the features that lie within a passage. Walls themselves aren't, since they're what
     * does the clipping (and an underlying wall crosses the walls above by definition), and nor are
     * freehand lines, which can be anything at all, including notes beyond the walls.
     */
    public boolean isClippedToWalls() {
        return isSemantic() && !isWall;
    }

    /**
     * Whether it matters which way the line runs: true for walls (passage side), for types with
     * ticks or teeth (which side they fall on), and for outlines (which side the rock is). For the
     * rest, flipping changes nothing.
     */
    public boolean isOriented() {
        return isWall || stamp != null || isOutline;
    }

    public float getStrokeWidthFactor() {
        return strokeWidthFactor;
    }

    /**
     * SVG stroke-dasharray value for a line of the given ornament size (in survey metres) at the
     * given export scale, or null for solid types.
     */
    public String getSvgDashArray(float ornamentSize, int scale) {
        if (dashPattern == null) {
            return null;
        }
        float unit = ornamentSize * scale;
        return (dashPattern[0] * unit) + " " + (dashPattern[1] * unit);
    }

    /**
     * The path effect giving this type its on-screen ornamentation, or null for a plain stroke. The
     * ornament size is passed in already converted to pixels (the line's survey-space size times
     * the current view scale), so the ornamentation is anchored to the cave rather than to the
     * screen and stays put as the view is zoomed.
     *
     * <p>A stamp takes precedence over a dash pattern since any dashing is built into the stamp
     * itself (the dash values are still used for SVG export). Returns null when the result would be
     * too small on screen to read.
     */
    public PathEffect createPathEffect(float ornamentSizePixels, float strokeWidth) {
        if (stamp != null) {
            if (stamp.advance * ornamentSizePixels < MINIMUM_VISIBLE_ADVANCE_PIXELS) {
                return null;
            }
            return stamp.createEffect(ornamentSizePixels, strokeWidth);
        }
        if (dashPattern != null) {
            float on = dashPattern[0] * ornamentSizePixels;
            float off = dashPattern[1] * ornamentSizePixels;
            if (on + off < MINIMUM_VISIBLE_ADVANCE_PIXELS) {
                return null;
            }
            return new DashPathEffect(new float[] {on, off}, 0);
        }
        return null;
    }
}
