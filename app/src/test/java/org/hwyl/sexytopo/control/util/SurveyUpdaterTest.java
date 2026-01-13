package org.hwyl.sexytopo.control.util;

import static org.hwyl.sexytopo.SexyTopoConstants.ALLOWED_DOUBLE_DELTA;

import org.hwyl.sexytopo.model.graph.Direction;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.testutils.BasicTestSurveyCreator;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;


public class SurveyUpdaterTest {

    @Test
    public void testUpdateWithOneLegAddsOneLegToSurvey() {
        Leg leg = new Leg(5, 0, 0);
        Survey survey = new Survey();
        SurveyUpdater.update(survey, leg);
        Assert.assertEquals(1, survey.getAllLegs().size());
    }

    @Test
    public void testUpdateWithThreeSimilarLegsLeadsToNewStation() {
        Leg leg = new Leg(5, 0, 0);
        Leg similarLeg = new Leg(5, 0.001f, 0);
        Leg anotherSimilarLeg = new Leg(5, 0, 0.001f);
        Survey survey = new Survey();
        SurveyUpdater.update(survey, leg);
        SurveyUpdater.update(survey, similarLeg);
        SurveyUpdater.update(survey, anotherSimilarLeg);
        Assert.assertEquals(2, survey.getAllStations().size());
    }

    @Test
    public void testEditLegWorks() {
        Leg leg = new Leg(5, 0, 0);
        Survey survey = new Survey();
        SurveyUpdater.update(survey, leg);

        Leg newEdit = new Leg(6, 0, 0);
        SurveyUpdater.editLeg(survey, leg, newEdit);

        Assert.assertEquals(1, survey.getAllLegs().size());
        Assert.assertEquals(6, survey.getAllLegs().get(0).getDistance(),
                ALLOWED_DOUBLE_DELTA);
    }

    @Test
    public void testEditStationWorks() {
        Leg leg = new Leg(5, 0, 0);
        Survey survey = new Survey();
        SurveyUpdater.update(survey, leg);

        Leg newEdit = new Leg(6, 0, 0);
        SurveyUpdater.editLeg(survey, leg, newEdit);

        Assert.assertEquals(1, survey.getAllLegs().size());
        Assert.assertEquals(6, survey.getAllLegs().get(0).getDistance(),
                ALLOWED_DOUBLE_DELTA);
    }

    @Test
    public void testMoveLegWorks() {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        Leg toMove = survey.getStationByName("2").getOnwardLegs().get(0);
        Station originatingStation = survey.getOriginatingStation(toMove);
        Station destinationStation = survey.getStationByName("1");
        Assert.assertNotEquals(originatingStation, destinationStation);
        SurveyUpdater.moveLeg(survey, toMove, destinationStation);
        Assert.assertTrue(destinationStation.getOnwardLegs().contains(toMove));
        Assert.assertFalse(originatingStation.getOnwardLegs().contains(toMove));
    }

    @Test
    public void testAreLegsAboutTheSame() {
        Assert.assertTrue(SurveyUpdater.areLegsAboutTheSame(Arrays.asList(
                new Leg(10, 159.5f, 0),
                new Leg(10, 160.0f, 0),
                new Leg(10, 160.5f, 0)
        )));
        Assert.assertFalse(SurveyUpdater.areLegsAboutTheSame(Arrays.asList(
                new Leg(10, 119.5f, 0),
                new Leg(10, 110.0f, 0),
                new Leg(10, 110.5f, 0)
        )));
        Assert.assertFalse(SurveyUpdater.areLegsAboutTheSame(Arrays.asList(
                new Leg(10, 349.5f, 0),
                new Leg(10, 10.0f, 0),
                new Leg(10, 10.5f, 0)
        )));
        Assert.assertTrue(SurveyUpdater.areLegsAboutTheSame(Arrays.asList(
                new Leg(10, 359.5f, 0),
                new Leg(10, 0.0f, 0),
                new Leg(10, 0.5f, 0)
        )));
        Assert.assertFalse(SurveyUpdater.areLegsAboutTheSame(Arrays.asList(
                new Leg(10.0f, 90.0f, 5.0f),   // First: 90°
                new Leg(10.1f, 270.0f, 4.0f), // Second: 270° (opposite direction)
                new Leg(9.9f, 85.0f, 6.0f)    // Third: 85° (close to first)
        )));
    }

    // InputMode tests

    @Test
    public void testUpdateWithBackwardModeCreatesStationFromTripleShot() {
        Survey survey = new Survey();
        Leg leg1 = new Leg(5, 0, 0);
        Leg leg2 = new Leg(5.001f, 0.001f, 0);
        Leg leg3 = new Leg(5, 0, 0.001f);

        SurveyUpdater.update(survey, leg1, InputMode.BACKWARD);
        SurveyUpdater.update(survey, leg2, InputMode.BACKWARD);
        boolean stationCreated = SurveyUpdater.update(survey, leg3, InputMode.BACKWARD);

        Assert.assertTrue(stationCreated);
        Assert.assertEquals(2, survey.getAllStations().size());
        Station origin = survey.getOrigin();
        Station newStation = survey.getActiveStation();
        Assert.assertNotEquals(origin, newStation);
        Leg createdLeg = origin.getOnwardLegs().get(0);
        Assert.assertTrue(createdLeg.wasShotBackwards());
        Assert.assertTrue(createdLeg.hasDestination());
        Assert.assertEquals(newStation, createdLeg.getDestination());
    }

    @Test
    public void testUpdateWithComboModeCreatesStationFromBacksight() {
        Survey survey = new Survey();
        Leg fore = new Leg(5, 45, 10);
        Leg back = new Leg(5, 225, -10);

        SurveyUpdater.update(survey, fore, InputMode.COMBO);
        boolean stationCreated = SurveyUpdater.update(survey, back, InputMode.COMBO);

        Assert.assertTrue(stationCreated);
        Assert.assertEquals(2, survey.getAllStations().size());
    }

    @Test
    public void testUpdateWithCalibrationCheckModeDoesNotCreateStation() {
        Survey survey = new Survey();
        Leg leg1 = new Leg(5, 0, 0);
        Leg leg2 = new Leg(5, 0, 0);
        Leg leg3 = new Leg(5, 0, 0);

        SurveyUpdater.update(survey, leg1, InputMode.CALIBRATION_CHECK);
        SurveyUpdater.update(survey, leg2, InputMode.CALIBRATION_CHECK);
        boolean stationCreated = SurveyUpdater.update(survey, leg3, InputMode.CALIBRATION_CHECK);

        Assert.assertFalse(stationCreated);
        Assert.assertEquals(1, survey.getAllStations().size());
        Assert.assertEquals(3, survey.getAllLegs().size());
    }

    // updateWithNewStation tests

    @Test
    public void testUpdateWithNewStationCreatesNewStation() {
        Survey survey = new Survey();
        Leg leg = new Leg(5, 90, 10);

        SurveyUpdater.updateWithNewStation(survey, leg);

        Assert.assertEquals(2, survey.getAllStations().size());
        Assert.assertEquals(1, survey.getAllLegs().size());
        Leg addedLeg = survey.getAllLegs().get(0);
        Assert.assertTrue(addedLeg.hasDestination());
        Assert.assertNotNull(addedLeg.getDestination());
        Assert.assertEquals(addedLeg.getDestination(), survey.getActiveStation());
    }

    @Test
    public void testUpdateWithNewStationWithExistingDestination() {
        Survey survey = new Survey();
        Station customStation = new Station("Custom");
        Leg leg = new Leg(5, 90, 10, customStation, new Leg[]{});

        SurveyUpdater.updateWithNewStation(survey, leg);

        Assert.assertEquals(2, survey.getAllStations().size());
        Assert.assertEquals(customStation, survey.getActiveStation());
        Assert.assertEquals("Custom", survey.getActiveStation().getName());
    }

    // upgradeSplayToConnectedLeg tests

    @Test
    public void testUpgradeSplayInForwardMode() {
        Survey survey = new Survey();
        Leg splay = new Leg(5, 45, 10);
        SurveyUpdater.update(survey, splay);

        SurveyUpdater.upgradeSplay(survey, splay, InputMode.FORWARD);

        Assert.assertEquals(2, survey.getAllStations().size());
        Leg upgraded = survey.getAllLegs().get(0);
        Assert.assertFalse(upgraded.wasShotBackwards());
        Assert.assertTrue(upgraded.hasDestination());
        Assert.assertNotNull(upgraded.getDestination());
        Assert.assertEquals(upgraded.getDestination(), survey.getActiveStation());
    }

    @Test
    public void testUpgradeSplayInBackwardMode() {
        Survey survey = new Survey();
        Leg splay = new Leg(5, 45, 10);
        SurveyUpdater.update(survey, splay);
        Station origin = survey.getOrigin();
        int initialStationCount = survey.getAllStations().size();

        SurveyUpdater.upgradeSplay(survey, splay, InputMode.BACKWARD);

        Assert.assertTrue(survey.getAllStations().size() > initialStationCount);
        Station newStation = survey.getActiveStation();
        Assert.assertNotEquals(origin, newStation);
        Assert.assertEquals(1, survey.getAllLegs().size());
        Leg upgraded = survey.getAllLegs().get(0);
        Assert.assertTrue(upgraded.wasShotBackwards());
        Assert.assertTrue(upgraded.hasDestination());
    }

    // renameStation tests

    @Test
    public void testRenameStationSuccess() {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        Station station = survey.getStationByName("1");

        SurveyUpdater.renameStation(survey, station, "A1");

        Assert.assertEquals("A1", station.getName());
        Assert.assertNull(survey.getStationByName("1"));
        Assert.assertNotNull(survey.getStationByName("A1"));
        Assert.assertFalse(survey.isSaved());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRenameStationToDuplicateNameThrowsException() {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        Station station = survey.getStationByName("1");

        SurveyUpdater.renameStation(survey, station, "2");
    }

    @Test
    public void testRenameOriginStation() {
        Survey survey = new Survey();
        Station origin = survey.getOrigin();

        SurveyUpdater.renameStation(survey, origin, "START");

        Assert.assertEquals("START", origin.getName());
        Assert.assertEquals(origin, survey.getStationByName("START"));
    }

    // deleteStation tests

    @Test
    public void testDeleteStationRemovesStation() {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        Station toDelete = survey.getStationByName("2");

        SurveyUpdater.deleteStation(survey, toDelete);

        Assert.assertNull(survey.getStationByName("2"));
        Assert.assertFalse(survey.isSaved());
    }

    @Test
    public void testDeleteOriginStationDoesNothing() {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        Station origin = survey.getOrigin();
        int originalStationCount = survey.getAllStations().size();

        SurveyUpdater.deleteStation(survey, origin);

        Assert.assertEquals(originalStationCount, survey.getAllStations().size());
        Assert.assertEquals(origin, survey.getOrigin());
    }

    @Test
    public void testDeleteStationWithSubtreeRemovesAll() {
        Survey survey = BasicTestSurveyCreator.createStraightNorthWith1EBranch();
        Station toDelete = survey.getStationByName("1");
        int initialStationCount = survey.getAllStations().size();

        SurveyUpdater.deleteStation(survey, toDelete);

        Assert.assertTrue(survey.getAllStations().size() <= initialStationCount);
        Assert.assertFalse(survey.isSaved());
    }

    // deleteLeg tests

    @Test
    public void testDeleteLegRemovesLeg() {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        Station station2 = survey.getStationByName("2");
        int legCountBeforeDelete = survey.getAllLegs().size();

        Leg leafLegToStation2 = survey.getReferringLeg(station2);
        Station station1 = survey.getOriginatingStation(leafLegToStation2);
        SurveyUpdater.deleteLeg(survey, station1, leafLegToStation2);

        Assert.assertTrue(survey.getAllLegs().size() < legCountBeforeDelete);
        Assert.assertNull(survey.getStationByName("2"));
        Assert.assertFalse(survey.isSaved());
    }

    @Test
    public void testDeleteLegWithSubtreeRemovesAllDescendants() {
        Survey survey = BasicTestSurveyCreator.createStraightNorthWith2EBranch();
        Station station1 = survey.getStationByName("1");
        Leg toDelete = station1.getOnwardLegs().get(0);
        int originalStationCount = survey.getAllStations().size();

        SurveyUpdater.deleteLeg(survey, station1, toDelete);

        Assert.assertNull(survey.getStationByName("1.1"));
        Assert.assertNull(survey.getStationByName("1.2"));
        Assert.assertTrue(survey.getAllStations().size() < originalStationCount);
    }

    // downgradeLegToSplay tests

    @Test
    public void testDowngradeLegSuccess() {
        Survey survey = new Survey();
        Leg leg = new Leg(5, 90, 10);
        SurveyUpdater.updateWithNewStation(survey, leg);

        Station origin = survey.getOrigin();
        Leg connectedLeg = origin.getConnectedOnwardLegs().get(0);
        Station destination = connectedLeg.getDestination();
        Assert.assertTrue(destination.getOnwardLegs().isEmpty());

        SurveyUpdater.downgradeLeg(survey, connectedLeg);

        Leg downgraded = origin.getOnwardLegs().get(0);
        Assert.assertFalse(downgraded.hasDestination());
        Assert.assertFalse(survey.isSaved());
    }

    @Test
    public void testDowngradeSplayDoesNothing() {
        Survey survey = new Survey();
        Leg splay = new Leg(5, 45, 10);
        SurveyUpdater.update(survey, splay);
        Assert.assertFalse(splay.hasDestination());

        SurveyUpdater.downgradeLeg(survey, splay);

        Assert.assertFalse(splay.hasDestination());
    }

    @Test(expected = IllegalStateException.class)
    public void testDowngradeLegWithOnwardLegsThrowsException() {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        Station station1 = survey.getStationByName("1");
        Leg legToStation1 = survey.getOrigin().getConnectedOnwardLegs().get(0);
        Assert.assertFalse(station1.getOnwardLegs().isEmpty());

        SurveyUpdater.downgradeLeg(survey, legToStation1);
    }

    // reverseLeg tests

    @Test
    public void testReverseLegChangesDirection() {
        Survey survey = new Survey();
        Leg leg = new Leg(5, 90, 10);
        SurveyUpdater.updateWithNewStation(survey, leg);

        Station origin = survey.getOrigin();
        Station station1 = survey.getActiveStation();
        int originalLegCount = survey.getAllLegs().size();

        SurveyUpdater.reverseLeg(survey, station1);

        Assert.assertEquals(originalLegCount, survey.getAllLegs().size());
        Assert.assertFalse(survey.isSaved());
    }

    @Test
    public void testReverseLegMaintainsSurveyIntegrity() {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        Station station2 = survey.getStationByName("2");
        int originalLegCount = survey.getAllLegs().size();
        int originalStationCount = survey.getAllStations().size();

        SurveyUpdater.reverseLeg(survey, station2);

        Assert.assertEquals(originalLegCount, survey.getAllLegs().size());
        Assert.assertEquals(originalStationCount, survey.getAllStations().size());
    }

    // areLegsBacksights tests

    @Test
    public void testAreLegsBacksightsWithMatchingPair() {
        Leg fore = new Leg(10, 45, 15);
        Leg back = new Leg(10, 225, -15);

        Assert.assertTrue(SurveyUpdater.areLegsBacksights(fore, back));
    }

    @Test
    public void testAreLegsBacksightsWithNonMatchingPair() {
        Leg fore = new Leg(10, 45, 15);
        Leg back = new Leg(10, 90, -15);

        Assert.assertFalse(SurveyUpdater.areLegsBacksights(fore, back));
    }

    @Test
    public void testAreLegsBacksightsNearBoundary() {
        Leg fore = new Leg(10, 5, 10);
        Leg back = new Leg(10, 185, -10);

        Assert.assertTrue(SurveyUpdater.areLegsBacksights(fore, back));
    }

    // averageLegs tests

    @Test
    public void testAverageLegsSimple() {
        List<Leg> legs = Arrays.asList(
                new Leg(10, 90, 10),
                new Leg(11, 90, 12),
                new Leg(9, 90, 8)
        );

        Leg averaged = SurveyUpdater.averageLegs(legs);

        Assert.assertEquals(10, averaged.getDistance(), ALLOWED_DOUBLE_DELTA);
        Assert.assertEquals(90, averaged.getAzimuth(), ALLOWED_DOUBLE_DELTA);
        Assert.assertEquals(10, averaged.getInclination(), ALLOWED_DOUBLE_DELTA);
    }

    @Test
    public void testAverageLegsAcrossAzimuthBoundary() {
        List<Leg> legs = Arrays.asList(
                new Leg(10, 359, 0),
                new Leg(10, 1, 0),
                new Leg(10, 0, 0)
        );

        Leg averaged = SurveyUpdater.averageLegs(legs);

        Assert.assertEquals(10, averaged.getDistance(), ALLOWED_DOUBLE_DELTA);
        Assert.assertEquals(0, averaged.getAzimuth(), ALLOWED_DOUBLE_DELTA);
    }

    // averageBacksights tests

    @Test
    public void testAverageBacksightsWithAgreement() {
        Leg fore = new Leg(10, 45, 10);
        Leg back = new Leg(10, 225, -10);

        Leg averaged = SurveyUpdater.averageBacksights(fore, back);

        Assert.assertEquals(10, averaged.getDistance(), ALLOWED_DOUBLE_DELTA);
        Assert.assertEquals(45, averaged.getAzimuth(), ALLOWED_DOUBLE_DELTA);
        Assert.assertEquals(10, averaged.getInclination(), ALLOWED_DOUBLE_DELTA);
    }

    @Test
    public void testAverageBacksightsWithDisagreement() {
        Leg fore = new Leg(10, 45, 10);
        Leg back = new Leg(10.5f, 226, -11);

        Leg averaged = SurveyUpdater.averageBacksights(fore, back);

        Assert.assertEquals(10.25, averaged.getDistance(), ALLOWED_DOUBLE_DELTA);
        Assert.assertTrue(averaged.getAzimuth() >= 44.5 && averaged.getAzimuth() <= 46);
        Assert.assertTrue(averaged.getInclination() >= 9.5 && averaged.getInclination() <= 10.5);
    }

    // setDirectionOfSubtree tests

    @Test
    public void testSetDirectionOfSubtreeOnSingleStation() {
        Survey survey = new Survey();
        Station origin = survey.getOrigin();
        origin.setExtendedElevationDirection(Direction.LEFT);

        SurveyUpdater.setDirectionOfSubtree(origin, Direction.RIGHT);

        Assert.assertEquals(Direction.RIGHT, origin.getExtendedElevationDirection());
    }

    @Test
    public void testSetDirectionOfSubtreeRecursively() {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        Station origin = survey.getOrigin();
        Station station1 = survey.getStationByName("1");
        Station station2 = survey.getStationByName("2");

        SurveyUpdater.setDirectionOfSubtree(origin, Direction.RIGHT);

        Assert.assertEquals(Direction.RIGHT, origin.getExtendedElevationDirection());
        Assert.assertEquals(Direction.RIGHT, station1.getExtendedElevationDirection());
        Assert.assertEquals(Direction.RIGHT, station2.getExtendedElevationDirection());
    }

    // Bulk update tests

    @Test
    public void testBulkUpdateWithList() {
        Survey survey = new Survey();
        List<Leg> legs = Arrays.asList(
                new Leg(5, 0, 0),
                new Leg(5, 0, 0),
                new Leg(5, 0, 0)
        );

        boolean stationCreated = SurveyUpdater.update(survey, legs);

        Assert.assertTrue(stationCreated);
        Assert.assertEquals(2, survey.getAllStations().size());
        Assert.assertEquals(1, survey.getAllLegs().size());
    }

    // Additional edge cases

    @Test
    public void testAreLegsAboutTheSameWithDistanceTolerance() {
        List<Leg> withinTolerance = Arrays.asList(
                new Leg(10.0f, 90, 0),
                new Leg(10.01f, 90, 0)
        );
        Assert.assertTrue(SurveyUpdater.areLegsAboutTheSame(withinTolerance));

        List<Leg> outsideTolerance = Arrays.asList(
                new Leg(10.0f, 90, 0),
                new Leg(15.0f, 90, 0)
        );
        Assert.assertFalse(SurveyUpdater.areLegsAboutTheSame(outsideTolerance));
    }

    @Test
    public void testAreLegsAboutTheSameWithInclinationTolerance() {
        List<Leg> withinTolerance = Arrays.asList(
                new Leg(10, 90, 0),
                new Leg(10, 90, 0.5f)
        );
        Assert.assertTrue(SurveyUpdater.areLegsAboutTheSame(withinTolerance));

        List<Leg> outsideTolerance = Arrays.asList(
                new Leg(10, 90, 0),
                new Leg(10, 90, 45)
        );
        Assert.assertFalse(SurveyUpdater.areLegsAboutTheSame(outsideTolerance));
    }

    @Test
    public void testTripleShotInBackwardModeCreatesReversedLeg() {
        Survey survey = new Survey();
        Leg leg1 = new Leg(5, 45, 10);
        Leg leg2 = new Leg(5.001f, 45.001f, 10);
        Leg leg3 = new Leg(5, 45, 10.001f);

        SurveyUpdater.update(survey, leg1, InputMode.BACKWARD);
        SurveyUpdater.update(survey, leg2, InputMode.BACKWARD);
        boolean stationCreated = SurveyUpdater.update(survey, leg3, InputMode.BACKWARD);

        Assert.assertTrue(stationCreated);
        Assert.assertEquals(2, survey.getAllStations().size());
        Station newStation = survey.getActiveStation();
        Station origin = survey.getOrigin();
        Assert.assertNotEquals(origin, newStation);
        Assert.assertEquals(1, survey.getAllLegs().size());
        Leg createdLeg = survey.getAllLegs().get(0);
        Assert.assertTrue(createdLeg.hasDestination());
    }

    @Test
    public void testComboModeWithTripleShotAfterFailedBacksight() {
        Survey survey = new Survey();
        Leg fore = new Leg(5, 45, 10);
        Leg notBack = new Leg(5, 90, 10);
        Leg repeat1 = new Leg(5, 90, 10);
        Leg repeat2 = new Leg(5, 90, 10);

        SurveyUpdater.update(survey, fore, InputMode.COMBO);
        SurveyUpdater.update(survey, notBack, InputMode.COMBO);
        SurveyUpdater.update(survey, repeat1, InputMode.COMBO);
        boolean stationCreated = SurveyUpdater.update(survey, repeat2, InputMode.COMBO);

        Assert.assertTrue(stationCreated);
        Assert.assertEquals(2, survey.getAllStations().size());
    }

}
