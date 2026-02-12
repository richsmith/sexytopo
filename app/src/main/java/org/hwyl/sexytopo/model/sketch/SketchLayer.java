package org.hwyl.sexytopo.model.sketch;

import java.util.ArrayList;
import java.util.List;

public class SketchLayer {

    public enum Visibility {
        SHOWING,
        FADED,
        HIDDEN
    }

    private int id;
    private String name;
    private Visibility visibility = Visibility.SHOWING;

    private List<PathDetail> pathDetails = new ArrayList<>();
    private List<SymbolDetail> symbolDetails = new ArrayList<>();
    private List<TextDetail> textDetails = new ArrayList<>();
    private List<CrossSectionDetail> crossSectionDetails = new ArrayList<>();

    public SketchLayer(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }

    public void cycleVisibility() {
        switch (visibility) {
            case SHOWING:
                visibility = Visibility.FADED;
                break;
            case FADED:
                visibility = Visibility.HIDDEN;
                break;
            case HIDDEN:
                visibility = Visibility.SHOWING;
                break;
        }
    }

    public List<PathDetail> getPathDetails() {
        return pathDetails;
    }

    public void setPathDetails(List<PathDetail> pathDetails) {
        this.pathDetails = pathDetails;
    }

    public List<SymbolDetail> getSymbolDetails() {
        return symbolDetails;
    }

    public void setSymbolDetails(List<SymbolDetail> symbolDetails) {
        this.symbolDetails = symbolDetails;
    }

    public List<TextDetail> getTextDetails() {
        return textDetails;
    }

    public void setTextDetails(List<TextDetail> textDetails) {
        this.textDetails = textDetails;
    }

    public List<CrossSectionDetail> getCrossSectionDetails() {
        return crossSectionDetails;
    }

    public void setCrossSectionDetails(List<CrossSectionDetail> crossSectionDetails) {
        this.crossSectionDetails = crossSectionDetails;
    }

    public void addPathDetail(PathDetail pathDetail) {
        pathDetails.add(pathDetail);
    }

    public void addSymbolDetail(SymbolDetail symbolDetail) {
        symbolDetails.add(symbolDetail);
    }

    public void addTextDetail(TextDetail textDetail) {
        textDetails.add(textDetail);
    }

    public void addCrossSectionDetail(CrossSectionDetail crossSectionDetail) {
        crossSectionDetails.add(crossSectionDetail);
    }

    public List<SketchDetail> getAllDetails() {
        List<SketchDetail> all = new ArrayList<>();
        all.addAll(pathDetails);
        all.addAll(symbolDetails);
        all.addAll(textDetails);
        all.addAll(crossSectionDetails);
        return all;
    }

    public boolean removeDetail(SketchDetail detail) {
        if (detail instanceof PathDetail) {
            return pathDetails.remove(detail);
        } else if (detail instanceof SymbolDetail) {
            return symbolDetails.remove(detail);
        } else if (detail instanceof TextDetail) {
            return textDetails.remove(detail);
        } else if (detail instanceof CrossSectionDetail) {
            return crossSectionDetails.remove(detail);
        }
        return false;
    }

    public void restoreDetail(SketchDetail detail) {
        if (detail instanceof PathDetail) {
            pathDetails.add((PathDetail) detail);
        } else if (detail instanceof SymbolDetail) {
            symbolDetails.add((SymbolDetail) detail);
        } else if (detail instanceof TextDetail) {
            textDetails.add((TextDetail) detail);
        } else if (detail instanceof CrossSectionDetail) {
            crossSectionDetails.add((CrossSectionDetail) detail);
        }
    }
}
