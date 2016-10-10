package org.hwyl.sexytopo.control.io.thirdparty.pockettopo;

import org.hwyl.sexytopo.control.io.translation.Importer;
import org.hwyl.sexytopo.control.io.basic.Loader;
import org.hwyl.sexytopo.control.util.SurveyUpdater;
import org.hwyl.sexytopo.control.util.TextTools;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.sketch.Colour;
import org.hwyl.sexytopo.model.sketch.PathDetail;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Basic import for the .txt file that is exported by PocketTopo.
 */
public class PocketTopoTxtImporter implements Importer {


    public Survey toSurvey(File file) {

        String text = Loader.slurpFile(file.getAbsolutePath());

        // FIXME we're ignoring the metadata for now

        Survey survey = new Survey("test");

        parseDataAndUpdateSurvey(survey, text);

        Sketch elevation = getElevation(text);
        survey.setElevationSketch(elevation);

        Sketch plan = getPlan(text);
        survey.setPlanSketch(plan);

        survey.setSaved(true);

        return survey;
    }


    public boolean canHandleFile(File file) {
        return file.getName().endsWith("txt");
    }


    private static void parseDataAndUpdateSurvey(Survey survey, String fullText) {
        String text = getSection(fullText, "DATA");
        for (String line : TextTools.toArrayOfLines(text)) {
            String[] fields = line.split("\\t");
            String fromStationName = fields[0];
            String toStationName = fields[1];

            double azimuth = Double.parseDouble(fields[2]);
            double inclination = Double.parseDouble(fields[3]);
            double distance = Double.parseDouble(fields[4]);

            if (fromStationName.equals("1.1")) {
                int ignore = 0;
            }

            Station fromStation = fromStationName.equals("1.0")?
                    survey.getOrigin() : survey.getStationByName(fromStationName);

            survey.setActiveStation(fromStation);

            if (toStationName.equals("")) {
                Leg leg = new Leg(distance, azimuth, inclination);
                SurveyUpdater.update(survey, leg);
            } else {
                Station toStation = new Station(toStationName);
                Leg leg = new Leg(distance, azimuth, inclination, toStation);
                SurveyUpdater.updateWithNewStation(survey, leg);
            }
        }

    }

    private static Sketch getPlan(String fullText) {
        String text = getSection(fullText, "PLAN");
        Sketch plan = parseSketch(text);
        return plan;
    }

    private static Sketch getElevation(String fullText) {
        String text = getSection(fullText, "ELEVATION");
        Sketch elevation = parseSketch(text);
        return elevation;
    }

    private static Sketch parseSketch(String text) {
        Sketch sketch = new Sketch();
        Set<PathDetail> pathDetails = parsePolylines(text);
        sketch.setPathDetails(pathDetails);
        return sketch;
    }

    public static String getSection(String text, String header) {
        // a section in .txt format appears to be made up of a header, followed by the content,
        // followed by a blank line. There are no intermediate blank lines.

        text = text + "\n\n"; // hack (ensure last section is terminated by newline)

        Pattern pattern = Pattern.compile(
            "(?sm)" +
            "^" + header.toUpperCase() + "\\n" +
            "(.*?)\\n" +
            "^\\n");
        Matcher matcher = pattern.matcher(text);
        matcher.find();
        return matcher.group(1);
    }

    public static String getNamedSubSection(String text, String header, String sectionHeader) {
        String section = getSection(text, sectionHeader);
        return getNamedSubSection(section, header);
    }

    public static String getNamedSubSection(String text, String header) {
        // a section in .txt format appears to be made up of a header, followed by the content,
        // followed by either another header (one line of uppercase text) or end of text

        List<String> subSection = new LinkedList<>();
        boolean inSubSection = false;
        for (String line : TextTools.toArrayOfLines(text)) {
            if (line.trim().equals(header)) {
                inSubSection = true;
                continue;
            }
            if (inSubSection) {
                if (line.matches("[A-Z]+")) {
                    break; // we've come to the next sub-section
                }
                subSection.add(line);
            }
        }

        return TextTools.join("\n", subSection);
    }

    public static Set<PathDetail> parsePolylines(String text) {
        Set<PathDetail> paths = new HashSet<>();
        boolean inPolyline = false;
        Colour currentPathColour = null;
        PathDetail currentPathDetail = null;
        for (String line : TextTools.toArrayOfLines(text)) {
            if (line.startsWith("POLYLINE")) {
                if (inPolyline) {
                    paths.add(currentPathDetail);
                    currentPathDetail = null;
                }
                String colourText = line.substring("POLYLINE ".length());
                if (colourText.equals("RED")) {
                    colourText = "PURPLE";
                }
                currentPathColour = Colour.valueOf(colourText);
                inPolyline = true;
                continue;
            }
            if (!inPolyline) {
                continue;
            }

            String[] coords = line.split("\t");
            double x = Double.parseDouble(coords[0]);
            double y = Double.parseDouble(coords[1]);
            Coord2D coord = new Coord2D(x, -y);

            if (currentPathDetail == null) {
                currentPathDetail = new PathDetail(coord, currentPathColour);
            } else {
                currentPathDetail.lineTo(coord);
            }
        }

        if (currentPathDetail != null) {
            paths.add(currentPathDetail);
        }

        return paths;

    }


}
