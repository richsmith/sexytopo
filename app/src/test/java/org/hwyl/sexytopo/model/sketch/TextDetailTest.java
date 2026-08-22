package org.hwyl.sexytopo.model.sketch;

import org.hwyl.sexytopo.model.graph.Coord2D;
import org.junit.Assert;
import org.junit.Test;

public class TextDetailTest {

    @Test
    public void testTextIsVisibleForHitTestAtNormalScale() {
        // Regression test: a TextDetail's bounding box is a zero-size point, so the default
        // visibility check culled it from the erase hit-test and text could never be deleted.
        TextDetail text = new TextDetail(Coord2D.ORIGIN, "Hello", Colour.BLACK, 0.3f);

        // 60 is the default surveyToViewScale
        Assert.assertTrue(text.couldBeVisibleAtScale(60f));
    }

    @Test
    public void testEraseFindsText() {
        Sketch sketch = new Sketch();
        sketch.addTextDetail(new Coord2D(0, 0), "Hello", 0.3f);

        SketchDetail found = sketch.findNearestVisibleDetailWithin(new Coord2D(0, 0), 1f, 60f);

        Assert.assertNotNull(found);
        Assert.assertTrue(found instanceof TextDetail);
    }

    @Test
    public void testHitBoxFollowsRenderingAnchor() {
        // Text is rendered with the position as its left edge and baseline, so glyphs extend right
        // and up from the anchor. A tap on the visible text must be nearer than the mirror-image
        // point left of and below the anchor, where the old centred box used to sit.
        TextDetail text = new TextDetail(Coord2D.ORIGIN, "Hello", Colour.BLACK, 1f);

        Coord2D onVisibleText = new Coord2D(1.0f, -0.5f); // right of anchor, above baseline
        Coord2D wrongSide = new Coord2D(-1.0f, 0.5f); // where the buggy centred box sat

        Assert.assertTrue(text.getDistanceFrom(onVisibleText) < text.getDistanceFrom(wrongSide));
    }

    @Test
    public void testMultilineBoxExtendsDownwards() {
        // Extra lines are drawn below the first, so a point on the second line must be nearer than
        // an equivalent point well below the last line.
        TextDetail text = new TextDetail(Coord2D.ORIGIN, "one\ntwo", Colour.BLACK, 1f);

        Coord2D onSecondLine = new Coord2D(0.5f, 1f);
        Coord2D wellBelow = new Coord2D(0.5f, 5f);

        Assert.assertTrue(text.getDistanceFrom(onSecondLine) < text.getDistanceFrom(wellBelow));
    }
}
