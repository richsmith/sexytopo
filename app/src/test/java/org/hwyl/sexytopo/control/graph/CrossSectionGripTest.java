package org.hwyl.sexytopo.control.graph;

import org.junit.Assert;
import org.junit.Test;

public class CrossSectionGripTest {

    private static final float DELTA = 1e-4f;

    // vertical ticks

    @Test
    public void testVerticalIsThreeTicksSideBySide() {
        CrossSectionGrip grip = CrossSectionGrip.vertical(100, 50, 3.6f, 5);

        Assert.assertEquals(3, grip.getMarkCount());
        Assert.assertEquals(95, grip.getStartX(0), DELTA);
        Assert.assertEquals(100, grip.getStartX(1), DELTA);
        Assert.assertEquals(105, grip.getStartX(2), DELTA);
    }

    @Test
    public void testVerticalTicksAreVerticalAndCentredOnTheBar() {
        CrossSectionGrip grip = CrossSectionGrip.vertical(100, 50, 3.6f, 5);

        for (int i = 0; i < grip.getMarkCount(); i++) {
            Assert.assertEquals(grip.getStartX(i), grip.getEndX(i), DELTA);
            Assert.assertEquals(48.2f, grip.getStartY(i), DELTA);
            Assert.assertEquals(51.8f, grip.getEndY(i), DELTA);
        }
    }

    // horizontal dashes

    @Test
    public void testHorizontalIsThreeDashesInARow() {
        CrossSectionGrip grip = CrossSectionGrip.horizontal(100, 50, 4, 5, 100);

        Assert.assertEquals(3, grip.getMarkCount());
        Assert.assertEquals(89, grip.getStartX(0), DELTA);
        Assert.assertEquals(93, grip.getEndX(0), DELTA);
        Assert.assertEquals(98, grip.getStartX(1), DELTA);
        Assert.assertEquals(102, grip.getEndX(1), DELTA);
        Assert.assertEquals(107, grip.getStartX(2), DELTA);
        Assert.assertEquals(111, grip.getEndX(2), DELTA);
    }

    @Test
    public void testHorizontalDashesAreHorizontalAndCentredOnTheBar() {
        CrossSectionGrip grip = CrossSectionGrip.horizontal(100, 50, 4, 5, 100);

        for (int i = 0; i < grip.getMarkCount(); i++) {
            Assert.assertEquals(50, grip.getStartY(i), DELTA);
            Assert.assertEquals(50, grip.getEndY(i), DELTA);
        }
        Assert.assertEquals(100, (grip.getStartX(0) + grip.getEndX(2)) / 2, DELTA);
    }

    @Test
    public void testHorizontalDashesAreEqualLengthAndEvenlySpaced() {
        CrossSectionGrip grip = CrossSectionGrip.horizontal(37, 12, 4.5f, 6, 100);

        float length = grip.getEndX(0) - grip.getStartX(0);
        Assert.assertEquals(length, grip.getEndX(1) - grip.getStartX(1), DELTA);
        Assert.assertEquals(length, grip.getEndX(2) - grip.getStartX(2), DELTA);

        float firstGap = grip.getStartX(1) - grip.getEndX(0);
        Assert.assertEquals(firstGap, grip.getStartX(2) - grip.getEndX(1), DELTA);
    }

    @Test
    public void testHorizontalKeepsItsFullSizeWhenThereIsRoom() {
        CrossSectionGrip grip = CrossSectionGrip.horizontal(100, 50, 4, 5, 22);

        Assert.assertEquals(4, grip.getEndX(0) - grip.getStartX(0), DELTA);
        Assert.assertEquals(5, grip.getStartX(1) - grip.getEndX(0), DELTA);
    }

    @Test
    public void testHorizontalShortensTheDashesFirstToFitANarrowBar() {
        // Full size the row is 3 * 4 + 2 * 5 = 22 wide
        CrossSectionGrip grip = CrossSectionGrip.horizontal(100, 50, 4, 5, 16);

        Assert.assertEquals(16, grip.getEndX(2) - grip.getStartX(0), DELTA);
        Assert.assertEquals(2, grip.getEndX(0) - grip.getStartX(0), DELTA);
        Assert.assertEquals(5, grip.getStartX(1) - grip.getEndX(0), DELTA);
    }

    @Test
    public void testHorizontalKeepsTheGapsAsLongAsTheDashesCanStillShrink() {
        // The dashes can go down to an eighth of their length, so the row can go down to 11.5
        for (float maxWidth = 11.5f; maxWidth <= 22; maxWidth += 0.5f) {
            CrossSectionGrip grip = CrossSectionGrip.horizontal(100, 50, 4, 5, maxWidth);

            Assert.assertEquals("width " + maxWidth, 5, grip.getStartX(1) - grip.getEndX(0), DELTA);
            Assert.assertEquals("width " + maxWidth, 5, grip.getStartX(2) - grip.getEndX(1), DELTA);
        }
    }

    @Test
    public void testHorizontalShrinksEverythingTogetherOnceTheDashesAreAsShortAsTheyGet() {
        // Half of the narrowest row that can keep its gaps, 3 * 0.5 + 2 * 5 = 11.5
        CrossSectionGrip grip = CrossSectionGrip.horizontal(100, 50, 4, 5, 5.75f);

        Assert.assertEquals(5.75f, grip.getEndX(2) - grip.getStartX(0), DELTA);
        Assert.assertEquals(0.25f, grip.getEndX(0) - grip.getStartX(0), DELTA);
        Assert.assertEquals(2.5f, grip.getStartX(1) - grip.getEndX(0), DELTA);
    }

    @Test
    public void testHorizontalStaysCentredWhenShrunk() {
        for (float maxWidth : new float[] {16, 5.75f}) {
            CrossSectionGrip grip = CrossSectionGrip.horizontal(100, 50, 4, 5, maxWidth);

            Assert.assertEquals(
                    "width " + maxWidth, 100, (grip.getStartX(0) + grip.getEndX(2)) / 2, DELTA);
        }
    }

    @Test
    public void testHorizontalAlwaysFitsWithinTheWidthGivenIfItIsNotTooSmall() {
        for (float maxWidth = 3; maxWidth <= 60; maxWidth += 0.5f) {
            CrossSectionGrip grip = CrossSectionGrip.horizontal(100, 50, 4, 5, maxWidth);

            Assert.assertTrue(
                    "width " + maxWidth, grip.getEndX(2) - grip.getStartX(0) <= maxWidth + DELTA);
        }
    }

    @Test
    public void testHorizontalNeverCollapsesToNothing() {
        for (float maxWidth : new float[] {0, -5, 0.0001f}) {
            CrossSectionGrip grip = CrossSectionGrip.horizontal(100, 50, 4, 5, maxWidth);

            for (int i = 0; i < grip.getMarkCount(); i++) {
                Assert.assertTrue("width " + maxWidth, grip.getEndX(i) - grip.getStartX(i) > 0);
            }
        }
    }

    // usable width

    @Test
    public void testUsableWidthLeavesRoomForTheCornersAndEndCaps() {
        Assert.assertEquals(186, CrossSectionGrip.getUsableWidth(200, 6, 2), DELTA);
    }

    @Test
    public void testUsableWidthIsAlwaysAtLeastHalfTheBar() {
        Assert.assertEquals(5, CrossSectionGrip.getUsableWidth(10, 6, 2), DELTA);
    }
}
