package org.hwyl.sexytopo.control.io.thirdparty.svg;

import android.content.Context;
import android.util.Xml;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.io.translation.DoubleSketchFileExporter;
import org.hwyl.sexytopo.control.util.TextTools;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.sketch.PathDetail;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.sketch.TextDetail;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;


@SuppressWarnings({"UnnecessaryLocalVariable", "SameParameterValue"})
public class SvgExporter extends DoubleSketchFileExporter {

    public static final int SCALE = 10;
    public static final int BORDER = 10;


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


    private static void writeSketch(XmlSerializer xmlSerializer, Sketch sketch, int scale)
            throws IOException {

        for (PathDetail pathDetail : sketch.getPathDetails()) {
            writePathDetail(xmlSerializer, pathDetail, scale);
        }

        for (TextDetail textDetail : sketch.getTextDetails()) {
            writeTextDetail(xmlSerializer, textDetail, scale);
        }
    }

    private static void writePathDetail(
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
