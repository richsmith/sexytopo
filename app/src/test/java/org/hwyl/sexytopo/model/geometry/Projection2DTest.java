package org.hwyl.sexytopo.model.geometry;

import org.hwyl.sexytopo.control.util.SurveyUpdater;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.testutils.BasicTestSurveyCreator;
import org.junit.Assert;
import org.junit.Test;

public class Projection2DTest {

    @Test
    public void testFromAbbreviationRoundTripsEveryProjection() {
        for (Projection2D projection : Projection2D.values()) {
            Assert.assertSame(
                    projection, Projection2D.fromAbbreviation(projection.getAbbreviation()));
        }
    }

    @Test
    public void testFromAbbreviationFindsExtendedElevation() {
        Assert.assertSame(
                Projection2D.EXTENDED_ELEVATION,
                Projection2D.fromAbbreviation(Projection2D.EXTENDED_ELEVATION.getAbbreviation()));
    }

    @Test
    public void testFromAbbreviationDefaultsToPlanWhenNull() {
        Assert.assertSame(Projection2D.PLAN, Projection2D.fromAbbreviation(null));
    }

    @Test
    public void testFromAbbreviationDefaultsToPlanWhenUnknown() {
        Assert.assertSame(Projection2D.PLAN, Projection2D.fromAbbreviation("not-a-projection"));
        Assert.assertSame(Projection2D.PLAN, Projection2D.fromAbbreviation(""));
    }

    private static Leg addSplay(Survey survey, Station station, float azimuth) {
        Leg splay = new Leg(2, azimuth, 0);
        station.addOnwardLeg(splay);
        survey.addLegRecord(splay);
        return splay;
    }

    @Test
    public void testHiddenSplayIsNotProjectedInPlan() {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        Leg visible = addSplay(survey, survey.getOrigin(), 90);
        Leg hidden = addSplay(survey, survey.getOrigin(), 270);
        int stationCount = Projection2D.PLAN.project(survey).getStationMap().size();
        SurveyUpdater.setSplayHidden(survey, hidden, true);

        Space<Coord2D> plan = Projection2D.PLAN.project(survey);

        Assert.assertTrue(plan.getLegMap().containsKey(visible));
        Assert.assertFalse(plan.getLegMap().containsKey(hidden));
        Assert.assertEquals(stationCount, plan.getStationMap().size());
    }

    @Test
    public void testHiddenSplayIsNotProjectedInExtendedElevation() {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        Station station = survey.getStationByName("2");
        Leg visible = addSplay(survey, station, 90);
        Leg hidden = addSplay(survey, station, 270);
        int stationCount = Projection2D.EXTENDED_ELEVATION.project(survey).getStationMap().size();
        SurveyUpdater.setSplayHidden(survey, hidden, true);

        Space<Coord2D> elevation = Projection2D.EXTENDED_ELEVATION.project(survey);

        Assert.assertTrue(elevation.getLegMap().containsKey(visible));
        Assert.assertFalse(elevation.getLegMap().containsKey(hidden));
        Assert.assertEquals(stationCount, elevation.getStationMap().size());
    }

    @Test
    public void testHiddenSplayIsNotIn3DSpace() {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        Leg visible = addSplay(survey, survey.getOrigin(), 90);
        Leg hidden = addSplay(survey, survey.getOrigin(), 270);
        SurveyUpdater.setSplayHidden(survey, hidden, true);

        Space<Coord3D> space = Projection2D.PLAN.transform(survey);

        Assert.assertTrue(space.getLegMap().containsKey(visible));
        Assert.assertFalse(space.getLegMap().containsKey(hidden));
    }

    @Test
    public void testShowingAHiddenSplayProjectsItAgain() {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        Leg splay = addSplay(survey, survey.getOrigin(), 270);
        SurveyUpdater.setSplayHidden(survey, splay, true);
        Assert.assertFalse(Projection2D.PLAN.project(survey).getLegMap().containsKey(splay));

        SurveyUpdater.setSplayHidden(survey, splay, false);

        Assert.assertTrue(Projection2D.PLAN.project(survey).getLegMap().containsKey(splay));
    }
}
