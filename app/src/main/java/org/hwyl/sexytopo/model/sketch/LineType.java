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
    SKETCH(R.string.line_type_sketch, R.drawable.pencil, null, false, 1.0f, null, null),
    WALL(R.string.line_type_wall, R.drawable.line_wall, "wall", true, 1.5f, null, null),
    PRESUMED_WALL(
            R.string.line_type_presumed_wall,
            R.drawable.line_presumed_wall,
            "wall:presumed",
            true,
            1.0f,
            new float[] {0.8f, 0.4f},
            null),
    // solid baseline with a perpendicular tick, repeating every ornament size
    PIT(
            R.string.line_type_pit,
            R.drawable.line_pit,
            "pit",
            false,
            1.0f,
            null,
            new Stamp(1.2f, 1.2f, -0.6f)),
    // dashed baseline (the bar covers two thirds of the advance) with a tick on the opposite
    // side to PIT
    CHIMNEY(
            R.string.line_type_chimney,
            R.drawable.line_chimney,
            "chimney",
            false,
            1.0f,
            new float[] {0.8f, 0.4f},
            new Stamp(1.2f, 0.8f, 0.5f));

    /**
     * A repeating tick stamp, measured in multiples of the line's ornament size: a thin baseline
     * bar of barLength (repeating every advance, so a shorter bar leaves a dashed gap) plus a
     * perpendicular tick of tickLength halfway along the bar (the sign picks the side).
     */
    private static final class Stamp {
        final float advance;
        final float barLength;
        final float tickLength;

        Stamp(float advance, float barLength, float tickLength) {
            this.advance = advance;
            this.barLength = barLength;
            this.tickLength = tickLength;
        }

        /** Builds the effect for an ornament size already converted to pixels. */
        PathEffect createEffect(float ornamentSizePixels, float strokeWidth) {
            float half = strokeWidth / 2;
            Path stamp = new Path();
            stamp.addRect(0, -half, barLength * ornamentSizePixels, half, Path.Direction.CW);
            float tickX = barLength / 2 * ornamentSizePixels;
            float tickTop = Math.min(0, tickLength * ornamentSizePixels);
            float tickBottom = Math.max(0, tickLength * ornamentSizePixels);
            stamp.addRect(tickX - half, tickTop, tickX + half, tickBottom, Path.Direction.CW);
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
            float strokeWidthFactor,
            float[] dashPattern,
            Stamp stamp) {
        this.stringId = stringId;
        this.drawableId = drawableId;
        this.therionName = therionName;
        this.isWall = isWall;
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
