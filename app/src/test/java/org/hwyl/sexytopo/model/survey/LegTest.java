package org.hwyl.sexytopo.model.survey;

import org.junit.Assert;
import org.junit.Test;

public class LegTest {

    private static final float DELTA = 0.0001f;

    @Test
    public void testValidDistanceBoundary() {
        Leg leg = new Leg(0.0f, 0.0f, 0.0f);
        Assert.assertEquals(0.0f, leg.getDistance(), DELTA);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeDistanceThrowsException() {
        new Leg(-0.1f, 0.0f, 0.0f);
    }

    @Test
    public void testValidAzimuthLowerBoundary() {
        Leg leg = new Leg(1.0f, 0.0f, 0.0f);
        Assert.assertEquals(0.0f, leg.getAzimuth(), DELTA);
    }

    @Test
    public void testValidAzimuthUpperBoundary() {
        Leg leg = new Leg(1.0f, 359.9f, 0.0f);
        Assert.assertEquals(359.9f, leg.getAzimuth(), DELTA);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAzimuthBelowMinimumThrowsException() {
        new Leg(1.0f, -0.1f, 0.0f);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAzimuthAtOrAbove360ThrowsException() {
        new Leg(1.0f, 360.0f, 0.0f);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAzimuthAbove360ThrowsException() {
        new Leg(1.0f, 360.1f, 0.0f);
    }

    @Test
    public void testValidInclinationLowerBoundary() {
        Leg leg = new Leg(1.0f, 0.0f, -90.0f);
        Assert.assertEquals(-90.0f, leg.getInclination(), DELTA);
    }

    @Test
    public void testValidInclinationUpperBoundary() {
        Leg leg = new Leg(1.0f, 0.0f, 90.0f);
        Assert.assertEquals(90.0f, leg.getInclination(), DELTA);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInclinationBelowMinimumThrowsException() {
        new Leg(1.0f, 0.0f, -90.1f);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInclinationAboveMaximumThrowsException() {
        new Leg(1.0f, 0.0f, 90.1f);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullDestinationThrowsException() {
        new Leg(1.0f, 0.0f, 0.0f, null, new Leg[] {});
    }

    @Test
    public void testReverseFlipsAzimuthBy180Degrees() {
        Leg leg = new Leg(5.0f, 45.0f, 30.0f);
        Leg reversed = leg.reverse();
        Assert.assertEquals(225.0f, reversed.getAzimuth(), DELTA);
        Assert.assertEquals(-30.0f, reversed.getInclination(), DELTA);
        Assert.assertEquals(5.0f, reversed.getDistance(), DELTA);
    }

    @Test
    public void testReverseWrapsAzimuthCorrectly() {
        Leg leg = new Leg(5.0f, 270.0f, 10.0f);
        Leg reversed = leg.reverse();
        Assert.assertEquals(90.0f, reversed.getAzimuth(), DELTA);
    }

    @Test
    public void testReverseFlipsWasShotBackwardsFlag() {
        Leg leg = new Leg(5.0f, 45.0f, 30.0f, false);
        Leg reversed = leg.reverse();
        Assert.assertTrue(reversed.wasShotBackwards());

        Leg legBackwards = new Leg(5.0f, 45.0f, 30.0f, true);
        Leg reversedBackwards = legBackwards.reverse();
        Assert.assertFalse(reversedBackwards.wasShotBackwards());
    }

    @Test
    public void testReversePreservesCommentWithDestination() {
        Station destination = new Station("A1");
        Leg leg = new Leg(5.0f, 45.0f, 30.0f, destination, new Leg[] {}, false);
        leg.setComment("A test comment");
        Leg reversed = leg.reverse();
        Assert.assertTrue(reversed.hasDestination());
        Assert.assertEquals("A test comment", reversed.getComment());
    }

    @Test
    public void testReversePreservesCommentWhenNoDestination() {
        Leg splay = new Leg(5.0f, 45.0f, 30.0f);
        splay.setComment("Splay comment");
        Leg reversed = splay.reverse();
        Assert.assertFalse(reversed.hasDestination());
        Assert.assertEquals("Splay comment", reversed.getComment());
    }

    @Test
    public void testRotateAddsAngle() {
        Leg leg = new Leg(5.0f, 45.0f, 30.0f);
        Leg rotated = leg.rotate(90.0f);
        Assert.assertEquals(135.0f, rotated.getAzimuth(), DELTA);
        Assert.assertEquals(30.0f, rotated.getInclination(), DELTA);
        Assert.assertEquals(5.0f, rotated.getDistance(), DELTA);
    }

    @Test
    public void testRotateWrapsAzimuthAbove360() {
        Leg leg = new Leg(5.0f, 350.0f, 0.0f);
        Leg rotated = leg.rotate(20.0f);
        Assert.assertEquals(10.0f, rotated.getAzimuth(), DELTA);
    }

    @Test
    public void testRotateWithNegativeDelta() {
        Leg leg = new Leg(5.0f, 45.0f, 0.0f);
        Leg rotated = leg.rotate(-30.0f);
        Assert.assertEquals(15.0f, rotated.getAzimuth(), DELTA);
    }

    @Test
    public void testAsBacksightFlipsAzimuthAndInclination() {
        Leg leg = new Leg(5.0f, 45.0f, 30.0f);
        Leg backsight = leg.asBacksight();
        Assert.assertEquals(225.0f, backsight.getAzimuth(), DELTA);
        Assert.assertEquals(-30.0f, backsight.getInclination(), DELTA);
        Assert.assertEquals(5.0f, backsight.getDistance(), DELTA);
    }

    @Test
    public void testAsBacksightPreservesComment() {
        Leg leg = new Leg(5.0f, 45.0f, 30.0f);
        leg.setComment("A test comment");
        Leg backsight = leg.asBacksight();
        Assert.assertEquals("A test comment", backsight.getComment());
    }

    @Test
    public void testAsBacksightWithStationPreservesDestination() {
        Station destination = new Station("A1");
        Leg leg = new Leg(5.0f, 45.0f, 30.0f);
        Leg backsight = leg.asBacksight(destination);
        Assert.assertEquals(destination, backsight.getDestination());
        Assert.assertTrue(backsight.hasDestination());
    }

    @Test
    public void testSplayHasNoDestination() {
        Leg splay = new Leg(5.0f, 45.0f, 30.0f);
        Assert.assertFalse(splay.hasDestination());
        Assert.assertEquals(Survey.NULL_STATION, splay.getDestination());
    }

    @Test
    public void testLegWithDestination() {
        Station destination = new Station("A1");
        Leg leg = new Leg(5.0f, 45.0f, 30.0f, destination, new Leg[] {});
        Assert.assertTrue(leg.hasDestination());
        Assert.assertEquals(destination, leg.getDestination());
    }

    @Test
    public void testPromotedLegTracksOriginals() {
        Leg splay1 = new Leg(5.0f, 45.0f, 30.0f);
        Leg splay2 = new Leg(4.8f, 46.0f, 29.0f);
        Leg[] promotedFrom = new Leg[] {splay1, splay2};

        Station destination = new Station("A1");
        Leg promoted = new Leg(5.0f, 45.0f, 30.0f, destination, promotedFrom);

        Assert.assertTrue(promoted.wasPromoted());
        Assert.assertArrayEquals(promotedFrom, promoted.getPromotedFrom());
    }

    @Test
    public void testNonPromotedLegHasEmptyPromotedFrom() {
        Leg leg = new Leg(5.0f, 45.0f, 30.0f);
        Assert.assertFalse(leg.wasPromoted());
        Assert.assertEquals(0, leg.getPromotedFrom().length);
    }

    @Test
    public void testUpgradeSplayToConnectedLeg() {
        Leg splay = new Leg(5.0f, 45.0f, 30.0f, true);
        Station destination = new Station("A1");

        Leg upgraded = Leg.upgradeSplayToConnectedLeg(splay, destination, new Leg[] {});

        Assert.assertTrue(upgraded.hasDestination());
        Assert.assertEquals(destination, upgraded.getDestination());
        Assert.assertEquals(5.0f, upgraded.getDistance(), DELTA);
        Assert.assertEquals(45.0f, upgraded.getAzimuth(), DELTA);
        Assert.assertEquals(30.0f, upgraded.getInclination(), DELTA);
        Assert.assertTrue(upgraded.wasShotBackwards());
    }

    @Test
    public void testToSplayRemovesDestination() {
        Station destination = new Station("A1");
        Leg leg = new Leg(5.0f, 45.0f, 30.0f, destination, new Leg[] {}, true);

        Leg splay = leg.toSplay();

        Assert.assertFalse(splay.hasDestination());
        Assert.assertEquals(5.0f, splay.getDistance(), DELTA);
        Assert.assertEquals(45.0f, splay.getAzimuth(), DELTA);
        Assert.assertEquals(30.0f, splay.getInclination(), DELTA);
    }

    @Test
    public void testToSplayClearsWasShotBackwards() {
        Station destination = new Station("A1");
        Leg backwardsLeg = new Leg(5.0f, 45.0f, 30.0f, destination, new Leg[] {}, true);

        Leg splay = backwardsLeg.toSplay();

        Assert.assertFalse(splay.wasShotBackwards());
    }

    @Test
    public void testToSplayOfForwardLegIsNotShotBackwards() {
        Station destination = new Station("A1");
        Leg forwardLeg = new Leg(5.0f, 45.0f, 30.0f, destination, new Leg[] {}, false);

        Leg splay = forwardLeg.toSplay();

        Assert.assertFalse(splay.wasShotBackwards());
    }

    @Test
    public void testAdjustAzimuthChangesOnlyAzimuth() {
        Leg leg = new Leg(5.0f, 45.0f, 30.0f);
        Leg adjusted = leg.adjustAzimuth(180.0f);

        Assert.assertEquals(180.0f, adjusted.getAzimuth(), DELTA);
        Assert.assertEquals(30.0f, adjusted.getInclination(), DELTA);
        Assert.assertEquals(5.0f, adjusted.getDistance(), DELTA);
    }

    @Test
    public void testIsDistanceLegal() {
        Assert.assertTrue(Leg.isDistanceLegal(0.0f));
        Assert.assertTrue(Leg.isDistanceLegal(100.0f));
        Assert.assertFalse(Leg.isDistanceLegal(-0.1f));
    }

    @Test
    public void testIsAzimuthLegal() {
        Assert.assertTrue(Leg.isAzimuthLegal(0.0f));
        Assert.assertTrue(Leg.isAzimuthLegal(359.9f));
        Assert.assertFalse(Leg.isAzimuthLegal(-0.1f));
        Assert.assertFalse(Leg.isAzimuthLegal(360.0f));
        Assert.assertFalse(Leg.isAzimuthLegal(360.1f));
    }

    @Test
    public void testIsInclinationLegal() {
        Assert.assertTrue(Leg.isInclinationLegal(-90.0f));
        Assert.assertTrue(Leg.isInclinationLegal(90.0f));
        Assert.assertTrue(Leg.isInclinationLegal(0.0f));
        Assert.assertFalse(Leg.isInclinationLegal(-90.1f));
        Assert.assertFalse(Leg.isInclinationLegal(90.1f));
    }

    // ---- fromRecordedReading / toRecordedReading ----

    @Test
    public void testFromRecordedReadingForwardKeepsTheReading() {
        Station destination = new Station("A1");

        Leg leg = Leg.fromRecordedReading(new Leg(5.0f, 45.0f, 10.0f), destination, false);

        Assert.assertSame(destination, leg.getDestination());
        Assert.assertFalse(leg.wasShotBackwards());
        Assert.assertEquals(5.0f, leg.getDistance(), DELTA);
        Assert.assertEquals(45.0f, leg.getAzimuth(), DELTA);
        Assert.assertEquals(10.0f, leg.getInclination(), DELTA);
    }

    @Test
    public void testFromRecordedReadingBackwardReversesAndFlagsTheLeg() {
        Leg leg = Leg.fromRecordedReading(new Leg(5.0f, 45.0f, 10.0f), new Station("A1"), true);

        Assert.assertTrue(leg.wasShotBackwards());
        Assert.assertEquals(5.0f, leg.getDistance(), DELTA);
        Assert.assertEquals(225.0f, leg.getAzimuth(), DELTA);
        Assert.assertEquals(-10.0f, leg.getInclination(), DELTA);
    }

    @Test
    public void testFromRecordedReadingKeepsPromotedFromAsRecorded() {
        Leg[] readings = {new Leg(5.0f, 44.0f, 9.0f), new Leg(5.0f, 46.0f, 11.0f)};

        Leg leg =
                Leg.fromRecordedReading(
                        new Leg(5.0f, 45.0f, 10.0f), new Station("A1"), readings, true);

        Assert.assertArrayEquals(readings, leg.getPromotedFrom());
        Assert.assertEquals(44.0f, leg.getPromotedFrom()[0].getAzimuth(), DELTA);
        Assert.assertFalse(leg.getPromotedFrom()[0].wasShotBackwards());
    }

    @Test
    public void testFromRecordedReadingKeepsTheComment() {
        Leg recorded = new Leg(5.0f, 45.0f, 10.0f);
        recorded.setComment("a comment");

        Assert.assertEquals(
                "a comment",
                Leg.fromRecordedReading(recorded, new Station("A1"), false).getComment());
        Assert.assertEquals(
                "a comment",
                Leg.fromRecordedReading(recorded, new Station("A1"), true).getComment());
    }

    @Test
    public void testFromRecordedReadingIgnoresTheFlagOnTheRecordedReading() {
        // A recorded reading is never shot backwards, so a stray flag must not flip the result
        Leg flagged = new Leg(5.0f, 45.0f, 10.0f, true);

        Leg leg = Leg.fromRecordedReading(flagged, new Station("A1"), false);

        Assert.assertFalse(leg.wasShotBackwards());
        Assert.assertEquals(45.0f, leg.getAzimuth(), DELTA);
    }

    @Test
    public void testToRecordedReadingOfBackwardLegReversesItBack() {
        Leg backwardsLeg = new Leg(5.0f, 225.0f, -10.0f, new Station("A1"), new Leg[] {}, true);

        Leg recorded = backwardsLeg.toRecordedReading();

        Assert.assertFalse(recorded.hasDestination());
        Assert.assertFalse(recorded.wasShotBackwards());
        Assert.assertEquals(5.0f, recorded.getDistance(), DELTA);
        Assert.assertEquals(45.0f, recorded.getAzimuth(), DELTA);
        Assert.assertEquals(10.0f, recorded.getInclination(), DELTA);
    }

    @Test
    public void testToRecordedReadingOfForwardLegKeepsTheNumbers() {
        Leg forwardLeg = new Leg(5.0f, 45.0f, 10.0f, new Station("A1"), new Leg[] {}, false);

        Leg recorded = forwardLeg.toRecordedReading();

        Assert.assertFalse(recorded.hasDestination());
        Assert.assertFalse(recorded.wasShotBackwards());
        Assert.assertEquals(45.0f, recorded.getAzimuth(), DELTA);
        Assert.assertEquals(10.0f, recorded.getInclination(), DELTA);
    }

    @Test
    public void testToRecordedReadingIsTheInverseOfFromRecordedReading() {
        for (boolean shotBackwards : new boolean[] {false, true}) {
            Leg original = new Leg(7.5f, 123.0f, -33.0f);

            Leg recovered =
                    Leg.fromRecordedReading(original, new Station("A1"), shotBackwards)
                            .toRecordedReading();

            Assert.assertEquals(original.getDistance(), recovered.getDistance(), DELTA);
            Assert.assertEquals(original.getAzimuth(), recovered.getAzimuth(), DELTA);
            Assert.assertEquals(original.getInclination(), recovered.getInclination(), DELTA);
        }
    }
}
