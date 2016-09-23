package org.hwyl.sexytopo.control.io.thirdparty.xvi;

import org.hwyl.sexytopo.control.io.basic.Loader;
import org.hwyl.sexytopo.control.io.translation.Importer;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.sketch.Colour;
import org.hwyl.sexytopo.model.sketch.PathDetail;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.survey.Survey;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hwyl.sexytopo.control.io.thirdparty.xvi.XviConstants.SKETCHLINE_COMMAND;


public class XviImporter implements Importer {

    public Survey toSurvey(File file) throws Exception {
        Survey survey = new Survey(file.getName());
        Sketch sketch = getSketch(file);
        survey.setPlanSketch(sketch);
        return survey;
    }

    public static Sketch getSketch(File file) throws Exception {

        Sketch sketch = new Sketch();

        String contents = Loader.slurpFile(file);
        Set<PathDetail> pathDetails = parseSketchlineBlock(contents);
        sketch.setPathDetails(pathDetails);

        return sketch;
    }


    public boolean canHandleFile(File file) {
        return file.getName().endsWith("xvi");
    }

    public static Set<PathDetail> parseSketchlineBlock(String contents) throws Exception {
        String sketchBlock = getBlockContents(contents, SKETCHLINE_COMMAND);
        List<String> entries = parseBlockEntries(sketchBlock);
        Set<PathDetail> pathDetails = new HashSet<>();
        for (String entry : entries) {
            PathDetail pathDetail = parseSketchEntry(entry);
            pathDetails.add(pathDetail);
        }
        return pathDetails;
    }

    public static List<String> parseBlockEntries(String content) {
        List<String> entries = new ArrayList<>();
        Pattern pattern = Pattern.compile("{(.*)}");
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            entries.add(matcher.group());
        }
        return entries;
    }


    public static PathDetail parseSketchEntry(String entry) throws IllegalArgumentException {
        List<String> tokens = Arrays.asList(entry.split(" "));

        if (tokens.size() <= 1) {
            throw new IllegalArgumentException("Incomplete token? {" + entry + "}");
        }

        String first = tokens.get(0);
        if (first.equals("connect")) {
            throw new IllegalArgumentException("Not sure what to do with token {" + entry + "}");
        }

        if (tokens.size() % 2 != 1) {
            throw new IllegalArgumentException(
                    "There was an odd number of data points in the token (excluding first item): "
                            + tokens.size());
        }

        Colour colour = Colour.valueOf(first.toUpperCase());

        List<Coord2D> points = new ArrayList<>(tokens.size() - 1);
        for (int i = 1; i < tokens.size();) {
            double x = Double.parseDouble(tokens.get(i++));
            double y = Double.parseDouble(tokens.get(i++));
            Coord2D coord2D = new Coord2D(x, y);
            points.add(coord2D);
        }

        PathDetail pathDetail = new PathDetail(points.get(0), colour);
        for (Coord2D point : points.subList(1, points.size())) {
            pathDetail.lineTo(point);
        }

        return pathDetail;
    }


    public static String getBlockContents(String text, String command) throws Exception {

        outer:
        for (int i = 0; i < text.length(); i++) {

            // keep trying to match the command string
            for (int j = 0; j < command.length(); j++) {
                char text_current = text.charAt(i + j);
                char command_current = command.charAt(j);
                if (text_current != command_current) {
                    continue outer;
                }
            }

            i += command.length();
            // from here we're at the end of a matched command string

            // eat up any whitespace
            while (Character.isWhitespace(text.charAt(i))) {
                i++;
            }

            // hopefully we've found an opening { block now
            if (text.charAt(i) == '{') {
                i++;
            } else {
                continue outer; // otherwise try to match again...
            }

            int openBraces = 1;
            int startOfContent = i;

            for(; i < text.length(); i++) {
                if (text.charAt(i) == '{') {
                    openBraces++;
                } else if (text.charAt(i) == '}') {
                    openBraces--;
                }

                if (openBraces == 0) {
                    int endOfContent = i;
                    String content = text.substring(startOfContent, endOfContent);
                    return content;
                }

            }

            throw new Exception("Malformed text: could not match braces");

        }

        throw new Exception("Could not match command in text");
    }

}
