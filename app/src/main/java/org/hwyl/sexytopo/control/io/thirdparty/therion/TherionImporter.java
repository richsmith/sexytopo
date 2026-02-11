package org.hwyl.sexytopo.control.io.thirdparty.therion;

import android.content.Context;

import androidx.documentfile.provider.DocumentFile;

import org.apache.commons.io.FilenameUtils;
import org.hwyl.sexytopo.SexyTopoConstants;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.io.IoUtils;
import org.hwyl.sexytopo.control.io.thirdparty.survextherion.SurveyFormat;
import org.hwyl.sexytopo.control.io.thirdparty.survextherion.SurvexTherionImporter;
import org.hwyl.sexytopo.control.io.thirdparty.xvi.XviImporter;
import org.hwyl.sexytopo.control.io.translation.Importer;
import org.hwyl.sexytopo.control.util.SurveyUpdater;
import org.hwyl.sexytopo.model.survey.Trip;
import org.hwyl.sexytopo.control.util.TextTools;
import org.hwyl.sexytopo.model.graph.Direction;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


public class TherionImporter extends Importer {

    public Survey toSurvey(Context context, DocumentFile directory) throws Exception {

        Survey survey = null;

        for (DocumentFile file : directory.listFiles()) {

            if (file.getName().endsWith("th")) {
                survey = parseTh(context, file);

            } else if (file.getName().endsWith("xvi")) {
                String filenameNoExtension = FilenameUtils.removeExtension(file.getName());
                if (filenameNoExtension.endsWith(SexyTopoConstants.PLAN_SUFFIX)) {
                    Sketch sketch = XviImporter.getSketch(context, file);
                    survey.setPlanSketch(sketch);
                } else if (filenameNoExtension.endsWith(SexyTopoConstants.EE_SUFFIX)) {
                    Sketch sketch = XviImporter.getSketch(context, file);
                    survey.setElevationSketch(sketch);
                }

            }
        }

        return survey;
    }

    private static Survey parseTh(Context context, DocumentFile file) throws Exception {
        Survey survey = new Survey();
        String contents = IoUtils.slurpFile(context, file);
        List<String> lines = Arrays.asList(contents.split("\n"));
        updateCentreline(lines, survey);

        Trip trip = SurvexTherionImporter.parseMetadata(contents, SurveyFormat.THERION);
        if (trip != null) {
            survey.setTrip(trip);
        }

        return survey;
    }

    public boolean canHandleFile(DocumentFile directory) {
        if (!directory.isDirectory()) {
            return false;
        }
        for (DocumentFile file : directory.listFiles()) {
            if (file.getName().endsWith("th")) {
                return true;
            }
        }
        return false;
    }

    public static void updateCentreline(List<String> lines, Survey survey) throws Exception {
        List<String> block = getContentsOfBeginEndBlock(lines, "centreline");

        String blockText = TextTools.join("\n", block);
        Map<String, String> passageComments = SurvexTherionImporter.parsePassageData(blockText, SurveyFormat.THERION);

        List<String> sanitisedCentrelineData = new ArrayList<>();
        List<String> sanitisedElevationDirectionData = new ArrayList<>();

        boolean inNormalDataBlock = false;
        for (String line: block) {
            String trimmed = line.trim();
            if (trimmed.equals("")) {
                continue;
            }

            if (trimmed.startsWith("data")) {
                inNormalDataBlock = trimmed.startsWith("data normal");
                continue;
            } else if (!inNormalDataBlock) {
                if (trimmed.startsWith("extend")) {
                    sanitisedElevationDirectionData.add(trimmed);
                }
                continue;
            }

            if (trimmed.startsWith("extend")) {
                sanitisedElevationDirectionData.add(trimmed);
            } else {
                sanitisedCentrelineData.add(trimmed);
            }
        }

        String centrelineText = TextTools.join("\n", sanitisedCentrelineData);
        SurvexTherionImporter.parseCentreline(centrelineText, survey);
        handleElevationDirectionData(sanitisedElevationDirectionData, survey);

        SurvexTherionImporter.mergePassageComments(survey, passageComments);
    }

    private static void handleElevationDirectionData(List<String> lines, Survey survey) {
        try {
            for (String line : lines) {
                final String EXTEND_PREFIX = "extend ";
                assert line.startsWith(EXTEND_PREFIX);
                String rest = line.substring(EXTEND_PREFIX.length());
                String[] tokens = rest.split(" ");
                assert tokens.length == 2;

                String directionName = tokens[0];
                if (directionName.equals("start")) {
                    continue;
                }
                Direction direction = Direction.valueOf(directionName.toUpperCase());

                String stationName = tokens[1];
                Station station = survey.getStationByName(stationName);

                SurveyUpdater.setDirectionOfSubtree(station, direction);
            }

        } catch (Exception exception) {
            Log.e("corrupted survey extended elevation directions: " + exception);
        }
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

            } else if (!foundStartBlock) {
                continue;
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
