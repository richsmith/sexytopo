package org.hwyl.sexytopo.control.util;

import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


@SuppressWarnings("UnnecessaryLocalVariable")
public class StationNamer {


    public static String generateOriginName() {
        return Integer.toString(1);
    }


    public static String generateNextStationName(Survey survey, Station originatingStation) {
        String originatingName = originatingStation.getName();
        String newUniqueName = advanceNumberIfNotUnique(survey, originatingName);
        return newUniqueName;
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
