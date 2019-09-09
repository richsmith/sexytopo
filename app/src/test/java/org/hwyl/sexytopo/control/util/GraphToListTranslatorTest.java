package org.hwyl.sexytopo.control.util;

import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.testhelpers.BasicTestSurveyCreator;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;


public class GraphToListTranslatorTest {

    @Test
    public void testChronoListGeneration() {
        GraphToListTranslator translator = new GraphToListTranslator();
        Survey baseSurvey = BasicTestSurveyCreator.createStraightNorth();
        baseSurvey.setActiveStation(baseSurvey.getStationByName("1"));
        SurveyUpdater.updateWithNewStation(baseSurvey, new Leg(5, 270, 0));
        List<GraphToListTranslator.SurveyListEntry> chronoList =
                translator.toChronoListOfSurveyListEntries(baseSurvey);
        GraphToListTranslator.SurveyListEntry last = chronoList.get(chronoList.size() - 1);
        Assert.assertEquals("5", last.getLeg().getDestination().getName());
    }

}
