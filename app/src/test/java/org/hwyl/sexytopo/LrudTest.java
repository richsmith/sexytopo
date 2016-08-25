package org.hwyl.sexytopo.tests;

import android.test.AndroidTestCase;

import junit.framework.Assert;

import org.hwyl.sexytopo.control.util.CrossSectioner;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.model.table.LRUD;


public class LrudTest extends AndroidTestCase {

    public void testStraightNorthCrossSectionLeftSplay() {
        Survey testSurvey = BasicTestSurveyCreator.createStraightNorth();

        Station s2 = testSurvey.getStationByName("2");
        double angle = CrossSectioner.getAngleOfSection(testSurvey, s2);
        Leg splay = LRUD.LEFT.createSplay(testSurvey, s2, 5);

        Assert.assertEquals(270.0, splay.getAzimuth());
    }


    public void testStraightNorthCrossSectionRightSplay() {
        Survey testSurvey = BasicTestSurveyCreator.createStraightNorth();

        Station s2 = testSurvey.getStationByName("2");
        double angle = CrossSectioner.getAngleOfSection(testSurvey, s2);
        Leg splay = LRUD.RIGHT.createSplay(testSurvey, s2, 5);

        Assert.assertEquals(90.0, splay.getAzimuth());
    }
}
