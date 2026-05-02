package org.hwyl.sexytopo.testutils;

import java.util.List;
import java.util.Map;
import java.util.Random;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.util.CrossSectioner;
import org.hwyl.sexytopo.control.util.SurveyUpdater;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.Projection2D;
import org.hwyl.sexytopo.model.sketch.CrossSection;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.model.table.LRUD;

/**
 * Creates a test survey with a random number of branches and stations. Unlike the other test utils
 * in this package, this is expected to be called from user-facing code, not from tests.
 */
public class ExampleSurveyCreator {

    private static final Random random = new Random();

    // number of stations to get a cross-section when requested
    private static final float X_SECTION_FRACTION = 0.25f;

    public static Survey create() {
        return create(10, 5, false);
    }

    public static Survey create(int numStations, int numBranches) {
        return create(numStations, numBranches, false);
    }

    public static Survey create(int numStations, int numBranches, boolean addCrossSections) {

        Log.i("Generating example survey");

        Survey survey = new Survey();

        createBranch(survey, numStations);

        for (int i = 0; i < numBranches; i++) {
            List<Station> stations = survey.getAllStations();
            Station active = getRandom(stations);
            survey.setActiveStation(active);
            createBranch(survey, 3);
        }

        if (addCrossSections) {
            addCrossSections(survey);
        }

        return survey;
    }

    private static void addCrossSections(Survey survey) {
        Sketch plan = survey.getSketch(Projection2D.PLAN);
        Map<Station, Coord2D> stationPositions =
                Projection2D.PLAN.project(survey).getStationMap();
        List<Station> candidates = survey.getAllStations();
        candidates.remove(survey.getOrigin());
        int count = Math.max(1, Math.round(candidates.size() * X_SECTION_FRACTION));
        for (int i = 0; i < count && !candidates.isEmpty(); i++) {
            Station station = candidates.remove(random.nextInt(candidates.size()));
            CrossSection xs = CrossSectioner.section(survey, station);
            Coord2D position = stationPositions.get(station).add(0, 5);
            plan.addCrossSection(xs, position);
        }
    }

    public static void createBranch(Survey survey, int numStations) {

        for (int i = 0; i < numStations; i++) {

            float distance = 5 + random.nextInt(10);
            float azimuth = 40 + random.nextInt(100);
            float inclination = -20 + random.nextInt(40);

            Leg leg = new Leg(distance, azimuth, inclination);
            SurveyUpdater.updateWithNewStation(survey, leg);

            Station newStation = survey.getMostRecentLeg().getDestination();
            createLruds(survey, newStation);
        }
    }

    public static void createLruds(Survey survey, Station station) {
        for (LRUD lrud : LRUD.values()) {
            Leg splay = lrud.createSplay(survey, station, LRUD.Mode.SURVEY, 1 + random.nextInt(3));
            SurveyUpdater.update(survey, splay);
        }
    }

    public static <T> T getRandom(List<T> list) {
        return list.get(random.nextInt(list.size()));
    }
}
