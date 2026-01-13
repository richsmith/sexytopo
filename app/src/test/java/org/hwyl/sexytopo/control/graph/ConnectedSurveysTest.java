package org.hwyl.sexytopo.control.graph;

import org.hwyl.sexytopo.SexyTopoConstants;
import org.hwyl.sexytopo.control.util.InputMode;
import org.hwyl.sexytopo.control.util.SurveyUpdater;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.Projection2D;
import org.hwyl.sexytopo.model.graph.Space;
import org.hwyl.sexytopo.model.sketch.PathDetail;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.testhelpers.SurveyMocker;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Map;

public class ConnectedSurveysTest {

    @Test
    public void testNoConnectedSurveysReturnNoUpdatedSurveys() {

        Survey currentSurvey = getBasicSurvey("current");
        Map<Survey, Space<Coord2D>> translated =
                ConnectedSurveys.getTranslatedConnectedSurveys(
                        Projection2D.PLAN, currentSurvey, new Space<>());

        Assert.assertEquals(0, translated.size());
    }

    @Test
    public void testBasicSurveyConnectionWorks() {

        Survey currentSurvey = getBasicSurvey("current");
        Survey joinedSurvey = getBasicSurvey("joined");

        connectTwoSurveys(currentSurvey, currentSurvey.getActiveStation(),
                joinedSurvey, joinedSurvey.getOrigin());

        Assert.assertTrue(currentSurvey.isConnectedTo(joinedSurvey));
        Assert.assertTrue(joinedSurvey.isConnectedTo(currentSurvey));
    }


    @Test
    public void testSecondarySurveyConnectionWorks() {

        Survey currentSurvey = getBasicSurvey("current");
        Survey joinedSurvey = getBasicSurvey("joined");
        Survey secondarySurvey = getBasicSurvey("joined-2");


        connectTwoSurveys(currentSurvey, currentSurvey.getActiveStation(),
                joinedSurvey, joinedSurvey.getOrigin());
        connectTwoSurveys(secondarySurvey, joinedSurvey.getActiveStation(),
                joinedSurvey, joinedSurvey.getOrigin());

        Assert.assertTrue(currentSurvey.isConnectedTo(joinedSurvey));
        Assert.assertTrue(joinedSurvey.isConnectedTo(currentSurvey));

        Assert.assertFalse(currentSurvey.isConnectedTo(secondarySurvey));
        Assert.assertFalse(secondarySurvey.isConnectedTo(currentSurvey));

        Assert.assertTrue(joinedSurvey.isConnectedTo(secondarySurvey));
        Assert.assertTrue(secondarySurvey.isConnectedTo(joinedSurvey));
    }

    @Test
    public void testConnectedSurveyGetsTranslatedCorrectly() throws Exception {

        Survey currentSurvey = getBasicSurvey("current");
        Survey joinedSurvey = getBasicSurvey("joined");
        connectTwoSurveys(currentSurvey, currentSurvey.getActiveStation(),
                joinedSurvey, joinedSurvey.getOrigin());

        Space<Coord2D> planProjection = Projection2D.PLAN.project(currentSurvey);
        Map<Survey, Space<Coord2D>> translated =
                ConnectedSurveys.getTranslatedConnectedSurveys(
                        Projection2D.PLAN, currentSurvey, planProjection);

        Assert.assertEquals(1, translated.size());
        Survey translatedSurvey = getSurveyWithUri(translated, "joined");
        Space<Coord2D> projection = translated.get(translatedSurvey);
        assert projection != null;
        Coord2D newStationPoint = getStationPosition(projection, "2");
        Assert.assertEquals(-2.0, newStationPoint.y, SexyTopoConstants.ALLOWED_DOUBLE_DELTA);
    }


    @Test
    public void testSecondaryConnectedSurveyGetsTranslatedCorrectly() throws Exception {

        Survey currentSurvey = getBasicSurvey("current");
        Survey joinedSurvey0 = getBasicSurvey("joined-0");
        Survey joinedSurvey1 = getBasicSurvey("joined-1");
        connectTwoSurveys(currentSurvey, currentSurvey.getActiveStation(),
                joinedSurvey0, joinedSurvey0.getOrigin());
        connectTwoSurveys(joinedSurvey0, joinedSurvey0.getActiveStation(),
                joinedSurvey1, joinedSurvey1.getOrigin());

        Space<Coord2D> planProjection = Projection2D.PLAN.project(currentSurvey);
        Map<Survey, Space<Coord2D>> translated =
                ConnectedSurveys.getTranslatedConnectedSurveys(
                        Projection2D.PLAN, currentSurvey, planProjection);

        Assert.assertEquals(2, translated.size());

        Survey translatedSurvey = getSurveyWithUri(translated, "joined-1");
        Space<Coord2D> projection = translated.get(translatedSurvey);

        assert projection != null;
        Coord2D newStationPoint = getStationPosition(projection, "2");
        Assert.assertEquals(-3.0, newStationPoint.y, SexyTopoConstants.ALLOWED_DOUBLE_DELTA);
    }

    private Survey getSurveyWithUri(Map<Survey, Space<Coord2D>> map, String uri) throws Exception {
        for (Survey survey : map.keySet()) {
            if (survey.getUri().toString().equals(uri)) {
                return survey;
            }
        }
        throw new Exception("Could not find survey " + uri);
    }


    @Test
    public void testConnectedSurveySketchGetsTranslatedCorrectly() {

        Survey currentSurvey = getBasicSurvey("current");
        Survey joinedSurvey = getBasicSurvey("joined");

        Sketch sketch = joinedSurvey.getPlanSketch();
        PathDetail pathDetail = sketch.startNewPath(new Coord2D(0, 0));
        pathDetail.lineTo(new Coord2D(0, 1));
        sketch.finishPath();

        connectTwoSurveys(currentSurvey, currentSurvey.getActiveStation(),
                joinedSurvey, joinedSurvey.getOrigin());

        Assert.assertTrue(currentSurvey.isConnectedTo(joinedSurvey));
        Assert.assertTrue(joinedSurvey.isConnectedTo(currentSurvey));

        Space<Coord2D> planProjection = Projection2D.PLAN.project(currentSurvey);
        Map<Survey, Space<Coord2D>> translated =
                ConnectedSurveys.getTranslatedConnectedSurveys(
                        Projection2D.PLAN, currentSurvey, planProjection);

        Sketch translatedSketch = translated.keySet().iterator().next().getPlanSketch();
        PathDetail translatedPathDetail =
                translatedSketch.getPathDetails().toArray(new PathDetail[] {})[0];
        List<Coord2D> coords = translatedPathDetail.getPath();
        Assert.assertEquals(new Coord2D(0, -1), coords.get(0));
        Assert.assertEquals(new Coord2D(0, 0), coords.get(1));
    }


    private static Survey getBasicSurvey(String uri) {
        Survey basicSurvey = new Survey();
        Leg l0 = new Leg(1.0f, 0.0f, 0.0f);
        SurveyUpdater.update(basicSurvey, l0);
        SurveyUpdater.upgradeSplay(basicSurvey, l0, InputMode.FORWARD);
        SurveyMocker.mockSurveyUri(basicSurvey, uri);
        return basicSurvey;
    }

    private static void connectTwoSurveys(Survey survey0, Station join0, Survey survey1, Station join1) {
        survey0.connect(join0, survey1, join1);
        survey1.connect(join1, survey0, join0);
    }

    private static Coord2D getStationPosition(Space<Coord2D> projection, String name) {
        Map<Station, Coord2D> map = projection.getStationMap();
        for (Station station : map.keySet()) {
            if (station.getName().equals(name)) {
                return map.get(station);
            }
        }
        throw new IllegalArgumentException("Station " + name + " not found");
    }
}
