package org.hwyl.sexytopo.model.sketch;

import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.util.Space2DUtils;
import org.hwyl.sexytopo.model.graph.Coord2D;

import java.util.LinkedList;
import java.util.List;

public final class PathDetail extends SketchDetail {


    private List<Coord2D> path;

    public PathDetail(Coord2D start, Colour colour) {
        super(colour);
        this.path = new LinkedList<>();
        path.add(start);
        updateBoundingBox(start);
    }

    public PathDetail(List<Coord2D> paths, Colour colour) {
        super(colour);
        this.path = paths;
        for (Coord2D point : paths) {
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

    @Override
    public double getDistanceFrom(Coord2D point) {
        return getClosestDistance(point, getPath());
    }


    @Override
    public PathDetail translate(Coord2D point) {
        List<Coord2D> newPath = new LinkedList<>();
        for (Coord2D step : path) {
            newPath.add(step.plus(point));
        }
        return new PathDetail(newPath, getColour());
    }


    public List<SketchDetail> getPathFragmentsOutsideRadius(Coord2D targetPoint, double radius) {
        List<SketchDetail> fragments = new LinkedList<>();

        List<Coord2D> currentLine = new LinkedList<>();
        for (Coord2D currentPoint : path) {

            if (currentLine.size() == 0) {
                currentLine.add(currentPoint);
                continue;
            }

            Coord2D lastPoint = currentLine.get(currentLine.size() - 1);

            double distance = Space2DUtils.getDistanceFromLine(targetPoint, lastPoint, currentPoint);
            if (distance < radius) {
                if (currentLine.size() > 1) {
                    PathDetail fragment = new PathDetail(currentLine, getColour());
                    fragments.add(fragment);
                }
                currentLine = new LinkedList<>();
                currentLine.add(currentPoint);

            } else {
                currentLine.add(currentPoint);
            }
        }

        if (currentLine.size() > 1) {
            PathDetail fragment = new PathDetail(currentLine, getColour());
            fragments.add(fragment);
        }

        return fragments;
    }


    private static double getClosestDistance(Coord2D point, List<Coord2D> line) {
        double minDistance = Double.MAX_VALUE;
        for (int i = 0, j = 1; i < (line.size() - 1); i++, j++) {
            try {
                minDistance = Math.min(minDistance,
                        Space2DUtils.getDistanceFromLine(point, line.get(i), line.get(j)));
            } catch (Exception e) {
                Log.e("Error calculating minimum distance: " + e);
            }
        }
        return minDistance;
    }
}
