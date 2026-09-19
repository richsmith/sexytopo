package org.hwyl.sexytopo.control.graph;

import org.junit.Assert;
import org.junit.Test;

public class CrossSectionIndicatorTest {

    private static final float DELTA = 1e-4f;

    @Test
    public void testAtAngleZeroRunsLeftToRightThroughThePoint() {
        CrossSectionIndicator indicator = CrossSectionIndicator.atAngle(100, 50, 20, 0);

        Assert.assertEquals(90, indicator.getStartX(), DELTA);
        Assert.assertEquals(50, indicator.getStartY(), DELTA);
        Assert.assertEquals(110, indicator.getEndX(), DELTA);
        Assert.assertEquals(50, indicator.getEndY(), DELTA);
    }

    @Test
    public void testAtAngleZeroPutsTheArrowheadOnTheStartEndPointingUp() {
        CrossSectionIndicator indicator = CrossSectionIndicator.atAngle(100, 50, 20, 0);

        Assert.assertTrue(indicator.hasArrowhead());
        Assert.assertEquals(91, indicator.getArrowInnerX(), DELTA);
        Assert.assertEquals(50, indicator.getArrowInnerY(), DELTA);
        Assert.assertEquals(90, indicator.getArrowOuterX(), DELTA);
        Assert.assertEquals(50, indicator.getArrowOuterY(), DELTA);
        Assert.assertEquals(90, indicator.getArrowTipX(), DELTA);
        Assert.assertEquals(42, indicator.getArrowTipY(), DELTA);
    }

    @Test
    public void testAtAngleNinetyRunsTopToBottomWithArrowheadToTheRight() {
        CrossSectionIndicator indicator = CrossSectionIndicator.atAngle(100, 50, 20, 90);

        Assert.assertEquals(100, indicator.getStartX(), DELTA);
        Assert.assertEquals(40, indicator.getStartY(), DELTA);
        Assert.assertEquals(100, indicator.getEndX(), DELTA);
        Assert.assertEquals(60, indicator.getEndY(), DELTA);
        Assert.assertEquals(108, indicator.getArrowTipX(), DELTA);
        Assert.assertEquals(40, indicator.getArrowTipY(), DELTA);
    }

    @Test
    public void testAtAngleOneEightyIsTheReverseOfZero() {
        CrossSectionIndicator indicator = CrossSectionIndicator.atAngle(100, 50, 20, 180);

        Assert.assertEquals(110, indicator.getStartX(), DELTA);
        Assert.assertEquals(50, indicator.getStartY(), DELTA);
        Assert.assertEquals(90, indicator.getEndX(), DELTA);
        Assert.assertEquals(50, indicator.getEndY(), DELTA);
        Assert.assertEquals(110, indicator.getArrowTipX(), DELTA);
        Assert.assertEquals(58, indicator.getArrowTipY(), DELTA);
    }

    @Test
    public void testAtAngleIsCentredOnThePointWhateverTheAngle() {
        CrossSectionIndicator indicator = CrossSectionIndicator.atAngle(100, 50, 20, 37.5f);

        Assert.assertEquals(100, (indicator.getStartX() + indicator.getEndX()) / 2, DELTA);
        Assert.assertEquals(50, (indicator.getStartY() + indicator.getEndY()) / 2, DELTA);

        float dx = indicator.getEndX() - indicator.getStartX();
        float dy = indicator.getEndY() - indicator.getStartY();
        Assert.assertEquals(20, Math.sqrt(dx * dx + dy * dy), DELTA);
    }

    @Test
    public void testVerticalIsAVerticalLineCentredOnThePoint() {
        CrossSectionIndicator indicator = CrossSectionIndicator.vertical(100, 50, 20);

        Assert.assertEquals(100, indicator.getStartX(), DELTA);
        Assert.assertEquals(40, indicator.getStartY(), DELTA);
        Assert.assertEquals(100, indicator.getEndX(), DELTA);
        Assert.assertEquals(60, indicator.getEndY(), DELTA);
    }

    @Test
    public void testVerticalHasNoArrowhead() {
        Assert.assertFalse(CrossSectionIndicator.vertical(100, 50, 20).hasArrowhead());
    }
}
