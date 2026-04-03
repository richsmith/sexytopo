package org.hwyl.sexytopo.control.io.thirdparty.xvi;

import static org.hwyl.sexytopo.control.io.thirdparty.xvi.XviConstants.GRID_COMMAND;
import static org.hwyl.sexytopo.control.io.thirdparty.xvi.XviConstants.SKETCHLINE_COMMAND;

import android.content.Context;

import androidx.documentfile.provider.DocumentFile;

import org.hwyl.sexytopo.control.io.IoUtils;
import org.hwyl.sexytopo.control.io.translation.Importer;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.sketch.Colour;
import org.hwyl.sexytopo.model.sketch.PathDetail;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.survey.Survey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@SuppressWarnings("UnnecessaryLocalVariable")
public class XviImporter extends Importer {

    private static final XviImporter instance = new XviImporter();

    public Survey toSurvey(Context context, DocumentFile file) throws Exception {
        Survey survey = new Survey();
        Sketch sketch = getSketch(context, file);
        survey.setPlanSketch(sketch);
        return survey;
    }

    public static Sketch getSketch(Context context, DocumentFile file) throws Exception {

        Sketch sketch = new Sketch();

        String contents = IoUtils.slurpFile(context, file);

        Grid grid = instance.parseGrid(contents);
        double scale = grid.dy;

        List<PathDetail> pathDetails = parseSketchlineBlock(scale, contents);
        sketch.setPathDetails(pathDetails);

        return sketch;
    }


    public boolean canHandleFile(DocumentFile file) {
        return file.isFile() && !file.isDirectory() && file.getName().endsWith("xvi");
    }


    public Grid parseGrid(String contents) throws Exception {

        String gridBlock = getBlockContents(contents, GRID_COMMAND);
        List<String> values = Arrays.asList(gridBlock.split("\\s"));
        double x = Double.parseDouble(values.get(0));
        double y = Double.parseDouble(values.get(1));
        double dx = Double.parseDouble(values.get(2));
        // 0
        // 0
        double dy = Double.parseDouble(values.get(5)); // grid * cfactor
        double nx = Double.parseDouble(values.get(6));
        double ny = Double.parseDouble(values.get(7));
        Grid grid = new Grid(x, y, dx, dy, nx, ny);
        return grid;
    }


    public static List<PathDetail> parseSketchlineBlock(double scale, String contents) throws Exception {
        String sketchBlock = getBlockContents(contents, SKETCHLINE_COMMAND);
        List<String> entries = parseBlockEntries(sketchBlock);
        List<PathDetail> pathDetails = new ArrayList<>();
        for (String entry : entries) {
            PathDetail pathDetail = parseSketchEntry(scale, entry);
            pathDetails.add(pathDetail);
        }
        return pathDetails;
    }


    public static List<String> parseBlockEntries(String content) {
        List<String> entries = new ArrayList<>();
        Pattern pattern = Pattern.compile("[{](.*?)[}]");
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            entries.add(matcher.group(1));
        }
        return entries;
    }


    public static PathDetail parseSketchEntry(double scale, String entry) throws IllegalArgumentException {
        List<String> tokens = Arrays.asList(entry.split(" "));

        if (tokens.size() <= 1) {
            throw new IllegalArgumentException("Incomplete token? {" + entry + "}");
        }

        String first = tokens.get(0);

        // "connect" entries link two points with a line
        if (first.equals("connect")) {
            if (tokens.size() < 5) {
                throw new IllegalArgumentException("Incomplete connect token: {" + entry + "}");
            }
            float x1 = Float.parseFloat(tokens.get(1)) / (float) scale;
            float y1 = Float.parseFloat(tokens.get(2)) / (float) scale;
            float x2 = Float.parseFloat(tokens.get(3)) / (float) scale;
            float y2 = Float.parseFloat(tokens.get(4)) / (float) scale;
            PathDetail pathDetail = new PathDetail(new Coord2D(x1, -y1), Colour.BLACK);
            pathDetail.lineTo(new Coord2D(x2, -y2));
            return pathDetail;
        }

        if (tokens.size() % 2 != 1) {
            throw new IllegalArgumentException(
                    "There was an odd number of data points in the token (excluding first item): "
                            + tokens.size());
        }

        Colour colour = Colour.valueOf(first.toUpperCase());

        List<Coord2D> points = new ArrayList<>(tokens.size() - 1);
        for (int i = 1; i < tokens.size();) {
            float x = Float.parseFloat(tokens.get(i++));
            float y = Float.parseFloat(tokens.get(i++));

            x /= scale;
            y /= scale;

            Coord2D coord2D = new Coord2D(x, -y);
            points.add(coord2D);
        }

        PathDetail pathDetail = new PathDetail(points.get(0), colour);
        for (Coord2D point : points.subList(1, points.size())) {
            pathDetail.lineTo(point);
        }

        return pathDetail;
    }


    @SuppressWarnings("UnnecessaryLabelOnContinueStatement")
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

    static class Grid {
        final double x, y, dx, dy, nx, ny;
        Grid(double x, double y, double dx, double dy, double nx, double ny) {
            this.x = x;
            this.y = y;
            this.dx = dx;
            this.dy = dy;
            this.nx = nx;
            this.ny = ny;
        }
    }

}


