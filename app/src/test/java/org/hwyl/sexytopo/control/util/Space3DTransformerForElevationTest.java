package org.hwyl.sexytopo.control.util;

import org.hwyl.sexytopo.model.graph.Coord3D;
import org.hwyl.sexytopo.model.graph.Direction;
import org.hwyl.sexytopo.model.graph.Space;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Survey;
import org.junit.Assert;
import org.junit.Test;

public class Space3DTransformerForElevationTest {

    private static final double DELTA = 0.0001;
    private static final Space3DTransformerForElevation TRANSFORMER =
            new Space3DTransformerForElevation();

    // --- RIGHT direction ---

    @Test
    public void testRightLegProjectsToPositiveY() {
        Survey survey = new Survey();
        SurveyUpdater.updateWithNewStation(survey, new Leg(5, 90, 0));
        setDirectionOnStation(survey, "2", Direction.RIGHT);

        Space<Coord3D> space = TRANSFORMER.transformTo3D(survey);
        Coord3D station2 = space.getStationMap().get(survey.getStationByName("2"));

        Assert.assertTrue("Right leg should produce positive y", station2.y > 0);
    }

    @Test
    public void testRightLegZeroesXComponent() {
        Survey survey = new Survey();
        SurveyUpdater.updateWithNewStation(survey, new Leg(5, 90, 0));
        setDirectionOnStation(survey, "2", Direction.RIGHT);

        Space<Coord3D> space = TRANSFORMER.transformTo3D(survey);
        Coord3D station2 = space.getStationMap().get(survey.getStationByName("2"));

        Assert.assertEquals(0, station2.x, DELTA);
    }

    // --- LEFT direction ---

    @Test
    public void testLeftLegProjectsToNegativeY() {
        Survey survey = new Survey();
        SurveyUpdater.updateWithNewStation(survey, new Leg(5, 90, 0));
        setDirectionOnStation(survey, "2", Direction.LEFT);

        Space<Coord3D> space = TRANSFORMER.transformTo3D(survey);
        Coord3D station2 = space.getStationMap().get(survey.getStationByName("2"));

        Assert.assertTrue("Left leg should produce negative y", station2.y < 0);
    }

    @Test
    public void testLeftLegZeroesXComponent() {
        Survey survey = new Survey();
        SurveyUpdater.updateWithNewStation(survey, new Leg(5, 90, 0));
        setDirectionOnStation(survey, "2", Direction.LEFT);

        Space<Coord3D> space = TRANSFORMER.transformTo3D(survey);
        Coord3D station2 = space.getStationMap().get(survey.getStationByName("2"));

        Assert.assertEquals(0, station2.x, DELTA);
    }

    // --- VERTICAL direction ---

    @Test
    public void testVerticalLegPreservesXY() {
        Survey survey = new Survey();
        SurveyUpdater.updateWithNewStation(survey, new Leg(5, 45, 60));
        setDirectionOnStation(survey, "2", Direction.VERTICAL);

        Space<Coord3D> space = TRANSFORMER.transformTo3D(survey);
        Coord3D station2 = space.getStationMap().get(survey.getStationByName("2"));

        Assert.assertEquals(0, station2.x, DELTA);
        Assert.assertEquals(0, station2.y, DELTA);
    }

    @Test
    public void testVerticalLegCorrectZ() {
        // 10m leg at 30° inclination: z = 10 * sin(30°) = 5
        Survey survey = new Survey();
        SurveyUpdater.updateWithNewStation(survey, new Leg(10, 45, 30));
        setDirectionOnStation(survey, "2", Direction.VERTICAL);

        Space<Coord3D> space = TRANSFORMER.transformTo3D(survey);
        Coord3D station2 = space.getStationMap().get(survey.getStationByName("2"));

        Assert.assertEquals(5.0, station2.z, DELTA);
    }

    // --- Delta / subsequent leg rotation ---

    @Test
    public void testSubsequentLegAfterVerticalNotRotated() {
        // Station 2 is vertical; station 3 should be plotted relative to station 2
        // with zero rotation contributed by the vertical leg.
        // Second leg azimuth=90 with RIGHT direction -> adjustAzimuth(0) -> azimuth=0 -> positive y
        Survey survey = new Survey();
        SurveyUpdater.updateWithNewStation(survey, new Leg(5, 90, 90));
        SurveyUpdater.updateWithNewStation(survey, new Leg(5, 90, 0));

        setDirectionOnStation(survey, "2", Direction.VERTICAL);
        setDirectionOnStation(survey, "3", Direction.RIGHT);

        Space<Coord3D> space = TRANSFORMER.transformTo3D(survey);
        Coord3D station2 = space.getStationMap().get(survey.getStationByName("2"));
        Coord3D station3 = space.getStationMap().get(survey.getStationByName("3"));

        // Station 3 should be offset in y from station 2 with no x or z change
        Assert.assertTrue(
                "Station 3 should be positively offset in y from station 2 after a vertical leg",
                station3.y > station2.y);
        Assert.assertEquals(station2.x, station3.x, DELTA);
    }

    // --- Helpers ---

    private void setDirectionOnStation(Survey survey, String name, Direction direction) {
        survey.getStationByName(name).setExtendedElevationDirection(direction);
    }
}
