package org.hwyl.sexytopo.control.io.thirdparty.survex;

import org.hwyl.sexytopo.SexyTopo;
import org.hwyl.sexytopo.control.io.basic.Loader;
import org.hwyl.sexytopo.control.io.translation.Importer;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;

import java.io.File;
import java.util.HashMap;
import java.util.Map;


public class SurvexImporter extends Importer {

    @Override
    public Survey toSurvey(File file) throws Exception {
        String text = Loader.slurpFile(file.getAbsolutePath());
        Survey survey = new Survey(getDefaultName(file));
        parse(text, survey);
        return survey;
    }

    public static void parse(String text, Survey survey) {

        Map<String, Station> nameToStation = new HashMap<>();

        Station origin = survey.getOrigin();
        nameToStation.put(origin.getName(), origin);

        String[] lines = text.split("\n");
        for (String line : lines) {
            if (line.trim().equals("")) {
                continue;
            }

            String comment = null;
            if (line.contains("; ")) {
                comment = line.substring(line.indexOf("; "));
            }

            String[] fields = line.trim().split("\t");
            addLegToSurvey(survey, nameToStation, fields, comment);
        }
    }

    private static void addLegToSurvey(
            Survey survey, Map<String, Station> nameToStation, String[] fields, String comment) {

        Station from = retrieveOrCreateStation(nameToStation, fields[0], comment);
        Station to = retrieveOrCreateStation(nameToStation, fields[1], comment);

        double distance = Double.parseDouble(fields[2]);
        double azimuth = Double.parseDouble(fields[3]);
        double inclination = Double.parseDouble(fields[4]);

        Leg leg = (to == Survey.NULL_STATION)?
                new Leg(distance, azimuth, inclination) :
                new Leg(distance, azimuth, inclination, to);

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


    @Override
    public boolean canHandleFile(File file) {
        return file.getName().endsWith("svx");
    }
}
