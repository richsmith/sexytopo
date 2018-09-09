package org.hwyl.sexytopo.control.io.thirdparty.survex;

import org.hwyl.sexytopo.SexyTopo;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.io.basic.Loader;
import org.hwyl.sexytopo.control.io.translation.Importer;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SurvexImporter extends Importer {

    public static final Pattern COMMENT_INSTRUCTION_REGEX = Pattern.compile("(\\{.*?\\})");


    @Override
    public Survey toSurvey(File file) throws Exception {
        String text = Loader.slurpFile(file.getAbsolutePath());
        Survey survey = new Survey(getDefaultName(file));
        parse(text, survey);
        return survey;
    }

    public static void parse(String text, Survey survey) throws Exception {

        Map<String, Station> nameToStation = new HashMap<>();

        String[] lines = text.split("\n");
        for (String line : lines) {

            String trimmed = line.trim();
            if (trimmed.equals("") || trimmed.startsWith(";") || trimmed.startsWith("*")) {
                continue;
            }

            try {
                String comment = "";
                if (line.contains(";")) {
                    comment = line.substring(line.indexOf(";") + 1).trim();
                }

                String[] fields = line.trim().split("\\s+");
                addLegToSurvey(survey, nameToStation, fields, comment);

            } catch (Exception exception) {
                throw new Exception("Error importing this line: " + line);
            }
        }
    }

    private static void addLegToSurvey(
            Survey survey, Map<String, Station> nameToStation, String[] fields, String comment) {

        String fromName = fields[0];
        String toName = fields[1];
        double distance = Double.parseDouble(fields[2]);
        double azimuth = Double.parseDouble(fields[3]);
        double inclination = Double.parseDouble(fields[4]);

        if (nameToStation.size() == 0) {
            Station origin = new Station(fromName);
            survey.setOrigin(origin);
            nameToStation.put(origin.getName(), origin);
        }

        String commentInstructions = extractCommentInstructions(comment);
        if (commentInstructions != null) {
            comment = comment.replace(commentInstructions, "").trim();
        }

        Station from = retrieveOrCreateStation(nameToStation, fromName, "");
        Station to = retrieveOrCreateStation(nameToStation, toName, comment);

        Leg[] promotedFrom = parseAnyPromotedLegs(commentInstructions);
        Leg leg = (to == Survey.NULL_STATION)?
                new Leg(distance, azimuth, inclination) :
                new Leg(distance, azimuth, inclination, to, promotedFrom);

        from.addOnwardLeg(leg);

        // FIXME: bit of a hack; hopefully the last station processed will be the active one
        // (should probably record the active station in the file somewhere)
        survey.setActiveStation(from);
    }

    private static Station retrieveOrCreateStation(Map<String, Station> nameToStation,
                                                   String name, String comment) {
        if (name.equals(SexyTopo.BLANK_STATION_NAME)) {
            return Survey.NULL_STATION;
        } else if (nameToStation.containsKey(name)) {
            return nameToStation.get(name);
        } else {
            Station station = new Station(name, comment);
            nameToStation.put(name, station);
            return station;
        }
    }

    private static String extractCommentInstructions(String comment) {
        Matcher matcher = COMMENT_INSTRUCTION_REGEX.matcher(comment);
        String instructions = null;
        if (matcher.find()) {
            instructions = matcher.group(1);
        }
        return instructions;
    }

    public static Leg[] parseAnyPromotedLegs(String instructions) {
        try {
            List<Leg> legs = new ArrayList<>();

            instructions = instructions.replace("{", "");
            instructions = instructions.replace("}", "");
            if (instructions.contains("from:")) {
                instructions = instructions.replace("from:", "");
            } else {
                throw new Exception("Not an instruction we understand");
            }

            String[] legStrings = instructions.split(",");

            for (String legString : legStrings) {
                legString = legString.trim();
                String[] fieldStrings = legString.split(" ");
                double distance = Double.parseDouble(fieldStrings[0].trim());
                double azimuth = Double.parseDouble(fieldStrings[1].trim());
                double inclination = Double.parseDouble(fieldStrings[2].trim());
                legs.add(new Leg(distance, azimuth, inclination));
            }

            return legs.toArray(new Leg[]{});
        } catch (Exception exception) {
            Log.e("Error parsing promoted leg (ignoring and carrying on): " + exception);
            return new Leg[]{}; // seems to be corrupted... feature not mission-critical so bail
        }
    }


    @Override
    public boolean canHandleFile(File file) {
        return file.getName().endsWith("svx");
    }
}
