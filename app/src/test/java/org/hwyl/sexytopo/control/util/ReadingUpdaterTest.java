package org.hwyl.sexytopo.control.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.junit.Before;
import org.junit.Test;

public class ReadingUpdaterTest {

    private static final float DELTA = 0.001f;

    private Station destination;
    private Leg reading1;
    private Leg reading2;
    private Leg reading3;

    @Before
    public void setUp() {
        destination = new Station("2");
        reading1 = new Leg(5.0f, 90.0f, 0.0f);
        reading2 = new Leg(5.2f, 92.0f, 1.0f);
        reading3 = new Leg(5.4f, 94.0f, 2.0f);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Leg promoted(Leg... readings) {
        Leg averaged = SurveyUpdater.averageLegs(java.util.Arrays.asList(readings));
        return new Leg(
                averaged.getDistance(),
                averaged.getAzimuth(),
                averaged.getInclination(),
                destination,
                readings);
    }

    // -------------------------------------------------------------------------
    // deleteReading
    // -------------------------------------------------------------------------

    @Test
    public void testDeleteReadingFromThreeReducesToTwoReadings() {
        Leg leg = promoted(reading1, reading2, reading3);
        Leg result = ReadingUpdater.deleteReading(leg, 1);

        assertTrue(result.wasPromoted());
        assertEquals(2, result.getPromotedFrom().length);
    }

    @Test
    public void testDeleteReadingFromTwoReducesToPlainLeg() {
        Leg leg = promoted(reading1, reading2);
        Leg result = ReadingUpdater.deleteReading(leg, 0);

        assertFalse(result.wasPromoted());
        assertEquals(0, result.getPromotedFrom().length);
    }

    @Test
    public void testDeleteReadingRecalculatesAverage() {
        Leg leg = promoted(reading1, reading2, reading3);
        // Delete reading2; survivors are reading1 (5.0, 90, 0) and reading3 (5.4, 94, 2)
        Leg result = ReadingUpdater.deleteReading(leg, 1);

        assertEquals((5.0f + 5.4f) / 2f, result.getDistance(), DELTA);
        assertEquals((0.0f + 2.0f) / 2f, result.getInclination(), DELTA);
    }

    @Test
    public void testDeleteReadingAtFirstIndex() {
        Leg leg = promoted(reading1, reading2, reading3);
        Leg result = ReadingUpdater.deleteReading(leg, 0);

        assertEquals(2, result.getPromotedFrom().length);
        assertEquals(reading2.getDistance(), result.getPromotedFrom()[0].getDistance(), DELTA);
    }

    @Test
    public void testDeleteReadingAtLastIndex() {
        Leg leg = promoted(reading1, reading2, reading3);
        Leg result = ReadingUpdater.deleteReading(leg, 2);

        assertEquals(2, result.getPromotedFrom().length);
        assertEquals(reading2.getDistance(), result.getPromotedFrom()[1].getDistance(), DELTA);
    }

    @Test
    public void testDeleteReadingPreservesDestination() {
        Leg leg = promoted(reading1, reading2);
        Leg result = ReadingUpdater.deleteReading(leg, 0);

        assertEquals(destination, result.getDestination());
    }

    // -------------------------------------------------------------------------
    // downgradeReading
    // -------------------------------------------------------------------------

    @Test
    public void testDowngradeReadingFromThreeReturnsSplayAndTwoReadingLeg() {
        Leg leg = promoted(reading1, reading2, reading3);
        Leg[] result = ReadingUpdater.downgradeReading(leg, 1);

        Leg newLeg = result[0];
        Leg splay = result[1];

        assertTrue(newLeg.wasPromoted());
        assertEquals(2, newLeg.getPromotedFrom().length);
        assertFalse(splay.hasDestination());
    }

    @Test
    public void testDowngradeReadingFromTwoReturnsSplayAndPlainLeg() {
        Leg leg = promoted(reading1, reading2);
        Leg[] result = ReadingUpdater.downgradeReading(leg, 0);

        Leg newLeg = result[0];
        Leg splay = result[1];

        assertFalse(newLeg.wasPromoted());
        assertEquals(0, newLeg.getPromotedFrom().length);
        assertFalse(splay.hasDestination());
    }

    @Test
    public void testDowngradeReadingSplayHasCorrectValues() {
        Leg leg = promoted(reading1, reading2);
        Leg[] result = ReadingUpdater.downgradeReading(leg, 0);
        Leg splay = result[1];

        assertEquals(reading1.getDistance(), splay.getDistance(), DELTA);
        assertEquals(reading1.getAzimuth(), splay.getAzimuth(), DELTA);
        assertEquals(reading1.getInclination(), splay.getInclination(), DELTA);
    }

    @Test
    public void testDowngradeReadingRecalculatesAverage() {
        Leg leg = promoted(reading1, reading2, reading3);
        // Downgrade reading1; survivors are reading2 (5.2, 92, 1) and reading3 (5.4, 94, 2)
        Leg[] result = ReadingUpdater.downgradeReading(leg, 0);
        Leg newLeg = result[0];

        assertEquals((5.2f + 5.4f) / 2f, newLeg.getDistance(), DELTA);
        assertEquals((1.0f + 2.0f) / 2f, newLeg.getInclination(), DELTA);
    }

    // -------------------------------------------------------------------------
    // editReading
    // -------------------------------------------------------------------------

    @Test
    public void testEditReadingReplacesCorrectIndexAndReAverages() {
        Leg leg = promoted(reading1, reading2);
        Leg replacement = new Leg(6.0f, 100.0f, 5.0f);

        Leg result = ReadingUpdater.editReading(leg, 0, replacement);

        assertTrue(result.wasPromoted());
        assertEquals(replacement.getDistance(), result.getPromotedFrom()[0].getDistance(), DELTA);
        assertEquals((6.0f + reading2.getDistance()) / 2f, result.getDistance(), DELTA);
    }

    @Test
    public void testEditReadingPreservesOtherReadings() {
        Leg leg = promoted(reading1, reading2, reading3);
        Leg replacement = new Leg(9.0f, 45.0f, -3.0f);

        Leg result = ReadingUpdater.editReading(leg, 1, replacement);

        assertEquals(3, result.getPromotedFrom().length);
        assertEquals(reading1.getDistance(), result.getPromotedFrom()[0].getDistance(), DELTA);
        assertEquals(replacement.getDistance(), result.getPromotedFrom()[1].getDistance(), DELTA);
        assertEquals(reading3.getDistance(), result.getPromotedFrom()[2].getDistance(), DELTA);
    }

    // -------------------------------------------------------------------------
    // applyUpdatedLeg
    // -------------------------------------------------------------------------

    @Test
    public void testApplyUpdatedLegReplacesInSurveyRecord() {
        org.hwyl.sexytopo.model.survey.Survey survey = new org.hwyl.sexytopo.model.survey.Survey();
        Station from = survey.getOrigin();
        Leg original = promoted(reading1, reading2);
        from.addOnwardLeg(original);
        survey.addLegRecord(original);

        Leg replacement = ReadingUpdater.deleteReading(original, 0);
        ReadingUpdater.applyUpdatedLeg(survey, from, original, replacement);

        assertTrue(from.getOnwardLegs().contains(replacement));
        assertFalse(from.getOnwardLegs().contains(original));
    }

    @Test
    public void testApplyUpdatedLegWithExtraSplayAddsItToStation() {
        org.hwyl.sexytopo.model.survey.Survey survey = new org.hwyl.sexytopo.model.survey.Survey();
        Station from = survey.getOrigin();
        Leg original = promoted(reading1, reading2);
        from.addOnwardLeg(original);
        survey.addLegRecord(original);

        Leg[] downgraded = ReadingUpdater.downgradeReading(original, 0);
        Leg newLeg = downgraded[0];
        Leg splay = downgraded[1];
        ReadingUpdater.applyUpdatedLeg(survey, from, original, newLeg, splay);

        assertTrue(from.getOnwardLegs().contains(splay));
        assertNotNull(splay);
    }
}
