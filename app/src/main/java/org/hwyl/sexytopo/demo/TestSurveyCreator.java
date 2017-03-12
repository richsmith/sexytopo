package org.hwyl.sexytopo.demo;

import org.hwyl.sexytopo.control.util.SurveyUpdater;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.model.table.LRUD;

import java.util.List;
import java.util.Random;


public class TestSurveyCreator {

    private static Random random = new Random();


    public static Survey create(String name, int numStations, int numBranches) {

        Survey survey = new Survey(name);

        createBranch(survey, numStations);

        for (int i = 0; i < numBranches; i++) {
            List<Station> stations = survey.getAllStations();
            Station active = getRandom(stations);
            survey.setActiveStation(active);
            createBranch(survey, 3);
        }

        return survey;

    }


    public static void createBranch(Survey survey, int numStations) {

        for (int i = 0; i < numStations; i++) {

            double distance = 5 + random.nextInt(10);
            double azimuth = 40 + random.nextInt(100);
            double inclination = -20 + random.nextInt(40);

            Leg leg = new Leg(distance, azimuth, inclination);
            SurveyUpdater.updateWithNewStation(survey, leg);

            Station newStation = survey.getMostRecentLeg().getDestination();
            createLruds(survey, newStation);
        }

    }

    public static void createLruds(Survey survey, Station station) {
        for (LRUD lrud : LRUD.values()) {
            Leg splay = lrud.createSplay(survey, station, 1 + random.nextInt(4));
            SurveyUpdater.update(survey, splay);
        }
    }

    public static <T> T getRandom(List<T> list) {
        return list.get(random.nextInt(list.size()));
    }
}
