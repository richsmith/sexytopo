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
        baseSurvey.connect(
                baseSurvey.getActiveStation(), otherSurvey, otherSurvey.getActiveStation());
        Assert.assertTrue(baseSurvey.isConnectedTo(otherSurvey));
        Assert.assertEquals(1, baseSurvey.getConnectedSurveys().size());
    }

    @Test
    public void testSurveyCanBeDisconnected() throws Exception {
        Survey baseSurvey = BasicTestSurveyCreator.createStraightNorth();
        Survey otherSurvey = BasicTestSurveyCreator.createRightRight();
        baseSurvey.connect(
                baseSurvey.getActiveStation(), otherSurvey, otherSurvey.getActiveStation());
        baseSurvey.disconnect(baseSurvey.getActiveStation(), otherSurvey);
        Assert.assertFalse(baseSurvey.isConnectedTo(otherSurvey));
        Assert.assertEquals(0, baseSurvey.getConnectedSurveys().size());
    }

    @Test
    public void testSettingCrossSectionScaleUpdatesBothSketches() {
        Survey survey = new Survey();
        survey.setCrossSectionScale(2.5f);
        Assert.assertEquals(2.5f, survey.getCrossSectionScale(), 0f);
        Assert.assertEquals(2.5f, survey.getPlanSketch().getCrossSectionScale(), 0f);
        Assert.assertEquals(2.5f, survey.getElevationSketch().getCrossSectionScale(), 0f);
    }

    @Test
    public void testSettingCrossSectionScaleMarksBothSketchesUnsaved() {
        Survey survey = new Survey();
        Assert.assertTrue(survey.isSaved());
        survey.setCrossSectionScale(2f);
        Assert.assertFalse(survey.getPlanSketch().isSaved());
        Assert.assertFalse(survey.getElevationSketch().isSaved());
        Assert.assertFalse(survey.isSaved());
    }

    @Test
    public void testSyncingCrossSectionScaleCopiesPlanScaleToElevation() {
        Survey survey = new Survey();
        survey.getPlanSketch().setCrossSectionScale(2f);
        survey.getElevationSketch().setCrossSectionScale(1f);

        survey.syncCrossSectionScale();

        Assert.assertEquals(2f, survey.getPlanSketch().getCrossSectionScale(), 0f);
        Assert.assertEquals(2f, survey.getElevationSketch().getCrossSectionScale(), 0f);
    }

    @Test
    public void testSyncingCrossSectionScaleDoesNotMarkAnythingUnsaved() {
        Survey survey = new Survey();
        survey.getPlanSketch().setCrossSectionScale(2f);

        survey.syncCrossSectionScale();

        Assert.assertTrue(survey.isSaved());
    }
}
