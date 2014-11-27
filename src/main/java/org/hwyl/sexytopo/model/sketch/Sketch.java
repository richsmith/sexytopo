package org.hwyl.sexytopo.model.sketch;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

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



    public Path getActivePath() {
        return activePath.getPath();
    }

    public Path startNewPath() {
        activePath = new PathDetail(new Path(), activeColour);
        pathDetails.add(activePath);
        return activePath.getPath();
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
        if (! pathDetails.isEmpty()) {
            PathDetail path = pathDetails.remove(pathDetails.size() - 1);
            undonePathDetails.add(path);
        }
    }

    public void redo() {
        if (! undonePathDetails.isEmpty()) {
            PathDetail path = undonePathDetails.remove(undonePathDetails.size() - 1);
            pathDetails.add(path);
        }
    }

    public class PathDetail {

        private final Path path;
        private final int colour;

        public PathDetail(Path path, int colour) {
            this.path = path;
            this.colour = colour;
        }

        public Path getPath() {
            return path;
        }

        public int getColour() {
            return colour;
        }
    }
}
