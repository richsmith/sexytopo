package org.hwyl.sexytopo.control.io.thirdparty.svg;

import android.content.Context;
import android.util.Xml;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.io.basic.ExportSizeCalculator;
import org.hwyl.sexytopo.control.io.translation.DoubleSketchFileExporter;
import org.hwyl.sexytopo.control.util.GeneralPreferences;
import org.hwyl.sexytopo.control.util.TextTools;
import org.hwyl.sexytopo.model.common.Frame;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.Line;
import org.hwyl.sexytopo.model.graph.Projection2D;
import org.hwyl.sexytopo.model.graph.Space;
import org.hwyl.sexytopo.model.sketch.Colour;
import org.hwyl.sexytopo.model.sketch.PathDetail;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.sketch.Symbol;
import org.hwyl.sexytopo.model.sketch.SymbolDetail;
import org.hwyl.sexytopo.model.sketch.TextDetail;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Survey;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;


/** @noinspection WrapperTypeMayBePrimitive*/
@SuppressWarnings({"UnnecessaryLocalVariable", "SameParameterValue"})
public class SvgExporter extends DoubleSketchFileExporter {

    public static final int SCALE = 10;
    public static final int BORDER = 10;


    @Override
    public String getContent(Survey survey, Projection2D projectionType) throws Exception {

        Sketch sketch = survey.getSketch(projectionType);
        Space<Coord2D> projection = projectionType.project(survey);

        Frame frame = ExportSizeCalculator.getExportFrame(survey, projectionType);
        frame = frame.scale(SCALE);


        double svgWidth = frame.getWidth();
        double svgHeight = frame.getHeight();
        Coord2D topLeft = frame.getTopLeft();
        double svgTopLeftX = topLeft.x;
        double svgTopLeftY = topLeft.y;

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

        Colour background = GeneralPreferences.getExportSvgBackgroundColour();
        if (background != Colour.TRANSPARENT) {
            xmlSerializer.startTag("", "g");
            xmlSerializer.attribute("", "id", "background");
            xmlSerializer.startTag(null,"rect");
            xmlSerializer.attribute(null, "x", Double.toString(svgTopLeftX));
            xmlSerializer.attribute(null, "y", Double.toString(svgTopLeftY));
            xmlSerializer.attribute(null, "width", Double.toString(svgWidth));
            xmlSerializer.attribute(null, "height", Double.toString(svgHeight));
            xmlSerializer.attribute(null, "fill", background.toString());
            xmlSerializer.endTag(null,"rect");
            xmlSerializer.endTag("", "g");
        }

        xmlSerializer.startTag("", "g");
        xmlSerializer.attribute("", "id", "sketch");
        writeSketch(xmlSerializer, sketch, SCALE);
        xmlSerializer.endTag("", "g");

        xmlSerializer.startTag("", "g");
        xmlSerializer.attribute("", "id", "data");
        writeSurveyData(xmlSerializer, projection, SCALE);
        xmlSerializer.endTag("", "g");

        xmlSerializer.endTag(null, "svg");
        xmlSerializer.endDocument();

        String text = writer.toString();
        text = unescapeTagsHack(text);
        text = prettyPrintXML(text);

        return text;
    }

    /**
     * Need this because idiot library developers don't provide an option to override escaping
     */
    public String unescapeTagsHack(String text) {

        text = text.replaceAll("&lt;", "<");
        text = text.replaceAll("&gt;", ">");
        return text;
    }

    private static void writeSketch(XmlSerializer xmlSerializer, Sketch sketch, int scale)
            throws Exception {

        Set<Symbol> usedSymbols = new HashSet<>();
        for (SymbolDetail symbolDetail : sketch.getSymbolDetails()) {
            usedSymbols.add(symbolDetail.getSymbol());
        }
        for (Symbol symbol : usedSymbols) {
            writeSymbolRef(xmlSerializer, symbol);
        }

        for (PathDetail pathDetail : sketch.getPathDetails()) {
            writePathDetail(xmlSerializer, pathDetail, scale);
        }

        for (TextDetail textDetail : sketch.getTextDetails()) {
            writeTextDetail(xmlSerializer, textDetail, scale);
        }

        for (SymbolDetail symbolDetail : sketch.getSymbolDetails()) {
            writeSymbolDetail(xmlSerializer, symbolDetail, scale);
        }
    }


    private static void writePathDetail(
            XmlSerializer xmlSerializer, PathDetail pathDetail, int scale)  throws IOException {
        Integer strokeWidth = GeneralPreferences.getExportSvgStrokeWidth();
        List<String> coordStrings = new ArrayList<>();
        for (Coord2D coord2D : pathDetail.getPath()) {
            coordStrings.add(toXmlText(coord2D, scale));
        }
        xmlSerializer.startTag(null,"polyline");
        xmlSerializer.attribute(null, "points", TextTools.join(" ", coordStrings));
        xmlSerializer.attribute(null, "stroke", pathDetail.getColour().toString());
        xmlSerializer.attribute(null, "stroke-width", strokeWidth.toString());
        xmlSerializer.attribute(null, "fill", "none");
        xmlSerializer.endTag(null,"polyline");
    }



    private static String toXmlText(Coord2D coord2D, int scale) {
        return coord2D.x * scale + "," + coord2D.y * scale;
    }


    private static void writeTextDetail(
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

    private static void writeSymbolDetail(
        XmlSerializer xmlSerializer, SymbolDetail symbolDetail, int scale)  throws IOException {
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
        xmlSerializer.attribute("", "color", symbolDetail.getColour().name());

        if (symbol.isDirectional()) {
            xmlSerializer.attribute("", "transform",
                "rotate(" + symbolDetail.getAngle() + "," + centreX + "," + centreY + ")");
        }

        xmlSerializer.endTag("", "use");

    }

    private static void writeSymbolRef(XmlSerializer xmlSerializer, Symbol symbol) throws Exception {
        String svgContent = symbol.asRawSvg();

        // this is super-hacky and fragile... how to do it properly?
        String innerSvgContent = svgContent
            .replace("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 40 40\">", "")
            .replace("</svg>", "")
            .trim();

        xmlSerializer.startTag("", "symbol");
        xmlSerializer.attribute("", "id", symbol.getSvgRefId());
        xmlSerializer.attribute("", "viewBox", "0 0 40 40");

        xmlSerializer.flush();
        xmlSerializer.text(innerSvgContent);

        xmlSerializer.endTag("", "symbol");

    }

     private static void writeSurveyData(XmlSerializer xmlSerializer, Space<Coord2D> projection, int scale)
            throws IOException {
        Map<Leg, Line<Coord2D>> legMap = projection.getLegMap();
        for (Leg leg : legMap.keySet()) {
            Line<Coord2D> line = legMap.get(leg);
            xmlSerializer.startTag("", "polyline");
            String pointsString = TextTools.join(
                ",", scale * line.getStart().x, scale * line.getStart().y, scale * line.getEnd().x, scale * line.getEnd().y);
            xmlSerializer.attribute("", "points", pointsString);
            xmlSerializer.attribute("", "stroke", "red");
            xmlSerializer.attribute("", "stroke-width", leg.hasDestination()? "2" : "1");
            xmlSerializer.attribute("", "fill", "none");
            xmlSerializer.endTag("", "polyline");
        }
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
            output = input;  // not essential...
        }

        return output;
    }
}
