package org.hwyl.sexytopo.model.sketch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.hwyl.sexytopo.control.util.PolygonUtils;
import org.hwyl.sexytopo.model.geometry.Coord2D;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.junit.Assert;
import org.junit.Test;

public class SketchTest {

    private static final float DELTA = 1e-4f;

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
    public void testStartNewPathUsesGivenLineType() {
        Sketch sketch = new Sketch();
        PathDetail pathDetail = sketch.startNewPath(Coord2D.ORIGIN, Colour.BLACK, LineType.WALL);
        Assert.assertEquals(LineType.WALL, pathDetail.getLineType());

        PathDetail plain = sketch.startNewPath(new Coord2D(1, 1), Colour.BLACK, LineType.SKETCH);
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
    public void testEraserCutsLongSegmentAtEdgeOfRadius() {
        // a single long segment: erasing its middle should leave a gap the size of the eraser,
        // not remove the whole segment
        PathDetail pathDetail =
                new PathDetail(
                        new ArrayList<>(Arrays.asList(new Coord2D(0, 0), new Coord2D(10, 0))),
                        Colour.BLACK,
                        LineType.WALL);

        List<SketchDetail> fragments =
                pathDetail.getPathFragmentsOutsideRadius(new Coord2D(5, 0.3f), 0.5);

        Assert.assertEquals(2, fragments.size());
        List<Coord2D> first = ((PathDetail) fragments.get(0)).getPath();
        List<Coord2D> second = ((PathDetail) fragments.get(1)).getPath();
        Assert.assertEquals(new Coord2D(0, 0), first.get(0));
        Assert.assertEquals(4.6f, first.get(first.size() - 1).x, 0.001f);
        Assert.assertEquals(5.4f, second.get(0).x, 0.001f);
        Assert.assertEquals(new Coord2D(10, 0), second.get(second.size() - 1));
    }

    @Test
    public void testEraserTrimsEndOfLine() {
        PathDetail pathDetail =
                new PathDetail(
                        new ArrayList<>(Arrays.asList(new Coord2D(0, 0), new Coord2D(10, 0))),
                        Colour.BLACK,
                        LineType.WALL);

        List<SketchDetail> fragments =
                pathDetail.getPathFragmentsOutsideRadius(new Coord2D(0, 0), 1);

        Assert.assertEquals(1, fragments.size());
        List<Coord2D> path = ((PathDetail) fragments.get(0)).getPath();
        Assert.assertEquals(1f, path.get(0).x, 0.001f);
        Assert.assertEquals(new Coord2D(10, 0), path.get(path.size() - 1));
    }

    @Test
    public void testFlipPathDetailReversesPointsAsSingleUndoStep() {
        Sketch sketch = new Sketch();
        PathDetail original = sketch.startNewPath(new Coord2D(0, 0), Colour.BLACK, LineType.PIT);
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
    public void testFinishPathDropsLinesSmallerThanTheMinimum() {
        Sketch sketch = new Sketch();
        sketch.startNewPath(new Coord2D(0, 0)).lineTo(new Coord2D(0.05f, 0.05f));
        sketch.finishPath(0.1f);
        Assert.assertTrue(sketch.getPathDetails().isEmpty());
        Assert.assertNull(sketch.getActivePath());

        sketch.startNewPath(new Coord2D(0, 0)).lineTo(new Coord2D(0.05f, 0.2f));
        sketch.finishPath(0.1f);
        Assert.assertEquals(1, sketch.getPathDetails().size());
    }

    @Test
    public void testFinishPathClipsTypedLinesToWallsAsOneUndoableStep() {
        Sketch sketch = new Sketch();
        // interior on the (-dy, dx) side, i.e. y > 0
        drawLine(sketch, LineType.WALL, new Coord2D(0, 0), new Coord2D(10, 0));

        drawLine(sketch, LineType.PIT, new Coord2D(5, 3), new Coord2D(5, -2));

        Assert.assertEquals(2, sketch.getPathDetails().size());
        PathDetail pit = sketch.getPathDetails().get(1);
        Assert.assertEquals(LineType.PIT, pit.getLineType());
        Assert.assertEquals(0, pit.getPath().get(pit.getPath().size() - 1).y, 0.0001f);

        sketch.undo();
        Assert.assertEquals(1, sketch.getPathDetails().size());
    }

    @Test
    public void testFinishPathDoesNotClipFreehandLinesOrWalls() {
        Sketch sketch = new Sketch();
        drawLine(sketch, LineType.WALL, new Coord2D(0, 0), new Coord2D(10, 0));

        drawLine(sketch, LineType.SKETCH, new Coord2D(5, 3), new Coord2D(5, -2));
        drawLine(sketch, LineType.PRESUMED_WALL, new Coord2D(3, 3), new Coord2D(3, -2));

        Assert.assertEquals(-2, sketch.getPathDetails().get(1).getPath().get(1).y, 0.0001f);
        Assert.assertEquals(-2, sketch.getPathDetails().get(2).getPath().get(1).y, 0.0001f);
    }

    private static void drawLine(Sketch sketch, LineType lineType, Coord2D from, Coord2D to) {
        sketch.startNewPath(from, Colour.BLACK, lineType).lineTo(to);
        sketch.finishPath();
    }

    @Test
    public void testRetypePathDetailIsOneUndoableStepKeepingPointOrder() {
        Sketch sketch = new Sketch();
        PathDetail original =
                new PathDetail(
                        new ArrayList<>(Arrays.asList(new Coord2D(0, 0), new Coord2D(10, 0))),
                        Colour.RED,
                        LineType.CHIMNEY);
        sketch.restoreDetailToSketch(original);

        PathDetail retyped = sketch.retypePathDetail(original, LineType.PIT);

        Assert.assertEquals(1, sketch.getPathDetails().size());
        Assert.assertSame(retyped, sketch.getPathDetails().get(0));
        Assert.assertEquals(LineType.PIT, retyped.getLineType());
        Assert.assertEquals(Colour.RED, retyped.getColour());
        Assert.assertEquals(original.getPath(), retyped.getPath());

        sketch.undo();
        Assert.assertSame(original, sketch.getPathDetails().get(0));
    }

    @Test
    public void testRetypeAreaDetailIsOneUndoableStepKeepingColour() {
        Sketch sketch = new Sketch();
        AreaDetail original =
                new AreaDetail(rectangleContour(0, 0, 10, 10), AreaType.PEBBLES, Colour.RED);
        sketch.restoreDetailToSketch(original);

        sketch.retypeAreaDetail(original, AreaType.DEBRIS);

        Assert.assertEquals(1, sketch.getAreaDetails().size());
        AreaDetail retyped = sketch.getAreaDetails().get(0);
        Assert.assertEquals(AreaType.DEBRIS, retyped.getAreaType());
        Assert.assertEquals(Colour.RED, retyped.getColour());
        Assert.assertEquals(original.getOutline(), retyped.getOutline());

        sketch.undo();
        Assert.assertEquals(1, sketch.getAreaDetails().size());
        Assert.assertSame(original, sketch.getAreaDetails().get(0));
    }

    @Test
    public void testRetypeAreaDetailToWaterTakesBlueWhenBlueWaterIsOn() {
        // Blue Water is on by default, which is what the unit tests see
        Sketch sketch = new Sketch();
        AreaDetail original =
                new AreaDetail(rectangleContour(0, 0, 10, 10), AreaType.PEBBLES, Colour.RED);
        sketch.restoreDetailToSketch(original);

        sketch.retypeAreaDetail(original, AreaType.WATER);

        Assert.assertEquals(Colour.WATER, sketch.getAreaDetails().get(0).getColour());
    }

    @Test
    public void testRecolourAreaDetailIsOneUndoableStepKeepingType() {
        Sketch sketch = new Sketch();
        AreaDetail original =
                new AreaDetail(rectangleContour(0, 0, 10, 10), AreaType.PEBBLES, Colour.RED);
        sketch.restoreDetailToSketch(original);

        sketch.recolourAreaDetail(original, Colour.GREEN);

        Assert.assertEquals(1, sketch.getAreaDetails().size());
        AreaDetail recoloured = sketch.getAreaDetails().get(0);
        Assert.assertEquals(Colour.GREEN, recoloured.getColour());
        Assert.assertEquals(AreaType.PEBBLES, recoloured.getAreaType());

        sketch.undo();
        Assert.assertSame(original, sketch.getAreaDetails().get(0));
    }

    @Test
    public void testRecolourAreaDetailOverridesAutoColour() {
        // Blue Water is on by default, which is what the unit tests see
        Sketch sketch = new Sketch();
        AreaDetail original =
                new AreaDetail(rectangleContour(0, 0, 10, 10), AreaType.WATER, Colour.WATER);
        sketch.restoreDetailToSketch(original);

        sketch.recolourAreaDetail(original, Colour.PURPLE);

        Assert.assertEquals(Colour.PURPLE, sketch.getAreaDetails().get(0).getColour());
    }

    @Test
    public void testRecolourPathDetailIsOneUndoableStepKeepingPointsAndType() {
        Sketch sketch = new Sketch();
        PathDetail original =
                new PathDetail(
                        new ArrayList<>(Arrays.asList(new Coord2D(0, 0), new Coord2D(5, 5))),
                        Colour.BLACK,
                        LineType.PIT);
        sketch.restoreDetailToSketch(original);

        PathDetail recoloured = sketch.recolourPathDetail(original, Colour.BROWN);

        Assert.assertEquals(Colour.BROWN, recoloured.getColour());
        Assert.assertEquals(LineType.PIT, recoloured.getLineType());
        Assert.assertEquals(original.getPath(), recoloured.getPath());
        Assert.assertEquals(1, sketch.getPathDetails().size());

        sketch.undo();
        Assert.assertSame(original, sketch.getPathDetails().get(0));
    }

    @Test
    public void testRecolourTextDetailKeepsTextPlaceAndSize() {
        Sketch sketch = new Sketch();
        sketch.addTextDetail(new Coord2D(3, 4), "sump", Colour.RED, 2);
        TextDetail original = sketch.getTextDetails().get(0);

        TextDetail recoloured = sketch.recolourTextDetail(original, Colour.BLUE);

        Assert.assertEquals(Colour.BLUE, recoloured.getColour());
        Assert.assertEquals("sump", recoloured.getText());
        Assert.assertEquals(original.getPosition(), recoloured.getPosition());
        Assert.assertEquals(2, recoloured.getSize(), DELTA);
        Assert.assertEquals(1, sketch.getTextDetails().size());
    }

    @Test
    public void testMoveDetailIsOneUndoableStepKeepingTextColourAndSize() {
        Sketch sketch = new Sketch();
        sketch.addTextDetail(new Coord2D(3, 4), "sump", Colour.RED, 2);
        TextDetail original = sketch.getTextDetails().get(0);

        sketch.moveDetail(original, new Coord2D(1, -2));

        Assert.assertEquals(1, sketch.getTextDetails().size());
        TextDetail moved = sketch.getTextDetails().get(0);
        Assert.assertEquals(new Coord2D(4, 2), moved.getPosition());
        Assert.assertEquals("sump", moved.getText());
        Assert.assertEquals(Colour.RED, moved.getColour());
        Assert.assertEquals(2, moved.getSize(), DELTA);

        sketch.undo();
        Assert.assertEquals(Collections.singletonList(original), sketch.getTextDetails());
    }

    @Test
    public void testResizeDetailIsOneUndoableStepKeepingTextPlaceAndColour() {
        Sketch sketch = new Sketch();
        sketch.addTextDetail(new Coord2D(3, 4), "sump", Colour.RED, 2);
        TextDetail original = sketch.getTextDetails().get(0);

        sketch.resizeDetail(original, 1.5f);

        Assert.assertEquals(1, sketch.getTextDetails().size());
        TextDetail resized = sketch.getTextDetails().get(0);
        Assert.assertEquals(3, resized.getSize(), DELTA);
        Assert.assertEquals(original.getPosition(), resized.getPosition());
        Assert.assertEquals("sump", resized.getText());
        Assert.assertEquals(Colour.RED, resized.getColour());

        sketch.undo();
        Assert.assertEquals(Collections.singletonList(original), sketch.getTextDetails());
    }

    @Test
    public void testChangeTextIsOneUndoableStepKeepingPlaceColourAndSize() {
        Sketch sketch = new Sketch();
        sketch.addTextDetail(new Coord2D(3, 4), "sump", Colour.RED, 2);
        TextDetail original = sketch.getTextDetails().get(0);

        TextDetail changed = sketch.changeText(original, "duck");

        Assert.assertEquals(1, sketch.getTextDetails().size());
        Assert.assertSame(changed, sketch.getTextDetails().get(0));
        Assert.assertEquals("duck", changed.getText());
        Assert.assertEquals(original.getPosition(), changed.getPosition());
        Assert.assertEquals(Colour.RED, changed.getColour());
        Assert.assertEquals(2, changed.getSize(), DELTA);

        sketch.undo();
        Assert.assertEquals(1, sketch.getTextDetails().size());
        Assert.assertSame(original, sketch.getTextDetails().get(0));
    }

    // Retyping never turns a line round, even into a wall, which a freshly drawn wall would be
    @Test
    public void testRetypePathDetailKeepsPointOrder() {
        List<Coord2D> points = Arrays.asList(new Coord2D(0, 0), new Coord2D(10, 0));

        for (List<Coord2D> drawn : Arrays.asList(points, reversed(points))) {
            Sketch sketch = new Sketch();
            PathDetail original = new PathDetail(new ArrayList<>(drawn), Colour.BLACK);
            sketch.restoreDetailToSketch(original);
            PathDetail retyped = sketch.retypePathDetail(original, LineType.WALL);
            Assert.assertEquals(drawn, retyped.getPath());
        }
    }

    private static List<Coord2D> reversed(List<Coord2D> points) {
        List<Coord2D> copy = new ArrayList<>(points);
        Collections.reverse(copy);
        return copy;
    }

    @Test
    public void testGetMostRecentOrientedPathSkipsUnorientedLines() {
        Sketch sketch = new Sketch();
        Assert.assertNull(sketch.getMostRecentOrientedPath());

        PathDetail wall = sketch.startNewPath(new Coord2D(0, 0), Colour.BLACK, LineType.WALL);
        wall.lineTo(new Coord2D(1, 0));
        sketch.finishPath();

        PathDetail scribble = sketch.startNewPath(new Coord2D(5, 5), Colour.BLACK, LineType.SKETCH);
        scribble.lineTo(new Coord2D(6, 5));
        sketch.finishPath();

        PathDetail boulderEdge =
                sketch.startNewPath(new Coord2D(8, 8), Colour.BLACK, LineType.ROCK_EDGE);
        boulderEdge.lineTo(new Coord2D(9, 8));
        sketch.finishPath();

        Assert.assertSame(wall, sketch.getMostRecentOrientedPath());
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
    public void testGroupedDeletionsAreSingleUndoStep() {
        // simulates dragging the eraser across two areas, nibbling one twice
        Sketch sketch = new Sketch();
        AreaDetail first = squareArea(0, 0, 4);
        AreaDetail second = squareArea(5, 0, 2);
        sketch.addAreaDetail(first);
        sketch.addAreaDetail(second);

        sketch.startGroupingDeletions();
        AreaDetail nibbled = squareArea(0, 0, 3);
        sketch.deleteDetail(first, Collections.singletonList(nibbled));
        AreaDetail nibbledAgain = squareArea(0, 0, 2);
        sketch.deleteDetail(nibbled, Collections.singletonList(nibbledAgain));
        sketch.deleteDetail(second);
        sketch.endGroupingDeletions();

        Assert.assertEquals(1, sketch.getAreaDetails().size());
        Assert.assertSame(nibbledAgain, sketch.getAreaDetails().get(0));

        sketch.undo();
        Assert.assertEquals(2, sketch.getAreaDetails().size());
        Assert.assertTrue(sketch.getAreaDetails().contains(first));
        Assert.assertTrue(sketch.getAreaDetails().contains(second));

        sketch.redo();
        Assert.assertEquals(1, sketch.getAreaDetails().size());
        Assert.assertSame(nibbledAgain, sketch.getAreaDetails().get(0));
    }

    @Test
    public void testDeletionsAfterGroupEndsAreSeparateUndoSteps() {
        Sketch sketch = new Sketch();
        AreaDetail first = squareArea(0, 0, 2);
        AreaDetail second = squareArea(3, 0, 2);
        sketch.addAreaDetail(first);
        sketch.addAreaDetail(second);

        sketch.startGroupingDeletions();
        sketch.deleteDetail(first);
        sketch.endGroupingDeletions();
        sketch.deleteDetail(second);

        sketch.undo();
        Assert.assertEquals(1, sketch.getAreaDetails().size());
        Assert.assertSame(second, sketch.getAreaDetails().get(0));
    }

    @Test
    public void testFinishAreaCreatesPolygonFromOutline() {
        Sketch sketch = new Sketch();
        sketch.startNewArea(new Coord2D(0, 0), Colour.BLACK, AreaType.WATER);
        sketch.getActivePath().lineTo(new Coord2D(4, 0));
        sketch.getActivePath().lineTo(new Coord2D(4, 4));
        sketch.getActivePath().lineTo(new Coord2D(0, 4));
        sketch.finishArea(AreaType.WATER, 0);

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
        sketch.startNewArea(new Coord2D(0, 0), Colour.BLACK, AreaType.WATER);
        sketch.getActivePath().lineTo(new Coord2D(4, 0));
        sketch.getActivePath().lineTo(new Coord2D(4, 4));
        sketch.getActivePath().lineTo(new Coord2D(0, 4));
        sketch.finishArea(AreaType.WATER, 0);

        Assert.assertEquals(1, sketch.getAreaDetails().size());
        Assert.assertFalse(sketch.getAreaDetails().get(0).hasHoles());
    }

    @Test
    public void testFinishAreaDiscardsDegenerateOutline() {
        Sketch sketch = new Sketch();
        sketch.startNewArea(new Coord2D(0, 0), Colour.BLACK, AreaType.WATER);
        sketch.getActivePath().lineTo(new Coord2D(1, 0));
        sketch.finishArea(AreaType.WATER, 0);

        Assert.assertEquals(0, sketch.getAreaDetails().size());
        Assert.assertEquals(0, sketch.getPathDetails().size());
        sketch.undo(); // must not blow up or resurrect anything
        Assert.assertEquals(0, sketch.getAreaDetails().size());
    }

    @Test
    public void testFinishAreaTrimsOvershootWhereTheEndsMeet() {
        Sketch sketch = new Sketch();
        sketch.startNewArea(new Coord2D(2, 0), Colour.BLACK, AreaType.WATER);
        sketch.getActivePath().lineTo(new Coord2D(10, 0));
        sketch.getActivePath().lineTo(new Coord2D(10, 10));
        sketch.getActivePath().lineTo(new Coord2D(0, 10));
        sketch.getActivePath().lineTo(new Coord2D(0, 0));
        sketch.getActivePath().lineTo(new Coord2D(3, 1));
        sketch.getActivePath().lineTo(new Coord2D(5, -1)); // overshoots below the start
        sketch.finishArea(AreaType.WATER, 0);

        AreaDetail area = sketch.getAreaDetails().get(0);
        // nothing of the overshoot is left poking out below the square
        Assert.assertTrue(area.getOutline().stream().allMatch(point -> point.y >= -1e-4));
    }

    @Test
    public void testFinishAreaTrimsHooksWhereTheEndsMeet() {
        Sketch sketch = new Sketch();
        sketch.startNewArea(new Coord2D(3, -2), Colour.BLACK, AreaType.WATER);
        Coord2D[] corners = {
            new Coord2D(2, 0),
            new Coord2D(10, 0),
            new Coord2D(10, 10),
            new Coord2D(0, 10),
            new Coord2D(0, 0),
            new Coord2D(-2, -2) // curls away from the start
        };
        for (Coord2D corner : corners) {
            sketch.getActivePath().lineTo(corner);
        }
        sketch.finishArea(AreaType.WATER, 0);

        AreaDetail area = sketch.getAreaDetails().get(0);
        // neither hook is left poking out below the square
        Assert.assertTrue(area.getOutline().stream().allMatch(point -> point.y >= -1e-4));
    }

    @Test
    public void testFinishAreaDiscardsAreaBelowMinimumSize() {
        Sketch sketch = new Sketch();
        sketch.startNewArea(new Coord2D(0, 0), Colour.BLACK, AreaType.WATER);
        sketch.getActivePath().lineTo(new Coord2D(2, 0));
        sketch.getActivePath().lineTo(new Coord2D(2, 2));
        sketch.getActivePath().lineTo(new Coord2D(0, 2));
        sketch.finishArea(AreaType.WATER, 5); // encloses 4 square metres

        Assert.assertEquals(0, sketch.getAreaDetails().size());
        Assert.assertEquals(0, sketch.getPathDetails().size());
        Assert.assertNull(sketch.getActivePath());
    }

    @Test
    public void testFinishAreaKeepsAreaAtMinimumSize() {
        Sketch sketch = new Sketch();
        sketch.startNewArea(new Coord2D(0, 0), Colour.BLACK, AreaType.WATER);
        sketch.getActivePath().lineTo(new Coord2D(2, 0));
        sketch.getActivePath().lineTo(new Coord2D(2, 2));
        sketch.getActivePath().lineTo(new Coord2D(0, 2));
        sketch.finishArea(AreaType.WATER, 4);

        Assert.assertEquals(1, sketch.getAreaDetails().size());
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

    @Test
    public void testSameStationCanHaveIndependentCrossSectionsInPlanAndElevation() {
        Survey survey = new Survey();
        Station station = survey.getOrigin();
        Sketch plan = survey.getPlanSketch();
        Sketch elevation = survey.getElevationSketch();

        CrossSectionDetail planDetail =
                new CrossSectionDetail(new CrossSection(station, 0f), new Coord2D(1, 2));
        plan.addCrossSection(planDetail);

        Assert.assertSame(planDetail, plan.getCrossSectionDetail(station));
        Assert.assertNull(elevation.getCrossSectionDetail(station));

        CrossSectionDetail elevationDetail =
                new CrossSectionDetail(new CrossSection(station, 0f), new Coord2D(5, 6));
        elevation.addCrossSection(elevationDetail);

        Assert.assertSame(planDetail, plan.getCrossSectionDetail(station));
        Assert.assertSame(elevationDetail, elevation.getCrossSectionDetail(station));

        elevation.deleteDetail(elevationDetail);

        Assert.assertSame(planDetail, plan.getCrossSectionDetail(station));
        Assert.assertNull(elevation.getCrossSectionDetail(station));
    }

    /** A section at (10, 0) whose only splay reaches 2m east of it. */
    private static Sketch sketchWithASectionReachingEast() {
        Station station = new Station("1");
        station.addOnwardLeg(new Leg(2, 90, 0));
        Sketch sketch = new Sketch();
        sketch.addCrossSection(
                new CrossSectionDetail(new CrossSection(station, 0f), new Coord2D(10, 0)));
        return sketch;
    }

    @Test
    public void testBoundingBoxHoldsCrossSectionsAtTheCrossSectionScale() {
        Sketch sketch = sketchWithASectionReachingEast();
        Assert.assertEquals(12, sketch.getRight(), DELTA);

        sketch.setCrossSectionScale(3f);
        Assert.assertEquals(16, sketch.getRight(), DELTA);

        sketch.setCrossSectionScale(0.5f);
        Assert.assertEquals(11, sketch.getRight(), DELTA);
    }

    @Test
    public void testCopiedSketchKeepsItsCrossSectionScaleInTheBoundingBox() {
        Sketch sketch = sketchWithASectionReachingEast();
        sketch.setCrossSectionScale(3f);

        Assert.assertEquals(16, new Sketch(sketch).getRight(), DELTA);
    }

    /**
     * A sketch holding one finished line of the given type drawn from (0, 0) to (10, 0) by (5, -2).
     */
    private static Sketch sketchWithLine(LineType lineType) {
        Sketch sketch = new Sketch();
        drawActivePath(sketch, lineType, Coord2D.ORIGIN, new Coord2D(5, -2), new Coord2D(10, 0));
        sketch.finishPath();
        return sketch;
    }

    private static PathDetail drawActivePath(Sketch sketch, LineType lineType, Coord2D... points) {
        PathDetail path = sketch.startNewPath(points[0], Colour.BLACK, lineType);
        for (int i = 1; i < points.length; i++) {
            path.lineTo(points[i]);
        }
        return path;
    }

    private static List<Coord2D> onlyLine(Sketch sketch) {
        Assert.assertEquals(1, sketch.getPathDetails().size());
        return sketch.getPathDetails().get(0).getPath();
    }

    @Test
    public void testLineSnappedOnToTheEndOfAnotherIsJoinedToIt() {
        Sketch sketch = sketchWithLine(LineType.WALL);
        // lands a little beyond the wall's end and wobbles back before heading off
        drawActivePath(
                sketch,
                LineType.WALL,
                new Coord2D(10.5f, 0),
                new Coord2D(9.8f, 0.3f),
                new Coord2D(10, 5),
                new Coord2D(10, 10));

        sketch.snapActivePath(1);
        sketch.finishPath();

        Assert.assertEquals(
                Arrays.asList(
                        Coord2D.ORIGIN,
                        new Coord2D(5, -2),
                        new Coord2D(10, 0),
                        new Coord2D(10, 10)),
                onlyLine(sketch));
    }

    @Test
    public void testLineSnappedInToTheStartOfAnotherIsJoinedToIt() {
        Sketch sketch = sketchWithLine(LineType.WALL);
        // overshoots the wall's start and wobbles back
        drawActivePath(
                sketch,
                LineType.WALL,
                new Coord2D(0, 10),
                new Coord2D(0, 5),
                new Coord2D(0, -0.5f),
                new Coord2D(0.3f, 0.2f));

        sketch.snapActivePath(1);
        sketch.finishPath();

        Assert.assertEquals(
                Arrays.asList(
                        new Coord2D(0, 10), Coord2D.ORIGIN, new Coord2D(5, -2), new Coord2D(10, 0)),
                onlyLine(sketch));
    }

    @Test
    public void testJoinCanBeUndone() {
        Sketch sketch = sketchWithLine(LineType.WALL);
        drawActivePath(sketch, LineType.WALL, new Coord2D(10.5f, 0), new Coord2D(10, 10));
        sketch.snapActivePath(1);
        sketch.finishPath();

        sketch.undo();

        Assert.assertEquals(
                Arrays.asList(Coord2D.ORIGIN, new Coord2D(5, -2), new Coord2D(10, 0)),
                onlyLine(sketch));
    }

    @Test
    public void testOrientedLineDrawnAgainstAnotherIsReversedToJoinIt() {
        Sketch sketch = sketchWithLine(LineType.WALL);
        // starts at the wall's start, so runs against it
        drawActivePath(sketch, LineType.WALL, new Coord2D(-0.5f, 0), new Coord2D(-10, 0));

        sketch.snapActivePath(1);
        sketch.finishPath();

        Assert.assertEquals(
                Arrays.asList(
                        new Coord2D(-10, 0),
                        Coord2D.ORIGIN,
                        new Coord2D(5, -2),
                        new Coord2D(10, 0)),
                onlyLine(sketch));
    }

    @Test
    public void testOrientedLineJoiningOneLineEachWayKeepsItsDirection() {
        Sketch sketch = sketchWithLine(LineType.WALL);
        drawActivePath(sketch, LineType.WALL, new Coord2D(20, 0.5f), new Coord2D(20, 10));
        sketch.finishPath();
        // runs on from the first wall's end and into the second's end: one join either way
        PathDetail path =
                drawActivePath(
                        sketch,
                        LineType.WALL,
                        new Coord2D(10.5f, 0),
                        new Coord2D(15, 5),
                        new Coord2D(20, 10.5f));

        sketch.snapActivePath(1);

        Assert.assertEquals(new Coord2D(10, 0), path.getStart());
        Assert.assertEquals(new Coord2D(20, 10.5f), path.getEnd());
    }

    @Test
    public void testUnorientedLinesJoinWhicheverWayTheyRun() {
        Sketch sketch = sketchWithLine(LineType.SKETCH);
        drawActivePath(sketch, LineType.SKETCH, new Coord2D(-0.5f, 0), new Coord2D(-10, 0));

        sketch.snapActivePath(1);
        sketch.finishPath();

        Assert.assertEquals(
                Arrays.asList(
                        new Coord2D(10, 0),
                        new Coord2D(5, -2),
                        Coord2D.ORIGIN,
                        new Coord2D(-10, 0)),
                onlyLine(sketch));
    }

    @Test
    public void testLinesOfAnotherTypeAreNotSnappedTo() {
        Sketch sketch = sketchWithLine(LineType.WALL);
        PathDetail path =
                drawActivePath(sketch, LineType.PIT, new Coord2D(10, 10), new Coord2D(10, 0.5f));

        sketch.snapActivePath(1);

        Assert.assertEquals(
                Arrays.asList(new Coord2D(10, 10), new Coord2D(10, 0.5f)), path.getPath());
    }

    @Test
    public void testLineSnappedAtBothEndsToOneLineClosesIt() {
        Sketch sketch = sketchWithLine(LineType.WALL);
        drawActivePath(
                sketch,
                LineType.WALL,
                new Coord2D(10.5f, 0),
                new Coord2D(5, 5),
                new Coord2D(-0.5f, 0));

        sketch.snapActivePath(1);
        sketch.finishPath();

        Assert.assertEquals(
                Arrays.asList(
                        Coord2D.ORIGIN,
                        new Coord2D(5, -2),
                        new Coord2D(10, 0),
                        new Coord2D(5, 5),
                        Coord2D.ORIGIN),
                onlyLine(sketch));
    }

    @Test
    public void testEndSnapsBackToItsOwnStart() {
        Sketch sketch = new Sketch();
        PathDetail path =
                drawActivePath(
                        sketch,
                        LineType.WALL,
                        new Coord2D(0, 0),
                        new Coord2D(5, 0),
                        new Coord2D(5, 5),
                        new Coord2D(0.5f, 0.3f),
                        new Coord2D(-0.2f, -0.5f));

        sketch.snapActivePath(1);

        Assert.assertEquals(
                Arrays.asList(
                        new Coord2D(0, 0), new Coord2D(5, 0), new Coord2D(5, 5), new Coord2D(0, 0)),
                path.getPath());
    }

    @Test
    public void testSmallStrokeDoesNotCloseOnItself() {
        Sketch sketch = new Sketch();
        PathDetail path =
                drawActivePath(
                        sketch,
                        LineType.WALL,
                        new Coord2D(0, 0),
                        new Coord2D(0.5f, 0),
                        new Coord2D(0.5f, 0.3f));

        sketch.snapActivePath(1);

        Assert.assertEquals(3, path.getPath().size());
        Assert.assertEquals(new Coord2D(0.5f, 0.3f), path.getPath().get(2));
    }

    @Test
    public void testFindNearestDetailSkipsThoseTheFilterRejects() {
        Sketch sketch = new Sketch();
        sketch.addTextDetail(new Coord2D(0, 0), "Nearer", Colour.BLACK, 0.3f);
        sketch.addTextDetail(new Coord2D(0.5f, 0), "Further", Colour.BLACK, 0.3f);

        SketchDetail found =
                sketch.findNearestVisibleDetailWithin(
                        new Coord2D(0, 0),
                        1f,
                        60f,
                        detail -> ((TextDetail) detail).getText().equals("Further"));

        Assert.assertEquals("Further", ((TextDetail) found).getText());
    }

    @Test
    public void testFindNearestDetailIsNullWhenTheFilterRejectsEverything() {
        Sketch sketch = new Sketch();
        sketch.addTextDetail(new Coord2D(0, 0), "Hello", Colour.BLACK, 0.3f);

        Assert.assertNull(
                sketch.findNearestVisibleDetailWithin(new Coord2D(0, 0), 1f, 60f, detail -> false));
    }
}
