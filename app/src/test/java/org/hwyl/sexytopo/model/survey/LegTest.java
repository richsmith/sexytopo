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
        new Leg(1.0f, 0.0f, 0.0f, null, new Leg[]{});
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
        Leg leg = new Leg(5.0f, 45.0f, 30.0f, destination, new Leg[]{});
        Assert.assertTrue(leg.hasDestination());
        Assert.assertEquals(destination, leg.getDestination());
    }

    @Test
    public void testPromotedLegTracksOriginals() {
        Leg splay1 = new Leg(5.0f, 45.0f, 30.0f);
        Leg splay2 = new Leg(4.8f, 46.0f, 29.0f);
        Leg[] promotedFrom = new Leg[]{splay1, splay2};

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

        Leg upgraded = Leg.upgradeSplayToConnectedLeg(splay, destination, new Leg[]{});

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
        Leg leg = new Leg(5.0f, 45.0f, 30.0f, destination, new Leg[]{}, true);

        Leg splay = leg.toSplay();

        Assert.assertFalse(splay.hasDestination());
        Assert.assertEquals(5.0f, splay.getDistance(), DELTA);
        Assert.assertEquals(45.0f, splay.getAzimuth(), DELTA);
        Assert.assertEquals(30.0f, splay.getInclination(), DELTA);
        Assert.assertTrue(splay.wasShotBackwards());
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
}
