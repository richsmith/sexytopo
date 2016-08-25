package org.hwyl.sexytopo.control.util;

import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;


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
        for (int i = 1; ; i++) {
            String candidateName = i + ".1";
            if (survey.getStationByName(candidateName) == null) {
                return candidateName;
            }
        }
    }

    public static String generateNextStationInLine(Survey survey, String originatingName) {
        String candidateName = TextTools.advanceLastNumber(originatingName);
        while (survey.getStationByName(candidateName) != null) {
            candidateName = TextTools.advanceLastNumber(originatingName);
        }
        return candidateName;
    }

}
