package org.hwyl.sexytopo.model.sketch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.survey.Station;
import org.junit.Assert;
import org.junit.Test;

public class SketchTest {

    private static AreaDetail squareArea(float left, float top, float size) {
        List<Coord2D> polygon =
                Arrays.asList(
                        new Coord2D(left, top),
                        new Coord2D(left + size, top),
                        new Coord2D(left + size, top + size),
                        new Coord2D(left, top + size));
        return new AreaDetail(new ArrayList<>(polygon), AreaType.WATER, Colour.BLUE);
    }

    @Test
    public void testAddAreaIsUndoable() {
        Sketch sketch = new Sketch();
        sketch.addAreaDetail(squareArea(0, 0, 2));
        Assert.assertEquals(1, sketch.getAreaDetails().size());

        sketch.undo();
        Assert.assertEquals(0, sketch.getAreaDetails().size());

        sketch.redo();
        Assert.assertEquals(1, sketch.getAreaDetails().size());
    }

    @Test
    public void testDeleteAreaWithReplacementsIsSingleUndoStep() {
        // simulates erasing through the middle of an area, splitting it in two
        Sketch sketch = new Sketch();
        AreaDetail original = squareArea(0, 0, 4);
        sketch.addAreaDetail(original);

        List<SketchDetail> fragments =
                Arrays.asList(squareArea(0, 0, 1.5f), squareArea(2.5f, 2.5f, 1.5f));
        sketch.deleteDetail(original, fragments);
        Assert.assertEquals(2, sketch.getAreaDetails().size());

        sketch.undo();
        Assert.assertEquals(1, sketch.getAreaDetails().size());
        Assert.assertSame(original, sketch.getAreaDetails().get(0));

        sketch.redo();
        Assert.assertEquals(2, sketch.getAreaDetails().size());
    }

    @Test
    public void testDeleteMultipleDetailsWithReplacementsIsSingleUndoStep() {
        // simulates a new area outline merging two existing areas into one
        Sketch sketch = new Sketch();
        AreaDetail first = squareArea(0, 0, 2);
        AreaDetail second = squareArea(3, 0, 2);
        sketch.addAreaDetail(first);
        sketch.addAreaDetail(second);

        AreaDetail merged = squareArea(0, 0, 5);
        sketch.deleteDetails(Arrays.asList(first, second), Arrays.<SketchDetail>asList(merged));
        Assert.assertEquals(1, sketch.getAreaDetails().size());
        Assert.assertSame(merged, sketch.getAreaDetails().get(0));

        sketch.undo();
        Assert.assertEquals(2, sketch.getAreaDetails().size());

        sketch.redo();
        Assert.assertEquals(1, sketch.getAreaDetails().size());
        Assert.assertSame(merged, sketch.getAreaDetails().get(0));
    }

    @Test
    public void testFinishAreaCreatesPolygonFromOutline() {
        Sketch sketch = new Sketch();
        sketch.startNewArea(new Coord2D(0, 0), AreaType.WATER);
        sketch.getActivePath().lineTo(new Coord2D(4, 0));
        sketch.getActivePath().lineTo(new Coord2D(4, 4));
        sketch.getActivePath().lineTo(new Coord2D(0, 4));
        sketch.finishArea(AreaType.WATER);

        Assert.assertEquals(1, sketch.getAreaDetails().size());
        Assert.assertEquals(0, sketch.getPathDetails().size());
        Assert.assertNull(sketch.getActivePath());
    }

    @Test
    public void testFinishAreaDiscardsDegenerateOutline() {
        Sketch sketch = new Sketch();
        sketch.startNewArea(new Coord2D(0, 0), AreaType.WATER);
        sketch.getActivePath().lineTo(new Coord2D(1, 0));
        sketch.finishArea(AreaType.WATER);

        Assert.assertEquals(0, sketch.getAreaDetails().size());
        Assert.assertEquals(0, sketch.getPathDetails().size());
        sketch.undo(); // must not blow up or resurrect anything
        Assert.assertEquals(0, sketch.getAreaDetails().size());
    }

    @Test
    public void testReplaceCrossSectionDetailIsSingleUndoStep() {
        Sketch sketch = new Sketch();
        CrossSection crossSection = new CrossSection(new Station("A1"), 0f);
        CrossSectionDetail oldDetail = new CrossSectionDetail(crossSection, new Coord2D(1, 2));
        sketch.addCrossSection(oldDetail);

        CrossSectionDetail newDetail = new CrossSectionDetail(crossSection, new Coord2D(10, 20));
        sketch.replaceCrossSectionDetail(oldDetail, newDetail);

        Assert.assertEquals(1, sketch.getCrossSectionDetails().size());
        Assert.assertSame(newDetail, sketch.getCrossSectionDetails().get(0));

        sketch.undo();
        Assert.assertEquals(1, sketch.getCrossSectionDetails().size());
        Assert.assertSame(oldDetail, sketch.getCrossSectionDetails().get(0));

        sketch.redo();
        Assert.assertEquals(1, sketch.getCrossSectionDetails().size());
        Assert.assertSame(newDetail, sketch.getCrossSectionDetails().get(0));
    }

    @Test
    public void testCreateDeleteUndoRedoDoesNotDuplicateCrossSection() {
        Sketch sketch = new Sketch();
        CrossSection crossSection = new CrossSection(new Station("A1"), 0f);
        CrossSectionDetail detail = new CrossSectionDetail(crossSection, new Coord2D(1, 2));

        sketch.addCrossSection(detail); // create
        sketch.deleteDetail(detail); // erase
        Assert.assertEquals(0, sketch.getCrossSectionDetails().size());

        // Undo the delete, then undo the create: collection should drain without going negative.
        sketch.undo();
        Assert.assertEquals(1, sketch.getCrossSectionDetails().size());
        sketch.undo();
        Assert.assertEquals(0, sketch.getCrossSectionDetails().size());

        // Redo the create, then redo the delete: must never leave two copies behind.
        sketch.redo();
        Assert.assertEquals(1, sketch.getCrossSectionDetails().size());
        sketch.redo();
        Assert.assertEquals(0, sketch.getCrossSectionDetails().size());
    }

    @Test
    public void testEditThenUndoRedoDoesNotDuplicateCrossSection() {
        // Regression test for the duplicate-cross-section bug. Committing a sub-sketch edit must
        // keep the detail's identity stable (it mutates in place) so the plan's undo/redo stacks
        // never reference a stale copy. If a commit instead swapped in a new instance, undoing past
        // the original creation would no-op and redoing it would add a second copy.
        Sketch sketch = new Sketch();
        CrossSection crossSection = new CrossSection(new Station("A1"), 0f);
        CrossSectionDetail detail = new CrossSectionDetail(crossSection, new Coord2D(1, 2));

        sketch.addCrossSection(detail); // create (pushed to history)

        Sketch editedSubSketch = new Sketch();
        editedSubSketch.startNewPath(new Coord2D(0, 0));
        detail.setSketch(editedSubSketch); // commit edit in place (NOT pushed to history)

        sketch.undo(); // undo the create
        sketch.redo(); // redo the create: must not leave two copies

        Assert.assertEquals(1, sketch.getCrossSectionDetails().size());
    }
}
