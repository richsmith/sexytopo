package org.hwyl.sexytopo.control.io.thirdparty.svg;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Xml;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Spinner;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.io.basic.ExportFrameFactory;
import org.hwyl.sexytopo.control.io.translation.DoubleSketchFileExporter;
import org.hwyl.sexytopo.control.util.GeneralPreferences;
import org.hwyl.sexytopo.control.util.Space2DUtils;
import org.hwyl.sexytopo.control.util.SurveyStats;
import org.hwyl.sexytopo.control.util.TextTools;
import org.hwyl.sexytopo.model.common.Frame;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.Line;
import org.hwyl.sexytopo.model.graph.Projection2D;
import org.hwyl.sexytopo.model.graph.Space;
import org.hwyl.sexytopo.model.sketch.Colour;
import org.hwyl.sexytopo.model.sketch.CrossSectionDetail;
import org.hwyl.sexytopo.model.sketch.PathDetail;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.sketch.SketchDetail;
import org.hwyl.sexytopo.model.sketch.Symbol;
import org.hwyl.sexytopo.model.sketch.SymbolDetail;
import org.hwyl.sexytopo.model.sketch.TextDetail;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.model.survey.Trip;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xmlpull.v1.XmlSerializer;

/**
 * @noinspection WrapperTypeMayBePrimitive
 */
@SuppressWarnings({"UnnecessaryLocalVariable", "SameParameterValue"})
public class SvgExporter extends DoubleSketchFileExporter {

    public static final int SCALE = 50;
    public static final int STATION_FONT = 15;
    public static final int BORDER = 10;

    private SvgExportOptions exportOptions;

    private SvgExportOptions getOrLoadOptions() {
        if (exportOptions == null) {
            exportOptions =
                    new SvgExportOptions(
                            GeneralPreferences.getExportSvgBackgroundColour() == Colour.WHITE,
                            GeneralPreferences.isExportSvgLegendEnabled(),
                            GeneralPreferences.isExportSvgNorthArrowEnabled(),
                            GeneralPreferences.isExportSvgScaleBarEnabled(),
                            GeneralPreferences.isExportSvgTeamEnabled(),
                            GeneralPreferences.isExportSvgCrossSectionsEnabled(),
                            GeneralPreferences.isExportSvgSymbolsEnabled(),
                            GeneralPreferences.isExportSvgCentrelineEnabled(),
                            GeneralPreferences.isExportSvgStationsEnabled(),
                            GeneralPreferences.isExportSvgSplaysEnabled(),
                            GeneralPreferences.isExportSvgGridEnabled(),
                            GeneralPreferences.isExportSvgTaglineEnabled());
        }
        return exportOptions;
    }

    @Override
    public String getContent(Survey survey, Projection2D projectionType) throws Exception {
        SvgExportOptions options = getOrLoadOptions();

        Sketch sketch = survey.getSketch(projectionType);
        Space<Coord2D> projection = projectionType.project(survey);

        Frame contentFrame = ExportFrameFactory.getExportFrame(survey, projectionType).scale(SCALE);
        Frame frame =
                ExportFrameFactory.addBorder(
                                ExportFrameFactory.getExportFrame(survey, projectionType))
                        .scale(SCALE);

        // Reserve a strip beneath the sketch for the legend so it can never overlap content.
        // The legend is left-aligned with the sketch content (not the page edge) and separated
        // from the sketch above and the page edge below by a margin.
        LegendModel legendModel =
                options.isShowLegend()
                        ? buildLegendModel(survey, projectionType, frame, SCALE, options)
                        : null;
        double legendLeftX = contentFrame.getLeft();
        double legendTopY = contentFrame.getBottom() + STATION_FONT * 2.0;
        if (legendModel != null) {
            double newBottom = legendTopY + legendModel.totalHeight + STATION_FONT * 2.0;
            frame =
                    new Frame(
                            frame.getLeft(),
                            frame.getRight(),
                            frame.getTop(),
                            (float) Math.max(frame.getBottom(), newBottom));
        }

        double svgWidth = frame.getWidth();
        double svgHeight = frame.getHeight();
        Coord2D topLeft = frame.getTopLeft();
        double svgTopLeftX = topLeft.x;
        double svgTopLeftY = topLeft.y;

        XmlSerializer xmlSerializer = Xml.newSerializer();
        StringWriter writer = new StringWriter();
        xmlSerializer.setOutput(writer);

        xmlSerializer.startDocument("UTF-8", true);

        xmlSerializer.startTag(null, "svg");

        xmlSerializer.attribute(null, "width", Double.toString(svgWidth));
        xmlSerializer.attribute(null, "height", Double.toString(svgHeight));
        xmlSerializer.attribute(
                null,
                "viewBox",
                TextTools.join(" ", svgTopLeftX, svgTopLeftY, svgWidth, svgHeight));
        xmlSerializer.attribute(null, "xmlns", "http://www.w3.org/2000/svg");

        Colour background = options.isWhiteBackground() ? Colour.WHITE : Colour.TRANSPARENT;
        if (background != Colour.TRANSPARENT) {
            xmlSerializer.startTag("", "g");
            xmlSerializer.attribute("", "id", "background");
            xmlSerializer.startTag(null, "rect");
            xmlSerializer.attribute(null, "x", Double.toString(svgTopLeftX));
            xmlSerializer.attribute(null, "y", Double.toString(svgTopLeftY));
            xmlSerializer.attribute(null, "width", Double.toString(svgWidth));
            xmlSerializer.attribute(null, "height", Double.toString(svgHeight));
            xmlSerializer.attribute(null, "fill", background.toString());
            xmlSerializer.endTag(null, "rect");
            xmlSerializer.endTag("", "g");
        }

        if (options.isShowGrid()) {
            xmlSerializer.startTag("", "g");
            xmlSerializer.attribute("", "id", "grid");
            writeGrid(xmlSerializer, contentFrame, SCALE);
            xmlSerializer.endTag("", "g");
        }

        xmlSerializer.startTag("", "g");
        xmlSerializer.attribute("", "id", "sketch");
        writeSketch(xmlSerializer, sketch, SCALE, options.isShowSymbols());
        xmlSerializer.endTag("", "g");

        if (options.isShowCrossSections()) {
            xmlSerializer.startTag("", "g");
            xmlSerializer.attribute("", "id", "cross-sections");
            writeCrossSections(xmlSerializer, sketch, projection, SCALE, options.isShowSymbols());
            xmlSerializer.endTag("", "g");
        }

        xmlSerializer.startTag("", "g");
        xmlSerializer.attribute("", "id", "data");

        if (options.isShowCentreline()) {
            xmlSerializer.startTag("", "g");
            xmlSerializer.attribute("", "id", "centreline");
            writeCentrelineLegs(xmlSerializer, projection, SCALE);
            xmlSerializer.endTag("", "g");
        }

        if (options.isShowSplays()) {
            xmlSerializer.startTag("", "g");
            xmlSerializer.attribute("", "id", "splays");
            writeSplayData(xmlSerializer, projection, SCALE);
            xmlSerializer.endTag("", "g");
        }

        if (options.isShowStations()) {
            xmlSerializer.startTag("", "g");
            xmlSerializer.attribute("", "id", "stations");
            writeStations(xmlSerializer, projection, SCALE);
            xmlSerializer.endTag("", "g");
        }

        xmlSerializer.endTag("", "g");

        if (legendModel != null) {
            writeLegend(xmlSerializer, legendModel, legendLeftX, legendTopY);
        }

        xmlSerializer.endTag(null, "svg");
        xmlSerializer.endDocument();

        String text = writer.toString();
        text = unescapeTagsHack(text);
        text = prettyPrintXML(text);

        return text;
    }

    /** Need this because idiot library developers don't provide an option to override escaping */
    public String unescapeTagsHack(String text) {

        text = text.replaceAll("&lt;", "<");
        text = text.replaceAll("&gt;", ">");
        return text;
    }

    /** Pre-computed legend layout: knows its total height so the frame can be expanded for it. */
    private static final class LegendModel {
        final String title;
        final List<String> bodyLines;
        final double barLengthMetres;
        final double barLengthPx;
        final int strokeWidth;
        final int scale;
        final boolean isPlan;
        final boolean showNorthArrow;
        final boolean showScaleBar;
        final boolean showTagline;

        final double titleFont = STATION_FONT * 1.6;
        final double bodyFont = STATION_FONT;
        final double scaleLabelFont = bodyFont * 0.8;
        final double taglineFont = bodyFont * 0.75;
        final double tickHeight = scaleLabelFont * 0.6;
        final double lineGap = bodyFont * 1.7;
        final double sectionGap = bodyFont * 0.8;
        final double preScaleBarGap = sectionGap * 1.5;
        final double arrowSize = STATION_FONT * 9.0;
        final double topPadding = bodyFont * 0.6;
        final double bottomPadding = bodyFont * 0.6;

        final double titleY;
        final double[] bodyYs;
        final double taglineY;
        final double barTopY;
        final double barBaselineY;
        final double scaleLabelY;
        final double arrowCentreX;
        final double arrowTopY;
        final double arrowBottomY;
        final double totalHeight;

        LegendModel(
                String title,
                List<String> bodyLines,
                double barLengthMetres,
                int scale,
                boolean isPlan,
                boolean showNorthArrow,
                boolean showScaleBar,
                boolean showTagline) {
            this.title = title;
            this.bodyLines = bodyLines;
            this.barLengthMetres = barLengthMetres;
            this.barLengthPx = barLengthMetres * scale;
            this.strokeWidth = Math.max(1, scale / 40);
            this.scale = scale;
            this.isPlan = isPlan;
            this.showNorthArrow = showNorthArrow && isPlan;
            this.showScaleBar = showScaleBar;
            this.showTagline = showTagline;

            double cursorY = topPadding;

            cursorY += titleFont;
            this.titleY = cursorY;
            cursorY += sectionGap;

            this.bodyYs = new double[bodyLines.size()];
            for (int i = 0; i < bodyLines.size(); i++) {
                cursorY += lineGap;
                this.bodyYs[i] = cursorY;
            }

            if (this.showTagline) {
                cursorY += sectionGap;
                cursorY += taglineFont;
                this.taglineY = cursorY;
            } else {
                this.taglineY = 0;
            }

            if (this.showScaleBar) {
                cursorY += preScaleBarGap;
                this.barTopY = cursorY;
                this.barBaselineY = barTopY + tickHeight;
                this.scaleLabelY = barBaselineY + scaleLabelFont;
                cursorY = scaleLabelY + bottomPadding;
            } else {
                this.barTopY = 0;
                this.barBaselineY = 0;
                this.scaleLabelY = 0;
                cursorY += bottomPadding;
            }

            // North arrow lives in the top-right; reserve its full vertical extent independently
            // so it doesn't push the body/tagline/scale-bar layout.
            this.arrowCentreX = Math.max(barLengthPx, titleFont * 8) + arrowSize;
            this.arrowTopY = topPadding;
            this.arrowBottomY = arrowTopY + arrowSize;
            if (this.showNorthArrow) {
                double arrowExtent = arrowBottomY + bodyFont + bottomPadding;
                cursorY = Math.max(cursorY, arrowExtent);
            }

            this.totalHeight = cursorY;
        }
    }

    private static LegendModel buildLegendModel(
            Survey survey,
            Projection2D projectionType,
            Frame frame,
            int scale,
            SvgExportOptions options) {
        double surveyWidthMetres = frame.getWidth() / (double) scale;
        if (surveyWidthMetres <= 0) {
            return null;
        }
        Trip trip = survey.getTrip();
        boolean isPlan = projectionType == Projection2D.PLAN;

        List<String> bodyLines = new ArrayList<>();
        if (trip != null && trip.getSurveyDate() != null) {
            bodyLines.add(formatLocaleDate(trip.getSurveyDate()));
        }
        if (options.isShowTeam()) {
            String teamLine = formatTeamNames(trip);
            if (!teamLine.isEmpty()) {
                bodyLines.add("Surveyed By: " + teamLine);
            }
        }
        bodyLines.add(formatStatsLine(survey));

        double barLengthMetres = pickScaleBarLength(surveyWidthMetres);
        return new LegendModel(
                survey.getName(),
                bodyLines,
                barLengthMetres,
                scale,
                isPlan,
                options.isShowNorthArrow(),
                options.isShowScaleBar(),
                options.isShowTagline());
    }

    private static void writeLegend(
            XmlSerializer xmlSerializer, LegendModel model, double frameLeft, double stripTop)
            throws IOException {
        double translateX = frameLeft;
        double translateY = stripTop;

        xmlSerializer.startTag("", "g");
        xmlSerializer.attribute("", "id", "legend");
        xmlSerializer.attribute(
                "", "transform", "translate(" + translateX + "," + translateY + ")");
        xmlSerializer.attribute("", "font-family", "sans-serif");
        xmlSerializer.attribute("", "fill", "black");

        // Title.
        writeLegendText(xmlSerializer, 0, model.titleY, model.titleFont, "bold", model.title);

        // Body lines (date, surveyors, stats).
        for (int i = 0; i < model.bodyLines.size(); i++) {
            writeLegendText(
                    xmlSerializer,
                    0,
                    model.bodyYs[i],
                    model.bodyFont,
                    "normal",
                    model.bodyLines.get(i));
        }

        // North arrow (plan view only).
        if (model.showNorthArrow) {
            writeNorthArrow(
                    xmlSerializer,
                    model.arrowCentreX,
                    model.arrowTopY,
                    model.arrowBottomY,
                    model.strokeWidth,
                    model.bodyFont);
        }

        // Tagline.
        if (model.showTagline) {
            writeLegendText(
                    xmlSerializer,
                    0,
                    model.taglineY,
                    model.taglineFont,
                    "italic",
                    "Surveyed with SexyTopo");
        }

        // Scale bar at the bottom.
        if (model.showScaleBar) {
            xmlSerializer.startTag("", "g");
            xmlSerializer.attribute("", "id", "scale-bar");
            xmlSerializer.attribute("", "stroke", "black");
            xmlSerializer.attribute("", "stroke-width", Integer.toString(model.strokeWidth));
            xmlSerializer.attribute("", "fill", "none");
            xmlSerializer.startTag("", "polyline");
            String barPoints =
                    TextTools.join(
                            " ",
                            0 + "," + model.barTopY,
                            0 + "," + model.barBaselineY,
                            model.barLengthPx + "," + model.barBaselineY,
                            model.barLengthPx + "," + model.barTopY);
            xmlSerializer.attribute("", "points", barPoints);
            xmlSerializer.endTag("", "polyline");
            xmlSerializer.endTag("", "g");

            writeLegendText(
                    xmlSerializer,
                    model.barLengthPx / 2.0,
                    model.scaleLabelY,
                    model.scaleLabelFont,
                    "normal",
                    "middle",
                    formatScaleBarLabel(model.barLengthMetres));
        }

        xmlSerializer.endTag("", "g");
    }

    private static void writeLegendText(
            XmlSerializer xmlSerializer,
            double x,
            double y,
            double fontSize,
            String style,
            String text)
            throws IOException {
        writeLegendText(xmlSerializer, x, y, fontSize, style, "start", text);
    }

    private static void writeLegendText(
            XmlSerializer xmlSerializer,
            double x,
            double y,
            double fontSize,
            String style,
            String anchor,
            String text)
            throws IOException {
        xmlSerializer.startTag("", "text");
        xmlSerializer.attribute("", "x", Double.toString(x));
        xmlSerializer.attribute("", "y", Double.toString(y));
        xmlSerializer.attribute("", "font-size", Double.toString(fontSize));
        if ("bold".equals(style)) {
            xmlSerializer.attribute("", "font-weight", "bold");
        } else if ("italic".equals(style)) {
            xmlSerializer.attribute("", "font-style", "italic");
        }
        if (!"start".equals(anchor)) {
            xmlSerializer.attribute("", "text-anchor", anchor);
        }
        xmlSerializer.text(text);
        xmlSerializer.endTag("", "text");
    }

    private static void writeNorthArrow(
            XmlSerializer xmlSerializer,
            double centreX,
            double topY,
            double bottomY,
            int strokeWidth,
            double labelFont)
            throws IOException {
        double height = bottomY - topY;
        // Slim arrow: triangular head + rectangular shaft.
        xmlSerializer.startTag("", "g");
        xmlSerializer.attribute("", "id", "north-arrow");
        xmlSerializer.attribute("", "stroke", "black");
        xmlSerializer.attribute("", "stroke-width", Integer.toString(strokeWidth));
        xmlSerializer.attribute("", "fill", "black");

        double headHeight = height * 0.28;
        double headHalfWidth = height * 0.08;
        xmlSerializer.startTag("", "polygon");
        String headPoints =
                TextTools.join(
                        " ",
                        centreX + "," + topY,
                        (centreX - headHalfWidth) + "," + (topY + headHeight),
                        (centreX + headHalfWidth) + "," + (topY + headHeight));
        xmlSerializer.attribute("", "points", headPoints);
        xmlSerializer.endTag("", "polygon");

        xmlSerializer.startTag("", "polyline");
        String shaftPoints =
                TextTools.join(" ", centreX + "," + (topY + headHeight), centreX + "," + bottomY);
        xmlSerializer.attribute("", "points", shaftPoints);
        xmlSerializer.attribute("", "fill", "none");
        xmlSerializer.endTag("", "polyline");

        xmlSerializer.endTag("", "g");

        writeLegendText(
                xmlSerializer, centreX, bottomY + labelFont, labelFont, "bold", "middle", "N");
    }

    private static String formatLocaleDate(Date date) {
        return DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(date);
    }

    private static String formatTeamNames(Trip trip) {
        if (trip == null) {
            return "";
        }
        List<String> names = new ArrayList<>();
        for (Trip.TeamEntry entry : trip.getTeam()) {
            if (entry.name != null && !entry.name.trim().isEmpty()) {
                names.add(entry.name.trim());
            }
        }
        return TextTools.join(", ", names);
    }

    private static String formatStatsLine(Survey survey) {
        long length = Math.round(SurveyStats.calcTotalLength(survey));
        long height = Math.round(SurveyStats.calcHeightRange(survey));
        return String.format(Locale.getDefault(), "L: %d m, H: %d m", length, height);
    }

    /** Picks a round bar length (1, 2, 5, 10, ... m) close to an eighth of the survey width. */
    static double pickScaleBarLength(double surveyWidthMetres) {
        double target = surveyWidthMetres / 8.0;
        double[] mantissas = {1, 2, 5};
        double exponent = Math.floor(Math.log10(Math.max(target, 1e-6)));
        double base = Math.pow(10, exponent);
        double best = base;
        for (double m : mantissas) {
            double candidate = m * base;
            if (candidate <= target) {
                best = candidate;
            }
        }
        return best;
    }

    private static String formatScaleBarLabel(double metres) {
        if (metres >= 1) {
            return ((long) metres) + " m";
        }
        if (metres >= 0.01) {
            return ((long) Math.round(metres * 100)) + " cm";
        }
        return metres + " m";
    }

    /**
     * Draws a faint grid at the scale-bar interval, snapped to whole metres, covering the sketch
     * content frame. Sits behind the sketch so it never obscures content.
     */
    private static void writeGrid(XmlSerializer xmlSerializer, Frame contentFrame, int scale)
            throws IOException {
        double widthMetres = contentFrame.getWidth() / (double) scale;
        if (widthMetres <= 0) {
            return;
        }
        double spacingMetres = pickScaleBarLength(widthMetres);
        if (spacingMetres <= 0) {
            return;
        }
        double spacingPx = spacingMetres * scale;

        double left = contentFrame.getLeft();
        double right = contentFrame.getRight();
        double top = contentFrame.getTop();
        double bottom = contentFrame.getBottom();

        // Snap origin to whole multiples of spacing.
        double startX = Math.ceil(left / spacingPx) * spacingPx;
        double startY = Math.ceil(top / spacingPx) * spacingPx;
        int gridStrokeWidth = Math.max(1, scale / 50);

        for (double x = startX; x <= right; x += spacingPx) {
            xmlSerializer.startTag("", "line");
            xmlSerializer.attribute("", "x1", Double.toString(x));
            xmlSerializer.attribute("", "y1", Double.toString(top));
            xmlSerializer.attribute("", "x2", Double.toString(x));
            xmlSerializer.attribute("", "y2", Double.toString(bottom));
            xmlSerializer.attribute("", "stroke", "#cccccc");
            xmlSerializer.attribute("", "stroke-width", Integer.toString(gridStrokeWidth));
            xmlSerializer.endTag("", "line");
        }
        for (double y = startY; y <= bottom; y += spacingPx) {
            xmlSerializer.startTag("", "line");
            xmlSerializer.attribute("", "x1", Double.toString(left));
            xmlSerializer.attribute("", "y1", Double.toString(y));
            xmlSerializer.attribute("", "x2", Double.toString(right));
            xmlSerializer.attribute("", "y2", Double.toString(y));
            xmlSerializer.attribute("", "stroke", "#cccccc");
            xmlSerializer.attribute("", "stroke-width", Integer.toString(gridStrokeWidth));
            xmlSerializer.endTag("", "line");
        }
    }

    private static void writeSketch(
            XmlSerializer xmlSerializer, Sketch sketch, int scale, boolean showSymbols)
            throws Exception {

        if (showSymbols) {
            Set<Symbol> usedSymbols = new HashSet<>();
            for (SymbolDetail symbolDetail : sketch.getSymbolDetails()) {
                usedSymbols.add(symbolDetail.getSymbol());
            }
            for (Symbol symbol : usedSymbols) {
                writeSymbolRef(xmlSerializer, symbol);
            }
        }

        for (PathDetail pathDetail : sketch.getPathDetails()) {
            writePathDetail(xmlSerializer, pathDetail, scale);
        }

        for (TextDetail textDetail : sketch.getTextDetails()) {
            writeTextDetail(xmlSerializer, textDetail, scale);
        }

        if (showSymbols) {
            for (SymbolDetail symbolDetail : sketch.getSymbolDetails()) {
                writeSymbolDetail(xmlSerializer, symbolDetail, scale);
            }
        }
    }

    private static void writeCrossSections(
            XmlSerializer xmlSerializer,
            Sketch sketch,
            Space<Coord2D> projection,
            int scale,
            boolean showSymbols)
            throws Exception {
        float xsScale = sketch.getCrossSectionScale();
        for (CrossSectionDetail xsDetail : sketch.getCrossSectionDetails()) {
            Station station = xsDetail.getCrossSection().getStation();

            xmlSerializer.startTag("", "g");
            xmlSerializer.attribute("", "id", "xs-" + station.getName());

            // Write scaled projection legs (splays)
            Space<Coord2D> rawProjection = xsDetail.getCrossSection().getProjection();
            Space<Coord2D> scaledProjection = rawProjection.scale(xsScale);
            Space<Coord2D> translatedProjection =
                    Space2DUtils.translate(scaledProjection, xsDetail.getPosition());
            Integer splayStrokeWidth = GeneralPreferences.getExportSvgSplayStrokeWidth();
            for (Line<Coord2D> line : translatedProjection.getLegMap().values()) {
                xmlSerializer.startTag("", "polyline");
                String points =
                        TextTools.join(
                                ",",
                                scale * line.getStart().x,
                                scale * line.getStart().y,
                                scale * line.getEnd().x,
                                scale * line.getEnd().y);
                xmlSerializer.attribute("", "points", points);
                xmlSerializer.attribute("", "stroke", "red");
                xmlSerializer.attribute("", "stroke-width", splayStrokeWidth.toString());
                xmlSerializer.attribute("", "fill", "none");
                xmlSerializer.endTag("", "polyline");
            }

            // Write sub-sketch paths scaled and translated to position
            Sketch subSketch =
                    xsDetail.getSketch().scale(xsScale).translate(xsDetail.getPosition());
            for (PathDetail pathDetail : subSketch.getPathDetails()) {
                writePathDetail(xmlSerializer, pathDetail, scale);
            }
            for (TextDetail textDetail : subSketch.getTextDetails()) {
                writeTextDetail(xmlSerializer, textDetail, scale);
            }
            if (showSymbols) {
                for (SymbolDetail symbolDetail : subSketch.getSymbolDetails()) {
                    writeSymbolDetail(xmlSerializer, symbolDetail, scale);
                }
            }

            xmlSerializer.endTag("", "g");
        }
    }

    private static void writePathDetail(
            XmlSerializer xmlSerializer, PathDetail pathDetail, int scale) throws IOException {
        Integer strokeWidth = GeneralPreferences.getExportSvgStrokeWidth();
        List<String> coordStrings = new ArrayList<>();
        for (Coord2D coord2D : pathDetail.getPath()) {
            coordStrings.add(toXmlText(coord2D, scale));
        }
        xmlSerializer.startTag(null, "polyline");
        xmlSerializer.attribute(null, "points", TextTools.join(" ", coordStrings));
        xmlSerializer.attribute(null, "stroke", getSvgColour(pathDetail));
        xmlSerializer.attribute(null, "stroke-width", strokeWidth.toString());
        xmlSerializer.attribute(null, "fill", "none");
        xmlSerializer.endTag(null, "polyline");
    }

    private static String toXmlText(Coord2D coord2D, int scale) {
        return coord2D.x * scale + "," + coord2D.y * scale;
    }

    private static void writeTextDetail(
            XmlSerializer xmlSerializer, TextDetail textDetail, int scale) throws IOException {
        xmlSerializer.startTag(null, "text");
        Coord2D coord2D = textDetail.getPosition();
        double x = coord2D.x * scale;
        double y = coord2D.y * scale;
        xmlSerializer.attribute(null, "x", Double.toString(x));
        xmlSerializer.attribute(null, "y", Double.toString(y));
        xmlSerializer.attribute(null, "font-size", Float.toString(textDetail.getSize() * scale));
        xmlSerializer.attribute(null, "stroke", getSvgColour(textDetail));
        xmlSerializer.text(textDetail.getText());
        xmlSerializer.endTag(null, "text");
    }

    private static void writeSymbolDetail(
            XmlSerializer xmlSerializer, SymbolDetail symbolDetail, int scale) throws IOException {
        Symbol symbol = symbolDetail.getSymbol();
        xmlSerializer.startTag("", "use");
        xmlSerializer.attribute("", "href", "#" + symbol.getSvgRefId());

        Float size = symbolDetail.getSize() * scale;
        xmlSerializer.attribute("", "width", size.toString());
        xmlSerializer.attribute("", "height", size.toString());
        Coord2D position = symbolDetail.getPosition();
        float centreX = position.x * scale;
        float centreY = position.y * scale;
        float offsetX = centreX - size / 2f;
        float offsetY = centreY - size / 2f;
        xmlSerializer.attribute("", "x", Double.toString(offsetX));
        xmlSerializer.attribute("", "y", Double.toString(offsetY));
        xmlSerializer.attribute("", "color", getSvgColour(symbolDetail));

        if (symbol.isDirectional()) {
            xmlSerializer.attribute(
                    "",
                    "transform",
                    "rotate(" + symbolDetail.getAngle() + "," + centreX + "," + centreY + ")");
        }

        xmlSerializer.endTag("", "use");
    }

    private static void writeSymbolRef(XmlSerializer xmlSerializer, Symbol symbol)
            throws Exception {
        String svgContent = symbol.asRawSvg();

        // this is super-hacky and fragile... how to do it properly?
        String innerSvgContent =
                svgContent
                        .replace(
                                "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 40 40\">",
                                "")
                        .replace("</svg>", "")
                        .trim();

        xmlSerializer.startTag("", "symbol");
        xmlSerializer.attribute("", "id", symbol.getSvgRefId());
        xmlSerializer.attribute("", "viewBox", "0 0 40 40");

        xmlSerializer.flush();
        xmlSerializer.text(innerSvgContent);

        xmlSerializer.endTag("", "symbol");
    }

    private static void writeCentrelineLegs(
            XmlSerializer xmlSerializer, Space<Coord2D> projection, int scale) throws IOException {
        Map<Station, Coord2D> stationMap = projection.getStationMap();
        Map<Leg, Line<Coord2D>> legMap = projection.getLegMap();
        Integer legStrokeWidth = GeneralPreferences.getExportSvgLegStrokeWidth();

        for (Station station : stationMap.keySet()) {
            for (Leg leg : station.getOnwardLegs()) {
                if (leg.hasDestination()) {
                    Line<Coord2D> line = legMap.get(leg);
                    Station destination = leg.getDestination();
                    String legId = station.getName() + "-" + destination.getName();
                    writeLeg(xmlSerializer, line, legId, scale, legStrokeWidth);
                }
            }
        }
    }

    private static void writeStations(
            XmlSerializer xmlSerializer, Space<Coord2D> projection, int scale) throws IOException {
        Map<Station, Coord2D> stationMap = projection.getStationMap();
        for (Station station : stationMap.keySet()) {
            Coord2D station2d = stationMap.get(station);
            writeStation(xmlSerializer, station, station2d, scale);
        }
    }

    private static void writeSplayData(
            XmlSerializer xmlSerializer, Space<Coord2D> projection, int scale) throws IOException {
        Map<Station, Coord2D> stationMap = projection.getStationMap();
        Map<Leg, Line<Coord2D>> legMap = projection.getLegMap();
        Integer splayStrokeWidth = GeneralPreferences.getExportSvgSplayStrokeWidth();

        for (Station station : stationMap.keySet()) {
            int splayCount = 0;
            for (Leg leg : station.getOnwardLegs()) {
                if (!leg.hasDestination()) {
                    Line<Coord2D> line = legMap.get(leg);
                    String splayId = String.format("%s-Splay%d", station.getName(), splayCount);
                    writeLeg(xmlSerializer, line, splayId, scale, splayStrokeWidth);
                    splayCount++;
                }
            }
        }
    }

    private static void writeLeg(
            XmlSerializer xmlSerializer,
            Line<Coord2D> line,
            String id,
            int scale,
            Integer strokeWidth)
            throws IOException {
        xmlSerializer.startTag("", "polyline");
        xmlSerializer.attribute("", "id", id);
        String pointsString =
                TextTools.join(
                        ",",
                        scale * line.getStart().x,
                        scale * line.getStart().y,
                        scale * line.getEnd().x,
                        scale * line.getEnd().y);
        xmlSerializer.attribute("", "points", pointsString);
        xmlSerializer.attribute("", "stroke", "red");
        xmlSerializer.attribute("", "stroke-width", strokeWidth.toString());
        xmlSerializer.attribute("", "fill", "none");
        xmlSerializer.endTag("", "polyline");
    }

    private static void writeStation(
            XmlSerializer xmlSerializer, Station station, Coord2D coord, int scale)
            throws IOException {
        xmlSerializer.startTag("", "text");
        xmlSerializer.attribute("", "id", station.getName());
        xmlSerializer.attribute("", "x", String.format(Locale.ROOT, "%.5f", scale * coord.x));
        xmlSerializer.attribute("", "y", String.format(Locale.ROOT, "%.5f", scale * coord.y));
        xmlSerializer.attribute("", "font-size", String.format("%d", STATION_FONT));
        xmlSerializer.attribute("", "stroke", "black");
        xmlSerializer.text(station.getName());
        xmlSerializer.endTag("", "text");
    }

    @Override
    public String getExportDirectoryName() {
        return "svg";
    }

    @Override
    public String getFileExtension() {
        return "svg";
    }

    @Override
    public String getExportTypeName(Context context) {
        return context.getString(R.string.third_party_svg);
    }

    public String getMimeType() {
        return "image/svg+xml";
    }

    @Override
    public void showOptionsDialog(Context context, Runnable onReady) {
        Activity activity = (Activity) context;
        View dialogView = activity.getLayoutInflater().inflate(R.layout.dialog_svg_export, null);

        Spinner backgroundSpinner = dialogView.findViewById(R.id.svgBackgroundSpinner);
        CheckBox legendCheckbox = dialogView.findViewById(R.id.svgLegendCheckbox);
        CheckBox northArrowCheckbox = dialogView.findViewById(R.id.svgNorthArrowCheckbox);
        CheckBox scaleBarCheckbox = dialogView.findViewById(R.id.svgScaleBarCheckbox);
        CheckBox teamCheckbox = dialogView.findViewById(R.id.svgTeamCheckbox);
        CheckBox crossSectionsCheckbox = dialogView.findViewById(R.id.svgCrossSectionsCheckbox);
        CheckBox symbolsCheckbox = dialogView.findViewById(R.id.svgSymbolsCheckbox);
        CheckBox centrelineCheckbox = dialogView.findViewById(R.id.svgCentrelineCheckbox);
        CheckBox stationsCheckbox = dialogView.findViewById(R.id.svgStationsCheckbox);
        CheckBox splaysCheckbox = dialogView.findViewById(R.id.svgSplaysCheckbox);
        CheckBox gridCheckbox = dialogView.findViewById(R.id.svgGridCheckbox);
        CheckBox taglineCheckbox = dialogView.findViewById(R.id.svgTaglineCheckbox);

        String[] backgroundValues =
                context.getResources()
                        .getStringArray(R.array.settings_export_svg_background_values);
        boolean currentlyWhite = GeneralPreferences.getExportSvgBackgroundColour() == Colour.WHITE;
        backgroundSpinner.setSelection(
                indexOf(backgroundValues, currentlyWhite ? "white" : "transparent"));
        legendCheckbox.setChecked(GeneralPreferences.isExportSvgLegendEnabled());
        northArrowCheckbox.setChecked(GeneralPreferences.isExportSvgNorthArrowEnabled());
        scaleBarCheckbox.setChecked(GeneralPreferences.isExportSvgScaleBarEnabled());
        teamCheckbox.setChecked(GeneralPreferences.isExportSvgTeamEnabled());
        crossSectionsCheckbox.setChecked(GeneralPreferences.isExportSvgCrossSectionsEnabled());
        symbolsCheckbox.setChecked(GeneralPreferences.isExportSvgSymbolsEnabled());
        centrelineCheckbox.setChecked(GeneralPreferences.isExportSvgCentrelineEnabled());
        stationsCheckbox.setChecked(GeneralPreferences.isExportSvgStationsEnabled());
        splaysCheckbox.setChecked(GeneralPreferences.isExportSvgSplaysEnabled());
        gridCheckbox.setChecked(GeneralPreferences.isExportSvgGridEnabled());
        taglineCheckbox.setChecked(GeneralPreferences.isExportSvgTaglineEnabled());

        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.svg_export_dialog_title)
                .setView(dialogView)
                .setPositiveButton(
                        R.string.ok,
                        (dialog, which) -> {
                            String selectedBackground =
                                    backgroundValues[backgroundSpinner.getSelectedItemPosition()];
                            boolean white = "white".equalsIgnoreCase(selectedBackground);
                            boolean legend = legendCheckbox.isChecked();
                            boolean north = northArrowCheckbox.isChecked();
                            boolean bar = scaleBarCheckbox.isChecked();
                            boolean team = teamCheckbox.isChecked();
                            boolean xsec = crossSectionsCheckbox.isChecked();
                            boolean symbols = symbolsCheckbox.isChecked();
                            boolean centreline = centrelineCheckbox.isChecked();
                            boolean stations = stationsCheckbox.isChecked();
                            boolean splays = splaysCheckbox.isChecked();
                            boolean grid = gridCheckbox.isChecked();
                            boolean tagline = taglineCheckbox.isChecked();
                            exportOptions =
                                    new SvgExportOptions(
                                            white,
                                            legend,
                                            north,
                                            bar,
                                            team,
                                            xsec,
                                            symbols,
                                            centreline,
                                            stations,
                                            splays,
                                            grid,
                                            tagline);
                            saveOptions(
                                    selectedBackground,
                                    legend,
                                    north,
                                    bar,
                                    team,
                                    xsec,
                                    symbols,
                                    centreline,
                                    stations,
                                    splays,
                                    grid,
                                    tagline);
                            onReady.run();
                        })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private static int indexOf(String[] values, String target) {
        for (int i = 0; i < values.length; i++) {
            if (values[i].equalsIgnoreCase(target)) {
                return i;
            }
        }
        return 0;
    }

    private static void saveOptions(
            String backgroundValue,
            boolean legend,
            boolean north,
            boolean bar,
            boolean team,
            boolean crossSections,
            boolean symbols,
            boolean centreline,
            boolean stations,
            boolean splays,
            boolean grid,
            boolean tagline) {
        SharedPreferences prefs = GeneralPreferences.getRawPreferences();
        if (prefs == null) {
            return;
        }
        prefs.edit()
                .putString("pref_export_svg_background", backgroundValue)
                .putBoolean("pref_export_svg_legend", legend)
                .putBoolean("pref_export_svg_north_arrow", north)
                .putBoolean("pref_export_svg_scale_bar", bar)
                .putBoolean("pref_export_svg_team", team)
                .putBoolean("pref_export_svg_cross_sections", crossSections)
                .putBoolean("pref_export_svg_symbols", symbols)
                .putBoolean("pref_export_svg_centreline", centreline)
                .putBoolean("pref_export_svg_stations", stations)
                .putBoolean("pref_export_svg_splays", splays)
                .putBoolean("pref_export_svg_grid", grid)
                .putBoolean("pref_export_svg_tagline", tagline)
                .apply();
    }

    public String prettyPrintXML(String input) {
        String output;
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(new InputSource(new StringReader(input)));

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            output = writer.toString();
        } catch (Exception e) {
            output = input; // not essential...
        }

        return output;
    }

    public static String getSvgColour(SketchDetail sketchDetail) {
        Colour colour = sketchDetail.getColour();

        // Special case hack! SVG should be able to handle British English
        // but it seems that CorelDraw gets confused by "grey" >:(
        if (colour == Colour.GREY) {
            colour = Colour.GRAY;
        }
        return colour.toString();
    }
}
