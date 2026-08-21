package org.hwyl.sexytopo.control.io.basic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.sketch.AreaDetail;
import org.hwyl.sexytopo.model.sketch.AreaType;
import org.hwyl.sexytopo.model.sketch.Colour;
import org.hwyl.sexytopo.model.sketch.CrossSection;
import org.hwyl.sexytopo.model.sketch.CrossSectionDetail;
import org.hwyl.sexytopo.model.sketch.LineType;
import org.hwyl.sexytopo.model.sketch.PathDetail;
import org.hwyl.sexytopo.model.sketch.Sketch;
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
    public void testAreaDetailRoundTrip() throws Exception {
        List<Coord2D> polygon = new ArrayList<>();
        polygon.add(new Coord2D(0, 0));
        polygon.add(new Coord2D(4, 0));
        polygon.add(new Coord2D(4, 4));
        polygon.add(new Coord2D(0, 4));
        AreaDetail areaDetail = new AreaDetail(polygon, AreaType.WATER, Colour.BLUE);

        JSONObject json = SketchJsonTranslater.toJson(areaDetail);
        AreaDetail restored = SketchJsonTranslater.toAreaDetail(json);

        Assert.assertEquals(AreaType.WATER, restored.getAreaType());
        Assert.assertEquals(Colour.BLUE, restored.getColour());
        Assert.assertEquals(4, restored.getOutline().size());
        Assert.assertEquals(4f, restored.getOutline().get(2).x, 0.0001f);
        Assert.assertFalse(restored.hasHoles());
    }

    @Test
    public void testAreaDetailWithHolesRoundTrip() throws Exception {
        List<Coord2D> outline =
                Arrays.asList(
                        new Coord2D(0, 0), new Coord2D(4, 0), new Coord2D(4, 4), new Coord2D(0, 4));
        List<Coord2D> hole =
                Arrays.asList(
                        new Coord2D(1, 1), new Coord2D(3, 1), new Coord2D(3, 3), new Coord2D(1, 3));
        AreaDetail areaDetail =
                new AreaDetail(
                        outline, Collections.singletonList(hole), AreaType.WATER, Colour.BLUE);

        JSONObject json = SketchJsonTranslater.toJson(areaDetail);
        AreaDetail restored = SketchJsonTranslater.toAreaDetail(json);

        Assert.assertEquals(4, restored.getOutline().size());
        Assert.assertEquals(1, restored.getHoles().size());
        Assert.assertEquals(4, restored.getHoles().get(0).size());
        Assert.assertEquals(3f, restored.getHoles().get(0).get(1).x, 0.0001f);
    }

    @Test
    public void testAreaDetailWithoutHolesTagLoads() throws Exception {
        // areas written before holes were supported have no "holes" key
        List<Coord2D> polygon =
                Arrays.asList(new Coord2D(0, 0), new Coord2D(2, 0), new Coord2D(1, 2));
        JSONObject json =
                SketchJsonTranslater.toJson(new AreaDetail(polygon, AreaType.WATER, Colour.BLUE));
        Assert.assertFalse(json.has(SketchJsonTranslater.HOLES_TAG));

        AreaDetail restored = SketchJsonTranslater.toAreaDetail(json);
        Assert.assertEquals(3, restored.getOutline().size());
        Assert.assertTrue(restored.getHoles().isEmpty());
    }

    @Test
    public void testSketchWithAreasRoundTrip() throws Exception {
        Survey survey = new Survey();
        Sketch sketch = new Sketch();
        List<Coord2D> polygon = new ArrayList<>();
        polygon.add(new Coord2D(0, 0));
        polygon.add(new Coord2D(2, 0));
        polygon.add(new Coord2D(1, 2));
        List<AreaDetail> areaDetails = new ArrayList<>();
        areaDetails.add(new AreaDetail(polygon, AreaType.WATER, Colour.BLACK));
        sketch.setAreaDetails(areaDetails);

        JSONObject json = SketchJsonTranslater.toJson(sketch, survey, "test", 1);
        Sketch restored = SketchJsonTranslater.toSketch(survey, json);

        Assert.assertEquals(1, restored.getAreaDetails().size());
        Assert.assertEquals(AreaType.WATER, restored.getAreaDetails().get(0).getAreaType());
    }

    @Test
    public void testPathDetailLineTypeRoundTrip() throws Exception {
        List<Coord2D> points =
                Arrays.asList(new Coord2D(0, 0), new Coord2D(2, 0), new Coord2D(4, 1));
        PathDetail pathDetail = new PathDetail(points, Colour.BLACK, LineType.WALL);

        JSONObject json = SketchJsonTranslater.toJson(pathDetail);
        Assert.assertTrue(json.has(SketchJsonTranslater.LINE_TYPE_TAG));

        PathDetail restored = SketchJsonTranslater.toPathDetail(json);
        Assert.assertEquals(LineType.WALL, restored.getLineType());
    }

    @Test
    public void testOrnamentSizeRoundTrip() throws Exception {
        List<Coord2D> points = Arrays.asList(new Coord2D(0, 0), new Coord2D(2, 0));
        PathDetail pathDetail = new PathDetail(points, Colour.BLACK, LineType.PIT, 0.35f);

        JSONObject json = SketchJsonTranslater.toJson(pathDetail);
        PathDetail restored = SketchJsonTranslater.toPathDetail(json);

        Assert.assertEquals(0.35f, restored.getOrnamentSize(), 0.0001f);
    }

    @Test
    public void testTypedPathWithoutOrnamentSizeTagGetsDefault() throws Exception {
        // typed lines written before ornament sizes existed have no "ornament-size" key
        List<Coord2D> points = Arrays.asList(new Coord2D(0, 0), new Coord2D(2, 0));
        JSONObject json =
                SketchJsonTranslater.toJson(
                        new PathDetail(points, Colour.BLACK, LineType.PIT, 0.35f));
        json.remove(SketchJsonTranslater.ORNAMENT_SIZE_TAG);

        PathDetail restored = SketchJsonTranslater.toPathDetail(json);
        Assert.assertEquals(PathDetail.DEFAULT_ORNAMENT_SIZE, restored.getOrnamentSize(), 0.0001f);
    }

    @Test
    public void testGeneralPathOmitsLineTypeTag() throws Exception {
        // paths written before line types existed have no "line-type" key, so general paths
        // are written the same way to keep old and new files alike
        List<Coord2D> points = Arrays.asList(new Coord2D(0, 0), new Coord2D(2, 0));
        JSONObject json = SketchJsonTranslater.toJson(new PathDetail(points, Colour.BLACK));
        Assert.assertFalse(json.has(SketchJsonTranslater.LINE_TYPE_TAG));
        Assert.assertFalse(json.has(SketchJsonTranslater.ORNAMENT_SIZE_TAG));

        PathDetail restored = SketchJsonTranslater.toPathDetail(json);
        Assert.assertEquals(LineType.SKETCH, restored.getLineType());
    }

    @Test
    public void testSketchWithoutAreasTagLoads() throws Exception {
        // sketch files written before areas existed have no "areas" key
        Survey survey = new Survey();
        Sketch sketch = new Sketch();
        JSONObject json = SketchJsonTranslater.toJson(sketch, survey, "test", 1);
        json.remove(SketchJsonTranslater.AREAS_TAG);

        Sketch restored = SketchJsonTranslater.toSketch(survey, json);
        Assert.assertTrue(restored.getAreaDetails().isEmpty());
    }
}
