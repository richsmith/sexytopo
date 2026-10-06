package org.hwyl.sexytopo.control.io.thirdparty.pockettopo;

import org.hwyl.sexytopo.control.util.SurveyUpdater;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Survey;
import org.junit.Assert;
import org.junit.Test;

public class PocketTopoTxtExporterTest {

    private static Survey createSurveyWithSplay(boolean toWall) {
        Survey survey = new Survey();
        SurveyUpdater.updateWithNewStation(survey, new Leg(5.0f, 0.0f, 0.0f));
        survey.setActiveStation(survey.getOrigin());
        Leg splay = new Leg(2.5f, 90.0f, 10.0f);
        SurveyUpdater.update(survey, splay);
        SurveyUpdater.setSplayToWall(survey, splay, toWall);
        return survey;
    }

    @Test
    public void testSplayToAnotherFeatureIsExportedLikeASplayToAWall() {
        Assert.assertEquals(
                PocketTopoTxtExporter.exportData(createSurveyWithSplay(true)),
                PocketTopoTxtExporter.exportData(createSurveyWithSplay(false)));
    }
}
