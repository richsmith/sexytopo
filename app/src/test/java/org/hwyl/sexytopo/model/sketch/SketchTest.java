package org.hwyl.sexytopo.model.sketch;

import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.survey.Station;
import org.junit.Assert;
import org.junit.Test;

public class SketchTest {

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
