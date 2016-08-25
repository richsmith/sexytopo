package org.hwyl.sexytopo.tests;

import android.test.AndroidTestCase;

import junit.framework.Assert;

import org.hwyl.sexytopo.control.util.CrossSectioner;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;


public class CrossSectionerTest extends AndroidTestCase {

    public void testStraightNorthCrossSection() {
        Survey testSurvey = BasicTestSurveyCreator.createStraightNorth();

        Station s2 = testSurvey.getStationByName("2");
        double angle = CrossSectioner.getAngleOfSection(testSurvey, s2);
        Assert.assertEquals(0.0, angle);
    }

    public void testStraightSouthCrossSection() {
        Survey testSurvey = BasicTestSurveyCreator.createStraightSouth();

        Station s2 = testSurvey.getStationByName("2");
        double angle = CrossSectioner.getAngleOfSection(testSurvey, s2);
        Assert.assertEquals(180.0, angle);
    }
}
