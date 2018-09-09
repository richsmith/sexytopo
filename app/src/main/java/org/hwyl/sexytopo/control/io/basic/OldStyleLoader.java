package org.hwyl.sexytopo.control.io.basic;

import org.hwyl.sexytopo.SexyTopo;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class OldStyleLoader {


    public static void parse(String text, Survey survey) throws Exception {

        Map<String, Station> nameToStation = new HashMap<>();

        Station origin = survey.getOrigin();
        nameToStation.put(origin.getName(), origin);

        String[] lines = text.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.equals("") || line.startsWith("*")) {
                continue;
            }

            String comment = "";
            if (line.contains("; ")) {
                comment = line.substring(line.indexOf("; ") + 2);
                comment = comment.replaceAll("\\\\n", "\n");
                line = line.substring(0, line.indexOf("; "));
            }

            String[] fields = line.trim().split("\t");
            addLegToSurvey(survey, nameToStation, fields, comment);
        }
    }

    private static void addLegToSurvey(Survey survey,
                                       Map<String, Station> nameToStation, String[] fields, String comment)
            throws Exception {

        Station from = retrieveOrCreateStation(nameToStation, fields[0], comment);
        Station to = retrieveOrCreateStation(nameToStation, fields[1], comment);

        double distance = Double.parseDouble(fields[2]);
        double azimuth = Double.parseDouble(fields[3]);
        double inclination = Double.parseDouble(fields[4]);

        List<Station> stationsSoFar = survey.getAllStations();

        if (!stationsSoFar.contains(from) && !stationsSoFar.contains(to)) {
            throw new Exception("Stations are out of order in file");
        } else if (stationsSoFar.contains(from) && stationsSoFar.contains(to)) {
            throw new Exception("Duplicate leg encountered");
        } else if (stationsSoFar.contains(from)) { // forward leg
            Leg leg = (to == Survey.NULL_STATION)?
                    new Leg(distance, azimuth, inclination) :
                    new Leg(distance, azimuth, inclination, to, new Leg[]{});
            from.addOnwardLeg(leg);
        } else if (stationsSoFar.contains(to)) { // backwards leg
            Leg leg = (from == Survey.NULL_STATION)?
                    new Leg(distance, azimuth, inclination) :
                    new Leg(distance, azimuth, inclination, from, new Leg[]{});
            to.addOnwardLeg(leg.reverse());
        }

        // Bit of a hack; hopefully the last station processed will be the active one
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
            Station station = new Station(name);
            station.setComment(comment);
            nameToStation.put(name, station);
            return station;
        }
    }

}
