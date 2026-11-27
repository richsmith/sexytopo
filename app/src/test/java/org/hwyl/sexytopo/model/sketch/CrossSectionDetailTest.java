package org.hwyl.sexytopo.model.sketch;

import java.util.ArrayList;
import java.util.List;
import org.hwyl.sexytopo.model.geometry.Coord2D;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.junit.Assert;
import org.junit.Test;

public class CrossSectionDetailTest {

    @Test
    public void testTranslatePreservesSubSketchReference() {
        CrossSection crossSection = new CrossSection(new Station("A1"), 0f);
        Sketch subSketch = new Sketch();
        List<LineDetail> lineDetails = new ArrayList<>();
        lineDetails.add(new LineDetail(Coord2D.ORIGIN, Colour.BLACK));
        subSketch.setLineDetails(lineDetails);

        CrossSectionDetail original =
                new CrossSectionDetail(crossSection, Coord2D.ORIGIN, subSketch);
        CrossSectionDetail translated = original.translate(new Coord2D(5, 7));

        Assert.assertSame(subSketch, translated.getSketch());
        Assert.assertEquals(new Coord2D(5, 7), translated.getPosition());
    }

    @Test
    public void testDefaultConstructorCreatesEmptySubSketch() {
        CrossSection crossSection = new CrossSection(new Station("A1"), 0f);
        CrossSectionDetail detail = new CrossSectionDetail(crossSection, Coord2D.ORIGIN);
        Assert.assertNotNull(detail.getSketch());
        Assert.assertTrue(detail.getSketch().getLineDetails().isEmpty());
    }

    @Test
    public void testHorizontalBoundingBoxCoversTheProjectedSplays() {
        Station station = new Station("A1");
        station.addOnwardLeg(new Leg(5, 90, 0));

        CrossSectionDetail detail =
                new CrossSectionDetail(CrossSection.horizontal(station), new Coord2D(10, 20));

        // The splay runs 5 to the east of the centre; the minimum extent is 0.5 either way
        Assert.assertEquals(15f, detail.getRight(), 1e-4f);
        Assert.assertEquals(9.5f, detail.getLeft(), 1e-4f);
    }

    @Test
    public void testTranslatePreservesOrientation() {
        CrossSection horizontal = CrossSection.horizontal(new Station("A1"));
        CrossSectionDetail detail = new CrossSectionDetail(horizontal, Coord2D.ORIGIN);

        CrossSectionDetail translated = detail.translate(new Coord2D(5, 7));

        Assert.assertEquals(
                CrossSection.Orientation.HORIZONTAL, translated.getCrossSection().getOrientation());
    }

    @Test
    public void testWithAngleRotatesAVerticalCrossSection() {
        Sketch subSketch = new Sketch();
        CrossSectionDetail detail =
                new CrossSectionDetail(
                        new CrossSection(new Station("A1"), 10f), new Coord2D(1, 2), subSketch);

        CrossSectionDetail rotated = detail.withAngle(80f);

        Assert.assertEquals(80f, rotated.getCrossSection().getAngle(), 1e-4f);
        Assert.assertEquals(new Coord2D(1, 2), rotated.getPosition());
        Assert.assertSame(subSketch, rotated.getSketch());
    }

    @Test
    public void testWithAngleLeavesAHorizontalCrossSectionAlone() {
        CrossSectionDetail detail =
                new CrossSectionDetail(
                        CrossSection.horizontal(new Station("A1")), new Coord2D(1, 2));

        Assert.assertSame(detail, detail.withAngle(80f));
    }

    @Test
    public void testBoundingBoxShrinksWhenTheSubSketchIsReplacedWithASmallerOne() {
        CrossSectionDetail detail =
                new CrossSectionDetail(new CrossSection(new Station("A1"), 0f), Coord2D.ORIGIN);

        detail.setSketch(sketchWithLine(new Coord2D(0, 0), new Coord2D(10, 0)));
        Assert.assertEquals(10f, detail.getRight(), 1e-4f);

        detail.setSketch(sketchWithLine(new Coord2D(0, 0), new Coord2D(2, 0)));
        Assert.assertEquals(2f, detail.getRight(), 1e-4f);
    }

    private static Sketch sketchWithLine(Coord2D start, Coord2D end) {
        LineDetail line = new LineDetail(start, Colour.BLACK);
        line.lineTo(end);
        Sketch sketch = new Sketch();
        List<LineDetail> lineDetails = new ArrayList<>();
        lineDetails.add(line);
        sketch.setLineDetails(lineDetails);
        return sketch;
    }
}
