package org.hwyl.sexytopo.model.sketch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.util.Space2DUtils;
import org.hwyl.sexytopo.model.geometry.Coord2D;

public final class PathDetail extends SketchDetail {

    // how far back along a line its end is bent round to meet a join, as a multiple of the snap
    // distance
    private static final float SNAP_BEND_LENGTH_FACTOR = 3;

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
     * Brings the start of the line to the join. The stroke is cut back to where it first came
     * within delta of the join, so one that starts a little beyond it, or wobbles about it, doesn't
     * double back; then its last stretch is bent smoothly round to meet it, rather than running
     * straight across, so the join doesn't leave a kink. The far end of the line stays put.
     */
    public void snapStartTo(Coord2D join, float delta) {
        Collections.reverse(path);
        snapEndTo(join, delta);
        Collections.reverse(path);
    }

    /** As snapStartTo, but for the end of the line. */
    public void snapEndTo(Coord2D join, float delta) {
        int last = path.size() - 1;
        while (last > 1 && Space2DUtils.getDistance(join, path.get(last - 1)) < delta) {
            last--;
        }
        path.subList(last + 1, path.size()).clear();

        if (path.size() < 2) {
            lineTo(join);
            return;
        }

        bendEndTo(join, delta * SNAP_BEND_LENGTH_FACTOR);

        resetBoundingBox();
        path.forEach(this::updateBoundingBox);
    }

    /**
     * Moves the end of the line onto the point, dragging the points before it along by less and
     * less the further back they are, easing off to nothing at the given distance along the line or
     * at its start, whichever comes first.
     */
    private void bendEndTo(Coord2D point, float bendLength) {
        int last = path.size() - 1;
        Coord2D offset = point.minus(path.get(last));

        float[] distancesFromEnd = new float[path.size()];
        for (int i = last - 1; i >= 0; i--) {
            distancesFromEnd[i] =
                    distancesFromEnd[i + 1]
                            + Space2DUtils.getDistance(path.get(i), path.get(i + 1));
        }
        float length = Math.min(bendLength, distancesFromEnd[0]);

        for (int i = 1; i < last; i++) {
            float t = 1 - distancesFromEnd[i] / length;
            if (t > 0) {
                float weight = t * t * (3 - 2 * t);
                path.set(i, path.get(i).plus(offset.scale(weight)));
            }
        }
        path.set(last, point);
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
    public GeometryType getGeometryType() {
        return GeometryType.LINE;
    }

    @Override
    public float getDistanceFrom(Coord2D point) {
        return getClosestDistance(point, getPath());
    }

    /** A line the same as this one but in a different colour. */
    public PathDetail withColour(Colour colour) {
        return new PathDetail(new ArrayList<>(path), colour, lineType);
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

    /**
     * What's left of this line after erasing a disc from it. Segments crossing the disc are cut at
     * its edge, so the gap is the size of the eraser however far apart the line's points are, the
     * same as erasing from an area.
     */
    public List<SketchDetail> getPathFragmentsOutsideRadius(Coord2D targetPoint, double radius) {
        List<SketchDetail> fragments = new ArrayList<>();
        if (path.isEmpty()) {
            return fragments;
        }

        List<Coord2D> currentLine = new ArrayList<>();
        if (targetPoint.minus(path.get(0)).mag() >= radius) {
            currentLine.add(path.get(0));
        }

        for (int i = 1; i < path.size(); i++) {
            Coord2D start = path.get(i - 1);
            Coord2D end = path.get(i);
            double[] inside = getStretchInsideDisc(start, end, targetPoint, radius);

            if (inside == null) {
                // segment misses the disc entirely
                currentLine.add(end);
                continue;
            }

            if (inside[0] > 0) {
                currentLine.add(pointAlong(start, end, inside[0]));
            }
            if (currentLine.size() > 1) {
                fragments.add(new PathDetail(currentLine, getColour(), lineType));
            }
            currentLine = new ArrayList<>();
            if (inside[1] < 1) {
                currentLine.add(pointAlong(start, end, inside[1]));
                currentLine.add(end);
            }
        }

        if (currentLine.size() > 1) {
            fragments.add(new PathDetail(currentLine, getColour(), lineType));
        }

        return fragments;
    }

    /**
     * The stretch of the segment from start to end lying inside the disc, as fractions of the way
     * along it clamped to 0..1, or null if the segment doesn't enter the disc.
     */
    private static double[] getStretchInsideDisc(
            Coord2D start, Coord2D end, Coord2D centre, double radius) {
        Coord2D direction = end.minus(start);
        Coord2D fromCentre = start.minus(centre);
        double a = direction.x * direction.x + direction.y * direction.y;
        double b = 2 * (fromCentre.x * direction.x + fromCentre.y * direction.y);
        double c = fromCentre.x * fromCentre.x + fromCentre.y * fromCentre.y - radius * radius;

        if (a == 0) {
            // zero-length segment: it's inside or it isn't
            return c < 0 ? new double[] {0, 1} : null;
        }

        double discriminant = b * b - 4 * a * c;
        if (discriminant <= 0) {
            return null;
        }
        double root = Math.sqrt(discriminant);
        double enter = (-b - root) / (2 * a);
        double exit = (-b + root) / (2 * a);
        if (exit <= 0 || enter >= 1) {
            return null;
        }
        return new double[] {Math.max(0, enter), Math.min(1, exit)};
    }

    private static Coord2D pointAlong(Coord2D start, Coord2D end, double fraction) {
        return start.plus(end.minus(start).scale((float) fraction));
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
