package org.hwyl.sexytopo.control.io.basic;

import org.hwyl.sexytopo.model.survey.Survey;
import org.junit.Test;


public class LoaderTest {

    @Test
    public void testEmptySurveyResultsIn1Station() throws Exception {
        String text = "";
        Survey survey = new Survey("TestSurvey");
        Loader.parse(text, survey);
        assert survey.getAllStations().size() == 1;
    }

    @Test
    public void testSimpleSurveyIsParsed() throws Exception {
        String text = "1\t2\t9.11\t121\t-23\n";
        Survey survey = new Survey("TestSurvey");
        Loader.parse(text, survey);
        assert survey.getAllStations().size() == 2;
        assert survey.getOrigin().getConnectedOnwardLegs().get(0)
                .getDestination().getName().equals("2");

    }

    @Test
    public void testStarredLinesAreIgnored() throws Exception {
        String text =
                "*data normal from to length compass clino\n" +
                "1\t2\t9.11\t121\t-23\n";
        Survey survey = new Survey("TestSurvey");
        Loader.parse(text, survey);
        assert survey.getAllStations().size() == 2;
    }

    @Test
    public void testReversedLegIsParsed() throws Exception {
        String text = "2\t1\t9.11\t121\t-23\n";
        Survey survey = new Survey("TestSurvey");
        Loader.parse(text, survey);
        assert survey.getAllStations().size() == 2;
        assert survey.getOrigin().getConnectedOnwardLegs().get(0)
                .getDestination().getName().equals("2");
        assert survey.getOrigin().getConnectedOnwardLegs().get(0)
                .wasShotBackwards();
    }
}
