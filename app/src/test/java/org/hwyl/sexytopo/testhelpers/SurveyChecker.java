package org.hwyl.sexytopo.testhelpers;

import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.model.survey.Trip;

import java.text.DecimalFormat;
import java.util.List;


public class SurveyChecker {

    public static final DecimalFormat FORMAT_2DP = new DecimalFormat("#.00");
    public static final DecimalFormat FORMAT_3DP = new DecimalFormat("#.000");

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

        List<Trip> oneTrips = one.getTrips();
        List<Trip> twoTrips = two.getTrips();
        if (oneTrips.size() != twoTrips.size()) {
            return false;
        }
        for (int i = 0; i < oneTrips.size(); i++) {
            if (! oneTrips.get(i).equalsTripData(twoTrips.get(i))) {
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
