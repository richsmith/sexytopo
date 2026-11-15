package org.hwyl.sexytopo.control.io.basic;

import android.content.Context;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.testutils.ExampleSurveyCreator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class ScratchAutosaveTest {

    private Context context;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        ScratchAutosave.clear(context);
    }

    @Test
    public void testNothingExistsInitially() {
        Assert.assertFalse(ScratchAutosave.exists(context));
    }

    @Test
    public void testSurveyRoundTrips() throws Exception {
        Survey survey = ExampleSurveyCreator.create(10, 2, false, true);
        survey.setActiveStation(survey.getAllStations().get(3));

        ScratchAutosave.save(context, survey);
        Assert.assertTrue(ScratchAutosave.exists(context));

        Survey loaded = ScratchAutosave.load(context);
        Assert.assertEquals(survey.getAllStations().size(), loaded.getAllStations().size());
        Assert.assertEquals(survey.getAllLegs().size(), loaded.getAllLegs().size());
        Assert.assertEquals(
                survey.getActiveStation().getName(), loaded.getActiveStation().getName());
        Assert.assertEquals(
                survey.getPlanSketch().getPathDetails().size(),
                loaded.getPlanSketch().getPathDetails().size());
        Assert.assertFalse(loaded.hasHome());
    }

    @Test
    public void testLoadedSurveyIsUnsaved() throws Exception {
        ScratchAutosave.save(context, ExampleSurveyCreator.create());
        Survey loaded = ScratchAutosave.load(context);
        Assert.assertFalse(loaded.isSaved());
        Assert.assertFalse(loaded.isAutosaved());
    }

    @Test
    public void testSavingMarksSurveyAutosaved() throws Exception {
        Survey survey = ExampleSurveyCreator.create();
        survey.setSaved(false);
        ScratchAutosave.save(context, survey);
        Assert.assertTrue(survey.isAutosaved());
    }

    @Test
    public void testClearRemovesSurvey() throws Exception {
        ScratchAutosave.save(context, ExampleSurveyCreator.create());
        ScratchAutosave.clear(context);
        Assert.assertFalse(ScratchAutosave.exists(context));
    }
}
