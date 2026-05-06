package org.hwyl.sexytopo.control.util;

import org.hwyl.sexytopo.model.graph.Direction;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.junit.Assert;
import org.junit.Test;

public class SurveyUpdaterInheritedDirectionTest {

    // Three identical splays are required to trigger createNewStationIfTripleShot,
    // which is the only path that calls resolveInheritedDirection.
    private static final Leg SPLAY = new Leg(5, 0, 0);

    /**
     * Adds a new station via three matching splays, which triggers the triple-shot path and causes
     * resolveInheritedDirection to be called. Returns the newly-created station.
     */
    private Station addStationViaTripleShot(Survey survey) {
        Station before = survey.getActiveStation();
        SurveyUpdater.update(survey, SPLAY);
        SurveyUpdater.update(survey, SPLAY);
        SurveyUpdater.update(survey, SPLAY);
        Station after = survey.getActiveStation();
        Assert.assertNotSame("Triple shot should have created a new station", before, after);
        return after;
    }

    // --- Non-vertical parent ---

    @Test
    public void testNonVerticalParentInheritsOwnDirection() {
        Survey survey = new Survey();
        survey.getOrigin().setExtendedElevationDirection(Direction.LEFT);

        Station newStation = addStationViaTripleShot(survey);

        Assert.assertEquals(Direction.LEFT, newStation.getExtendedElevationDirection());
    }

    @Test
    public void testRightParentInheritsRight() {
        Survey survey = new Survey();
        survey.getOrigin().setExtendedElevationDirection(Direction.RIGHT);

        Station newStation = addStationViaTripleShot(survey);

        Assert.assertEquals(Direction.RIGHT, newStation.getExtendedElevationDirection());
    }

    // --- Single vertical parent ---

    @Test
    public void testVerticalParentInheritsRightFromGrandparent() {
        // Origin = RIGHT, then add station via triple-shot, mark it VERTICAL,
        // then add another station — should inherit RIGHT from origin.
        Survey survey = new Survey();
        survey.getOrigin().setExtendedElevationDirection(Direction.RIGHT);

        Station firstStation = addStationViaTripleShot(survey);
        firstStation.setExtendedElevationDirection(Direction.VERTICAL);

        Station secondStation = addStationViaTripleShot(survey);

        Assert.assertEquals(Direction.RIGHT, secondStation.getExtendedElevationDirection());
    }

    @Test
    public void testVerticalParentInheritsLeftFromGrandparent() {
        // Origin = LEFT, then add station via triple-shot, mark it VERTICAL,
        // then add another station — should inherit LEFT from origin.
        Survey survey = new Survey();
        survey.getOrigin().setExtendedElevationDirection(Direction.LEFT);

        Station firstStation = addStationViaTripleShot(survey);
        firstStation.setExtendedElevationDirection(Direction.VERTICAL);

        Station secondStation = addStationViaTripleShot(survey);

        Assert.assertEquals(Direction.LEFT, secondStation.getExtendedElevationDirection());
    }

    // --- Multiple consecutive vertical parents (the case the old code got wrong) ---

    @Test
    public void testTwoConsecutiveVerticalsInheritsRightFromGreatGrandparent() {
        Survey survey = new Survey();
        survey.getOrigin().setExtendedElevationDirection(Direction.RIGHT);

        Station firstStation = addStationViaTripleShot(survey);
        firstStation.setExtendedElevationDirection(Direction.VERTICAL);

        Station secondStation = addStationViaTripleShot(survey);
        secondStation.setExtendedElevationDirection(Direction.VERTICAL);

        Station thirdStation = addStationViaTripleShot(survey);

        Assert.assertEquals(Direction.RIGHT, thirdStation.getExtendedElevationDirection());
    }

    @Test
    public void testTwoConsecutiveVerticalsInheritsLeftFromGreatGrandparent() {
        Survey survey = new Survey();
        survey.getOrigin().setExtendedElevationDirection(Direction.LEFT);

        Station firstStation = addStationViaTripleShot(survey);
        firstStation.setExtendedElevationDirection(Direction.VERTICAL);

        Station secondStation = addStationViaTripleShot(survey);
        secondStation.setExtendedElevationDirection(Direction.VERTICAL);

        Station thirdStation = addStationViaTripleShot(survey);

        Assert.assertEquals(Direction.LEFT, thirdStation.getExtendedElevationDirection());
    }

    // --- Vertical at origin (edge case) ---

    @Test
    public void testVerticalOriginFallsBackToRight() {
        Survey survey = new Survey();
        survey.getOrigin().setExtendedElevationDirection(Direction.VERTICAL);

        Station newStation = addStationViaTripleShot(survey);

        Assert.assertEquals(Direction.RIGHT, newStation.getExtendedElevationDirection());
    }
}
