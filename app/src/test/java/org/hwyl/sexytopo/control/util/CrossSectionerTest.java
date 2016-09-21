package org.hwyl.sexytopo.control.util;

import junit.framework.Assert;

import org.hwyl.sexytopo.testhelpers.BasicTestSurveyCreator;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.junit.Test;


public class CrossSectionerTest {

    @Test
    public void testStraightNorthCrossSection() {
        Survey testSurvey = BasicTestSurveyCreator.createStraightNorth();

        Station s2 = testSurvey.getStationByName("2");
        double angle = CrossSectioner.getAngleOfSection(testSurvey, s2);
        Assert.assertEquals(0.0, angle);
    }

    @Test
    public void testStraightSouthCrossSection() {
        Survey testSurvey = BasicTestSurveyCreator.createStraightSouth();

        Station s2 = testSurvey.getStationByName("2");
        double angle = CrossSectioner.getAngleOfSection(testSurvey, s2);
        Assert.assertEquals(180.0, angle);
    }
}
