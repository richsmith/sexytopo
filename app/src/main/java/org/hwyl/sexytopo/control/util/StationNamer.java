package org.hwyl.sexytopo.control.util;

import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class StationNamer {

    public static String generateOriginName() {
        return Integer.toString(1);
    }


    public static String generateNextStationName(Survey survey, Station originatingStation) {

        int numberOfExistingConnections = originatingStation.getConnectedOnwardLegs().size();

        String originatingName = originatingStation.getName();

        if (numberOfExistingConnections == 0) {
            return generateNextStationInLine(survey, originatingName);
        } else {
            return generateNextBranch(survey, originatingName);
        }
    }

    public static String generateNextBranch(Survey survey, String originatingName) {
        String candidateName = originatingName + ".1";
        String uniqueName = advanceNumberIfNotUnique(survey, candidateName);
        return uniqueName;
    }

    public static String generateNextStationInLine(Survey survey, String originatingName) {
        String candidateName = TextTools.advanceLastNumber(originatingName);
        String uniqueName = advanceNumberIfNotUnique(survey, candidateName);
        return uniqueName;
    }

    public static String advanceNumberIfNotUnique(Survey survey, String candidateName) {
        Set<String> allNames = getAllStationNames(survey);
        while (allNames.contains(candidateName)) {
            candidateName = TextTools.advanceLastNumber(candidateName);
        }
        return candidateName;
    }

    public static Set<String> getAllStationNames(Survey survey) {
        List<Station> stations = survey.getAllStations();
        Set<String> allNames = new HashSet<>();
        for (Station station : stations) {
            allNames.add(station.getName());
        }
        return allNames;
    }

}
