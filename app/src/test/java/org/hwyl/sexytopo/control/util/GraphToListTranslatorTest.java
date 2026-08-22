package org.hwyl.sexytopo.control.util;

import java.util.List;
import java.util.Map;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.model.table.TableCol;
import org.hwyl.sexytopo.testutils.BasicTestSurveyCreator;
import org.junit.Assert;
import org.junit.Test;

public class GraphToListTranslatorTest {

    private static final float DELTA = 0.0001f;

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

    @Test
    public void testCreateMapReturnsAsTakenReadingForForwardLeg() {
        Survey survey = BasicTestSurveyCreator.createEmptySurvey();
        SurveyUpdater.updateWithNewStation(survey, new Leg(5, 45, 30));
        Station origin = survey.getOrigin();
        Leg storedLeg = origin.getOnwardLegs().get(0);

        GraphToListTranslator.SurveyListEntry entry =
                new GraphToListTranslator.SurveyListEntry(origin, storedLeg);
        Map<TableCol, Object> map = GraphToListTranslator.createMap(entry);

        Assert.assertEquals(origin, map.get(TableCol.FROM));
        Assert.assertEquals(storedLeg.getDestination(), map.get(TableCol.TO));
        Assert.assertEquals(45.0f, (float) map.get(TableCol.AZIMUTH), DELTA);
        Assert.assertEquals(30.0f, (float) map.get(TableCol.INCLINATION), DELTA);
    }

    @Test
    public void testCreateMapSwapsStationsAndUnreversesReadingForBackwardsLeg() {
        // Leg is stored internally reversed (as it would be after a backward-mode shot):
        // azimuth/inclination are the far-station reading rotated to the graph's forward
        // direction. createMap should present it exactly as it was physically taken.
        Survey survey = BasicTestSurveyCreator.createEmptySurvey();
        SurveyUpdater.updateWithNewStation(survey, new Leg(5, 225, -10, true));
        Station origin = survey.getOrigin();
        Leg storedLeg = origin.getOnwardLegs().get(0);
        Station destination = storedLeg.getDestination();

        GraphToListTranslator.SurveyListEntry entry =
                new GraphToListTranslator.SurveyListEntry(origin, storedLeg);
        Map<TableCol, Object> map = GraphToListTranslator.createMap(entry);

        Assert.assertEquals(destination, map.get(TableCol.FROM));
        Assert.assertEquals(origin, map.get(TableCol.TO));
        Assert.assertEquals(45.0f, (float) map.get(TableCol.AZIMUTH), DELTA);
        Assert.assertEquals(10.0f, (float) map.get(TableCol.INCLINATION), DELTA);
    }

    @Test
    public void testToAsTakenReadingMatchesCreateMapForBackwardsLeg() {
        Survey survey = BasicTestSurveyCreator.createEmptySurvey();
        SurveyUpdater.updateWithNewStation(survey, new Leg(5, 225, -10, true));
        Station origin = survey.getOrigin();
        Leg storedLeg = origin.getOnwardLegs().get(0);
        Station destination = storedLeg.getDestination();

        GraphToListTranslator.SurveyListEntry entry =
                new GraphToListTranslator.SurveyListEntry(origin, storedLeg);
        GraphToListTranslator.AsTakenReading reading =
                GraphToListTranslator.toAsTakenReading(entry);

        Assert.assertEquals(destination, reading.getFrom());
        Assert.assertEquals(origin, reading.getTo());
        Assert.assertEquals(45.0f, reading.getLeg().getAzimuth(), DELTA);
        Assert.assertEquals(10.0f, reading.getLeg().getInclination(), DELTA);
    }
}
