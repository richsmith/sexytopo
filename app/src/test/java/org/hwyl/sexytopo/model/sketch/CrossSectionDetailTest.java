package org.hwyl.sexytopo.model.sketch;

import java.util.ArrayList;
import java.util.List;
import org.hwyl.sexytopo.model.graph.Coord2D;
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
}
