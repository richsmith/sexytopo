package org.hwyl.sexytopo.model.sketch;

import android.graphics.Path;

import org.hwyl.sexytopo.model.graph.Coord2D;

import java.util.ArrayList;
import java.util.List;

/**
* Created by rls on 15/02/15.
*/
public class PathDetail {

    private final List<Coord2D> path;
    private final int colour;

    public PathDetail(Coord2D start, int colour) {
        this.path = new ArrayList<>();
        path.add(start);
        this.colour = colour;
    }

    public PathDetail(List<Coord2D> paths, int colour) {
        this.path = paths;
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
