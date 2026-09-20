package org.hwyl.sexytopo.control.graph;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.model.sketch.CrossSection;
import org.junit.Assert;
import org.junit.Test;

public class OrientationMarkerTest {

    private static final float DELTA = 1e-4f;

    @Test
    public void testLayoutSitsAboveTheScaleBarAtTheLeft() {
        // text size 20, line height 24, view 800 high
        OrientationMarker marker = OrientationMarker.layout(20, 24, 800);

        Assert.assertEquals(70, marker.getCentreX(), DELTA);
        Assert.assertEquals(671, marker.getCentreY(), DELTA);
        Assert.assertEquals(646, marker.getTipY(), DELTA);
        Assert.assertEquals(696, marker.getTailY(), DELTA);
        Assert.assertEquals(12, marker.getHeadSize(), DELTA);
        Assert.assertEquals(642, marker.getLabelBaselineY(), DELTA);
    }

    @Test
    public void testArrowPointsUpTheScreenWithTheLabelAboveIt() {
        OrientationMarker marker = OrientationMarker.layout(30, 36, 1200);

        // Screen y grows downwards, so the tip being above the tail means it points up
        Assert.assertTrue(marker.getTipY() < marker.getTailY());
        Assert.assertTrue(marker.getLabelBaselineY() < marker.getTipY());
    }

    @Test
    public void testTheCentreIsHalfwayAlongTheArrow() {
        OrientationMarker marker = OrientationMarker.layout(30, 36, 1200);

        Assert.assertEquals((marker.getTipY() + marker.getTailY()) / 2, marker.getCentreY(), DELTA);
    }

    @Test
    public void testArrowStaysAboveTheBottomOfTheView() {
        OrientationMarker marker = OrientationMarker.layout(20, 24, 800);

        Assert.assertTrue(marker.getTailY() < 800);
    }

    @Test
    public void testSizeFollowsTheTextSize() {
        OrientationMarker small = OrientationMarker.layout(10, 12, 800);
        OrientationMarker large = OrientationMarker.layout(20, 24, 800);

        Assert.assertEquals(2 * small.getHeadSize(), large.getHeadSize(), DELTA);
        Assert.assertEquals(
                2 * (small.getTailY() - small.getTipY()),
                large.getTailY() - large.getTipY(),
                DELTA);
    }

    @Test
    public void testVerticalIsLabelledUp() {
        Assert.assertEquals(
                R.string.xsection_orientation_up,
                OrientationMarker.getLabelResource(CrossSection.Orientation.VERTICAL));
    }

    @Test
    public void testHorizontalIsLabelledNorth() {
        Assert.assertEquals(
                R.string.xsection_orientation_north,
                OrientationMarker.getLabelResource(CrossSection.Orientation.HORIZONTAL));
    }

    @Test
    public void testEveryOrientationHasItsOwnLabel() {
        Assert.assertNotEquals(
                OrientationMarker.getLabelResource(CrossSection.Orientation.VERTICAL),
                OrientationMarker.getLabelResource(CrossSection.Orientation.HORIZONTAL));
    }

    @Test
    public void testEditorTitleFollowsTheOrientation() {
        Assert.assertEquals(
                R.string.title_activity_cross_section_vertical,
                OrientationMarker.getTitleResource(CrossSection.Orientation.VERTICAL));
        Assert.assertEquals(
                R.string.title_activity_cross_section_horizontal,
                OrientationMarker.getTitleResource(CrossSection.Orientation.HORIZONTAL));
    }

    @Test
    public void testEveryOrientationHasItsOwnTitle() {
        Assert.assertNotEquals(
                OrientationMarker.getTitleResource(CrossSection.Orientation.VERTICAL),
                OrientationMarker.getTitleResource(CrossSection.Orientation.HORIZONTAL));
    }
}
