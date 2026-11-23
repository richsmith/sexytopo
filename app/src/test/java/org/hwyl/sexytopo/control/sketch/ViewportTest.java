package org.hwyl.sexytopo.control.sketch;

import java.util.Arrays;
import org.hwyl.sexytopo.model.geometry.Coord2D;
import org.hwyl.sexytopo.model.sketch.Colour;
import org.hwyl.sexytopo.model.sketch.PathDetail;
import org.junit.Assert;
import org.junit.Test;

public class ViewportTest {

    private static final float DELTA = 0.0001f;

    @Test
    public void testDefaultsToOriginAtDefaultScale() {
        Viewport viewport = new Viewport();
        Assert.assertEquals(Coord2D.ORIGIN, viewport.getOffset());
        Assert.assertEquals(Viewport.DEFAULT_SCALE, viewport.getScale(), DELTA);
    }

    @Test
    public void testToViewAndToSurveyAreInverses() {
        Viewport viewport = new Viewport();
        viewport.setOffset(new Coord2D(3, -2));
        viewport.setZoom(20, Coord2D.ORIGIN);

        Coord2D surveyPoint = new Coord2D(5.5f, 1.25f);
        Coord2D viewPoint = viewport.toView(surveyPoint);
        Assert.assertEquals(surveyPoint, viewport.toSurvey(viewPoint));
    }

    @Test
    public void testToViewAppliesOffsetThenScale() {
        Viewport viewport = new Viewport();
        viewport.setOffset(new Coord2D(1, 2));
        // 60 pixels per metre by default
        Assert.assertEquals(new Coord2D(60, 120), viewport.toView(new Coord2D(2, 4)));
    }

    @Test
    public void testZoomKeepsFocusPointStill() {
        Viewport viewport = new Viewport();
        viewport.setOffset(new Coord2D(10, 10));

        Coord2D focusOnView = new Coord2D(200, 150);
        Coord2D focusOnSurvey = viewport.toSurvey(focusOnView);

        viewport.setZoom(25, focusOnView);

        Assert.assertEquals(25, viewport.getScale(), DELTA);
        Assert.assertEquals(focusOnView, viewport.toView(focusOnSurvey));
    }

    @Test
    public void testZoomOutsideLimitsIsIgnored() {
        Viewport viewport = new Viewport();
        viewport.setZoom(Viewport.MAX_ZOOM, Coord2D.ORIGIN);
        viewport.setZoom(Viewport.MIN_ZOOM, Coord2D.ORIGIN);
        viewport.setZoom(Viewport.MIN_ZOOM / 2, Coord2D.ORIGIN);
        Assert.assertEquals(Viewport.DEFAULT_SCALE, viewport.getScale(), DELTA);
    }

    @Test
    public void testCentreOnPutsPointInMiddleOfView() {
        Viewport viewport = new Viewport();
        Coord2D point = new Coord2D(7, -3);
        viewport.centreOn(point, 600, 400);
        Assert.assertEquals(new Coord2D(300, 200), viewport.toView(point));
    }

    @Test
    public void testRectOnCanvasIncludesPartialOverlapAndExcludesRectsBeyondAnEdge() {
        Viewport viewport = new Viewport();
        viewport.updateBounds(100, 100);

        Assert.assertTrue(viewport.isRectOnCanvas(10, 10, 20, 20));
        Assert.assertTrue(viewport.isRectOnCanvas(-50, 40, 5, 60));
        Assert.assertFalse(viewport.isRectOnCanvas(-50, 40, -1, 60));
        Assert.assertFalse(viewport.isRectOnCanvas(101, 40, 150, 60));
        Assert.assertFalse(viewport.isRectOnCanvas(40, -50, 60, -1));
        Assert.assertFalse(viewport.isRectOnCanvas(40, 101, 60, 150));
    }

    @Test
    public void testCouldBeVisibleCullsOffscreenShapes() {
        Viewport viewport = new Viewport();
        // 600x600 pixels at 60px/m shows survey (0,0) to (10,10)
        viewport.updateBounds(600, 600);

        PathDetail onScreen = path(new Coord2D(1, 1), new Coord2D(2, 2));
        PathDetail offScreen = path(new Coord2D(20, 20), new Coord2D(21, 21));

        Assert.assertTrue(viewport.couldBeVisible(onScreen));
        Assert.assertFalse(viewport.couldBeVisible(offScreen));
    }

    @Test
    public void testCouldBeVisibleCullsShapesSmallerThanAPixel() {
        Viewport viewport = new Viewport();
        viewport.updateBounds(600, 600);

        // a hundredth of a metre is less than a pixel at 60px/m
        PathDetail tiny = path(new Coord2D(1, 1), new Coord2D(1.01f, 1));
        Assert.assertFalse(viewport.couldBeVisible(tiny));
    }

    @Test
    public void testBoundsFollowPanOnlyAfterUpdate() {
        Viewport viewport = new Viewport();
        viewport.updateBounds(600, 600);
        PathDetail shape = path(new Coord2D(1, 1), new Coord2D(2, 2));

        viewport.setOffset(new Coord2D(50, 50));
        Assert.assertTrue(viewport.couldBeVisible(shape));

        viewport.updateBounds(600, 600);
        Assert.assertFalse(viewport.couldBeVisible(shape));
    }

    @Test
    public void testDpToSurveyConvertsAtCurrentZoom() {
        Viewport viewport = new Viewport();
        // without Android resources, as in unit tests, a dp is a pixel; 60px/m by default
        Assert.assertEquals(0.5f, viewport.dpToSurvey(30), DELTA);

        viewport.setZoom(120, Coord2D.ORIGIN);
        Assert.assertEquals(0.25f, viewport.dpToSurvey(30), DELTA);
    }

    @Test
    public void testPatternScaleIsUnchangedByDoublingTheZoom() {
        Viewport viewport = new Viewport();
        float atDefault = viewport.getPatternScale();

        viewport.setZoom(Viewport.DEFAULT_SCALE * 2, Coord2D.ORIGIN);
        Assert.assertEquals(atDefault, viewport.getPatternScale(), DELTA);
    }

    @Test
    public void testPatternScaleStaysWithinRootTwoOfDesignedSize() {
        Viewport viewport = new Viewport();
        // from where the far-out cap (0.05 x scale, with a dp a pixel) no longer applies
        for (float scale = 30; scale < Viewport.MAX_ZOOM; scale *= 1.1f) {
            viewport.setZoom(scale, Coord2D.ORIGIN);
            float patternScale = viewport.getPatternScale();
            Assert.assertTrue(patternScale <= Math.sqrt(2) + DELTA);
            Assert.assertTrue(patternScale >= 1 / Math.sqrt(2) - DELTA);
        }
    }

    @Test
    public void testPatternScaleIsCappedWhenZoomedFarOut() {
        Viewport viewport = new Viewport();
        viewport.setZoom(1, Coord2D.ORIGIN);
        // at 1px/m a dp can stand for at most 0.05m of cave
        Assert.assertEquals(0.05f, viewport.getPatternScale(), DELTA);
    }

    @Test
    public void testInsetDrawsAPointWhereItsScaledAndMovedPlaceIsInTheParent() {
        Viewport parent = new Viewport();
        parent.setOffset(new Coord2D(3, -2));
        parent.setZoom(25, Coord2D.ORIGIN);
        parent.updateBounds(600, 400);

        Coord2D origin = new Coord2D(7, 4);
        float insetScale = 0.5f;
        Viewport inset = new Viewport();
        inset.setAsInsetOf(parent, origin, insetScale);

        Coord2D point = new Coord2D(-1.5f, 2.25f);
        Coord2D expected = parent.toView(point.scale(insetScale).plus(origin));
        Coord2D actual = inset.toView(point);
        Assert.assertEquals(expected.x, actual.x, DELTA);
        Assert.assertEquals(expected.y, actual.y, DELTA);
    }

    @Test
    public void testInsetCullsAgainstTheParentsView() {
        Viewport parent = new Viewport();
        // 600x600 pixels at 60px/m shows survey (0,0) to (10,10)
        parent.updateBounds(600, 600);

        // a scale of 2 about (5, 5) shows the inset's (-2.5, -2.5) to (2.5, 2.5)
        Viewport inset = new Viewport();
        inset.setAsInsetOf(parent, new Coord2D(5, 5), 2);

        Assert.assertTrue(inset.couldBeVisible(path(new Coord2D(2, 2), new Coord2D(2.4f, 2.4f))));
        Assert.assertFalse(inset.couldBeVisible(path(new Coord2D(3, 3), new Coord2D(4, 4))));
    }

    @Test
    public void testInsetKeepsItsParentsPatternScale() {
        Viewport parent = new Viewport();
        parent.updateBounds(600, 600);
        Viewport inset = new Viewport();
        // a third of the parent's zoom would otherwise round to a different octave
        inset.setAsInsetOf(parent, Coord2D.ORIGIN, 0.3f);
        Assert.assertEquals(parent.getPatternScale(), inset.getPatternScale(), DELTA);
    }

    private static PathDetail path(Coord2D... points) {
        return new PathDetail(Arrays.asList(points), Colour.BLACK);
    }
}
