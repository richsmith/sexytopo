package org.hwyl.sexytopo.control.io.thirdparty.therion;

import org.apache.commons.io.FilenameUtils;
import org.hwyl.sexytopo.control.io.basic.Loader;
import org.hwyl.sexytopo.control.io.thirdparty.survex.SurvexImporter;
import org.hwyl.sexytopo.control.io.thirdparty.xvi.XviImporter;
import org.hwyl.sexytopo.control.io.translation.Importer;
import org.hwyl.sexytopo.control.util.TextTools;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.survey.Survey;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class TherionImporter extends Importer {

    public Survey toSurvey(File directory) throws Exception {

        Survey survey = null;

        for (File file : directory.listFiles()) {

            if (file.getName().endsWith("th")) {
                survey = parseTh(file);
                break;

            } else if (file.getName().endsWith("xvi")) {
                String filenameNoExtension = FilenameUtils.removeExtension(file.getName());
                if (filenameNoExtension.endsWith("_p")) {
                    Sketch sketch = XviImporter.getSketch(file);
                    survey.setPlanSketch(sketch);
                } else if (filenameNoExtension.endsWith("_e")) {
                    String contents = Loader.slurpFile(file);
                    Sketch sketch = XviImporter.getSketch(file);
                    survey.setElevationSketch(sketch);
                }

            }
        }

        return survey;
    }

    private static Survey parseTh(File file) throws Exception {
        String contents = Loader.slurpFile(file);

        String name = getSurveyName(contents);
        Survey survey = new Survey(name);

        List<String> lines = Arrays.asList(contents.split("\n"));
        updateCentreline(lines, survey);

        return survey;
    }

    public static String getSurveyName(String text) {
        Pattern pattern = Pattern.compile("-title\\s*\"(.*)+?\"");
        Matcher matcher = pattern.matcher(text);
        matcher.find();
        return matcher.group(1);
    }


    public boolean canHandleFile(File directory) {
        if (!directory.isDirectory()) {
            return false;
        }
        for (File file : directory.listFiles()) {
            if (file.getName().endsWith("th")) {
                return true;
            }
        }
        return false;
    }


    public static void updateCentreline(List<String> lines, Survey survey) throws Exception {
        List<String> block = getContentsOfBeginEndBlock(lines, "centreline");
        String centrelineText = TextTools.join("\n", block);
        SurvexImporter.parse(centrelineText, survey);

    }


    public static List<String> getContentsOfBeginEndBlock(List<String> lines, String tag)
            throws Exception {

        boolean foundStartBlock = false;
        boolean foundEndBlock = false;
        String endTag = "end" + tag;

        List<String> contents = new ArrayList<>();

        for (String line : lines) {
            line = line.trim();

            if (line.equals(tag)) {
                if (foundStartBlock) {
                    throw new Exception("Opening block tag " + tag + " encountered twice");
                } else {
                    foundStartBlock = true;
                    continue;
                }

            } else if (line.equals(endTag)) {
                if (foundStartBlock) {
                    foundEndBlock = true;
                    break;
                } else {
                    throw new Exception("End block tag " + endTag + " encountered before block start");
                }
            }

            contents.add(line);
        }

        if (!foundStartBlock) {
            throw new Exception("Failed to find opening block tag " + tag);
        } else if (!foundEndBlock) {
            throw new Exception("Failed to find end block tag " + endTag);
        }

        return contents;
    }

}
