package org.hwyl.sexytopo.model.sketch;

import org.hwyl.sexytopo.control.util.Space2DUtils;
import org.hwyl.sexytopo.model.common.Shape;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.survey.Station;

import java.util.ArrayList;
import java.util.List;


public class Sketch extends Shape {

    private static final String DEFAULT_LAYER_NAME = "Layer 0";

    private List<SketchLayer> layers = new ArrayList<>();
    private int activeLayerId = 0;
    private int nextLayerId = 0;

    private final List<SketchDetail> sketchHistory = new ArrayList<>();
    private final List<SketchDetail> undoneHistory = new ArrayList<>();

    private PathDetail activePath;
    private Colour activeColour = Colour.BLACK;

    private boolean isSaved = true;

    public Sketch() {
        addLayer(DEFAULT_LAYER_NAME);
    }

    public boolean isSaved() {
        return isSaved;
    }

    public void setSaved(boolean isSaved) {
        this.isSaved = isSaved;
    }

    // Layer management

    public List<SketchLayer> getLayers() {
        return layers;
    }

    public void setLayers(List<SketchLayer> layers) {
        this.layers = layers;
        if (!layers.isEmpty()) {
            nextLayerId = layers.stream().mapToInt(SketchLayer::getId).max().orElse(0) + 1;
            activeLayerId = layers.get(0).getId();
        }
        recalculateBoundingBox();
    }

    public SketchLayer addLayer(String name) {
        SketchLayer layer = new SketchLayer(nextLayerId++, name);
        layers.add(layer);
        return layer;
    }

    public SketchLayer getActiveLayer() {
        return getLayerById(activeLayerId);
    }

    public int getActiveLayerId() {
        return activeLayerId;
    }

    public void setActiveLayerId(int layerId) {
        if (getLayerById(layerId) != null) {
            int previousLayerId = this.activeLayerId;
            this.activeLayerId = layerId;
            
            // Ensure active layer is visible
            SketchLayer activeLayer = getActiveLayer();
            if (activeLayer != null && activeLayer.getVisibility() != SketchLayer.Visibility.SHOWING) {
                activeLayer.setVisibility(SketchLayer.Visibility.SHOWING);
            }
            
            // Record layer switch in undo history
            if (previousLayerId != layerId) {
                LayerSwitchDetail switchDetail = new LayerSwitchDetail(previousLayerId, layerId);
                sketchHistory.add(switchDetail);
                undoneHistory.clear();
                setSaved(false);
            }
        }
    }

    public SketchLayer getLayerById(int id) {
        for (SketchLayer layer : layers) {
            if (layer.getId() == id) {
                return layer;
            }
        }
        return null;
    }

    public void removeLayer(int layerId) {
        if (layers.size() <= 1) {
            return; // Don't remove last layer
        }
        SketchLayer layer = getLayerById(layerId);
        if (layer != null) {
            layers.remove(layer);
            if (activeLayerId == layerId) {
                activeLayerId = layers.get(0).getId();
            }
            recalculateBoundingBox();
        }
    }

    // Backward compatibility methods - delegate to active layer

    public void setPathDetails(List<PathDetail> pathDetails) {
        getActiveLayer().setPathDetails(pathDetails);
        recalculateBoundingBox();
    }

    public void setSymbolDetails(List<SymbolDetail> symbolDetails) {
        getActiveLayer().setSymbolDetails(symbolDetails);
        recalculateBoundingBox();
    }

    public void setTextDetails(List<TextDetail> textDetails) {
        getActiveLayer().setTextDetails(textDetails);
        recalculateBoundingBox();
    }

    public List<PathDetail> getPathDetails() {
        List<PathDetail> all = new ArrayList<>();
        for (SketchLayer layer : layers) {
            all.addAll(layer.getPathDetails());
        }
        return all;
    }

    public List<SymbolDetail> getSymbolDetails() {
        List<SymbolDetail> all = new ArrayList<>();
        for (SketchLayer layer : layers) {
            all.addAll(layer.getSymbolDetails());
        }
        return all;
    }

    public List<TextDetail> getTextDetails() {
        List<TextDetail> all = new ArrayList<>();
        for (SketchLayer layer : layers) {
            all.addAll(layer.getTextDetails());
        }
        return all;
    }

    public List<CrossSectionDetail> getCrossSectionDetails() {
        List<CrossSectionDetail> all = new ArrayList<>();
        for (SketchLayer layer : layers) {
            all.addAll(layer.getCrossSectionDetails());
        }
        return all;
    }

    public void setCrossSectionDetails(List<CrossSectionDetail> crossSectionDetails) {
        getActiveLayer().setCrossSectionDetails(crossSectionDetails);
    }

    // Path handling

    public PathDetail getActivePath() {
        return activePath;
    }

    public PathDetail startNewPath(Coord2D start) {
        activePath = new PathDetail(start, activeColour);
        getActiveLayer().addPathDetail(activePath);
        addSketchDetail(activePath);
        return activePath;
    }

    private void addSketchDetail(SketchDetail sketchDetail) {
        setSaved(false);
        sketchHistory.add(sketchDetail);
        undoneHistory.clear();
        updateBoundingBox(sketchDetail);
    }

    public void finishPath() {
        float epsilon = Space2DUtils.simplificationEpsilon(activePath);
        activePath.setPath(Space2DUtils.simplify(activePath.getPath(), epsilon));
        updateBoundingBox(activePath);
        activePath = null;
    }

    public void addTextDetail(Coord2D location, String text, float size) {
        TextDetail textDetail = new TextDetail(location, text, activeColour, size);
        getActiveLayer().addTextDetail(textDetail);
        addSketchDetail(textDetail);
    }

    public void addSymbolDetail(Coord2D location, Symbol symbol, float size, float angle) {
        SymbolDetail symbolDetail = new SymbolDetail(location, symbol, activeColour, size, angle);
        getActiveLayer().addSymbolDetail(symbolDetail);
        addSketchDetail(symbolDetail);
    }

    public void setActiveColour(Colour colour) {
        this.activeColour = colour;
    }

    // Undo/Redo

    public void undo() {
        if (!sketchHistory.isEmpty()) {
            SketchDetail toUndo = sketchHistory.remove(sketchHistory.size() - 1);

            if (toUndo instanceof LayerSwitchDetail) {
                LayerSwitchDetail switchDetail = (LayerSwitchDetail) toUndo;
                activeLayerId = switchDetail.getFromLayerId();
                SketchLayer layer = getActiveLayer();
                if (layer != null) {
                    layer.setVisibility(SketchLayer.Visibility.SHOWING);
                }
            } else if (toUndo instanceof DeletedDetail) {
                DeletedDetail deletedDetail = (DeletedDetail) toUndo;
                restoreDetailToSketch(deletedDetail.getDeletedDetail());
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

            if (toRedo instanceof LayerSwitchDetail) {
                LayerSwitchDetail switchDetail = (LayerSwitchDetail) toRedo;
                activeLayerId = switchDetail.getToLayerId();
                SketchLayer layer = getActiveLayer();
                if (layer != null) {
                    layer.setVisibility(SketchLayer.Visibility.SHOWING);
                }
            } else if (toRedo instanceof DeletedDetail) {
                DeletedDetail deletedDetail = (DeletedDetail) toRedo;
                removeDetailFromSketch(deletedDetail.getDeletedDetail());
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
        DeletedDetail deletedDetail = new DeletedDetail(sketchDetail, replacementDetails);
        addSketchDetail(deletedDetail);
        removeDetailFromSketch(sketchDetail);
        for (SketchDetail replacementDetail : replacementDetails) {
            restoreDetailToSketch(replacementDetail);
        }
    }

    private void removeDetailFromSketch(SketchDetail sketchDetail) {
        for (SketchLayer layer : layers) {
            if (layer.removeDetail(sketchDetail)) {
                break;
            }
        }
        recalculateBoundingBox();
    }

    public void restoreDetailToSketch(SketchDetail sketchDetail) {
        getActiveLayer().restoreDetail(sketchDetail);
        updateBoundingBox(sketchDetail);
    }

    // Snap point finding (across all visible layers)

    public Coord2D findEligibleSnapPointWithin(Coord2D point, float delta) {
        Coord2D closest = null;
        float minDistance = Float.MAX_VALUE;

        for (SketchLayer layer : layers) {
            if (layer.getVisibility() == SketchLayer.Visibility.HIDDEN) {
                continue;
            }
            for (PathDetail path : layer.getPathDetails()) {
                if (activePath == path) {
                    continue;
                }

                Coord2D start = path.getPath().get(0);
                Coord2D end = path.getPath().get(path.getPath().size() - 1);
                for (Coord2D coord2D : new Coord2D[]{start, end}) {
                    float distance = Space2DUtils.getDistance(point, coord2D);
                    if (distance < delta && distance < minDistance) {
                        closest = coord2D;
                        minDistance = distance;
                    }
                }
            }
        }
        return closest;
    }

    private List<SketchDetail> allSketchDetails() {
        List<SketchDetail> all = new ArrayList<>();
        for (SketchLayer layer : layers) {
            all.addAll(layer.getAllDetails());
        }
        return all;
    }

    public SketchDetail findNearestDetailWithin(Coord2D point, float delta) {
        SketchDetail closest = null;
        float minDistance = Float.MAX_VALUE;

        for (SketchDetail detail : allSketchDetails()) {
            float distance = detail.getDistanceFrom(point);
            if (distance < delta && distance < minDistance) {
                closest = detail;
                minDistance = distance;
            }
        }

        return closest;
    }

    // Cross-section handling

    public void addCrossSection(CrossSection crossSection, Coord2D touchPointOnSurvey) {
        CrossSectionDetail sectionDetail = new CrossSectionDetail(crossSection, touchPointOnSurvey);
        getActiveLayer().addCrossSectionDetail(sectionDetail);
        addSketchDetail(sectionDetail);
    }

    public CrossSectionDetail getCrossSectionDetail(Station station) {
        for (SketchLayer layer : layers) {
            for (CrossSectionDetail detail : layer.getCrossSectionDetails()) {
                CrossSection crossSection = detail.getCrossSection();
                if (crossSection.getStation() == station) {
                    return detail;
                }
            }
        }
        return null;
    }

    // Copy/translate

    public Sketch getTranslatedCopy(Coord2D point) {
        Sketch sketch = new Sketch();
        sketch.layers.clear(); // Remove default layer

        for (SketchLayer layer : layers) {
            SketchLayer newLayer = sketch.addLayer(layer.getName());
            newLayer.setVisibility(layer.getVisibility());

            List<PathDetail> newPathDetails = new ArrayList<>();
            for (PathDetail pathDetail : layer.getPathDetails()) {
                newPathDetails.add(pathDetail.translate(point));
            }
            newLayer.setPathDetails(newPathDetails);

            List<SymbolDetail> newSymbolDetails = new ArrayList<>();
            for (SymbolDetail symbolDetail : layer.getSymbolDetails()) {
                newSymbolDetails.add(symbolDetail.translate(point));
            }
            newLayer.setSymbolDetails(newSymbolDetails);

            List<TextDetail> newTextDetails = new ArrayList<>();
            for (TextDetail textDetail : layer.getTextDetails()) {
                newTextDetails.add(textDetail.translate(point));
            }
            newLayer.setTextDetails(newTextDetails);

            List<CrossSectionDetail> newCrossSectionDetails = new ArrayList<>();
            for (CrossSectionDetail crossSectionDetail : layer.getCrossSectionDetails()) {
                newCrossSectionDetails.add(crossSectionDetail.translate(point));
            }
            newLayer.setCrossSectionDetails(newCrossSectionDetails);
        }

        return sketch;
    }

    public void recalculateBoundingBox() {
        resetBoundingBox();
        for (SketchDetail sketchDetail : allSketchDetails()) {
            updateBoundingBox(sketchDetail);
        }
    }
}
