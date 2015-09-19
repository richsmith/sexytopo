package org.hwyl.sexytopo.model.sketch;

import android.graphics.Color;

import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.control.util.Space2DUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by rls on 23/09/14.
 */
public class Sketch {

    private List<PathDetail> pathDetails = new ArrayList<>();
    private List<TextDetail> textDetails = new ArrayList<>();

    private List<SketchDetail> sketchHistory = new ArrayList<>();
    private List<SketchDetail> undoneHistory = new ArrayList<>();


    private PathDetail activePath;

    private Colour activeColour = Colour.BLACK;


    public void setPathDetails(List<PathDetail> pathDetails) {
        this.pathDetails = pathDetails;
    }

    public void setTextDetails(List<TextDetail> textDetails) {
        this.textDetails = textDetails;
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
        sketchHistory.add(activePath);
        undoneHistory.clear();
        return activePath;
    }


    public void lineTo(Coord2D point) {
        activePath.lineTo(point);
        undoneHistory.clear();
    }


    public void finishPath() {
        activePath = null;
    }

    public void addTextDetail(Coord2D location, String text) {
        TextDetail textDetail = new TextDetail(location, text, activeColour);
        textDetails.add(textDetail);
        sketchHistory.add(textDetail);
        undoneHistory.clear();
    }

    public List<TextDetail> getTextDetails() {
        return textDetails;
    }

    public void setActiveColour(Colour colour) {
        this.activeColour = colour;
    }


    public void undo() {
        if (!sketchHistory.isEmpty()) {
            SketchDetail toUndo = sketchHistory.remove(sketchHistory.size() - 1);

            if (toUndo instanceof PathDetail) {
                pathDetails.remove(toUndo);
            } else if (toUndo instanceof TextDetail) {
                textDetails.remove(toUndo);
            }

            undoneHistory.add(toUndo);
        }
    }


    public void redo() {

        if (!undoneHistory.isEmpty()) {
            SketchDetail toRedo = undoneHistory.remove(undoneHistory.size() - 1);

            if (toRedo instanceof PathDetail) {
                pathDetails.add((PathDetail) toRedo);
            } else if (toRedo instanceof TextDetail) {
                textDetails.add((TextDetail) toRedo);
            }

            sketchHistory.add(toRedo);
        }
    }

    public void deleteDetail(SketchDetail sketchDetail) {
        if (sketchDetail instanceof PathDetail) {
            pathDetails.remove(sketchDetail);
        } else if (sketchDetail instanceof TextDetail) {
            textDetails.remove(sketchDetail);
        } else if (sketchDetail instanceof CrossSection) {
            crossSections.remove(sketchDetail);
        }
    }


    public Coord2D findEligibleSnapPointWithin(Coord2D point, double delta) {

        Coord2D closest = null;
        double minDistance = Double.MAX_VALUE;

        for (PathDetail path : pathDetails) {

            if (activePath == path) {
                continue;
            }

            Coord2D start = path.getPath().get(0);
            Coord2D end = path.getPath().get(path.getPath().size() - 1);
            for (Coord2D coord2D : new Coord2D[]{start, end}) {
                double distance = Space2DUtils.getDistance(point, coord2D);
                if (distance < delta && distance < minDistance) {
                    closest = coord2D;
                    minDistance = distance;
                }
            }
        }
        return closest;
    }


    public PathDetail findNearestPathWithin(Coord2D point, double delta) {

        PathDetail closest = null;
        double minDistance = Double.MAX_VALUE;

        for (PathDetail path : pathDetails) {
            double distance = getClosestDistance(point, path.getPath());
            if (distance < delta && distance < minDistance) {
                closest = path;
                minDistance = distance;
            }
        }

        return closest;
    }

    public SketchDetail findNearestDetailWithin(Coord2D point, double delta) {

        SketchDetail closest = null;
        double minDistance = Double.MAX_VALUE;

        for (PathDetail path : pathDetails) {
            double distance = getClosestDistance(point, path.getPath());
            if (distance < delta && distance < minDistance) {
                closest = path;
                minDistance = distance;
            }
        }

        for (TextDetail textDetail : textDetails) {
            double distance = Space2DUtils.getDistance(point, textDetail.getLocation());
            if (distance < delta && distance < minDistance) {
                closest = textDetail;
                minDistance = distance;
            }
        }

        for (CrossSection section : crossSections.keySet()) {
            double distance = Space2DUtils.getDistance(point, crossSections.get(section));
            if (distance < delta && distance < minDistance) {
                closest = section;
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
                Log.d("Error calculating minimum distance: " + e);
            }
        }
        return minDistance;
    }


    private Map<CrossSection, Coord2D> crossSections = new HashMap<>();

    public void addCrossSection(CrossSection crossSection, Coord2D touchPointOnSurvey) {
        crossSections.put(crossSection, touchPointOnSurvey);
    }

    public Map<CrossSection, Coord2D> getCrossSections() {
        return crossSections;
    }
}
