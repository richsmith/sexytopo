package org.hwyl.sexytopo.control.io;

import android.util.Log;

import org.hwyl.sexytopo.SexyTopo;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by rls on 08/10/14.
 */
public class Loader {

    public static Survey loadSurvey(String name) throws JSONException {

        String path = Util.getPathForSurveyFile(name, "svx");
        String text = slurpFile(path);

        if (text.length() == 0) {
            throw new IllegalArgumentException("Empty save file");
        }

        Survey survey = new Survey(name);
        parse(text, survey);

        String planPath = Util.getPathForSurveyFile(survey.getName(), SexyTopo.PLAN_SKETCH_EXTENSION);
        if (Util.doesFileExist(planPath)) {
            String planText = slurpFile(planPath);
            Sketch plan = SketchJsonTranslater.translate(survey, planText);
            survey.setPlanSketch(plan);
        }

        String elevationPath = Util.getPathForSurveyFile(survey.getName(), SexyTopo.EXT_ELEVATION_SKETCH_EXTENSION);
        if (Util.doesFileExist(elevationPath)) {
            String elevationText = slurpFile(elevationPath);
            Sketch elevation = SketchJsonTranslater.translate(survey, elevationText);
            survey.setElevationSketch(elevation);
        }

        return survey;

    }

    private static String slurpFile(String filename) {

        StringBuilder text = new StringBuilder();

        try {
            File file = new File(filename);
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }

        } catch (Exception e) {
            Log.d(SexyTopo.TAG, "Error trying to read " + filename + ": " + e.getMessage());
        }
        return text.toString();
    }

    private static void parse(String text, Survey survey) {

        Map<String, Station> nameToStation = new HashMap<>();

        Station origin = survey.getOrigin();
        nameToStation.put(origin.getName(), origin);

        String[] lines = text.split("\n");
        for (String line : lines) {
            if (line.trim().equals("")) {
                continue;
            }
            String[] fields = line.trim().split("\t");
            addLegToSurvey(survey, nameToStation, fields);
        }
    }

    private static void addLegToSurvey(Survey survey,
            Map<String, Station> nameToStation, String[] fields) {

        Station from = retrieveOrCreateStation(nameToStation, fields[0]);
        Station to = retrieveOrCreateStation(nameToStation, fields[1]);

        double distance = Double.parseDouble(fields[2]);
        double bearing = Double.parseDouble(fields[3]);
        double inclination = Double.parseDouble(fields[4]);

        Leg leg = (to == Survey.NULL_STATION)?
            new Leg(distance, bearing, inclination) :
            new Leg(distance, bearing, inclination, to);

        from.addOnwardLeg(leg);

        // FIXME: bit of a hack; hopefully the last station processed will be the active one
        // (should probably record the active station in the file somewhere)
        survey.setActiveStation(from);
    }

    private static Station retrieveOrCreateStation(Map<String, Station> nameToStation,
                                                   String name) {
        if (name.equals(SexyTopo.BLANK_STATION_NAME)) {
            return Survey.NULL_STATION;
        } else if (nameToStation.containsKey(name)) {
            return nameToStation.get(name);
        } else {
            Station station = new Station(name);
            nameToStation.put(name, station);
            return station;
        }
    }


}
