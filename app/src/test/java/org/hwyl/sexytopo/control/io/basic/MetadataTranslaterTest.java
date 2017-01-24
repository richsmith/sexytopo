package org.hwyl.sexytopo.control.io.basic;

import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.testhelpers.BasicTestSurveyCreator;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;


@RunWith(PowerMockRunner.class)
@PrepareForTest(Loader.class)
public class MetadataTranslaterTest {


    @Test
    public void testActiveStationIsTranslatedToJson() throws Exception {
        Survey survey = new Survey("test");
        String translated = MetadataTranslater.translate(survey);
        Assert.assertEquals("{\"active-station\":\"1\",\"connections\":{}}", translated);
    }


    @Test
    public void testActiveStationIsTParsed() throws Exception {
        Survey survey = new Survey("test");
        String text = "{\"active-station\":\"1\",\"connections\":{}}";
        MetadataTranslater.translateAndUpdate(survey, text);
        Assert.assertEquals(survey.getActiveStation().getName(), "1");
    }


    @Test
    public void testConnectedSurveyIsTranslatedToJson() throws Exception {
        Survey survey = new Survey("first");
        Survey connectedSurvey = new Survey("connected");
        connectTwoSurveys(survey, survey.getOrigin(), connectedSurvey, connectedSurvey.getOrigin());
        String translated = MetadataTranslater.translate(survey);
        Assert.assertEquals(
                "{\"active-station\":\"1\",\"connections\":{\"1\":[[\"connected\",\"1\"]]}}",
                translated);
    }

    @Test
    public void testJsonIsTranslatedToSurveyWithActiveStation() throws Exception {
        String json = "{\"active-station\":\"1\",\"connections\":{}}";

        Survey testSurvey = BasicTestSurveyCreator.createStraightNorth();
        testSurvey.setActiveStation(testSurvey.getStationByName("2"));

        MetadataTranslater.translateAndUpdate(testSurvey, json);
        Assert.assertEquals("1", testSurvey.getActiveStation().getName());
        Assert.assertEquals(0, testSurvey.getConnectedSurveys().size());

    }


    @Test
    public void testConnectedSurveyJsonIsTranslatedToConnectedSurvey() throws Exception {
        String json = "{\"active-station\":\"1\",\"connections\":{\"1\":[[\"connected\",\"1\"]]}}";

        Survey survey = new Survey("test");
        Survey connected = new Survey("connected");

        PowerMockito.mockStatic(Loader.class);
        Mockito.when(Loader.loadSurvey("connected")).thenReturn(connected);

        MetadataTranslater.translateAndUpdate(survey, json);
        Assert.assertEquals(1, survey.getConnectedSurveys().size());
    }


    private static void connectTwoSurveys(Survey survey0, Station join0,
                                          Survey survey1, Station join1) {
        survey0.connect(join0, survey1, join1);
        survey1.connect(join1, survey0, join0);
    }

}