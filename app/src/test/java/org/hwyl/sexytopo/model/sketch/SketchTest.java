package org.hwyl.sexytopo.model.sketch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.hwyl.sexytopo.control.util.PolygonUtils;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.survey.Station;
import org.junit.Assert;
import org.junit.Test;

public class SketchTest {

    private static List<Coord2D> rectangleContour(
            float left, float top, float width, float height) {
        return Arrays.asList(
                new Coord2D(left, top),
                new Coord2D(left + width, top),
                new Coord2D(left + width, top + height),
                new Coord2D(left, top + height));
    }

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
    public void testStartNewPathUsesActiveLineType() {
        Sketch sketch = new Sketch();
        sketch.setActiveLineType(LineType.WALL);
        PathDetail pathDetail = sketch.startNewPath(Coord2D.ORIGIN);
        Assert.assertEquals(LineType.WALL, pathDetail.getLineType());

        sketch.setActiveLineType(LineType.SKETCH);
        PathDetail plain = sketch.startNewPath(new Coord2D(1, 1));
        Assert.assertEquals(LineType.SKETCH, plain.getLineType());
    }

    @Test
    public void testEraserFragmentsKeepLineType() {
        List<Coord2D> points =
                Arrays.asList(
                        new Coord2D(0, 0),
                        new Coord2D(2, 0),
                        new Coord2D(4, 0),
                        new Coord2D(6, 0),
                        new Coord2D(8, 0));
        PathDetail pathDetail = new PathDetail(new ArrayList<>(points), Colour.BLACK, LineType.PIT);

        // erase through the middle, splitting the path in two
        List<SketchDetail> fragments =
                pathDetail.getPathFragmentsOutsideRadius(new Coord2D(4, 0), 0.5);

        Assert.assertEquals(2, fragments.size());
        for (SketchDetail fragment : fragments) {
            Assert.assertEquals(LineType.PIT, ((PathDetail) fragment).getLineType());
        }
    }

    @Test
    public void testFlipPathDetailReversesPointsAsSingleUndoStep() {
        Sketch sketch = new Sketch();
        sketch.setActiveLineType(LineType.PIT);
        PathDetail original = sketch.startNewPath(new Coord2D(0, 0));
        original.lineTo(new Coord2D(5, 0));
        original.lineTo(new Coord2D(10, 0));
        sketch.finishPath();

        PathDetail flipped = sketch.flipPathDetail(original);

        Assert.assertEquals(1, sketch.getPathDetails().size());
        Assert.assertSame(flipped, sketch.getPathDetails().get(0));
        Assert.assertEquals(10f, flipped.getPath().get(0).x, 0.0001f);
        Assert.assertEquals(LineType.PIT, flipped.getLineType());

        sketch.undo();
        Assert.assertEquals(1, sketch.getPathDetails().size());
        Assert.assertSame(original, sketch.getPathDetails().get(0));

        sketch.redo();
        Assert.assertSame(flipped, sketch.getPathDetails().get(0));
    }

    @Test
    public void testOrnamentSizeSurvivesFragmentingFlippingAndScaling() {
        List<Coord2D> points =
                Arrays.asList(
                        new Coord2D(0, 0),
                        new Coord2D(2, 0),
                        new Coord2D(4, 0),
                        new Coord2D(6, 0),
                        new Coord2D(8, 0));
        PathDetail pathDetail =
                new PathDetail(new ArrayList<>(points), Colour.BLACK, LineType.PIT, 0.5f);

        // erasing keeps the size on each surviving fragment
        List<SketchDetail> fragments =
                pathDetail.getPathFragmentsOutsideRadius(new Coord2D(4, 0), 0.5);
        Assert.assertEquals(2, fragments.size());
        for (SketchDetail fragment : fragments) {
            Assert.assertEquals(0.5f, ((PathDetail) fragment).getOrnamentSize(), 0.0001f);
        }

        // flipping keeps it too
        Sketch sketch = new Sketch();
        sketch.restoreDetailToSketch(pathDetail);
        PathDetail flipped = sketch.flipPathDetail(pathDetail);
        Assert.assertEquals(0.5f, flipped.getOrnamentSize(), 0.0001f);

        // but geometric scaling scales it, as it is a survey-space size (cf. symbols)
        PathDetail scaled = pathDetail.scale(2f);
        Assert.assertEquals(1.0f, scaled.getOrnamentSize(), 0.0001f);
        PathDetail translated = pathDetail.translate(new Coord2D(3, 3));
        Assert.assertEquals(0.5f, translated.getOrnamentSize(), 0.0001f);
    }

    @Test
    public void testGetMostRecentSemanticPathSkipsGeneralLines() {
        Sketch sketch = new Sketch();
        Assert.assertNull(sketch.getMostRecentSemanticPath());

        sketch.setActiveLineType(LineType.WALL);
        PathDetail wall = sketch.startNewPath(new Coord2D(0, 0));
        wall.lineTo(new Coord2D(1, 0));
        sketch.finishPath();

        sketch.setActiveLineType(LineType.SKETCH);
        PathDetail scribble = sketch.startNewPath(new Coord2D(5, 5));
        scribble.lineTo(new Coord2D(6, 5));
        sketch.finishPath();

        Assert.assertSame(wall, sketch.getMostRecentSemanticPath());
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
    public void testFinishAreaNeverSilentlyLosesTheStroke() {
        // finishArea normalises the outline through android.graphics.Path, which is stubbed here
        // (and could in principle fail on a device). Either way the user's stroke must still
        // become an area rather than vanishing.
        Sketch sketch = new Sketch();
        sketch.startNewArea(new Coord2D(0, 0), AreaType.WATER);
        sketch.getActivePath().lineTo(new Coord2D(4, 0));
        sketch.getActivePath().lineTo(new Coord2D(4, 4));
        sketch.getActivePath().lineTo(new Coord2D(0, 4));
        sketch.finishArea(AreaType.WATER);

        Assert.assertEquals(1, sketch.getAreaDetails().size());
        Assert.assertFalse(sketch.getAreaDetails().get(0).hasHoles());
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
    public void testAreaWithHolesSurvivesUndoRedo() {
        // a ring of water (e.g. built from four walls) must come back intact, holes and all
        Sketch sketch = new Sketch();
        AreaDetail ring =
                new AreaDetail(
                        new ArrayList<>(rectangleContour(0, 0, 10, 10)),
                        Collections.singletonList(new ArrayList<>(rectangleContour(2, 2, 6, 6))),
                        AreaType.WATER,
                        Colour.BLUE);
        sketch.addAreaDetail(ring);

        sketch.undo();
        Assert.assertEquals(0, sketch.getAreaDetails().size());

        sketch.redo();
        Assert.assertEquals(1, sketch.getAreaDetails().size());
        AreaDetail restored = sketch.getAreaDetails().get(0);
        Assert.assertEquals(1, restored.getHoles().size());
        Assert.assertTrue(PolygonUtils.contains(restored.getHoles().get(0), new Coord2D(5, 5)));
    }

    @Test
    public void testAreaWithHolesTranslatesAndScalesItsHoles() {
        AreaDetail ring =
                new AreaDetail(
                        new ArrayList<>(rectangleContour(0, 0, 10, 10)),
                        Collections.singletonList(new ArrayList<>(rectangleContour(2, 2, 6, 6))),
                        AreaType.WATER,
                        Colour.BLUE);

        AreaDetail moved = ring.translate(new Coord2D(100, 0));
        Assert.assertEquals(1, moved.getHoles().size());
        Assert.assertEquals(102f, moved.getHoles().get(0).get(0).x, 0.0001f);

        AreaDetail scaled = ring.scale(2);
        Assert.assertEquals(1, scaled.getHoles().size());
        Assert.assertEquals(4f, scaled.getHoles().get(0).get(0).x, 0.0001f);
    }

    @Test
    public void testAreaCanBeGrabbedByItsHoleRim() {
        // tapping the edge of a hole should find the area, not fall through to whatever is behind
        AreaDetail ring =
                new AreaDetail(
                        new ArrayList<>(rectangleContour(0, 0, 10, 10)),
                        Collections.singletonList(new ArrayList<>(rectangleContour(2, 2, 6, 6))),
                        AreaType.WATER,
                        Colour.BLUE);

        // just inside the hole's left edge
        Assert.assertEquals(0.1f, ring.getDistanceFrom(new Coord2D(2.1f, 5)), 0.0001f);
        // the middle of the hole is far from every boundary
        Assert.assertEquals(3f, ring.getDistanceFrom(new Coord2D(5, 5)), 0.0001f);
    }

    @Test
    public void testAreaContainsIgnoresHoles() {
        AreaDetail ring =
                new AreaDetail(
                        new ArrayList<>(rectangleContour(0, 0, 10, 10)),
                        Collections.singletonList(new ArrayList<>(rectangleContour(2, 2, 6, 6))),
                        AreaType.WATER,
                        Colour.BLUE);

        Assert.assertTrue("point in the filled ring", ring.contains(new Coord2D(1, 5)));
        Assert.assertFalse("point in the courtyard", ring.contains(new Coord2D(5, 5)));
        Assert.assertFalse("point outside entirely", ring.contains(new Coord2D(20, 5)));
    }

    @Test
    public void testFindAreaContainingFindsAreaTappedInTheMiddle() {
        // the case the eraser was missing: a tap deep inside a large area, far from any boundary
        Sketch sketch = new Sketch();
        AreaDetail pool =
                new AreaDetail(
                        new ArrayList<>(rectangleContour(0, 0, 100, 100)),
                        AreaType.WATER,
                        Colour.BLUE);
        sketch.addAreaDetail(pool);

        Assert.assertSame(pool, sketch.findAreaContaining(new Coord2D(50, 50)));
        Assert.assertNull(sketch.findAreaContaining(new Coord2D(150, 50)));
    }

    @Test
    public void testFindAreaContainingSkipsAreaWhoseHoleIsTapped() {
        // tapping the courtyard of a ring must not find the ring: that space isn't part of it
        Sketch sketch = new Sketch();
        AreaDetail ring =
                new AreaDetail(
                        new ArrayList<>(rectangleContour(0, 0, 100, 100)),
                        Collections.singletonList(
                                new ArrayList<>(rectangleContour(20, 20, 60, 60))),
                        AreaType.WATER,
                        Colour.BLUE);
        sketch.addAreaDetail(ring);

        Assert.assertNull(sketch.findAreaContaining(new Coord2D(50, 50)));
        Assert.assertSame(ring, sketch.findAreaContaining(new Coord2D(10, 50)));
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
