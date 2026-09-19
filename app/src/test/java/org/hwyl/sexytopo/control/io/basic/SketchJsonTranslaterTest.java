package org.hwyl.sexytopo.control.io.basic;

import java.util.ArrayList;
import java.util.List;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.sketch.Colour;
import org.hwyl.sexytopo.model.sketch.CrossSection;
import org.hwyl.sexytopo.model.sketch.CrossSectionDetail;
import org.hwyl.sexytopo.model.sketch.PathDetail;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

public class SketchJsonTranslaterTest {

    @Test
    public void testCrossSectionDetailWithEmptySubSketchOmitsSketchKey() throws Exception {
        Survey survey = new Survey();
        CrossSection crossSection = new CrossSection(survey.getOrigin(), 0f);
        CrossSectionDetail detail = new CrossSectionDetail(crossSection, new Coord2D(3, 4));

        JSONObject json = SketchJsonTranslater.toJson(detail);
        Assert.assertFalse(
                "empty sub-sketch should not write a sketch key",
                json.has(SketchJsonTranslater.SKETCH_TAG));
    }

    @Test
    public void testCrossSectionDetailRoundTripWithSubSketch() throws Exception {
        Survey survey = new Survey();
        CrossSection crossSection = new CrossSection(survey.getOrigin(), 0f);
        Sketch subSketch = new Sketch();
        List<PathDetail> paths = new ArrayList<>();
        PathDetail path = new PathDetail(Coord2D.ORIGIN, Colour.BLACK);
        path.lineTo(new Coord2D(1, 2));
        paths.add(path);
        subSketch.setPathDetails(paths);
        CrossSectionDetail detail =
                new CrossSectionDetail(crossSection, new Coord2D(3, 4), subSketch);

        JSONObject json = SketchJsonTranslater.toJson(detail);
        Assert.assertTrue(json.has(SketchJsonTranslater.SKETCH_TAG));

        CrossSectionDetail restored = SketchJsonTranslater.toCrossSectionDetail(survey, json);
        Assert.assertEquals(1, restored.getSketch().getPathDetails().size());
    }

    @Test
    public void testCrossSectionDetailRoundTripWithoutSketchKeyYieldsEmptySubSketch()
            throws Exception {
        Survey survey = new Survey();
        CrossSection crossSection = new CrossSection(survey.getOrigin(), 0f);
        CrossSectionDetail detail = new CrossSectionDetail(crossSection, new Coord2D(3, 4));

        JSONObject json = SketchJsonTranslater.toJson(detail);
        CrossSectionDetail restored = SketchJsonTranslater.toCrossSectionDetail(survey, json);
        Assert.assertTrue(restored.getSketch().getPathDetails().isEmpty());
    }

    @Test
    public void testElevationSketchCrossSectionsAndScaleRoundTrip() throws Exception {
        Survey survey = new Survey();
        Station station = survey.getOrigin();
        Sketch elevation = survey.getElevationSketch();
        elevation.setCrossSectionScale(2.5f);
        elevation.addCrossSection(new CrossSection(station, 90f), new Coord2D(3, 4));

        Sketch restored = roundTrip(survey, elevation);

        Assert.assertEquals(2.5f, restored.getCrossSectionScale(), 0f);
        Assert.assertEquals(1, restored.getCrossSectionDetails().size());
        CrossSectionDetail detail = restored.getCrossSectionDetail(station);
        Assert.assertNotNull(detail);
        Assert.assertEquals(90f, detail.getCrossSection().getAngle(), 0f);
        Assert.assertEquals(new Coord2D(3, 4), detail.getPosition());
    }

    @Test
    public void testSameStationInPlanAndElevationRoundTripsIndependently() throws Exception {
        Survey survey = new Survey();
        Station station = survey.getOrigin();
        survey.getPlanSketch().addCrossSection(new CrossSection(station, 10f), new Coord2D(1, 1));
        survey.getElevationSketch()
                .addCrossSection(new CrossSection(station, 20f), new Coord2D(9, 9));

        Sketch restoredPlan = roundTrip(survey, survey.getPlanSketch());
        Sketch restoredElevation = roundTrip(survey, survey.getElevationSketch());

        CrossSectionDetail planDetail = restoredPlan.getCrossSectionDetail(station);
        CrossSectionDetail elevationDetail = restoredElevation.getCrossSectionDetail(station);
        Assert.assertEquals(new Coord2D(1, 1), planDetail.getPosition());
        Assert.assertEquals(10f, planDetail.getCrossSection().getAngle(), 0f);
        Assert.assertEquals(new Coord2D(9, 9), elevationDetail.getPosition());
        Assert.assertEquals(20f, elevationDetail.getCrossSection().getAngle(), 0f);
    }

    private static Sketch roundTrip(Survey survey, Sketch sketch) throws Exception {
        String text = SketchJsonTranslater.translate(sketch, survey, "test", 1);
        return SketchJsonTranslater.translate(survey, text);
    }
}
