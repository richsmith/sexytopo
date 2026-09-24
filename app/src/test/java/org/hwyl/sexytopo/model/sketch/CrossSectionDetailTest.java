package org.hwyl.sexytopo.model.sketch;

import java.util.ArrayList;
import java.util.List;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.testutils.BasicTestSketchCreator;
import org.junit.Assert;
import org.junit.Test;

public class CrossSectionDetailTest {

    @Test
    public void testTranslatePreservesSubSketchReference() {
        CrossSection crossSection = new CrossSection(new Station("A1"), 0f);
        Sketch subSketch = new Sketch();
        List<PathDetail> pathDetails = new ArrayList<>();
        pathDetails.add(new PathDetail(Coord2D.ORIGIN, Colour.BLACK));
        subSketch.setPathDetails(pathDetails);

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
        Assert.assertTrue(detail.getSketch().getPathDetails().isEmpty());
    }

    @Test
    public void testHorizontalBoundingBoxCoversTheProjectedSplays() {
        Station station = new Station("A1");
        station.addOnwardLeg(new Leg(5, 90, 0));

        CrossSectionDetail detail =
                new CrossSectionDetail(CrossSection.horizontal(station), new Coord2D(10, 20));

        // The splay runs 5 to the east of the centre; the minimum extent is 1 either way
        Assert.assertEquals(15f, detail.getRight(), 1e-4f);
        Assert.assertEquals(9f, detail.getLeft(), 1e-4f);
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
    public void testBoundingBoxGrowsWithTheCrossSectionScale() {
        Station station = new Station("A1");
        station.addOnwardLeg(new Leg(2, 90, 0));
        CrossSectionDetail detail =
                new CrossSectionDetail(new CrossSection(station, 0f), Coord2D.ORIGIN);

        detail.setCrossSectionScale(3f);

        Assert.assertEquals(6f, detail.getRight(), 1e-4f);
    }

    @Test
    public void testBoundingBoxScalesTheMinimumFloorToo() {
        // A station with no splays at all still gets a minimum-size box, and that too should
        // grow with the scale, since that is how it is actually drawn
        CrossSectionDetail detail =
                new CrossSectionDetail(new CrossSection(new Station("A1"), 0f), Coord2D.ORIGIN);
        float naturalRight = detail.getRight();

        detail.setCrossSectionScale(2f);

        Assert.assertEquals(naturalRight * 2, detail.getRight(), 1e-4f);
    }

    @Test
    public void testBoundingBoxScalesTheSubSketchToo() {
        // A line drawn well beyond the minimum-extent floor on both sides, so that a bug scaling
        // only one end of it cannot hide behind that floor
        Sketch subSketch = new Sketch();
        subSketch.startNewPath(new Coord2D(-20, 0));
        subSketch.getActivePath().lineTo(new Coord2D(20, 0));
        subSketch.finishPath();
        CrossSectionDetail detail =
                new CrossSectionDetail(
                        new CrossSection(new Station("A1"), 0f), Coord2D.ORIGIN, subSketch);

        detail.setCrossSectionScale(2f);

        Assert.assertEquals(-40f, detail.getLeft(), 1e-4f);
        Assert.assertEquals(40f, detail.getRight(), 1e-4f);
    }

    @Test
    public void testBoundingBoxShrinksBackWhenTheScaleIsLowered() {
        CrossSectionDetail detail =
                new CrossSectionDetail(new CrossSection(new Station("A1"), 0f), Coord2D.ORIGIN);
        detail.setCrossSectionScale(3f);
        float enlargedRight = detail.getRight();

        detail.setCrossSectionScale(1f);

        Assert.assertTrue(detail.getRight() < enlargedRight);
    }

    @Test
    public void testBoundingBoxShrinksWhenTheSubSketchIsReplacedWithASmallerOne() {
        Sketch bigSketch = new Sketch();
        BasicTestSketchCreator.drawOneHorizontalLine(bigSketch);
        CrossSectionDetail detail =
                new CrossSectionDetail(
                        new CrossSection(new Station("A1"), 0f), Coord2D.ORIGIN, bigSketch);
        float bigRight = detail.getRight();

        detail.setSketch(new Sketch());

        Assert.assertTrue(detail.getRight() < bigRight);
    }

    @Test
    public void testNewDetailsDefaultToTheSketchsDefaultScale() {
        CrossSectionDetail detail =
                new CrossSectionDetail(new CrossSection(new Station("A1"), 0f), Coord2D.ORIGIN);

        Assert.assertEquals(Sketch.DEFAULT_XSECTION_SCALE, detail.getCrossSectionScale(), 0f);
    }

    @Test
    public void testTranslatePreservesTheCrossSectionScale() {
        CrossSectionDetail detail =
                new CrossSectionDetail(new CrossSection(new Station("A1"), 0f), Coord2D.ORIGIN);
        detail.setCrossSectionScale(2.5f);

        CrossSectionDetail translated = detail.translate(new Coord2D(5, 5));

        Assert.assertEquals(2.5f, translated.getCrossSectionScale(), 0f);
    }

    @Test
    public void testWithAnglePreservesTheCrossSectionScale() {
        CrossSectionDetail detail =
                new CrossSectionDetail(new CrossSection(new Station("A1"), 10f), Coord2D.ORIGIN);
        detail.setCrossSectionScale(2.5f);

        CrossSectionDetail rotated = detail.withAngle(80f);

        Assert.assertEquals(2.5f, rotated.getCrossSectionScale(), 0f);
    }
}
