package org.hwyl.sexytopo.control.util;

import static org.hwyl.sexytopo.SexyTopoConstants.ALLOWED_DOUBLE_DELTA;

import java.util.Arrays;
import java.util.List;
import org.hwyl.sexytopo.model.geometry.ExtendedElevationDirection;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.testutils.BasicTestSurveyCreator;
import org.hwyl.sexytopo.testutils.SurveyAssertions;
import org.junit.Assert;
import org.junit.Test;

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
        Assert.assertEquals(6, survey.getAllLegs().get(0).getDistance(), ALLOWED_DOUBLE_DELTA);
    }

    @Test
    public void testEditStationWorks() {
        Leg leg = new Leg(5, 0, 0);
        Survey survey = new Survey();
        SurveyUpdater.update(survey, leg);

        Leg newEdit = new Leg(6, 0, 0);
        SurveyUpdater.editLeg(survey, leg, newEdit);

        Assert.assertEquals(1, survey.getAllLegs().size());
        Assert.assertEquals(6, survey.getAllLegs().get(0).getDistance(), ALLOWED_DOUBLE_DELTA);
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
        Assert.assertTrue(
                SurveyUpdater.areLegsAboutTheSame(
                        Arrays.asList(
                                new Leg(10, 159.5f, 0),
                                new Leg(10, 160.0f, 0),
                                new Leg(10, 160.5f, 0))));
        Assert.assertFalse(
                SurveyUpdater.areLegsAboutTheSame(
                        Arrays.asList(
                                new Leg(10, 119.5f, 0),
                                new Leg(10, 110.0f, 0),
                                new Leg(10, 110.5f, 0))));
        Assert.assertFalse(
                SurveyUpdater.areLegsAboutTheSame(
                        Arrays.asList(
                                new Leg(10, 349.5f, 0),
                                new Leg(10, 10.0f, 0),
                                new Leg(10, 10.5f, 0))));
        Assert.assertTrue(
                SurveyUpdater.areLegsAboutTheSame(
                        Arrays.asList(
                                new Leg(10, 359.5f, 0),
                                new Leg(10, 0.0f, 0),
                                new Leg(10, 0.5f, 0))));
        Assert.assertFalse(
                SurveyUpdater.areLegsAboutTheSame(
                        Arrays.asList(
                                new Leg(10.0f, 90.0f, 5.0f), // First: 90°
                                new Leg(10.1f, 270.0f, 4.0f), // Second: 270° (opposite direction)
                                new Leg(9.9f, 85.0f, 6.0f) // Third: 85° (close to first)
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
        Leg leg = new Leg(5, 90, 10, customStation, new Leg[] {});

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
        List<Leg> legs = Arrays.asList(new Leg(10, 90, 10), new Leg(11, 90, 12), new Leg(9, 90, 8));

        Leg averaged = SurveyUpdater.averageLegs(legs);

        Assert.assertEquals(10, averaged.getDistance(), ALLOWED_DOUBLE_DELTA);
        Assert.assertEquals(90, averaged.getAzimuth(), ALLOWED_DOUBLE_DELTA);
        Assert.assertEquals(10, averaged.getInclination(), ALLOWED_DOUBLE_DELTA);
    }

    @Test
    public void testAverageLegsAcrossAzimuthBoundary() {
        List<Leg> legs = Arrays.asList(new Leg(10, 359, 0), new Leg(10, 1, 0), new Leg(10, 0, 0));

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

    // setExtendedElevationDirectionOfSubtree tests

    @Test
    public void testSetDirectionOfSubtreeOnSingleStation() {
        Survey survey = new Survey();
        Station origin = survey.getOrigin();
        origin.setExtendedElevationDirection(ExtendedElevationDirection.LEFT);

        SurveyUpdater.setExtendedElevationDirectionOfSubtree(
                origin, ExtendedElevationDirection.RIGHT);

        Assert.assertEquals(
                ExtendedElevationDirection.RIGHT, origin.getExtendedElevationDirection());
    }

    @Test
    public void testSetDirectionOfSubtreeRecursively() {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        Station origin = survey.getOrigin();
        Station station1 = survey.getStationByName("1");
        Station station2 = survey.getStationByName("2");

        SurveyUpdater.setExtendedElevationDirectionOfSubtree(
                origin, ExtendedElevationDirection.RIGHT);

        Assert.assertEquals(
                ExtendedElevationDirection.RIGHT, origin.getExtendedElevationDirection());
        Assert.assertEquals(
                ExtendedElevationDirection.RIGHT, station1.getExtendedElevationDirection());
        Assert.assertEquals(
                ExtendedElevationDirection.RIGHT, station2.getExtendedElevationDirection());
    }

    // Bulk update tests

    @Test
    public void testBulkUpdateWithList() {
        Survey survey = new Survey();
        List<Leg> legs = Arrays.asList(new Leg(5, 0, 0), new Leg(5, 0, 0), new Leg(5, 0, 0));

        boolean stationCreated = SurveyUpdater.update(survey, legs);

        Assert.assertTrue(stationCreated);
        Assert.assertEquals(2, survey.getAllStations().size());
        Assert.assertEquals(1, survey.getAllLegs().size());
    }

    // Additional edge cases

    @Test
    public void testAreLegsAboutTheSameWithDistanceTolerance() {
        List<Leg> withinTolerance = Arrays.asList(new Leg(10.0f, 90, 0), new Leg(10.01f, 90, 0));
        Assert.assertTrue(SurveyUpdater.areLegsAboutTheSame(withinTolerance));

        List<Leg> outsideTolerance = Arrays.asList(new Leg(10.0f, 90, 0), new Leg(15.0f, 90, 0));
        Assert.assertFalse(SurveyUpdater.areLegsAboutTheSame(outsideTolerance));
    }

    @Test
    public void testAreLegsAboutTheSameWithInclinationTolerance() {
        List<Leg> withinTolerance = Arrays.asList(new Leg(10, 90, 0), new Leg(10, 90, 0.5f));
        Assert.assertTrue(SurveyUpdater.areLegsAboutTheSame(withinTolerance));

        List<Leg> outsideTolerance = Arrays.asList(new Leg(10, 90, 0), new Leg(10, 90, 45));
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
    public void testPromoteToAboveBackwardLegKeepsLegBackwardsAndAddsUnflaggedReading() {
        Survey survey = createSurveyWithBackwardTripleShot();
        Station origin = survey.getOrigin();
        Leg backwardsLeg = origin.getConnectedOnwardLegs().get(0);
        Assert.assertTrue(backwardsLeg.wasShotBackwards());

        Leg splay = addSplayToOrigin(survey, new Leg(3, 50, 8));

        boolean success = SurveyUpdater.promoteToAboveLeg(survey, splay);
        Assert.assertTrue(success);

        Leg updatedLeg = origin.getConnectedOnwardLegs().get(0);
        Assert.assertTrue(updatedLeg.wasShotBackwards());
        Assert.assertTrue(updatedLeg.wasPromoted());
        Leg[] promotedFrom = updatedLeg.getPromotedFrom();
        Leg addedShot = promotedFrom[promotedFrom.length - 1];
        Assert.assertFalse(addedShot.wasShotBackwards());
        SurveyAssertions.assertNoBackwardSplays(survey);
    }

    @Test
    public void testPromoteToAboveBackwardLegRecalculatesPlottedNumbers() {
        Survey survey = createSurveyWithBackwardTripleShot();
        Station origin = survey.getOrigin();
        Leg splay = addSplayToOrigin(survey, new Leg(6, 272, 12));

        SurveyUpdater.promoteToAboveLeg(survey, splay);

        // readings 5/270/10 x3 and 6/272/12 average to 5.25/270.5/10.5; the leg is plotted the
        // other way round because it was shot backwards
        Leg updatedLeg = origin.getConnectedOnwardLegs().get(0);
        Assert.assertTrue(updatedLeg.wasShotBackwards());
        Assert.assertEquals(5.25f, updatedLeg.getDistance(), ALLOWED_DOUBLE_DELTA);
        Assert.assertEquals(90.5f, updatedLeg.getAzimuth(), ALLOWED_DOUBLE_DELTA);
        Assert.assertEquals(-10.5f, updatedLeg.getInclination(), ALLOWED_DOUBLE_DELTA);
    }

    @Test
    public void testPromoteToAboveBackwardLegAddsSplayToReadingsUnchanged() {
        Survey survey = createSurveyWithBackwardTripleShot();
        Station origin = survey.getOrigin();
        Leg splay = addSplayToOrigin(survey, new Leg(6, 272, 12));

        SurveyUpdater.promoteToAboveLeg(survey, splay);

        Leg[] promotedFrom = origin.getConnectedOnwardLegs().get(0).getPromotedFrom();
        Assert.assertEquals(4, promotedFrom.length);
        for (Leg reading : promotedFrom) {
            Assert.assertFalse(reading.hasDestination());
            Assert.assertFalse(reading.wasShotBackwards());
        }
        Leg added = promotedFrom[3];
        Assert.assertEquals(6f, added.getDistance(), ALLOWED_DOUBLE_DELTA);
        Assert.assertEquals(272f, added.getAzimuth(), ALLOWED_DOUBLE_DELTA);
        Assert.assertEquals(12f, added.getInclination(), ALLOWED_DOUBLE_DELTA);
    }

    @Test
    public void testPromoteToAboveBackwardLegIsStillShownBackwardsInTable() {
        Survey survey = createSurveyWithBackwardTripleShot();
        Station origin = survey.getOrigin();
        Leg splay = addSplayToOrigin(survey, new Leg(6, 272, 12));

        SurveyUpdater.promoteToAboveLeg(survey, splay);

        Leg updatedLeg = origin.getConnectedOnwardLegs().get(0);
        GraphToListTranslator.AsTakenReading reading =
                GraphToListTranslator.toAsTakenReading(
                        new GraphToListTranslator.SurveyListEntry(origin, updatedLeg));
        Assert.assertEquals(updatedLeg.getDestination(), reading.getFrom());
        Assert.assertEquals(origin, reading.getTo());
        Assert.assertEquals(270.5f, reading.getLeg().getAzimuth(), ALLOWED_DOUBLE_DELTA);
        Assert.assertEquals(10.5f, reading.getLeg().getInclination(), ALLOWED_DOUBLE_DELTA);
    }

    @Test
    public void testPromoteSeveralSplaysToAboveBackwardLeg() {
        Survey survey = createSurveyWithBackwardTripleShot();
        Station origin = survey.getOrigin();

        SurveyUpdater.promoteToAboveLeg(survey, addSplayToOrigin(survey, new Leg(6, 272, 12)));
        SurveyUpdater.promoteToAboveLeg(survey, addSplayToOrigin(survey, new Leg(5, 268, 8)));

        // 5/270/10 x3, 6/272/12 and 5/268/8 average to 5.2/270/10, plotted reversed
        Leg updatedLeg = origin.getConnectedOnwardLegs().get(0);
        Assert.assertTrue(updatedLeg.wasShotBackwards());
        Assert.assertEquals(5, updatedLeg.getPromotedFrom().length);
        Assert.assertEquals(5.2f, updatedLeg.getDistance(), ALLOWED_DOUBLE_DELTA);
        Assert.assertEquals(90f, updatedLeg.getAzimuth(), ALLOWED_DOUBLE_DELTA);
        Assert.assertEquals(-10f, updatedLeg.getInclination(), ALLOWED_DOUBLE_DELTA);
        Assert.assertEquals(1, origin.getOnwardLegs().size());
        SurveyAssertions.assertNoBackwardSplays(survey);
    }

    @Test
    public void testPromoteToAboveBackwardSingleReadingLegUsesReversedLegAsFirstReading() {
        Survey survey = new Survey();
        Leg firstSplay = new Leg(5, 45, 10);
        SurveyUpdater.update(survey, firstSplay);
        SurveyUpdater.upgradeSplay(survey, firstSplay, InputMode.BACKWARD);
        Station origin = survey.getOrigin();
        Leg backwardsLeg = origin.getConnectedOnwardLegs().get(0);
        Assert.assertTrue(backwardsLeg.wasShotBackwards());
        Assert.assertFalse(backwardsLeg.wasPromoted());
        Leg splay = addSplayToOrigin(survey, new Leg(7, 47, 12));

        SurveyUpdater.promoteToAboveLeg(survey, splay);

        Leg updatedLeg = origin.getConnectedOnwardLegs().get(0);
        Assert.assertTrue(updatedLeg.wasShotBackwards());
        Leg[] promotedFrom = updatedLeg.getPromotedFrom();
        Assert.assertEquals(2, promotedFrom.length);
        // the leg was stored reversed, so the reading it came from is the original splay
        Assert.assertFalse(promotedFrom[0].hasDestination());
        Assert.assertEquals(5f, promotedFrom[0].getDistance(), ALLOWED_DOUBLE_DELTA);
        Assert.assertEquals(45f, promotedFrom[0].getAzimuth(), ALLOWED_DOUBLE_DELTA);
        Assert.assertEquals(10f, promotedFrom[0].getInclination(), ALLOWED_DOUBLE_DELTA);
        Assert.assertSame(splay, promotedFrom[1]);
        // 5/45/10 and 7/47/12 average to 6/46/11, plotted reversed
        Assert.assertEquals(6f, updatedLeg.getDistance(), ALLOWED_DOUBLE_DELTA);
        Assert.assertEquals(226f, updatedLeg.getAzimuth(), ALLOWED_DOUBLE_DELTA);
        Assert.assertEquals(-11f, updatedLeg.getInclination(), ALLOWED_DOUBLE_DELTA);
        SurveyAssertions.assertNoBackwardSplays(survey);
    }

    @Test
    public void testPromoteToAboveBackwardLegKeepsLegComment() {
        Survey survey = createSurveyWithBackwardTripleShot();
        Station origin = survey.getOrigin();
        origin.getConnectedOnwardLegs().get(0).setComment("a comment");
        Leg splay = addSplayToOrigin(survey, new Leg(6, 272, 12));

        SurveyUpdater.promoteToAboveLeg(survey, splay);

        Assert.assertEquals("a comment", origin.getConnectedOnwardLegs().get(0).getComment());
    }

    @Test
    public void testPromoteToAboveForwardLegRecalculatesNumbersWithoutReversing() {
        Survey survey = new Survey();
        SurveyUpdater.update(survey, new Leg(5, 270, 10), InputMode.FORWARD);
        SurveyUpdater.update(survey, new Leg(5, 270, 10), InputMode.FORWARD);
        SurveyUpdater.update(survey, new Leg(5, 270, 10), InputMode.FORWARD);
        Station origin = survey.getOrigin();
        Leg splay = addSplayToOrigin(survey, new Leg(6, 272, 12));

        SurveyUpdater.promoteToAboveLeg(survey, splay);

        Leg updatedLeg = origin.getConnectedOnwardLegs().get(0);
        Assert.assertFalse(updatedLeg.wasShotBackwards());
        Assert.assertEquals(5.25f, updatedLeg.getDistance(), ALLOWED_DOUBLE_DELTA);
        Assert.assertEquals(270.5f, updatedLeg.getAzimuth(), ALLOWED_DOUBLE_DELTA);
        Assert.assertEquals(10.5f, updatedLeg.getInclination(), ALLOWED_DOUBLE_DELTA);
        SurveyAssertions.assertNoBackwardSplays(survey);
    }

    @Test
    public void testPromoteToAboveForwardSingleReadingLegUsesLegAsFirstReading() {
        Survey survey = new Survey();
        SurveyUpdater.updateWithNewStation(survey, new Leg(5, 90, 10));
        Station origin = survey.getOrigin();
        Leg splay = addSplayToOrigin(survey, new Leg(7, 92, 12));

        SurveyUpdater.promoteToAboveLeg(survey, splay);

        Leg updatedLeg = origin.getConnectedOnwardLegs().get(0);
        Leg[] promotedFrom = updatedLeg.getPromotedFrom();
        Assert.assertEquals(2, promotedFrom.length);
        Assert.assertFalse(promotedFrom[0].hasDestination());
        Assert.assertEquals(5f, promotedFrom[0].getDistance(), ALLOWED_DOUBLE_DELTA);
        Assert.assertEquals(90f, promotedFrom[0].getAzimuth(), ALLOWED_DOUBLE_DELTA);
        Assert.assertEquals(6f, updatedLeg.getDistance(), ALLOWED_DOUBLE_DELTA);
        Assert.assertEquals(91f, updatedLeg.getAzimuth(), ALLOWED_DOUBLE_DELTA);
        Assert.assertEquals(11f, updatedLeg.getInclination(), ALLOWED_DOUBLE_DELTA);
        SurveyAssertions.assertNoBackwardSplays(survey);
    }

    @Test
    public void testPromoteToAboveLegForwardLegDoesNotSetBackwards() {
        Survey survey = new Survey();

        Leg fore1 = new Leg(5, 45, 10);
        Leg fore2 = new Leg(5.001f, 45.001f, 10);
        Leg fore3 = new Leg(5, 45, 10.001f);
        SurveyUpdater.update(survey, fore1, InputMode.FORWARD);
        SurveyUpdater.update(survey, fore2, InputMode.FORWARD);
        SurveyUpdater.update(survey, fore3, InputMode.FORWARD);

        Station origin = survey.getOrigin();
        Leg forwardLeg = origin.getConnectedOnwardLegs().get(0);
        Assert.assertFalse(forwardLeg.wasShotBackwards());

        Leg splay = new Leg(3, 50, 8);
        origin.addOnwardLeg(splay);
        survey.addLegRecord(splay);

        boolean success = SurveyUpdater.promoteToAboveLeg(survey, splay);
        Assert.assertTrue(success);

        Leg updatedLeg = origin.getConnectedOnwardLegs().get(0);
        Assert.assertTrue(updatedLeg.wasPromoted());
        Leg[] promotedFrom = updatedLeg.getPromotedFrom();
        Leg addedShot = promotedFrom[promotedFrom.length - 1];
        Assert.assertFalse(addedShot.wasShotBackwards());
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

    @Test
    public void testDowngradeNonPromotedLegProducesOneSplay() {
        Survey survey = new Survey();
        Leg leg = new Leg(5, 90, 10);
        SurveyUpdater.updateWithNewStation(survey, leg);

        Station origin = survey.getOrigin();
        Leg connectedLeg = origin.getConnectedOnwardLegs().get(0);
        Assert.assertFalse(connectedLeg.wasPromoted());

        SurveyUpdater.downgradeLeg(survey, connectedLeg);

        List<Leg> onwardLegs = origin.getOnwardLegs();
        Assert.assertEquals(1, onwardLegs.size());
        Assert.assertFalse(onwardLegs.get(0).hasDestination());
    }

    @Test
    public void testDowngradePromotedLegRestoresAllSplays() {
        Survey survey = new Survey();
        SurveyUpdater.updateWithNewStation(survey, new Leg(5, 90, 10));

        Station origin = survey.getOrigin();
        Leg splay2 = new Leg(6, 91, 11);
        origin.addOnwardLeg(splay2);
        survey.addLegRecord(splay2);
        SurveyUpdater.promoteToAboveLeg(survey, splay2);

        Leg splay3 = new Leg(7, 92, 12);
        origin.addOnwardLeg(splay3);
        survey.addLegRecord(splay3);
        SurveyUpdater.promoteToAboveLeg(survey, splay3);

        Leg promotedLeg = origin.getConnectedOnwardLegs().get(0);
        Assert.assertTrue(promotedLeg.wasPromoted());
        Assert.assertEquals(3, promotedLeg.getPromotedFrom().length);

        SurveyUpdater.downgradeLeg(survey, promotedLeg);

        List<Leg> onwardLegs = origin.getOnwardLegs();
        Assert.assertEquals(3, onwardLegs.size());
        for (Leg splay : onwardLegs) {
            Assert.assertFalse(splay.hasDestination());
        }
    }

    @Test
    public void testDowngradePromotedLegPreservesOriginalReadings() {
        Survey survey = new Survey();
        SurveyUpdater.updateWithNewStation(survey, new Leg(5, 90, 10));

        Station origin = survey.getOrigin();
        Leg splay2 = new Leg(6, 91, 11);
        origin.addOnwardLeg(splay2);
        survey.addLegRecord(splay2);
        SurveyUpdater.promoteToAboveLeg(survey, splay2);

        Leg splay3 = new Leg(7, 92, 12);
        origin.addOnwardLeg(splay3);
        survey.addLegRecord(splay3);
        SurveyUpdater.promoteToAboveLeg(survey, splay3);

        Leg promotedLeg = origin.getConnectedOnwardLegs().get(0);
        Leg[] promotedFrom = promotedLeg.getPromotedFrom();

        SurveyUpdater.downgradeLeg(survey, promotedLeg);

        List<Leg> onwardLegs = origin.getOnwardLegs();
        Assert.assertEquals(promotedFrom.length, onwardLegs.size());
        for (int i = 0; i < promotedFrom.length; i++) {
            Assert.assertEquals(
                    promotedFrom[i].getDistance(),
                    onwardLegs.get(i).getDistance(),
                    ALLOWED_DOUBLE_DELTA);
            Assert.assertEquals(
                    promotedFrom[i].getAzimuth(),
                    onwardLegs.get(i).getAzimuth(),
                    ALLOWED_DOUBLE_DELTA);
            Assert.assertEquals(
                    promotedFrom[i].getInclination(),
                    onwardLegs.get(i).getInclination(),
                    ALLOWED_DOUBLE_DELTA);
        }
    }

    @Test
    public void testDowngradePromotedLegSurveyIntegrity() {
        Survey survey = new Survey();
        SurveyUpdater.updateWithNewStation(survey, new Leg(5, 90, 10));

        Station origin = survey.getOrigin();
        Leg splay2 = new Leg(6, 91, 11);
        origin.addOnwardLeg(splay2);
        survey.addLegRecord(splay2);
        SurveyUpdater.promoteToAboveLeg(survey, splay2);

        Leg splay3 = new Leg(7, 92, 12);
        origin.addOnwardLeg(splay3);
        survey.addLegRecord(splay3);
        SurveyUpdater.promoteToAboveLeg(survey, splay3);

        Leg promotedLeg = origin.getConnectedOnwardLegs().get(0);

        SurveyUpdater.downgradeLeg(survey, promotedLeg);

        survey.checkSurveyIntegrity();
    }

    @Test
    public void testDowngradePromotedBackwardLegRestoresRecordedReadingsAsSplays() {
        Survey survey = createSurveyWithBackwardTripleShot();
        Station origin = survey.getOrigin();
        SurveyUpdater.promoteToAboveLeg(survey, addSplayToOrigin(survey, new Leg(6, 272, 12)));
        Leg promotedLeg = origin.getConnectedOnwardLegs().get(0);
        Leg[] promotedFrom = promotedLeg.getPromotedFrom();

        SurveyUpdater.downgradeLeg(survey, promotedLeg);

        List<Leg> onwardLegs = origin.getOnwardLegs();
        Assert.assertEquals(4, onwardLegs.size());
        for (int i = 0; i < promotedFrom.length; i++) {
            Leg splay = onwardLegs.get(i);
            Assert.assertFalse(splay.hasDestination());
            Assert.assertFalse(splay.wasShotBackwards());
            Assert.assertEquals(
                    promotedFrom[i].getDistance(), splay.getDistance(), ALLOWED_DOUBLE_DELTA);
            Assert.assertEquals(
                    promotedFrom[i].getAzimuth(), splay.getAzimuth(), ALLOWED_DOUBLE_DELTA);
            Assert.assertEquals(
                    promotedFrom[i].getInclination(), splay.getInclination(), ALLOWED_DOUBLE_DELTA);
        }
        SurveyAssertions.assertNoBackwardSplays(survey);
    }

    @Test
    public void testDowngradedBackwardLegSplaysAreShownFromStationToNothingInTable() {
        Survey survey = createSurveyWithBackwardTripleShot();
        Station origin = survey.getOrigin();
        SurveyUpdater.promoteToAboveLeg(survey, addSplayToOrigin(survey, new Leg(6, 272, 12)));

        SurveyUpdater.downgradeLeg(survey, origin.getConnectedOnwardLegs().get(0));

        for (Leg splay : origin.getOnwardLegs()) {
            GraphToListTranslator.AsTakenReading reading =
                    GraphToListTranslator.toAsTakenReading(
                            new GraphToListTranslator.SurveyListEntry(origin, splay));
            Assert.assertEquals(origin, reading.getFrom());
            Assert.assertEquals(Survey.NULL_STATION, reading.getTo());
            Assert.assertEquals(splay.getAzimuth(), reading.getLeg().getAzimuth(), 0.0001f);
        }
    }

    @Test
    public void testDowngradeBackwardSingleReadingLegReversesItBackToTheReading() {
        Survey survey = new Survey();
        Leg firstSplay = new Leg(5, 45, 10);
        SurveyUpdater.update(survey, firstSplay);
        SurveyUpdater.upgradeSplay(survey, firstSplay, InputMode.BACKWARD);
        Station origin = survey.getOrigin();
        Leg backwardsLeg = origin.getConnectedOnwardLegs().get(0);
        Assert.assertEquals(225f, backwardsLeg.getAzimuth(), ALLOWED_DOUBLE_DELTA);

        SurveyUpdater.downgradeLeg(survey, backwardsLeg);

        Assert.assertEquals(1, origin.getOnwardLegs().size());
        Leg splay = origin.getOnwardLegs().get(0);
        Assert.assertFalse(splay.hasDestination());
        Assert.assertFalse(splay.wasShotBackwards());
        Assert.assertEquals(5f, splay.getDistance(), ALLOWED_DOUBLE_DELTA);
        Assert.assertEquals(45f, splay.getAzimuth(), ALLOWED_DOUBLE_DELTA);
        Assert.assertEquals(10f, splay.getInclination(), ALLOWED_DOUBLE_DELTA);
        SurveyAssertions.assertNoBackwardSplays(survey);
    }

    @Test
    public void testDowngradeForwardSingleReadingLegKeepsNumbers() {
        Survey survey = new Survey();
        SurveyUpdater.updateWithNewStation(survey, new Leg(5, 90, 10));
        Station origin = survey.getOrigin();

        SurveyUpdater.downgradeLeg(survey, origin.getConnectedOnwardLegs().get(0));

        Leg splay = origin.getOnwardLegs().get(0);
        Assert.assertFalse(splay.wasShotBackwards());
        Assert.assertEquals(90f, splay.getAzimuth(), ALLOWED_DOUBLE_DELTA);
        Assert.assertEquals(10f, splay.getInclination(), ALLOWED_DOUBLE_DELTA);
    }

    @Test
    public void testPromoteThenDowngradeBackwardSingleReadingLegRestoresOriginalSplays() {
        Survey survey = new Survey();
        Leg firstSplay = new Leg(5, 45, 10);
        SurveyUpdater.update(survey, firstSplay);
        SurveyUpdater.upgradeSplay(survey, firstSplay, InputMode.BACKWARD);
        Station origin = survey.getOrigin();
        Leg secondSplay = addSplayToOrigin(survey, new Leg(7, 47, 12));
        SurveyUpdater.promoteToAboveLeg(survey, secondSplay);

        SurveyUpdater.downgradeLeg(survey, origin.getConnectedOnwardLegs().get(0));

        List<Leg> onwardLegs = origin.getOnwardLegs();
        Assert.assertEquals(2, onwardLegs.size());
        Assert.assertEquals(5f, onwardLegs.get(0).getDistance(), ALLOWED_DOUBLE_DELTA);
        Assert.assertEquals(45f, onwardLegs.get(0).getAzimuth(), ALLOWED_DOUBLE_DELTA);
        Assert.assertEquals(10f, onwardLegs.get(0).getInclination(), ALLOWED_DOUBLE_DELTA);
        Assert.assertEquals(7f, onwardLegs.get(1).getDistance(), ALLOWED_DOUBLE_DELTA);
        Assert.assertEquals(47f, onwardLegs.get(1).getAzimuth(), ALLOWED_DOUBLE_DELTA);
        Assert.assertEquals(12f, onwardLegs.get(1).getInclination(), ALLOWED_DOUBLE_DELTA);
        SurveyAssertions.assertNoBackwardSplays(survey);
    }

    @Test
    public void testCreateLegFromSingleReadingUsesItAsItIs() {
        Station destination = new Station("2");

        Leg leg =
                SurveyUpdater.createLegFromReadings(
                        Arrays.asList(new Leg(5, 45, 10)), destination, false);

        Assert.assertSame(destination, leg.getDestination());
        Assert.assertFalse(leg.wasShotBackwards());
        Assert.assertFalse(leg.wasPromoted());
        Assert.assertEquals(5f, leg.getDistance(), ALLOWED_DOUBLE_DELTA);
        Assert.assertEquals(45f, leg.getAzimuth(), ALLOWED_DOUBLE_DELTA);
        Assert.assertEquals(10f, leg.getInclination(), ALLOWED_DOUBLE_DELTA);
    }

    @Test
    public void testCreateLegFromSingleBackwardReadingReversesIt() {
        Leg leg =
                SurveyUpdater.createLegFromReadings(
                        Arrays.asList(new Leg(5, 45, 10)), new Station("2"), true);

        Assert.assertTrue(leg.wasShotBackwards());
        Assert.assertFalse(leg.wasPromoted());
        Assert.assertEquals(225f, leg.getAzimuth(), ALLOWED_DOUBLE_DELTA);
        Assert.assertEquals(-10f, leg.getInclination(), ALLOWED_DOUBLE_DELTA);
    }

    @Test
    public void testCreateLegFromRepeatedReadingsAveragesAndKeepsTheReadings() {
        Leg first = new Leg(5, 268, 8);
        Leg second = new Leg(5, 272, 12);

        Leg leg =
                SurveyUpdater.createLegFromReadings(
                        Arrays.asList(first, second), new Station("2"), false);

        Assert.assertFalse(leg.wasShotBackwards());
        Assert.assertEquals(270f, leg.getAzimuth(), ALLOWED_DOUBLE_DELTA);
        Assert.assertEquals(10f, leg.getInclination(), ALLOWED_DOUBLE_DELTA);
        Assert.assertEquals(2, leg.getPromotedFrom().length);
        Assert.assertSame(first, leg.getPromotedFrom()[0]);
        Assert.assertSame(second, leg.getPromotedFrom()[1]);
    }

    @Test
    public void testCreateLegFromRepeatedBackwardReadingsReversesOnlyTheAverage() {
        Leg first = new Leg(5, 268, 8);
        Leg second = new Leg(5, 272, 12);

        Leg leg =
                SurveyUpdater.createLegFromReadings(
                        Arrays.asList(first, second), new Station("2"), true);

        Assert.assertTrue(leg.wasShotBackwards());
        Assert.assertEquals(90f, leg.getAzimuth(), ALLOWED_DOUBLE_DELTA);
        Assert.assertEquals(-10f, leg.getInclination(), ALLOWED_DOUBLE_DELTA);
        Assert.assertEquals(2, leg.getPromotedFrom().length);
        for (Leg reading : leg.getPromotedFrom()) {
            Assert.assertFalse(reading.wasShotBackwards());
        }
        Assert.assertEquals(268f, leg.getPromotedFrom()[0].getAzimuth(), ALLOWED_DOUBLE_DELTA);
        Assert.assertEquals(272f, leg.getPromotedFrom()[1].getAzimuth(), ALLOWED_DOUBLE_DELTA);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateLegFromNoReadingsIsRejected() {
        SurveyUpdater.createLegFromReadings(Arrays.<Leg>asList(), new Station("2"), false);
    }

    // ---- splays that go to another feature rather than a wall ----

    @Test
    public void testSetSplayToWallChangesWhatTheSplayGoesToAndKeepsItsPlace() {
        Survey survey = new Survey();
        Leg first = new Leg(1, 10, 0);
        Leg second = new Leg(2, 100, 5);
        Leg third = new Leg(3, 200, 10);
        SurveyUpdater.update(survey, first);
        SurveyUpdater.update(survey, second);
        SurveyUpdater.update(survey, third);

        SurveyUpdater.setSplayToWall(survey, second, false);

        List<Leg> legs = survey.getAllLegsInChronoOrder();
        Assert.assertEquals(3, legs.size());
        Assert.assertSame(first, legs.get(0));
        Assert.assertSame(third, legs.get(2));
        Leg edited = legs.get(1);
        Assert.assertFalse(edited.isToWall());
        Assert.assertEquals(2f, edited.getDistance(), ALLOWED_DOUBLE_DELTA);
        Assert.assertEquals(100f, edited.getAzimuth(), ALLOWED_DOUBLE_DELTA);
        Assert.assertEquals(5f, edited.getInclination(), ALLOWED_DOUBLE_DELTA);
        Assert.assertEquals(3, survey.getOrigin().getOnwardLegs().size());
        Assert.assertTrue(survey.getOrigin().getOnwardLegs().contains(edited));
    }

    @Test
    public void testSetSplayToWallDoesNothingWhenTheSplayAlreadyGoesThere() {
        Survey survey = new Survey();
        Leg splay = new Leg(2, 100, 5);
        SurveyUpdater.update(survey, splay);

        SurveyUpdater.setSplayToWall(survey, splay, true);

        Assert.assertSame(splay, survey.getAllLegsInChronoOrder().get(0));
    }

    @Test
    public void testSetSplayToWallIgnoresALegWithADestination() {
        Survey survey = new Survey();
        SurveyUpdater.updateWithNewStation(survey, new Leg(5, 90, 10));
        Leg leg = survey.getOrigin().getConnectedOnwardLegs().get(0);

        SurveyUpdater.setSplayToWall(survey, leg, false);

        Assert.assertSame(leg, survey.getOrigin().getConnectedOnwardLegs().get(0));
    }

    @Test
    public void testRepeatedSplaysOfAnyTypeMakeALegAndComeBackAsTheyWere() {
        Survey survey = new Survey();
        Leg first = new Leg(5, 90, 10);
        Leg second = new Leg(5, 90, 10);
        SurveyUpdater.update(survey, first, InputMode.FORWARD);
        SurveyUpdater.update(survey, second, InputMode.FORWARD);
        SurveyUpdater.setSplayToWall(survey, second, false);
        SurveyUpdater.update(survey, new Leg(5, 90, 10), InputMode.FORWARD);

        Station origin = survey.getOrigin();
        Assert.assertEquals(1, origin.getConnectedOnwardLegs().size());
        Leg leg = origin.getConnectedOnwardLegs().get(0);
        Leg[] readings = leg.getPromotedFrom();
        Assert.assertEquals(3, readings.length);
        Assert.assertTrue(readings[0].isToWall());
        Assert.assertFalse(readings[1].isToWall());
        Assert.assertTrue(readings[2].isToWall());

        SurveyUpdater.downgradeLeg(survey, leg);

        List<Leg> splays = origin.getOnwardLegs();
        Assert.assertEquals(3, splays.size());
        Assert.assertTrue(splays.get(0).isToWall());
        Assert.assertFalse(splays.get(1).isToWall());
        Assert.assertTrue(splays.get(2).isToWall());
        SurveyAssertions.assertNoBackwardSplays(survey);
    }

    @Test
    public void testSplayPromotedToALegKeepsWhatItGoesToAsAReading() {
        Survey survey = new Survey();
        SurveyUpdater.updateWithNewStation(survey, new Leg(5, 90, 10));
        Station origin = survey.getOrigin();
        Leg splay = addSplayToOrigin(survey, new Leg(7, 92, 12));
        SurveyUpdater.setSplayToWall(survey, splay, false);
        Leg toFeature = origin.getUnconnectedOnwardLegs().get(0);

        SurveyUpdater.promoteToAboveLeg(survey, toFeature);

        Leg leg = origin.getConnectedOnwardLegs().get(0);
        Assert.assertEquals(2, leg.getPromotedFrom().length);
        Assert.assertTrue(leg.getPromotedFrom()[0].isToWall());
        Assert.assertFalse(leg.getPromotedFrom()[1].isToWall());

        SurveyUpdater.downgradeLeg(survey, leg);

        List<Leg> splays = origin.getOnwardLegs();
        Assert.assertTrue(splays.get(0).isToWall());
        Assert.assertFalse(splays.get(1).isToWall());
    }

    private static Survey createSurveyWithBackwardTripleShot() {
        Survey survey = new Survey();
        SurveyUpdater.update(survey, new Leg(5, 270, 10), InputMode.BACKWARD);
        SurveyUpdater.update(survey, new Leg(5, 270, 10), InputMode.BACKWARD);
        SurveyUpdater.update(survey, new Leg(5, 270, 10), InputMode.BACKWARD);
        return survey;
    }

    private static Leg addSplayToOrigin(Survey survey, Leg splay) {
        survey.getOrigin().addOnwardLeg(splay);
        survey.addLegRecord(splay);
        return splay;
    }
}
