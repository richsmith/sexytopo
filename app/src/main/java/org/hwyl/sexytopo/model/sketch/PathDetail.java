package org.hwyl.sexytopo.model.sketch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.util.Space2DUtils;
import org.hwyl.sexytopo.model.geometry.Coord2D;

public final class PathDetail extends SketchDetail {

    private List<Coord2D> path;
    private final LineType lineType;

    public PathDetail(Coord2D start, Colour colour) {
        this(start, colour, LineType.SKETCH);
    }

    public PathDetail(Coord2D start, Colour colour, LineType lineType) {
        super(colour);
        this.lineType = lineType;
        this.path = new ArrayList<>();
        path.add(start);
        updateBoundingBox(start);
    }

    public PathDetail(List<Coord2D> path, Colour colour) {
        this(path, colour, LineType.SKETCH);
    }

    public PathDetail(List<Coord2D> path, Colour colour, LineType lineType) {
        super(colour);
        this.lineType = lineType;
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

    public Coord2D getStart() {
        return path.get(0);
    }

    public Coord2D getEnd() {
        return path.get(path.size() - 1);
    }

    /** Whether the line's ends meet, making it a loop. */
    public boolean isClosed() {
        return path.size() > 2 && getStart().equals(getEnd());
    }

    /** Whether the line ever gets distance or more away from its start. */
    public boolean reachesBeyond(float distance) {
        Coord2D start = getStart();
        return path.stream().anyMatch(point -> Space2DUtils.getDistance(start, point) >= distance);
    }

    /**
     * Whether another line could be joined on to this one to make a single line: it has to be of
     * the same type and colour, and neither can be a closed loop, with no loose end to join.
     */
    public boolean canJoin(PathDetail other) {
        return other.lineType == lineType
                && other.getColour() == getColour()
                && !isClosed()
                && !other.isClosed();
    }

    /**
     * The end of this line, nearest the point, that a line joined on after it could start from.
     * Where it matters which way the line runs that's only its end, so the two run the same way;
     * otherwise it could be either end, with this line read backwards to join.
     */
    public Coord2D getEndToJoinAfter(Coord2D point) {
        return lineType.isOriented() ? getEnd() : getEndNearest(point);
    }

    /** As getEndToJoinAfter, but the end a line joined on before this one could finish at. */
    public Coord2D getEndToJoinBefore(Coord2D point) {
        return lineType.isOriented() ? getStart() : getEndNearest(point);
    }

    private Coord2D getEndNearest(Coord2D point) {
        return Space2DUtils.getDistance(point, getStart())
                        < Space2DUtils.getDistance(point, getEnd())
                ? getStart()
                : getEnd();
    }

    /**
     * Moves the start of the line to the join, first dropping the points drawn within delta of it,
     * so a stroke that starts a little beyond the join, or wobbles about it, runs straight out from
     * it rather than doubling back. At least one point is kept.
     */
    public void snapStartTo(Coord2D join, float delta) {
        while (path.size() > 1 && Space2DUtils.getDistance(join, getStart()) < delta) {
            path.remove(0);
        }
        path.add(0, join);
        updateBoundingBox(join);
    }

    /** As snapStartTo, but for the end of the line. */
    public void snapEndTo(Coord2D join, float delta) {
        while (path.size() > 1 && Space2DUtils.getDistance(join, getEnd()) < delta) {
            path.remove(path.size() - 1);
        }
        lineTo(join);
    }

    /**
     * This line with before joined on at its start and after at its end, as one new line, or this
     * line itself if neither meets it. A line counts as meeting only where its end and this one's
     * coincide and the two run the same way (see getEndToJoinAfter). If before and after are the
     * same line, it's joined once, which closes it into a loop. The lines actually joined are added
     * to joined.
     */
    public PathDetail joinedWith(PathDetail before, PathDetail after, List<PathDetail> joined) {
        List<Coord2D> points = new ArrayList<>(path);

        List<Coord2D> leadingIn = before == null ? null : before.pointsRunningInto(getStart());
        if (leadingIn != null) {
            leadingIn.addAll(points.subList(1, points.size()));
            points = leadingIn;
            joined.add(before);
        }

        // joined at both ends to the same line, this one has already closed it into a loop
        boolean closedBefore = leadingIn != null && after == before;
        List<Coord2D> leadingOut =
                after == null || closedBefore ? null : after.pointsRunningOutOf(getEnd());
        if (leadingOut != null) {
            points.addAll(leadingOut.subList(1, leadingOut.size()));
            joined.add(after);
        }

        if (leadingIn == null && leadingOut == null) {
            return this;
        }
        return new PathDetail(points, getColour(), lineType);
    }

    /**
     * A copy of the line's points ordered to finish at the given end, or null if a line finishing
     * there couldn't be joined on to: the point isn't one of its ends, or the line is oriented and
     * would have to be read backwards.
     */
    private List<Coord2D> pointsRunningInto(Coord2D end) {
        List<Coord2D> points = new ArrayList<>(path);
        if (getEnd().equals(end)) {
            return points;
        }
        if (!lineType.isOriented() && getStart().equals(end)) {
            Collections.reverse(points);
            return points;
        }
        return null;
    }

    /** As pointsRunningInto, but ordered to start at the given end. */
    private List<Coord2D> pointsRunningOutOf(Coord2D start) {
        List<Coord2D> points = new ArrayList<>(path);
        if (getStart().equals(start)) {
            return points;
        }
        if (!lineType.isOriented() && getEnd().equals(start)) {
            Collections.reverse(points);
            return points;
        }
        return null;
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
        return new PathDetail(newPath, getColour(), lineType);
    }

    @Override
    public PathDetail scale(float scale) {
        List<Coord2D> newPath = new ArrayList<>();
        for (Coord2D step : path) {
            newPath.add(step.scale(scale));
        }
        return new PathDetail(newPath, getColour(), lineType);
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
                    PathDetail fragment = new PathDetail(currentLine, getColour(), lineType);
                    fragments.add(fragment);
                }
                currentLine = new ArrayList<>();
            }

            currentLine.add(currentPoint);
        }

        if (currentLine.size() > 1) {
            PathDetail fragment = new PathDetail(currentLine, getColour(), lineType);
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
