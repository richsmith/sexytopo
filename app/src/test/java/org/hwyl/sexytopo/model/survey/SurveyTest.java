package org.hwyl.sexytopo.model.survey;

import org.hwyl.sexytopo.control.util.SurveyUpdater;
import org.hwyl.sexytopo.testutils.BasicTestSurveyCreator;
import org.junit.Assert;
import org.junit.Test;


public class SurveyTest {

    @Test
    public void testAddedLegCanBeUndone() {
        Survey baseSurvey = BasicTestSurveyCreator.createStraightNorth();
        baseSurvey.undoAddLeg();
        Assert.assertEquals(2, baseSurvey.getAllLegs().size());
    }

    @Test
    public void testAddedSplayCanBeUndone() {
        Survey baseSurvey = BasicTestSurveyCreator.createStraightNorth();
        SurveyUpdater.update(baseSurvey, new Leg(5, 0, 0));
        baseSurvey.undoAddLeg();
        Assert.assertEquals(3, baseSurvey.getAllLegs().size());
    }

    @Test
    public void testSurveyCanBeConnected() {
        Survey baseSurvey = BasicTestSurveyCreator.createStraightNorth();
        Survey otherSurvey = BasicTestSurveyCreator.createRightRight();
        baseSurvey.connect(baseSurvey.getActiveStation(),
                otherSurvey, otherSurvey.getActiveStation());
        Assert.assertTrue(baseSurvey.isConnectedTo(otherSurvey));
        Assert.assertEquals(1, baseSurvey.getConnectedSurveys().size());
    }


    @Test
    public void testSurveyCanBeDisconnected() throws Exception {
        Survey baseSurvey = BasicTestSurveyCreator.createStraightNorth();
        Survey otherSurvey = BasicTestSurveyCreator.createRightRight();
        baseSurvey.connect(baseSurvey.getActiveStation(),
                otherSurvey, otherSurvey.getActiveStation());
        baseSurvey.disconnect(baseSurvey.getActiveStation(), otherSurvey);
        Assert.assertFalse(baseSurvey.isConnectedTo(otherSurvey));
        Assert.assertEquals(0, baseSurvey.getConnectedSurveys().size());
    }

}
