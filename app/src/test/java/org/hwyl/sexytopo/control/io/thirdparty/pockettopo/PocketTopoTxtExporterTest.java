package org.hwyl.sexytopo.control.io.thirdparty.pockettopo;

import org.hwyl.sexytopo.control.util.SurveyUpdater;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Survey;
import org.junit.Assert;
import org.junit.Test;

public class PocketTopoTxtExporterTest {

    private static Survey createSurvey(boolean withSplay, boolean hidden) {
        Survey survey = new Survey();
        SurveyUpdater.updateWithNewStation(survey, new Leg(5.0f, 0.0f, 0.0f));
        survey.setActiveStation(survey.getOrigin());
        if (withSplay) {
            Leg splay = new Leg(2.5f, 90.0f, 10.0f);
            SurveyUpdater.update(survey, splay);
            SurveyUpdater.setSplayHidden(survey, splay, hidden);
        }
        return survey;
    }

    @Test
    public void testHiddenSplayIsExportedAsIfItWereNotHidden() {
        String shown = PocketTopoTxtExporter.exportData(createSurvey(true, false));
        String hidden = PocketTopoTxtExporter.exportData(createSurvey(true, true));

        Assert.assertEquals(shown, hidden);
        Assert.assertNotEquals(
                PocketTopoTxtExporter.exportData(createSurvey(false, false)), hidden);
    }

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
