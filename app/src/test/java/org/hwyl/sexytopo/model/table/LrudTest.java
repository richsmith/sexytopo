package org.hwyl.sexytopo.model.table;

import org.hwyl.sexytopo.SexyTopoConstants;
import org.junit.Assert;

import org.hwyl.sexytopo.testutils.BasicTestSurveyCreator;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.junit.Test;


public class LrudTest {

    @Test
    public void testStraightNorthSurveyModeLeftSplay() {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        Station s2 = survey.getStationByName("2");
        Leg splay = LRUD.LEFT.createSplay(survey, s2, LRUD.Mode.SURVEY, 5);
        Assert.assertEquals(270.0, splay.getAzimuth(), SexyTopoConstants.ALLOWED_DOUBLE_DELTA);
    }

    @Test
    public void testStraightNorthSurveyModeRightSplay() {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        Station s2 = survey.getStationByName("2");
        Leg splay = LRUD.RIGHT.createSplay(survey, s2, LRUD.Mode.SURVEY, 5);
        Assert.assertEquals(90.0, splay.getAzimuth(), SexyTopoConstants.ALLOWED_DOUBLE_DELTA);
    }

    @Test
    public void testCornerSurveyModeLeftSplay() {
        // Station 2 has incoming leg from north (0°) and outgoing leg going east (90°)
        // SURVEY mode bisects to 45°, so LEFT = 315°
        Survey survey = BasicTestSurveyCreator.createRightRight();
        Station s2 = survey.getStationByName("2");
        Leg splay = LRUD.LEFT.createSplay(survey, s2, LRUD.Mode.SURVEY, 5);
        Assert.assertEquals(315.0, splay.getAzimuth(), SexyTopoConstants.ALLOWED_DOUBLE_DELTA);
    }

    @Test
    public void testCornerSurveyModeRightSplay() {
        // Station 2 has incoming leg from north (0°) and outgoing leg going east (90°)
        // SURVEY mode bisects to 45°, so RIGHT = 135°
        Survey survey = BasicTestSurveyCreator.createRightRight();
        Station s2 = survey.getStationByName("2");
        Leg splay = LRUD.RIGHT.createSplay(survey, s2, LRUD.Mode.SURVEY, 5);
        Assert.assertEquals(135.0, splay.getAzimuth(), SexyTopoConstants.ALLOWED_DOUBLE_DELTA);
    }

    @Test
    public void testCornerShotModeLeftSplay() {
        // Station 2 has outgoing leg going east (90°)
        // SHOT mode uses outgoing azimuth 90°, so LEFT = 0°
        Survey survey = BasicTestSurveyCreator.createRightRight();
        Station s2 = survey.getStationByName("2");
        Leg splay = LRUD.LEFT.createSplay(survey, s2, LRUD.Mode.SHOT, 5);
        Assert.assertEquals(0.0, splay.getAzimuth(), SexyTopoConstants.ALLOWED_DOUBLE_DELTA);
    }

    @Test
    public void testCornerShotModeRightSplay() {
        // Station 2 has outgoing leg going east (90°)
        // SHOT mode uses outgoing azimuth 90°, so RIGHT = 180°
        Survey survey = BasicTestSurveyCreator.createRightRight();
        Station s2 = survey.getStationByName("2");
        Leg splay = LRUD.RIGHT.createSplay(survey, s2, LRUD.Mode.SHOT, 5);
        Assert.assertEquals(180.0, splay.getAzimuth(), SexyTopoConstants.ALLOWED_DOUBLE_DELTA);
    }
}
