package org.hwyl.sexytopo.control.graph;

import org.hwyl.sexytopo.model.graph.ExtendedElevationDirection;
import org.hwyl.sexytopo.model.sketch.CrossSection;
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
    public void testAtAngleTwoSeventyRunsBottomToTopWithArrowheadToTheLeft() {
        CrossSectionIndicator indicator = CrossSectionIndicator.atAngle(100, 50, 20, 270);

        Assert.assertEquals(100, indicator.getStartX(), DELTA);
        Assert.assertEquals(60, indicator.getStartY(), DELTA);
        Assert.assertEquals(100, indicator.getEndX(), DELTA);
        Assert.assertEquals(40, indicator.getEndY(), DELTA);
        Assert.assertEquals(92, indicator.getArrowTipX(), DELTA);
        Assert.assertEquals(60, indicator.getArrowTipY(), DELTA);
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

    // getElevationFacingAngle tests

    @Test
    public void testHorizontalFacesDownWhicheverWayTheSurveyIsHeading() {
        for (ExtendedElevationDirection direction : ExtendedElevationDirection.values()) {
            Assert.assertEquals(
                    direction.name(),
                    180f,
                    CrossSectionIndicator.getElevationFacingAngle(
                            CrossSection.Orientation.HORIZONTAL, direction),
                    DELTA);
        }
    }

    @Test
    public void testVerticalFacesRightWhenTheSurveyHeadsRight() {
        Assert.assertEquals(
                90f,
                CrossSectionIndicator.getElevationFacingAngle(
                        CrossSection.Orientation.VERTICAL, ExtendedElevationDirection.RIGHT),
                DELTA);
    }

    @Test
    public void testVerticalFacesLeftWhenTheSurveyHeadsLeft() {
        Assert.assertEquals(
                270f,
                CrossSectionIndicator.getElevationFacingAngle(
                        CrossSection.Orientation.VERTICAL, ExtendedElevationDirection.LEFT),
                DELTA);
    }

    @Test
    public void testVerticalFallsBackToTheDefaultIfGivenAVerticalDirection() {
        Assert.assertEquals(
                CrossSectionIndicator.getElevationFacingAngle(
                        CrossSection.Orientation.VERTICAL, ExtendedElevationDirection.DEFAULT),
                CrossSectionIndicator.getElevationFacingAngle(
                        CrossSection.Orientation.VERTICAL, ExtendedElevationDirection.VERTICAL),
                DELTA);
    }

    // The elevation's indicators as drawn, using the facing angle

    private static CrossSectionIndicator elevationIndicator(
            CrossSection.Orientation orientation, ExtendedElevationDirection direction) {
        float angle = CrossSectionIndicator.getElevationFacingAngle(orientation, direction);
        return CrossSectionIndicator.atAngle(100, 50, 20, angle);
    }

    @Test
    public void testVerticalHeadingRightIsAVerticalLineWithTheArrowheadAtTheTopPointingRight() {
        CrossSectionIndicator indicator =
                elevationIndicator(
                        CrossSection.Orientation.VERTICAL, ExtendedElevationDirection.RIGHT);

        Assert.assertEquals(indicator.getStartX(), indicator.getEndX(), DELTA);
        Assert.assertEquals(40, indicator.getStartY(), DELTA);
        Assert.assertEquals(108, indicator.getArrowTipX(), DELTA);
        Assert.assertEquals(40, indicator.getArrowTipY(), DELTA);
    }

    @Test
    public void testVerticalHeadingLeftIsAVerticalLineWithTheArrowheadAtTheBottomPointingLeft() {
        CrossSectionIndicator indicator =
                elevationIndicator(
                        CrossSection.Orientation.VERTICAL, ExtendedElevationDirection.LEFT);

        Assert.assertEquals(indicator.getStartX(), indicator.getEndX(), DELTA);
        Assert.assertEquals(60, indicator.getStartY(), DELTA);
        Assert.assertEquals(92, indicator.getArrowTipX(), DELTA);
        Assert.assertEquals(60, indicator.getArrowTipY(), DELTA);
    }

    @Test
    public void testHorizontalIsAHorizontalLineWithTheArrowheadPointingDown() {
        CrossSectionIndicator indicator =
                elevationIndicator(
                        CrossSection.Orientation.HORIZONTAL, ExtendedElevationDirection.RIGHT);

        Assert.assertEquals(indicator.getStartY(), indicator.getEndY(), DELTA);
        Assert.assertEquals(58, indicator.getArrowTipY(), DELTA);
        Assert.assertEquals(indicator.getStartX(), indicator.getArrowTipX(), DELTA);
    }
}
