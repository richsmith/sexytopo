package org.hwyl.sexytopo.control.io.thirdparty.svg;

import android.content.Context;
import android.util.Xml;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.io.translation.DoubleSketchFileExporter;
import org.hwyl.sexytopo.control.util.GraphToListTranslator;
import org.hwyl.sexytopo.control.util.TextTools;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.sketch.PathDetail;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.sketch.TextDetail;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;


@SuppressWarnings({"UnnecessaryLocalVariable", "SameParameterValue"})
public class SvgExporter extends DoubleSketchFileExporter {

    public static final int SCALE = 10;
    public static final int BORDER = 10;

    private static final GraphToListTranslator graphToListTranslator = new GraphToListTranslator();

    HashMap<String, LinkedBlockingQueue<PathDetail>> pathsByColors = new HashMap<String, LinkedBlockingQueue<PathDetail>>();
    HashMap<String, LinkedBlockingQueue<TextDetail>> textsByColors = new HashMap<String, LinkedBlockingQueue<TextDetail>>();
//    LinkedHashSet<Station> stations = new LinkedHashSet<Station>();
//    LinkedBlockingQueue<Leg> legs = new LinkedBlockingQueue<Leg>();
//    HashMap<String, LinkedBlockingQueue<Leg>> splaysByStation = new HashMap<String, LinkedBlockingQueue<Leg>>();

    @Override
    public String getContent(Sketch sketch) throws IOException {

        double svgWidth = (sketch.getWidth() + (2 * BORDER)) * SCALE;
        double svgHeight = (sketch.getHeight() + (2 * BORDER)) * SCALE;
        Coord2D topLeft = sketch.getTopLeft();
        double svgTopLeftX = (topLeft.x - BORDER) * SCALE;
        double svgTopLeftY = (topLeft.y - BORDER) * SCALE;

        XmlSerializer xmlSerializer = Xml.newSerializer();
        StringWriter writer = new StringWriter();
        xmlSerializer.setOutput(writer);

        xmlSerializer.startDocument("UTF-8", true);

        xmlSerializer.startTag(null,"svg");

        xmlSerializer.attribute(null, "width", Double.toString(svgWidth));
        xmlSerializer.attribute(null, "height", Double.toString(svgHeight));
        xmlSerializer.attribute(null, "viewBox",
                TextTools.join(" ", svgTopLeftX, svgTopLeftY, svgWidth, svgHeight));
        xmlSerializer.attribute(null, "xmlns", "http://www.w3.org/2000/svg");

        writeSketch(xmlSerializer, sketch, SCALE);

        xmlSerializer.endTag(null, "svg");
        xmlSerializer.endDocument();

        String text = writer.toString();
        return text;
    }

    private void writeSketch(XmlSerializer xmlSerializer, Sketch sketch, int scale)
            throws IOException {
//        List<GraphToListTranslator.SurveyListEntry> data =
//                graphToListTranslator.toChronoListOfSurveyListEntries(survey);
//
//        for (GraphToListTranslator.SurveyListEntry entry : data) {
//            Leg leg = entry.getLeg();
//            Station from = entry.getFrom();
//
//            stations.add(from);
//            if (leg.hasDestination()) {
//                legs.add(leg);
//            }
//            else {
//                addSplayToMap(from.getName(), leg);
//            }
//        }
//
//        xmlSerializer.startTag(null, "g");
//        xmlSerializer.attribute(null, "id", "legs");
//        for (Leg leg: legs) {
//            writeLeg(xmlSerializer, leg, scale);
//        }
//        xmlSerializer.endTag(null,"g");


        for (PathDetail pathDetail : sketch.getPathDetails()) {
            addPathToMap(pathDetail);
        }
        for (HashMap.Entry<String, LinkedBlockingQueue<PathDetail>> pathsByColor : pathsByColors.entrySet()) {
            String color = pathsByColor.getKey();
            LinkedBlockingQueue<PathDetail> paths = pathsByColor.getValue();
            xmlSerializer.startTag(null, "g");
            xmlSerializer.attribute(null, "id", "path_" + color);
            for (PathDetail path : paths) {
                writePathDetail(xmlSerializer, path, scale);
            }
            xmlSerializer.endTag(null,"g");
        }
        pathsByColors.clear();

        for (TextDetail textDetail : sketch.getTextDetails()) {
            addTextToMap(textDetail);
        }
        for (HashMap.Entry<String, LinkedBlockingQueue<TextDetail>> textsByColor : textsByColors.entrySet()) {
            String color = textsByColor.getKey();
            LinkedBlockingQueue<TextDetail> texts = textsByColor.getValue();
            xmlSerializer.startTag(null, "g");
            xmlSerializer.attribute(null, "id", "path_" + color);
            for (TextDetail text : texts) {
                writeTextDetail(xmlSerializer, text, scale);
            }
            xmlSerializer.endTag(null,"g");
        }
        textsByColors.clear();
    }

//    private void addSplayToMap(String station, Leg splay) {
//        if (!splaysByStation.containsKey(station)) {
//            splaysByStation.put(station, new LinkedBlockingQueue<Leg>());
//        }
//        splaysByStation.get(station).add(leg);
//    }

    private void addTextToMap(TextDetail textDetail) {
        String color = textDetail.getColour().toString();
        if (!textsByColors.containsKey(color)) {
            textsByColors.put(color, new LinkedBlockingQueue<TextDetail>());
        }
        textsByColors.get(color).add(textDetail);
    }

    private void addPathToMap(PathDetail pathDetail) {
        String color = pathDetail.getColour().toString();
        if (!pathsByColors.containsKey(color)) {
            pathsByColors.put(color, new LinkedBlockingQueue<PathDetail>());
        }
        pathsByColors.get(color).add(pathDetail);
    }

//    private void writeLeg(
//            XmlSerializer xmlSerializer, Leg leg, int scale)  throws IOException {
//        List<String> coordStrings = new ArrayList<>();
//
////        for (Coord2D coord2D : leg.getPath()) {
////            coordStrings.add(toXmlText(coord2D, scale));
////        }
////
////        // Write the SVG line element
////        printWriter.println("<line x1=\"" + x1 + "\" y1=\"" + y1 + "\" x2=\"" + x2 + "\" y2=\"" + y2 + "\" stroke=\"black\" />");
////
//
//        xmlSerializer.startTag(null,"line");
//        xmlSerializer.attribute(null, "points", TextTools.join(" ", coordStrings));
//        xmlSerializer.attribute(null, "stroke", pathDetail.getColour().toString());
//        xmlSerializer.attribute(null, "stroke-width", "3");
//        xmlSerializer.attribute(null, "fill", "none");
//        xmlSerializer.endTag(null,"polyline");
//    }

    private void writePathDetail(
            XmlSerializer xmlSerializer, PathDetail pathDetail, int scale)  throws IOException {
        List<String> coordStrings = new ArrayList<>();

        for (Coord2D coord2D : pathDetail.getPath()) {
            coordStrings.add(toXmlText(coord2D, scale));
        }
        xmlSerializer.startTag(null,"polyline");
        xmlSerializer.attribute(null, "points", TextTools.join(" ", coordStrings));
        xmlSerializer.attribute(null, "stroke", pathDetail.getColour().toString());
        xmlSerializer.attribute(null, "stroke-width", "3");
        xmlSerializer.attribute(null, "fill", "none");
        xmlSerializer.endTag(null,"polyline");
    }

    private String toXmlText(Coord2D coord2D, int scale) {
        return coord2D.x * scale + "," + coord2D.y * scale;
    }


    private void writeTextDetail(
            XmlSerializer xmlSerializer, TextDetail textDetail, int scale)  throws IOException {
        xmlSerializer.startTag(null,"text");
        Coord2D coord2D = textDetail.getPosition();
        double x = coord2D.x * scale;
        double y = coord2D.y * scale;
        xmlSerializer.attribute(null, "x", Double.toString(x));
        xmlSerializer.attribute(null, "y", Double.toString(y));
        xmlSerializer.attribute(null, "stroke", textDetail.getColour().toString());
        xmlSerializer.text(textDetail.getText());
        xmlSerializer.endTag(null,"text");
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
}
