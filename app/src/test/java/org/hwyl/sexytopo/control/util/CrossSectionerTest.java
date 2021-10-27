package org.hwyl.sexytopo.control.util;

import org.hwyl.sexytopo.SexyTopo;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.testhelpers.BasicTestSurveyCreator;
import org.junit.Assert;
import org.junit.Test;


public class CrossSectionerTest {

    @Test
    public void testStraightNorthCrossSection() {
        Survey testSurvey = BasicTestSurveyCreator.createStraightNorth();

        Station s2 = testSurvey.getStationByName("2");
        double angle = CrossSectioner.getAngleOfSection(testSurvey, s2);
        Assert.assertEquals(0.0, angle, SexyTopo.ALLOWED_DOUBLE_DELTA);
    }

    @Test
    public void testStraightSouthCrossSection() {
        Survey testSurvey = BasicTestSurveyCreator.createStraightSouth();

        Station s2 = testSurvey.getStationByName("2");
        double angle = CrossSectioner.getAngleOfSection(testSurvey, s2);
        Assert.assertEquals(180.0, angle, SexyTopo.ALLOWED_DOUBLE_DELTA);
    }
}
