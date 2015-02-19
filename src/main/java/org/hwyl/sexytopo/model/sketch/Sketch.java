package org.hwyl.sexytopo.model.sketch;

import android.graphics.Color;

import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.util.Space2DUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rls on 23/09/14.
 */
public class Sketch {

    private List<PathDetail> pathDetails = new ArrayList<>();
    private List<PathDetail> undonePathDetails = new ArrayList<>();


    private PathDetail activePath;

    private int activeColour = Color.BLACK;


    public void setPathDetails(List<PathDetail> pathDetails) {
        this.pathDetails = pathDetails;
    }


    public List<PathDetail> getPathDetails() {
        return pathDetails;
    }


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
                        Space2DUtils.getDistanceFromLine(point, line.get(i), line.get(j)));
            } catch (Exception e) {
                int ignore = 12;
            }
        }
        return minDistance;
    }


}
