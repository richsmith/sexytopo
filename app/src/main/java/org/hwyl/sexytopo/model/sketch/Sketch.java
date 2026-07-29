package org.hwyl.sexytopo.model.sketch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hwyl.sexytopo.control.util.PolygonUtils;
import org.hwyl.sexytopo.control.util.SketchPreferences;
import org.hwyl.sexytopo.control.util.Space2DUtils;
import org.hwyl.sexytopo.model.common.Shape;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.survey.Station;

public class Sketch extends Shape {

    public static final float DEFAULT_XSECTION_SCALE = 1.0f;

    private List<PathDetail> pathDetails = new ArrayList<>();
    private List<AreaDetail> areaDetails = new ArrayList<>();
    private List<SymbolDetail> symbolDetails = new ArrayList<>();
    private List<TextDetail> textDetails = new ArrayList<>();
    private List<CrossSectionDetail> crossSectionDetails = new ArrayList<>();

    private final List<SketchDetail> sketchHistory = new ArrayList<>();
    private final List<SketchDetail> undoneHistory = new ArrayList<>();

    private PathDetail activePath;
    private Colour activeColour = Colour.BLACK;

    private float crossSectionScale = DEFAULT_XSECTION_SCALE;

    private boolean isSaved = true;

    public Sketch() {}

    public Sketch(Sketch sketch) {
        // shallow copies are OK here because paths are immutable
        setPathDetails(new ArrayList<>(sketch.getPathDetails()));
        setAreaDetails(new ArrayList<>(sketch.getAreaDetails()));
        setSymbolDetails(new ArrayList<>(sketch.getSymbolDetails()));
        setTextDetails(new ArrayList<>(sketch.getTextDetails()));
        setCrossSectionDetails(new ArrayList<>(sketch.getCrossSectionDetails()));
        this.crossSectionScale = sketch.crossSectionScale;
    }

    public boolean isSaved() {
        return isSaved;
    }

    public void setSaved(boolean isSaved) {
        this.isSaved = isSaved;
    }

    public void setPathDetails(List<PathDetail> pathDetails) {
        this.pathDetails = pathDetails;
        recalculateBoundingBox();
    }

    public void setAreaDetails(List<AreaDetail> areaDetails) {
        this.areaDetails = areaDetails;
        recalculateBoundingBox();
    }

    public List<AreaDetail> getAreaDetails() {
        return areaDetails;
    }

    public void setSymbolDetails(List<SymbolDetail> symbolDetails) {
        this.symbolDetails = symbolDetails;
        recalculateBoundingBox();
    }

    public void setTextDetails(List<TextDetail> textDetails) {
        this.textDetails = textDetails;
        recalculateBoundingBox();
    }

    public List<PathDetail> getPathDetails() {
        return pathDetails;
    }

    public PathDetail getActivePath() {
        return activePath;
    }

    public PathDetail startNewPath(Coord2D start) {
        activePath = new PathDetail(start, activeColour);
        // Registered in the undo history only on finishPath, so an unfinished path can be
        // abandoned without leaving a trace.
        pathDetails.add(activePath);
        return activePath;
    }

    private void addSketchDetail(SketchDetail sketchDetail) {
        setSaved(false);
        sketchHistory.add(sketchDetail);
        undoneHistory.clear();
        updateBoundingBox(sketchDetail);
    }

    // Drops the in-progress path without committing it to the sketch or undo history.
    public void abandonActivePath() {
        if (activePath == null) {
            return;
        }
        pathDetails.remove(activePath);
        activePath = null;
        recalculateBoundingBox();
    }

    public void finishPath() {
        if (activePath == null) {
            return;
        }
        float epsilon = Space2DUtils.simplificationEpsilon(activePath);
        activePath.setPath(Space2DUtils.simplify(activePath.getPath(), epsilon));
        addSketchDetail(activePath);
        activePath = null;
    }

    /**
     * Start sketching the outline of a new area. The outline is drawn like a normal path (so it
     * renders as the user drags) and is turned into a polygon by finishArea.
     */
    public PathDetail startNewArea(Coord2D start, AreaType areaType) {
        Colour colour = activeColour;
        if (areaType.isWater() && SketchPreferences.Toggle.BLUE_WATER.isOn()) {
            colour = Colour.BLUE;
        }
        activePath = new PathDetail(start, colour);
        pathDetails.add(activePath);
        return activePath;
    }

    /**
     * Close the outline being sketched into a polygon and add it as an area. If it overlaps any
     * existing areas of the same type and colour, they are all merged into one (as a single
     * undoable operation).
     */
    public void finishArea(AreaType areaType) {
        if (activePath == null) {
            return;
        }
        PathDetail outlinePath = activePath;
        abandonActivePath();

        float epsilon = Space2DUtils.simplificationEpsilon(outlinePath);
        List<Coord2D> outline = Space2DUtils.simplify(outlinePath.getPath(), epsilon);
        if (outline.size() < 3) {
            return; // too small to form a polygon; treat as an accidental tap
        }

        addAreaDetail(new AreaDetail(outline, areaType, outlinePath.getColour()));
    }

    public void addAreaDetail(AreaDetail newArea) {

        List<SketchDetail> overlapping = new ArrayList<>();
        for (AreaDetail existing : areaDetails) {
            if (existing.getAreaType() == newArea.getAreaType()
                    && existing.getColour() == newArea.getColour()
                    && existing.intersectsRectangle(newArea.getTopLeft(), newArea.getBottomRight())
                    && PolygonUtils.overlap(existing.getPolygon(), newArea.getPolygon())) {
                overlapping.add(existing);
            }
        }

        List<SketchDetail> merged = null;
        if (!overlapping.isEmpty()) {
            List<List<Coord2D>> polygons = new ArrayList<>();
            polygons.add(newArea.getPolygon());
            for (SketchDetail detail : overlapping) {
                polygons.add(((AreaDetail) detail).getPolygon());
            }
            List<List<Coord2D>> union = PolygonUtils.union(polygons);
            if (union != null && !union.isEmpty()) {
                merged = new ArrayList<>();
                for (List<Coord2D> polygon : union) {
                    merged.add(new AreaDetail(polygon, newArea.getAreaType(), newArea.getColour()));
                }
            }
        }

        if (merged == null) {
            // nothing to merge with (or the merge failed): just add the new area as-is
            areaDetails.add(newArea);
            addSketchDetail(newArea);
        } else {
            deleteDetails(overlapping, merged);
        }
    }

    public void addTextDetail(Coord2D location, String text, float size) {
        TextDetail textDetail = new TextDetail(location, text, activeColour, size);
        textDetails.add(textDetail);
        addSketchDetail(textDetail);
    }

    public List<SymbolDetail> getSymbolDetails() {
        return symbolDetails;
    }

    public void addSymbolDetail(Coord2D location, Symbol symbol, float size, float angle) {
        Colour colour = activeColour;
        if (symbol.isWater() && SketchPreferences.Toggle.BLUE_WATER.isOn()) {
            colour = Colour.BLUE;
        }
        SymbolDetail symbolDetail = new SymbolDetail(location, symbol, colour, size, angle);
        symbolDetails.add(symbolDetail);
        addSketchDetail(symbolDetail);
    }

    public List<TextDetail> getTextDetails() {
        return textDetails;
    }

    public void setActiveColour(Colour colour) {
        this.activeColour = colour;
    }

    public float getCrossSectionScale() {
        return crossSectionScale;
    }

    public void setCrossSectionScale(float crossSectionScale) {
        this.crossSectionScale = crossSectionScale;
    }

    public void undo() {
        if (!sketchHistory.isEmpty()) {
            SketchDetail toUndo = sketchHistory.remove(sketchHistory.size() - 1);

            if (toUndo instanceof DeletedDetail) {
                DeletedDetail deletedDetail = (DeletedDetail) toUndo;
                for (SketchDetail sketchDetail : deletedDetail.getDeletedDetails()) {
                    restoreDetailToSketch(sketchDetail);
                }
                for (SketchDetail sketchDetail : deletedDetail.getReplacementDetails()) {
                    removeDetailFromSketch(sketchDetail);
                }
            } else {
                removeDetailFromSketch(toUndo);
            }

            undoneHistory.add(toUndo);
        }
    }

    public void redo() {
        if (!undoneHistory.isEmpty()) {
            SketchDetail toRedo = undoneHistory.remove(undoneHistory.size() - 1);

            if (toRedo instanceof DeletedDetail) {
                DeletedDetail deletedDetail = (DeletedDetail) toRedo;
                for (SketchDetail sketchDetail : deletedDetail.getDeletedDetails()) {
                    removeDetailFromSketch(sketchDetail);
                }
                for (SketchDetail sketchDetail : deletedDetail.getReplacementDetails()) {
                    restoreDetailToSketch(sketchDetail);
                }
            } else {
                restoreDetailToSketch(toRedo);
            }

            sketchHistory.add(toRedo);
        }
    }

    public void deleteDetail(SketchDetail sketchDetail) {
        deleteDetail(sketchDetail, new ArrayList<>());
    }

    public void deleteDetail(SketchDetail sketchDetail, List<SketchDetail> replacementDetails) {
        deleteDetails(Collections.singletonList(sketchDetail), replacementDetails);
    }

    /**
     * Delete several details in one undoable step, optionally replacing them with others (e.g.
     * areas merged into one).
     */
    public void deleteDetails(
            List<SketchDetail> sketchDetails, List<SketchDetail> replacementDetails) {
        DeletedDetail deletedDetail = new DeletedDetail(sketchDetails, replacementDetails);
        addSketchDetail(deletedDetail);
        for (SketchDetail sketchDetail : sketchDetails) {
            removeDetailFromSketch(sketchDetail);
        }
        for (SketchDetail replacementDetail : replacementDetails) {
            restoreDetailToSketch(replacementDetail);
        }
    }

    private void removeDetailFromSketch(SketchDetail sketchDetail) {
        // this is a separate function to deleteDetail because former is user-called and handles
        // undo history etc. whereas this actually removes the data
        if (sketchDetail instanceof PathDetail) {
            pathDetails.remove(sketchDetail);
        } else if (sketchDetail instanceof AreaDetail) {
            areaDetails.remove(sketchDetail);
        } else if (sketchDetail instanceof SymbolDetail) {
            symbolDetails.remove(sketchDetail);
        } else if (sketchDetail instanceof TextDetail) {
            textDetails.remove(sketchDetail);
        } else if (sketchDetail instanceof CrossSectionDetail) {
            crossSectionDetails.remove(sketchDetail);
        }

        recalculateBoundingBox();
    }

    public void restoreDetailToSketch(SketchDetail sketchDetail) {
        if (sketchDetail instanceof PathDetail) {
            pathDetails.add((PathDetail) sketchDetail);
        } else if (sketchDetail instanceof AreaDetail) {
            areaDetails.add((AreaDetail) sketchDetail);
        } else if (sketchDetail instanceof SymbolDetail) {
            symbolDetails.add((SymbolDetail) sketchDetail);
        } else if (sketchDetail instanceof TextDetail) {
            textDetails.add((TextDetail) sketchDetail);
        } else if (sketchDetail instanceof CrossSectionDetail) {
            crossSectionDetails.add((CrossSectionDetail) sketchDetail);
        }

        updateBoundingBox(sketchDetail);
    }

    public Coord2D findEligibleSnapPointWithin(Coord2D point, float delta) {

        Coord2D closest = null;
        float minDistance = Float.MAX_VALUE;

        for (PathDetail path : pathDetails) {

            if (activePath == path) {
                continue;
            }

            Coord2D start = path.getPath().get(0);
            Coord2D end = path.getPath().get(path.getPath().size() - 1);
            for (Coord2D coord2D : new Coord2D[] {start, end}) {
                float distance = Space2DUtils.getDistance(point, coord2D);
                if (distance < delta && distance < minDistance) {
                    closest = coord2D;
                    minDistance = distance;
                }
            }
        }
        return closest;
    }

    private List<SketchDetail> allSketchDetails() {
        List<SketchDetail> all = new ArrayList<>();
        all.addAll(pathDetails);
        all.addAll(areaDetails);
        all.addAll(symbolDetails);
        all.addAll(textDetails);
        all.addAll(crossSectionDetails);
        return all;
    }

    public SketchDetail findNearestDetailWithin(Coord2D point, float delta) {
        return findNearestVisibleDetailWithin(point, delta, Float.MAX_VALUE);
    }

    public SketchDetail findNearestVisibleDetailWithin(
            Coord2D point, float delta, float viewScale) {

        SketchDetail closest = null;
        float minDistance = Float.MAX_VALUE;

        for (SketchDetail detail : allSketchDetails()) {
            if (!detail.couldBeVisibleAtScale(viewScale)) {
                continue;
            }
            float distance = detail.getDistanceFrom(point);
            if (distance < delta && distance < minDistance) {
                closest = detail;
                minDistance = distance;
            }
        }

        return closest;
    }

    public void addCrossSection(CrossSection crossSection, Coord2D touchPointOnSurvey) {
        CrossSectionDetail sectionDetail = new CrossSectionDetail(crossSection, touchPointOnSurvey);
        addCrossSection(sectionDetail);
    }

    public void addCrossSection(CrossSectionDetail sectionDetail) {
        crossSectionDetails.add(sectionDetail);
        addSketchDetail(sectionDetail);
    }

    /**
     * Replace a cross-section detail as an undoable operation on this sketch's undo stack. Use this
     * for plan-level edits like move and rotate.
     */
    public void replaceCrossSectionDetail(
            CrossSectionDetail oldDetail, CrossSectionDetail newDetail) {
        List<SketchDetail> replacements = new ArrayList<>();
        replacements.add(newDetail);
        deleteDetail(oldDetail, replacements);
    }

    public List<CrossSectionDetail> getCrossSectionDetails() {
        return crossSectionDetails;
    }

    public void setCrossSectionDetails(List<CrossSectionDetail> crossSectionDetails) {
        this.crossSectionDetails = crossSectionDetails;
    }

    public CrossSectionDetail getCrossSectionDetail(Station station) {
        // this is a bit inefficient... not sure if it's worth caching this in a map though since
        // there'll probably be max a couple of dozen x-sections per survey chunk
        for (CrossSectionDetail detail : crossSectionDetails) {
            CrossSection crossSection = detail.getCrossSection();
            if (crossSection.getStation() == station) {
                return detail;
            }
        }
        return null;
    }

    @Override
    public Sketch translate(Coord2D translation) {
        Sketch sketch = new Sketch();

        List<PathDetail> newPathDetails = new ArrayList<>();
        for (PathDetail pathDetail : pathDetails) {
            newPathDetails.add(pathDetail.translate(translation));
        }
        sketch.setPathDetails(newPathDetails);

        List<AreaDetail> newAreaDetails = new ArrayList<>();
        for (AreaDetail areaDetail : areaDetails) {
            newAreaDetails.add(areaDetail.translate(translation));
        }
        sketch.setAreaDetails(newAreaDetails);

        List<SymbolDetail> newSymbolDetails = new ArrayList<>();
        for (SymbolDetail symbolDetail : symbolDetails) {
            newSymbolDetails.add(symbolDetail.translate(translation));
        }
        sketch.setSymbolDetails(newSymbolDetails);

        List<TextDetail> newTextDetails = new ArrayList<>();
        for (TextDetail textDetail : textDetails) {
            newTextDetails.add(textDetail.translate(translation));
        }
        sketch.setTextDetails(newTextDetails);

        List<CrossSectionDetail> newCrossSectionDetails = new ArrayList<>();
        for (CrossSectionDetail crossSectionDetail : crossSectionDetails) {
            newCrossSectionDetails.add(crossSectionDetail.translate(translation));
        }
        sketch.setCrossSectionDetails(newCrossSectionDetails);

        return sketch;
    }

    @Override
    public Sketch scale(float scale) {
        Sketch sketch = new Sketch();

        List<PathDetail> newPathDetails = new ArrayList<>();
        for (PathDetail pathDetail : pathDetails) {
            newPathDetails.add(pathDetail.scale(scale));
        }
        sketch.setPathDetails(newPathDetails);

        List<AreaDetail> newAreaDetails = new ArrayList<>();
        for (AreaDetail areaDetail : areaDetails) {
            newAreaDetails.add(areaDetail.scale(scale));
        }
        sketch.setAreaDetails(newAreaDetails);

        List<SymbolDetail> newSymbolDetails = new ArrayList<>();
        for (SymbolDetail symbolDetail : symbolDetails) {
            newSymbolDetails.add(symbolDetail.scale(scale));
        }
        sketch.setSymbolDetails(newSymbolDetails);

        List<TextDetail> newTextDetails = new ArrayList<>();
        for (TextDetail textDetail : textDetails) {
            newTextDetails.add(textDetail.scale(scale));
        }
        sketch.setTextDetails(newTextDetails);

        List<CrossSectionDetail> newCrossSectionDetails = new ArrayList<>();
        for (CrossSectionDetail crossSectionDetail : crossSectionDetails) {
            SketchDetail scaled = crossSectionDetail.scale(scale);
            newCrossSectionDetails.add((CrossSectionDetail) scaled);
        }
        sketch.setCrossSectionDetails(newCrossSectionDetails);

        return sketch;
    }

    public void recalculateBoundingBox() {
        resetBoundingBox();
        for (SketchDetail sketchDetail : allSketchDetails()) {
            updateBoundingBox(sketchDetail);
        }
    }
}
