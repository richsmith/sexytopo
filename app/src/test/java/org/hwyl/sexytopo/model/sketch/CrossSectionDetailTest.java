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
}
