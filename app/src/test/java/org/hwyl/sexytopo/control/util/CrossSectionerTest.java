package org.hwyl.sexytopo.control.util;

import org.hwyl.sexytopo.SexyTopoConstants;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.testutils.BasicTestSurveyCreator;
import org.junit.Assert;
import org.junit.Test;


public class CrossSectionerTest {

    @Test
    public void testStraightNorthCrossSection() {
        Survey testSurvey = BasicTestSurveyCreator.createStraightNorth();

        Station s2 = testSurvey.getStationByName("2");
        double angle = CrossSectioner.getAngleOfSection(testSurvey, s2);
        Assert.assertEquals(0.0, angle, SexyTopoConstants.ALLOWED_DOUBLE_DELTA);
    }

    @Test
    public void testStraightSouthCrossSection() {
        Survey testSurvey = BasicTestSurveyCreator.createStraightSouth();

        Station s2 = testSurvey.getStationByName("2");
        double angle = CrossSectioner.getAngleOfSection(testSurvey, s2);
        Assert.assertEquals(180.0, angle, SexyTopoConstants.ALLOWED_DOUBLE_DELTA);
    }

    @Test
    public void testCrossSectionSpanningZeroBoundary() {
        // Issue #176: legs at 350° and 10° should give cross-section at 0°, not 180°
        Survey testSurvey = BasicTestSurveyCreator.createSpanningZeroBoundary();

        Station s2 = testSurvey.getStationByName("2");
        double angle = CrossSectioner.getAngleOfSection(testSurvey, s2);
        Assert.assertEquals(0.0, angle, SexyTopoConstants.ALLOWED_DOUBLE_DELTA);
    }
}
