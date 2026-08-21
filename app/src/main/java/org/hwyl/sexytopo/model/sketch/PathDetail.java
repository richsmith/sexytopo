package org.hwyl.sexytopo.model.sketch;

import java.util.ArrayList;
import java.util.List;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.util.Space2DUtils;
import org.hwyl.sexytopo.model.graph.Coord2D;

public final class PathDetail extends SketchDetail {

    /**
     * Ornament size for a line drawn before line types existed, or where no size was recorded. In
     * survey-space metres, as ornamentSize always is.
     */
    public static final float DEFAULT_ORNAMENT_SIZE = 0.2f;

    private List<Coord2D> path;
    private final LineType lineType;
    private final float ornamentSize;

    public PathDetail(Coord2D start, Colour colour) {
        this(start, colour, LineType.SKETCH, DEFAULT_ORNAMENT_SIZE);
    }

    public PathDetail(Coord2D start, Colour colour, LineType lineType) {
        this(start, colour, lineType, DEFAULT_ORNAMENT_SIZE);
    }

    public PathDetail(Coord2D start, Colour colour, LineType lineType, float ornamentSize) {
        super(colour);
        this.lineType = lineType;
        this.ornamentSize = ornamentSize;
        this.path = new ArrayList<>();
        path.add(start);
        updateBoundingBox(start);
    }

    public PathDetail(List<Coord2D> path, Colour colour) {
        this(path, colour, LineType.SKETCH, DEFAULT_ORNAMENT_SIZE);
    }

    public PathDetail(List<Coord2D> path, Colour colour, LineType lineType) {
        this(path, colour, lineType, DEFAULT_ORNAMENT_SIZE);
    }

    public PathDetail(List<Coord2D> path, Colour colour, LineType lineType, float ornamentSize) {
        super(colour);
        this.lineType = lineType;
        this.ornamentSize = ornamentSize;
        this.path = path;
        for (Coord2D point : path) {
            updateBoundingBox(point);
        }
    }

    public void lineTo(Coord2D point) {
        path.add(point);
        updateBoundingBox(point);
    }

    public List<Coord2D> getPath() {
        return path;
    }

    public void setPath(List<Coord2D> path) {
        this.path = path;
    }

    public LineType getLineType() {
        return lineType;
    }

    /**
     * The size of this line's ornamentation (tick length and spacing) in survey-space metres, fixed
     * when the line was drawn from the zoom level in force at the time — the same approach symbols
     * use for their size. Being a survey-space quantity, the ornamentation stays put over the cave
     * as the view is zoomed instead of crawling along the line.
     *
     * <p>This is a display property only: Therion applies its own styling to line commands, so it
     * is not exported to th2 (SVG, whose appearance we do control, does use it).
     */
    public float getOrnamentSize() {
        return ornamentSize;
    }

    @Override
    public float getDistanceFrom(Coord2D point) {
        return getClosestDistance(point, getPath());
    }

    @Override
    public PathDetail translate(Coord2D translation) {
        List<Coord2D> newPath = new ArrayList<>();
        for (Coord2D step : path) {
            newPath.add(step.plus(translation));
        }
        return new PathDetail(newPath, getColour(), lineType, ornamentSize);
    }

    @Override
    public PathDetail scale(float scale) {
        List<Coord2D> newPath = new ArrayList<>();
        for (Coord2D step : path) {
            newPath.add(step.scale(scale));
        }
        // the ornamentation is a survey-space size, so it scales with the geometry (as a
        // symbol's size does) — e.g. when a cross-section sub-sketch is scaled onto the plan
        return new PathDetail(newPath, getColour(), lineType, ornamentSize * scale);
    }

    public List<SketchDetail> getPathFragmentsOutsideRadius(Coord2D targetPoint, double radius) {
        List<SketchDetail> fragments = new ArrayList<>();

        List<Coord2D> currentLine = new ArrayList<>();
        for (Coord2D currentPoint : path) {

            if (currentLine.isEmpty()) {
                currentLine.add(currentPoint);
                continue;
            }

            Coord2D lastPoint = currentLine.get(currentLine.size() - 1);

            double distance =
                    Space2DUtils.getDistanceFromLine(targetPoint, lastPoint, currentPoint);
            if (distance < radius) {
                if (currentLine.size() > 1) {
                    PathDetail fragment =
                            new PathDetail(currentLine, getColour(), lineType, ornamentSize);
                    fragments.add(fragment);
                }
                currentLine = new ArrayList<>();
            }

            currentLine.add(currentPoint);
        }

        if (currentLine.size() > 1) {
            PathDetail fragment = new PathDetail(currentLine, getColour(), lineType, ornamentSize);
            fragments.add(fragment);
        }

        return fragments;
    }

    private static float getClosestDistance(Coord2D point, List<Coord2D> line) {
        float minDistance = Float.MAX_VALUE;
        for (int i = 0, j = 1; i < (line.size() - 1); i++, j++) {
            try {
                minDistance =
                        Math.min(
                                minDistance,
                                Space2DUtils.getDistanceFromLine(point, line.get(i), line.get(j)));
            } catch (Exception exception) {
                Log.e(exception);
            }
        }
        return minDistance;
    }
}
