package org.hwyl.sexytopo.control.util;

import junit.framework.Assert;

import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.Line;
import org.hwyl.sexytopo.model.graph.Projection2D;
import org.hwyl.sexytopo.model.graph.Space;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.testhelpers.BasicTestSurveyCreator;
import org.junit.Test;

import java.util.Map;


public class ExtendedElevationProjectionTest {

    @Test
    public void testProject5MNorth() {

        Survey survey = BasicTestSurveyCreator.createStraightNorth();

        Space<Coord2D> space = Projection2D.EXTENDED_ELEVATION.project(survey);
        Map<Station, Coord2D> stationMap = space.getStationMap();

        Station two = survey.getStationByName("2");
        Coord2D twoCoord = stationMap.get(two);

        Coord2D expected = new Coord2D(5, 0);
        Assert.assertEquals(expected, twoCoord);
    }


    @Test
    public void testProject5MNorthReversed() {

        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        survey.getStationByName("2").switchDirection();

        Space<Coord2D> space = Projection2D.EXTENDED_ELEVATION.project(survey);
        Map<Station, Coord2D> stationMap = space.getStationMap();

        Station two = survey.getStationByName("2");
        Coord2D twoCoord = stationMap.get(two);

        Coord2D expected = new Coord2D(-5, 0);
        Assert.assertEquals(expected, twoCoord);
    }


    @Test
    public void testProject5MDown() {

        Survey survey = BasicTestSurveyCreator.create5MDown();

        Space<Coord2D> space = Projection2D.EXTENDED_ELEVATION.project(survey);
        Map<Station, Coord2D> stationMap = space.getStationMap();

        Station two = survey.getStationByName("2");
        Coord2D twoCoord = stationMap.get(two);

        Coord2D expected = new Coord2D(0, 5);
        Assert.assertEquals(expected, twoCoord);
    }

    @Test
    public void testProject1MNorth1MEast() {

        Survey survey = BasicTestSurveyCreator.create5MEast();

        Space<Coord2D> space = Projection2D.EXTENDED_ELEVATION.project(survey);
        Map<Station, Coord2D> stationMap = space.getStationMap();
        Map<Leg, Line<Coord2D>> legMap = space.getLegMap();


        Station two = survey.getStationByName("2");
        Coord2D twoCoord = stationMap.get(two);
        Coord2D expected = new Coord2D(5, 0);
        Assert.assertEquals(expected, twoCoord);

        Leg splayLeft = two.getUnconnectedOnwardLegs().get(0);
        Line splayLeftLine = legMap.get(splayLeft);
        expected = new Coord2D(5, 0);
        Assert.assertEquals(expected, splayLeftLine.getEnd());

        Leg splayRight = two.getUnconnectedOnwardLegs().get(1);
        Line splayRightLine = legMap.get(splayRight);
        expected = new Coord2D(5, 0);
        Assert.assertEquals(expected, splayRightLine.getEnd());

    }

}