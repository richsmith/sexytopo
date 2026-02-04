package org.hwyl.sexytopo.control.io.basic;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anySet;

import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.testutils.BasicTestSurveyCreator;
import org.hwyl.sexytopo.testhelpers.SurveyMocker;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;


public class MetadataTranslaterTest {


    @Test
    public void testActiveStationIsTranslatedToJson() throws Exception {
        Survey survey = new Survey();
        String translated = MetadataTranslater.translate(survey);
        String expected = "{\"active-station\":\"1\",\"connections\":{}}";
        Assert.assertEquals(expected, translated.replaceAll("\\s", ""));
    }


    @Test
    public void testActiveStationIsTParsed() throws Exception {
        Survey survey = new Survey();
        String text = "{\"active-station\":\"1\",\"connections\":{}}";
        MetadataTranslater.translateAndUpdate(null, survey, text);
        Assert.assertEquals("1", survey.getActiveStation().getName());
    }


    @Test
    public void testConnectedSurveyIsTranslatedToJson() throws Exception {
        Survey survey = new Survey();
        SurveyMocker.mockSurveyUri(survey, "basic");
        Survey connectedSurvey = new Survey();
        SurveyMocker.mockSurveyUri(connectedSurvey, "connected");
        connectTwoSurveys(survey, survey.getOrigin(), connectedSurvey, connectedSurvey.getOrigin());
        String translated = MetadataTranslater.translate(survey);
        Assert.assertEquals(
            "{\"active-station\":\"1\",\"connections\":{\"1\":[[\"connected\",\"1\"]]}}",
            translated.replaceAll("\\s", ""));
    }

    @Test
    public void testJsonIsTranslatedToSurveyWithActiveStation() throws Exception {
        String json = "{\"active-station\":\"1\",\"connections\":{}}";

        Survey testSurvey = BasicTestSurveyCreator.createStraightNorth();
        testSurvey.setActiveStation(testSurvey.getStationByName("2"));

        MetadataTranslater.translateAndUpdate(null, testSurvey, json);
        Assert.assertEquals("1", testSurvey.getActiveStation().getName());
        Assert.assertEquals(0, testSurvey.getConnectedSurveys().size());

    }

    @Ignore("To mock static methods, need to use inline mocks, which breaks other tests")
    @Test
    public void testConnectedSurveyJsonIsTranslatedToConnectedSurvey() throws Exception {
        String json = "{\"active-station\":\"1\",\"connections\":{\"1\":[[\"connected\",\"1\"]]}}";

        Survey survey = new Survey();
        Survey connected = new Survey();

        try (MockedStatic<Loader> mockLoader = Mockito.mockStatic(Loader.class)) {
            mockLoader.when(() -> Loader.loadSurvey(
                any(), any(), anySet(), anyBoolean())
            ).thenReturn(connected);
        }

        MetadataTranslater.translateAndUpdate(null, survey, json);
        Assert.assertEquals(1, survey.getConnectedSurveys().size());
    }


    private static void connectTwoSurveys(Survey survey0, Station join0,
                                          Survey survey1, Station join1) {
        survey0.connect(join0, survey1, join1);
        survey1.connect(join1, survey0, join0);
    }


    @Test
    public void testBidirectionalConnectionWithDifferentStationNames() throws Exception {
        Survey surveyA = BasicTestSurveyCreator.createStraightNorth();
        SurveyMocker.mockSurveyUri(surveyA, "surveyA");
        Station joinPointA = surveyA.getStationByName("2");

        Survey surveyB = new Survey();
        SurveyMocker.mockSurveyUri(surveyB, "surveyB");
        surveyB.getOrigin().setName("1");

        connectTwoSurveys(surveyA, joinPointA, surveyB, surveyB.getOrigin());

        // Verify in-memory bidirectional connection
        Assert.assertTrue(surveyA.isConnectedTo(surveyB));
        Assert.assertTrue(surveyB.isConnectedTo(surveyA));

        // Verify JSON serialization stores correct station names for survey A
        String jsonA = MetadataTranslater.translate(surveyA);
        Assert.assertTrue("Survey A should reference station '1' in survey B",
            jsonA.contains("\"1\""));
        Assert.assertTrue("Survey A should connect from station '2'",
            jsonA.contains("\"2\""));

        // Verify JSON serialization stores correct station names for survey B
        String jsonB = MetadataTranslater.translate(surveyB);
        Assert.assertTrue("Survey B should reference station '2' in survey A",
            jsonB.contains("\"2\""));
    }


    @Test
    public void testBidirectionalConnectionWithAlphanumericStationName() throws Exception {
        Survey surveyA = BasicTestSurveyCreator.createStraightNorth();
        SurveyMocker.mockSurveyUri(surveyA, "surveyA");
        Station joinPointA = surveyA.getStationByName("2");

        Survey surveyB = new Survey();
        SurveyMocker.mockSurveyUri(surveyB, "surveyB");
        surveyB.getOrigin().setName("A23");

        connectTwoSurveys(surveyA, joinPointA, surveyB, surveyB.getOrigin());

        // Verify in-memory bidirectional connection
        Assert.assertTrue(surveyA.isConnectedTo(surveyB));
        Assert.assertTrue(surveyB.isConnectedTo(surveyA));

        // Verify station lookup works with alphanumeric name
        Assert.assertNotNull("Should find station A23",
            surveyB.getStationByName("A23"));

        // Verify JSON serialization for survey A contains alphanumeric station name
        String jsonA = MetadataTranslater.translate(surveyA);
        Assert.assertTrue("Survey A should reference alphanumeric station 'A23' in survey B",
            jsonA.contains("\"A23\""));

        // Verify JSON serialization for survey B contains the join point from survey A
        String jsonB = MetadataTranslater.translate(surveyB);
        Assert.assertTrue("Survey B should reference station '2' in survey A",
            jsonB.contains("\"2\""));
        Assert.assertTrue("Survey B connection should be from station 'A23'",
            jsonB.contains("\"A23\""));
    }


    @Test
    public void testBidirectionalConnectionWithTextStationName() throws Exception {
        Survey surveyA = BasicTestSurveyCreator.createStraightNorth();
        SurveyMocker.mockSurveyUri(surveyA, "surveyA");
        Station joinPointA = surveyA.getStationByName("2");

        Survey surveyB = new Survey();
        SurveyMocker.mockSurveyUri(surveyB, "surveyB");
        surveyB.getOrigin().setName("Entrance");

        connectTwoSurveys(surveyA, joinPointA, surveyB, surveyB.getOrigin());

        // Verify in-memory connection
        Assert.assertTrue(surveyA.isConnectedTo(surveyB));
        Assert.assertTrue(surveyB.isConnectedTo(surveyA));

        // Verify station lookup works with text name
        Assert.assertNotNull("Should find station 'Entrance'",
            surveyB.getStationByName("Entrance"));

        // Verify JSON contains the text station name
        String jsonA = MetadataTranslater.translate(surveyA);
        Assert.assertTrue("Survey A should reference station 'Entrance'",
            jsonA.contains("\"Entrance\""));
    }

}
