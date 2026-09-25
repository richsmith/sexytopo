package org.hwyl.sexytopo.control.util;

import org.hwyl.sexytopo.SexyTopoConstants;
import org.hwyl.sexytopo.model.graph.ExtendedElevationDirection;
import org.hwyl.sexytopo.model.sketch.CrossSection;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.testutils.BasicTestSurveyCreator;
import org.junit.Assert;
import org.junit.Test;

public class CrossSectionerTest {

    @Test
    public void testStraightNorthCrossSection() {
        Survey testSurvey = BasicTestSurveyCreator.createStraightNorth();

        Station s2 = testSurvey.getStationByName("2");
        double angle = CrossSectioner.getAngleOfSection(testSurvey, s2);
        Assert.assertEquals(0.0, angle, SexyTopoConstants.ALLOWED_DOUBLE_DELTA);
    }

    @Test
    public void testStraightSouthCrossSection() {
        Survey testSurvey = BasicTestSurveyCreator.createStraightSouth();

        Station s2 = testSurvey.getStationByName("2");
        double angle = CrossSectioner.getAngleOfSection(testSurvey, s2);
        Assert.assertEquals(180.0, angle, SexyTopoConstants.ALLOWED_DOUBLE_DELTA);
    }

    @Test
    public void testCrossSectionSpanningZeroBoundary() {
        // Issue #176: legs at 350° and 10° should give cross-section at 0°, not 180°
        Survey testSurvey = BasicTestSurveyCreator.createSpanningZeroBoundary();

        Station s2 = testSurvey.getStationByName("2");
        double angle = CrossSectioner.getAngleOfSection(testSurvey, s2);
        Assert.assertEquals(0.0, angle, SexyTopoConstants.ALLOWED_DOUBLE_DELTA);
    }

    @Test
    public void testBendInPassageBisectsIncomingAndOutgoingLegs() {
        // Legs run 0 degrees then 90 degrees, so the section faces halfway between them.
        Survey testSurvey = BasicTestSurveyCreator.createRightRight();

        Station s2 = testSurvey.getStationByName("2");
        double angle = CrossSectioner.getAngleOfSection(testSurvey, s2);
        Assert.assertEquals(45.0, angle, SexyTopoConstants.ALLOWED_DOUBLE_DELTA);
    }

    @Test
    public void testEndOfPassageIsPerpendicularToTheOnlyLeg() {
        // Station 4 only has the incoming leg, which runs at 180 degrees.
        Survey testSurvey = BasicTestSurveyCreator.createRightRight();

        Station s4 = testSurvey.getStationByName("4");
        double angle = CrossSectioner.getAngleOfSection(testSurvey, s4);
        Assert.assertEquals(180.0, angle, SexyTopoConstants.ALLOWED_DOUBLE_DELTA);
    }

    @Test
    public void testOriginWithOneOutgoingLegFollowsThatLeg() {
        Survey testSurvey = BasicTestSurveyCreator.createRightRight();

        double angle = CrossSectioner.getAngleOfSection(testSurvey, testSurvey.getOrigin());
        Assert.assertEquals(0.0, angle, SexyTopoConstants.ALLOWED_DOUBLE_DELTA);
    }

    @Test
    public void testOriginWithNoLegsFallsBackToZero() {
        Survey testSurvey = BasicTestSurveyCreator.createEmptySurvey();

        double angle = CrossSectioner.getAngleOfSection(testSurvey, testSurvey.getOrigin());
        Assert.assertEquals(0.0, angle, SexyTopoConstants.ALLOWED_DOUBLE_DELTA);
    }

    @Test
    public void testStationWithSeveralWaysOnOnlyConsidersTheIncomingLeg() {
        // Arrive at station 2 heading east, then leave it both north and south. Averaging would
        // give a misleading answer, so only the incoming leg counts.
        Survey testSurvey = new Survey();
        SurveyUpdater.updateWithNewStation(testSurvey, new Leg(5, 90, 0));
        Station s2 = testSurvey.getStationByName("2");
        SurveyUpdater.updateWithNewStation(testSurvey, new Leg(5, 0, 0));
        testSurvey.setActiveStation(s2);
        SurveyUpdater.updateWithNewStation(testSurvey, new Leg(5, 180, 0));

        double angle = CrossSectioner.getAngleOfSection(testSurvey, s2);
        Assert.assertEquals(90.0, angle, SexyTopoConstants.ALLOWED_DOUBLE_DELTA);
    }

    @Test
    public void testOriginWithSeveralWaysOnFallsBackToZero() {
        Survey testSurvey = new Survey();
        SurveyUpdater.updateWithNewStation(testSurvey, new Leg(5, 90, 0));
        testSurvey.setActiveStation(testSurvey.getOrigin());
        SurveyUpdater.updateWithNewStation(testSurvey, new Leg(5, 180, 0));

        double angle = CrossSectioner.getAngleOfSection(testSurvey, testSurvey.getOrigin());
        Assert.assertEquals(0.0, angle, SexyTopoConstants.ALLOWED_DOUBLE_DELTA);
    }

    @Test
    public void testAngleDoesNotDependOnExtendedElevationDirection() {
        Survey testSurvey = BasicTestSurveyCreator.createRightRight();
        Station s2 = testSurvey.getStationByName("2");

        for (ExtendedElevationDirection direction : ExtendedElevationDirection.values()) {
            s2.setExtendedElevationDirection(direction);

            double angle = CrossSectioner.getAngleOfSection(testSurvey, s2);
            Assert.assertEquals(
                    direction.name(), 45.0, angle, SexyTopoConstants.ALLOWED_DOUBLE_DELTA);
        }
    }

    @Test
    public void testSectionUsesTheAngleOfTheSection() {
        Survey testSurvey = BasicTestSurveyCreator.createRightRight();
        Station s2 = testSurvey.getStationByName("2");

        CrossSection crossSection = CrossSectioner.section(testSurvey, s2);

        Assert.assertSame(s2, crossSection.getStation());
        Assert.assertEquals(45.0, crossSection.getAngle(), SexyTopoConstants.ALLOWED_DOUBLE_DELTA);
    }

    @Test
    public void testHorizontalSectionIgnoresTheLegsAndLiesFlat() {
        Survey testSurvey = BasicTestSurveyCreator.createRightRight();
        Station s2 = testSurvey.getStationByName("2");

        CrossSection crossSection =
                CrossSectioner.section(testSurvey, s2, CrossSection.Orientation.HORIZONTAL);

        Assert.assertEquals(CrossSection.Orientation.HORIZONTAL, crossSection.getOrientation());
        Assert.assertSame(s2, crossSection.getStation());
        Assert.assertEquals(0f, crossSection.getAngle(), SexyTopoConstants.ALLOWED_DOUBLE_DELTA);
    }

    @Test
    public void testVerticalSectionByOrientationUsesTheAngleOfTheSection() {
        Survey testSurvey = BasicTestSurveyCreator.createRightRight();
        Station s2 = testSurvey.getStationByName("2");

        CrossSection crossSection =
                CrossSectioner.section(testSurvey, s2, CrossSection.Orientation.VERTICAL);

        Assert.assertEquals(CrossSection.Orientation.VERTICAL, crossSection.getOrientation());
        Assert.assertEquals(45.0, crossSection.getAngle(), SexyTopoConstants.ALLOWED_DOUBLE_DELTA);
    }

    @Test
    public void testFacingRightOnTheElevationFollowsTheSurveyWhenItIsDrawnRight() {
        // The survey heads north and is drawn to the right
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        Station station = survey.getStationByName("2");

        Assert.assertEquals(
                0f, CrossSectioner.getAngleFacingOnElevation(survey, station, true), 1e-4f);
        Assert.assertEquals(
                180f, CrossSectioner.getAngleFacingOnElevation(survey, station, false), 1e-4f);
    }

    @Test
    public void testFacingRightOnTheElevationLooksBackWhenTheSurveyIsDrawnLeft() {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        Station station = survey.getStationByName("2");
        SurveyUpdater.setExtendedElevationDirection(
                survey, survey.getStationByName("1"), ExtendedElevationDirection.LEFT);

        Assert.assertEquals(
                180f, CrossSectioner.getAngleFacingOnElevation(survey, station, true), 1e-4f);
        Assert.assertEquals(
                0f, CrossSectioner.getAngleFacingOnElevation(survey, station, false), 1e-4f);
    }

    @Test
    public void testASectionSetOnTheElevationIsSeenFacingThatWay() {
        for (ExtendedElevationDirection direction :
                new ExtendedElevationDirection[] {
                    ExtendedElevationDirection.LEFT, ExtendedElevationDirection.RIGHT
                }) {
            Survey survey = BasicTestSurveyCreator.createStraightNorth();
            Station station = survey.getStationByName("2");
            SurveyUpdater.setExtendedElevationDirection(
                    survey, survey.getStationByName("1"), direction);

            for (boolean facingRight : new boolean[] {true, false}) {
                float angle =
                        CrossSectioner.getAngleFacingOnElevation(survey, station, facingRight);
                Assert.assertEquals(
                        direction + " " + facingRight,
                        facingRight,
                        CrossSectioner.isFacingRightOnElevation(survey, station, angle));
            }
        }
    }

    @Test
    public void testASectionAtAnAngleFacesWhicheverWayIsNearer() {
        // Heading north, drawn right: a section facing east of north still faces along the survey
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        Station station = survey.getStationByName("2");

        Assert.assertTrue(CrossSectioner.isFacingRightOnElevation(survey, station, 60));
        Assert.assertFalse(CrossSectioner.isFacingRightOnElevation(survey, station, 120));
    }
}
