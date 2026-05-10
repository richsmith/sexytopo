package org.hwyl.sexytopo.control.io.thirdparty.svg;

/** Options for SVG export, typically set via a dialog before export. */
public class SvgExportOptions {

    private boolean whiteBackground;
    private boolean showLegend = true;
    private boolean showNorthArrow = true;
    private boolean showScaleBar = true;
    private boolean showTeam = true;
    private boolean showCrossSections = true;
    private boolean showSymbols = true;
    private boolean showCentreline = true;
    private boolean showStations = true;
    private boolean showSplays = true;
    private boolean showGrid = false;
    private boolean showTagline = true;

    public SvgExportOptions() {}

    public SvgExportOptions(
            boolean whiteBackground,
            boolean showLegend,
            boolean showNorthArrow,
            boolean showScaleBar,
            boolean showTeam,
            boolean showCrossSections,
            boolean showSymbols,
            boolean showCentreline,
            boolean showStations,
            boolean showSplays,
            boolean showGrid,
            boolean showTagline) {
        this.whiteBackground = whiteBackground;
        this.showLegend = showLegend;
        this.showNorthArrow = showNorthArrow;
        this.showScaleBar = showScaleBar;
        this.showTeam = showTeam;
        this.showCrossSections = showCrossSections;
        this.showSymbols = showSymbols;
        this.showCentreline = showCentreline;
        this.showStations = showStations;
        this.showSplays = showSplays;
        this.showGrid = showGrid;
        this.showTagline = showTagline;
    }

    public boolean isWhiteBackground() {
        return whiteBackground;
    }

    public boolean isShowLegend() {
        return showLegend;
    }

    public boolean isShowNorthArrow() {
        return showNorthArrow;
    }

    public boolean isShowScaleBar() {
        return showScaleBar;
    }

    public boolean isShowTeam() {
        return showTeam;
    }

    public boolean isShowCrossSections() {
        return showCrossSections;
    }

    public boolean isShowSymbols() {
        return showSymbols;
    }

    public boolean isShowCentreline() {
        return showCentreline;
    }

    public boolean isShowStations() {
        return showStations;
    }

    public boolean isShowSplays() {
        return showSplays;
    }

    public boolean isShowGrid() {
        return showGrid;
    }

    public boolean isShowTagline() {
        return showTagline;
    }
}
