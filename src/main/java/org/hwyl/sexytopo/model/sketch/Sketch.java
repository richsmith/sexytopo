package org.hwyl.sexytopo.model.sketch;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

import org.hwyl.sexytopo.model.graph.Coord2D;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rls on 23/09/14.
 */
public class Sketch {

    private List<PathDetail> pathDetails = new ArrayList<>();
    private List<PathDetail> undonePathDetails = new ArrayList<>();


    private PathDetail activePath;
    //drawing and canvas paint
    private Paint drawPaint;

    private int activeColour = Color.BLACK;


    public PathDetail getActivePath() {
        return activePath;
    }

    public PathDetail startNewPath(Coord2D start) {
        activePath = new PathDetail(start, activeColour);
        pathDetails.add(activePath);
        return activePath;
    }

    public void lineTo(Coord2D point) {
        activePath.lineTo(point);
    }

    public void finishPath() {
        activePath = null;
    }

    public List<PathDetail> getPathDetails() {
        return pathDetails;
    }

    public void setActiveColour(int colour) {
        this.activeColour = colour;
    }

    public void undo() {
        if (!pathDetails.isEmpty()) {
            PathDetail path = pathDetails.remove(pathDetails.size() - 1);
            undonePathDetails.add(path);
        }
    }

    public void redo() {
        if (!undonePathDetails.isEmpty()) {
            PathDetail path = undonePathDetails.remove(undonePathDetails.size() - 1);
            pathDetails.add(path);
        }
    }


    public static PathDetail findNearestPathWithin(List<PathDetail> paths, Coord2D point,
                                                   double delta) {

        PathDetail closest = null;
        double minDistance = Double.MAX_VALUE;

        for (PathDetail path : paths) {
            double distance = getClosestDistance(point, path.getPath());
            if (distance < delta && distance < minDistance) {
                closest = path;
                minDistance = distance;
            }
        }

        return closest;
    }

    private static double getClosestDistance(Coord2D point, List<Coord2D> line) {
        double minDistance = Double.MAX_VALUE;
        for (int i = 0, j = 1; i < (line.size() - 1); i++, j++) {
            try {
                minDistance = Math.min(minDistance,
                        getDistanceFromLine(point, line.get(i), line.get(j)));
            } catch (Exception e) {
                int ignore = 12;
            }
        }
        return minDistance;
    }

    public static double getDistanceFromLine(Coord2D point, Coord2D lineStart, Coord2D lineEnd) {

        // this is a bit of a cheat/hack, but in practice sketched lines are going to be very short
        // and just checking the end points of the line should be enough for our purposes
        // (calculating the minimum distance to a line segment looks pretty involved/costly!)
        return Math.min(getDistance(point, lineStart), getDistance(point, lineEnd));

/*
        final double x0 = point.getX();
        final double y0 = point.getY();
        final double x1 = lineStart.getX();
        final double y1 = lineStart.getY();
        final double x2 = lineEnd.getX();
        final double y2 = lineEnd.getY();

        double denom = Math.sqrt(Math.pow(y2 - y1, 2) + Math.pow(x2 - x1, 2));
        double enumerator = Math.abs((y2 - y1) * x0 - (x2 - x1) * y0 + x2 * y1 - y2 * x1);

        double distance =
                Math.abs((y2 - y1) * x0 - (x2 - x1) * y0 + x2 * y1 - y2 * x1) /
                        Math.sqrt(Math.pow((y2 - y1), 2.0) + Math.pow((x2 - x1), 2.0));
        Log.d("sexytopo", "between " + point + " and " + lineStart + "-" + lineEnd + " = " + distance + "(" + enumerator + "/" + denom + ")");
        return distance;*/
    }

    public static double getDistance(Coord2D a, Coord2D b) {
        return Math.sqrt(Math.pow((a.getX() - b.getX()), 2) + Math.pow((a.getY() - b.getY()), 2));
    }



    public class PathDetail {

        private final List<Coord2D> path;
        private final int colour;

        public PathDetail(Coord2D start, int colour) {
            this.path = new ArrayList<>();
            path.add(start);
            this.colour = colour;
        }

        public void lineTo(Coord2D point) {
            path.add(point);
        }

        public List<Coord2D> getPath() {
            return path;
        }

        public Path getAndroidPath() {
            Path androidPath = new Path();

            boolean first = true;
            for (Coord2D point : path) {
                if (first) {
                    androidPath.moveTo((float) point.getX(), (float) point.getY());
                    first = false;
                } else {
                    androidPath.lineTo((float) point.getX(), (float) point.getY());
                }
            }
            return androidPath;
        }

        public int getColour() {
            return colour;
        }
    }
}
