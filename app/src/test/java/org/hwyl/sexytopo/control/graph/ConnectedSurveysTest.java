package org.hwyl.sexytopo.control.graph;

import org.hwyl.sexytopo.SexyTopo;
import org.hwyl.sexytopo.control.activity.GraphActivity;
import org.hwyl.sexytopo.control.activity.PlanActivity;
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
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ConnectedSurveysTest {

    @Mock
    GraphActivity mockActivity;

    @Test
    public void testNoConnectedSurveysReturnNoUpdatedSurveys() {

        Survey currentSurvey = getBasicSurvey("not-connected");

        Sketch fakeSketch = new Sketch();
        when(mockActivity.getSketch(currentSurvey)).thenReturn(fakeSketch);

        Map<Survey, Space<Coord2D>> translated =
                ConnectedSurveys.getTranslatedConnectedSurveys(
                        mockActivity, currentSurvey, new Space<Coord2D>());

        Assert.assertEquals(translated.size(), 0);
    }


    @Test
    public void testConnectedSurveyGetsTranslatedCorrectly() throws Exception {

        Survey currentSurvey = getBasicSurvey("current");
        Survey joinedSurvey = getBasicSurvey("joined");
        connectTwoSurveys(currentSurvey, currentSurvey.getActiveStation(),
                joinedSurvey, joinedSurvey.getOrigin());

        GraphActivity activity = new PlanActivity();

        Space<Coord2D> planProjection = Projection2D.PLAN.project(currentSurvey);
        Map<Survey, Space<Coord2D>> translated =
                ConnectedSurveys.getTranslatedConnectedSurveys(
                        activity, currentSurvey, planProjection);

        Assert.assertEquals(translated.size(), 1);
        Survey translatedSurvey = getNamedSurvey(translated, "joined");
        Space<Coord2D> projection = translated.get(translatedSurvey);
        Coord2D newStationPoint = getStationPosition(projection, "2");
        Assert.assertEquals(-2.0, newStationPoint.y, SexyTopo.ALLOWED_DOUBLE_DELTA);
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

        GraphActivity activity = new PlanActivity();

        Space<Coord2D> planProjection = Projection2D.PLAN.project(currentSurvey);
        Map<Survey, Space<Coord2D>> translated =
                ConnectedSurveys.getTranslatedConnectedSurveys(
                        activity, currentSurvey, planProjection);

        Assert.assertEquals(translated.size(), 2);

        Survey translatedSurvey = getNamedSurvey(translated, "joined-1");
        Space<Coord2D> projection = translated.get(translatedSurvey);

        Coord2D newStationPoint = getStationPosition(projection, "2");
        Assert.assertEquals(-3.0, newStationPoint.y, SexyTopo.ALLOWED_DOUBLE_DELTA);
    }

    private Survey getNamedSurvey(Map<Survey, Space<Coord2D>> map, String name) throws Exception {
        for (Survey survey : map.keySet()) {
            if (survey.getName().equals(name)) {
                return survey;
            }
        }
        throw new Exception("Could not find survey " + name);
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

        GraphActivity activity = new PlanActivity();

        Space<Coord2D> planProjection = Projection2D.PLAN.project(currentSurvey);
        Map<Survey, Space<Coord2D>> translated =
                ConnectedSurveys.getTranslatedConnectedSurveys(
                        activity, currentSurvey, planProjection);

        Sketch translatedSketch = translated.keySet().iterator().next().getPlanSketch();
        PathDetail translatedPathDetail =
                translatedSketch.getPathDetails().toArray(new PathDetail[] {})[0];
        List<Coord2D> coords = translatedPathDetail.getPath();
        Assert.assertEquals(new Coord2D(0, -1), coords.get(0));
        Assert.assertEquals(new Coord2D(0, 0), coords.get(1));
    }


    private static Survey getBasicSurvey(String name) {
        Survey basicSurvey = new Survey(name);
        Leg l0 = new Leg(1.0, 0.0, 0.0);
        SurveyUpdater.update(basicSurvey, l0);
        SurveyUpdater.upgradeSplayToConnectedLeg(basicSurvey, l0, InputMode.FORWARD);
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
