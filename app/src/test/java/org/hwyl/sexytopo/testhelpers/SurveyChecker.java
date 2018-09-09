package org.hwyl.sexytopo.testhelpers;

import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;

import java.util.List;


public class SurveyChecker {

    public static boolean areEqual(Survey one, Survey two) {

        List<Station> oneStations = one.getAllStations();
        List<Station> twoStations = two.getAllStations();

        if (oneStations.size() != twoStations.size()) {
            return false;
        }

        for (Station station : one.getAllStations()) {
            Station other = two.getStationByName(station.getName());
            if (!areEqual(station, other)) {
                return false;
            }
        }

        return true;
    }


    public static boolean areEqual(Station one, Station two) {
        if ((!one.getName().equals(two.getName())) ||
            (!one.getComment().equals(two.getComment())) ||
            (!(one.getExtendedElevationDirection() != two.getExtendedElevationDirection()))) {

            return false;
        }

        List<Leg> oneLegs = one.getOnwardLegs();
        List<Leg> twoLegs = two.getOnwardLegs();

        if (oneLegs.size() != twoLegs.size()) {
            return false;
        }

        for (int i = 0; i < oneLegs.size(); i++) {
            if (!oneLegs.get(i).toString().equals(twoLegs.get(i).toString())) {
                return false;
            }
        }

        return true;
    }
}
