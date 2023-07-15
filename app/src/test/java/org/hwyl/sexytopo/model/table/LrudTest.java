package org.hwyl.sexytopo.model.table;

import org.hwyl.sexytopo.SexyTopoConstants;
import org.junit.Assert;

import org.hwyl.sexytopo.testhelpers.BasicTestSurveyCreator;
import org.hwyl.sexytopo.control.util.CrossSectioner;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.junit.Test;


public class LrudTest {

    @Test
    public void testStraightNorthCrossSectionLeftSplay() {
        Survey testSurvey = BasicTestSurveyCreator.createStraightNorth();

        Station s2 = testSurvey.getStationByName("2");
        double angle = CrossSectioner.getAngleOfSection(testSurvey, s2);
        Leg splay = LRUD.LEFT.createSplay(testSurvey, s2, 5);

        Assert.assertEquals(270.0, splay.getAzimuth(), SexyTopoConstants.ALLOWED_DOUBLE_DELTA);
    }


    @Test
    public void testStraightNorthCrossSectionRightSplay() {
        Survey testSurvey = BasicTestSurveyCreator.createStraightNorth();

        Station s2 = testSurvey.getStationByName("2");
        double angle = CrossSectioner.getAngleOfSection(testSurvey, s2);
        Leg splay = LRUD.RIGHT.createSplay(testSurvey, s2, 5);

        Assert.assertEquals(90.0, splay.getAzimuth(), SexyTopoConstants.ALLOWED_DOUBLE_DELTA);
    }
}
