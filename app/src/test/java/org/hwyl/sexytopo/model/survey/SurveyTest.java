package org.hwyl.sexytopo.model.survey;

import org.hwyl.sexytopo.testhelpers.BasicTestSurveyCreator;
import org.junit.Assert;
import org.junit.Test;


public class SurveyTest {

    @Test
    public void testSurveyCanBeConnected() {
        Survey baseSurvey = BasicTestSurveyCreator.createStraightNorth();
        Survey otherSurvey = BasicTestSurveyCreator.createRightRight();
        baseSurvey.connect(baseSurvey.getActiveStation(),
                otherSurvey, otherSurvey.getActiveStation());
        Assert.assertTrue(baseSurvey.isConnectedTo(otherSurvey));
        Assert.assertEquals(baseSurvey.getConnectedSurveys().size(), 1);
    }


    @Test
    public void testSurveyCanBeDisconnected() throws Exception {
        Survey baseSurvey = BasicTestSurveyCreator.createStraightNorth();
        Survey otherSurvey = BasicTestSurveyCreator.createRightRight();
        baseSurvey.connect(baseSurvey.getActiveStation(),
                otherSurvey, otherSurvey.getActiveStation());
        baseSurvey.disconnect(baseSurvey.getActiveStation(), otherSurvey);
        Assert.assertFalse(baseSurvey.isConnectedTo(otherSurvey));
        Assert.assertEquals(baseSurvey.getConnectedSurveys().size(), 0);
    }

}
